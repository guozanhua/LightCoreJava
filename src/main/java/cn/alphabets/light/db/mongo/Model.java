package cn.alphabets.light.db.mongo;

import cn.alphabets.light.Constant;
import cn.alphabets.light.Environment;
import cn.alphabets.light.Helper;
import cn.alphabets.light.entity.ModFile;
import cn.alphabets.light.model.ModCommon;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Projections;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.atteo.evo.inflector.English;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Indexes.descending;

/**
 * Model
 */
public class Model {

    private static final Logger logger = LogManager.getLogger(Model.class);

    private MongoDatabase db;
    private MongoCollection<Document> collection;
    private String name;

    private Model() {
    }

    public Model(String domain, String code) {
        this(domain, code, null);
    }

    public Model(String domain, String code, String table) {

        MongoClient client = Connection.instance(Environment.instance());

        this.db = client.getDatabase(domain);
        this.name = table;

        if (table != null) {
            table = English.plural(table);
            if (!Constant.SYSTEM_DB.equals(domain)) {
                table = code + '.' + table;
            }

            this.collection = this.db.getCollection(table);
        }

        logger.info("table : " + table);
    }

    public <T extends ModCommon> List<T> list() {
        return this.list(null);
    }

    public <T extends ModCommon> List<T> list(Document condition) {
        return this.list(condition, null);
    }

    public <T extends ModCommon> List<T> list(Document condition, List<String> fieldNames) {
        return this.list(condition, fieldNames, null, 0, Constant.DEFAULT_LIMIT);
    }

    @SuppressWarnings("unchecked")
    public <T extends ModCommon> List<T> list(
            Document condition,
            List<String> fieldNames,
            List<String> sortField,
            int skipCount,
            int limitCount) {

        // default value
        condition = condition == null ? new Document() : condition;
        fieldNames = fieldNames == null ? Collections.emptyList() : fieldNames;
        sortField = sortField == null ? Collections.emptyList() : sortField;

        // set fetch condition
        FindIterable<Document> find = this.collection.find(condition);
        FindIterable<Document> skip = find.skip(skipCount);
        FindIterable<Document> limit = skip.limit(limitCount);
        FindIterable<Document> sort = limit.sort(descending(sortField));
        FindIterable<Document> projection = sort.projection(Projections.include(fieldNames));

        // fetch and convert
        List<T> result = new ArrayList<>();
        projection.forEach((Block<? super Document>) document -> {
            result.add((T) ModCommon.fromDocument(document, this.getModelType()));
        });
        return result;
    }

    public List<Document> document(Document condition, List<String> fieldNames) {

        FindIterable<Document> find = this.collection.find(condition);
        FindIterable<Document> projection = find.projection(Projections.include(fieldNames));

        List<Document> result = new ArrayList<>();
        projection.forEach((Block<? super Document>) result::add);
        return result;
    }

    public <T extends ModCommon> T get(Document condition) {
        return this.get(condition, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends ModCommon> T get(Document condition, List<String> fieldNames) {

        // default value
        condition = condition == null ? new Document() : condition;
        fieldNames = fieldNames == null ? Collections.emptyList() : fieldNames;

        // set fetch condition
        FindIterable<Document> find = this.collection.find(condition);
        FindIterable<Document> projection = find.projection(Projections.include(fieldNames));

        Document document = find.first();
        return (T) ModCommon.fromDocument(document, this.getModelType());
    }

    public Long remove(Document condition) {
        return this.update(condition, new Document("valid", Constant.INVALID));
    }

    public Long delete(Document condition) {
        return this.collection.deleteMany(condition).getDeletedCount();
    }

    public Long update(Document condition, Document data) {
        return this.collection.updateMany(condition, data).getModifiedCount();
    }

    public Long count(Document condition) {
        return this.collection.count(condition);
    }

    @SuppressWarnings("unchecked")
    public <T extends ModCommon> List<T> add(List<Document> document) {

        this.collection.insertMany(document);

        List<T> result = new ArrayList<>();
        document.forEach((x) ->
                result.add((T) ModCommon.fromDocument(x, this.getModelType()))
        );
        return result;
    }

    /**
     * Write the file to GridFS
     *
     * @param path full path
     * @return file meta info
     */
    public ModFile writeFileToGrid(String path) {

        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        FileInputStream stream;
        String contentType;
        try {
            stream = new FileInputStream(file);
            contentType = Helper.getContentType(stream);    // get content type
            stream.getChannel().position(0);                // Resets the file stream position
        } catch (IOException e) {
            logger.error(e);
            return null;
        }

        return this.writeStreamToGrid(file.getName(), stream, contentType);
    }


    public ModFile writeFileToGrid(Document meta) {

        String name = meta.getString(Constant.PARAM_FILE_NAME);
        String type = meta.getString(Constant.PARAM_FILE_TYPE);

        try {
            FileInputStream stream = new FileInputStream(new File(meta.getString(Constant.PARAM_FILE_PHYSICAL)));
            return this.writeStreamToGrid(name, stream, type);
        } catch (FileNotFoundException e) {
            logger.error(e);
            return null;
        }
    }

    /**
     * Writes a stream to GridFS
     *
     * @param name        file name
     * @param stream      stream
     * @param contentType file content-type
     * @return file meta info
     */
    public ModFile writeStreamToGrid(String name, InputStream stream, String contentType) {

        // create a gridFSBucket using the default bucket name "fs"
        GridFSBucket gridFSBucket = GridFSBuckets.create(this.db);

        // create some custom options
        Document meta = new Document("contentType", contentType);
        GridFSUploadOptions options = new GridFSUploadOptions().metadata(meta);

        // upload stream
        ObjectId fileId = gridFSBucket.uploadFromStream(name, stream, options);
        GridFSFile fs = gridFSBucket.find(new Document("_id", fileId)).first();

        ModFile file = new ModFile();
        file.setName(fs.getFilename());
        file.setLength(fs.getLength());
        file.setContentType(fs.getMetadata().getString("contentType"));
        file.setFileId(fileId);

        return file;
    }

    /**
     * Reads the stream from GridFS
     *
     * @param fileId       file id
     * @param outputStream stream
     * @return file meta info
     */
    public ModFile readStreamFromGrid(String fileId, OutputStream outputStream) {
        return this.readStreamFromGrid(new ObjectId(fileId), outputStream);
    }

    public ModFile readStreamFromGrid(ObjectId fileId, OutputStream outputStream) {

        GridFSBucket gridFSBucket = GridFSBuckets.create(this.db);

        gridFSBucket.downloadToStream(fileId, outputStream);
        GridFSFile fs = gridFSBucket.find(new Document("_id", fileId)).first();

        ModFile file = new ModFile();
        file.setName(fs.getFilename());
        file.setLength(fs.getLength());
        file.setContentType(fs.getMetadata().getString("contentType"));
        file.setFileId(fileId);

        return file;
    }

    public void deleteFromGrid(ObjectId fileId) {
        GridFSBuckets.create(this.db).delete(fileId);
    }

    private Class getModelType() {

        String className = Constant.MODEL_PREFIX + WordUtils.capitalize(this.name);
        String packageName = reserved.contains(this.name)
                ? Constant.DEFAULT_PACKAGE_NAME + ".entity"
                : Environment.instance().getPackages() + ".entity";

        try {
            return Class.forName(packageName + "." + className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(Constant.DEFAULT_PACKAGE_NAME + ".entity." + className);
            } catch (ClassNotFoundException e1) {
                throw new RuntimeException();
            }
        }
    }

    private List<String> reserved = Arrays.asList(
            Constant.SYSTEM_DB_BOARD,
            Constant.SYSTEM_DB_CONFIG,
            Constant.SYSTEM_DB_VALIDATOR,
            Constant.SYSTEM_DB_I18N,
            Constant.SYSTEM_DB_STRUCTURE,
            Constant.SYSTEM_DB_BOARD,
            Constant.SYSTEM_DB_ROUTE,
            Constant.SYSTEM_DB_TENANT
    );
}
