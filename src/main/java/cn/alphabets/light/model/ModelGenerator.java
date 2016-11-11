package cn.alphabets.light.model;

import cn.alphabets.light.Config;
import cn.alphabets.light.db.mongo.Model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.*;
import org.apache.commons.lang3.text.WordUtils;
import org.bson.Document;
import org.bson.types.ObjectId;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Generator
 * Created by lilin on 2016/11/8.
 */
public class ModelGenerator {

    private String packageName;

    public ModelGenerator(String packageName) {
        this.packageName = packageName;
    }

    /**
     * Generates a POJO source
     *
     * @param domain domain
     * @param schema schema
     */
    public void generate(String domain, List<String> schema) {

        Model model = new Model(domain, Config.CONSTANT.SYSTEM_DB_PREFIX, Config.CONSTANT.SYSTEM_DB_STRUCTURE);

        Document condition = new Document("valid", 1);
        condition.put("schema", new Document("$in", schema));
        List<Document> defines = model.list_(condition, Arrays.asList("schema", "items"));

        defines.forEach((define) -> {

            final TypeSpec.Builder builder = this.mainClass(define.getString("schema"));
            Document items = (Document) define.get("items");

            items.entrySet().forEach(item -> {
                if (!lightReserved.contains(item.getKey())) {
                    this.subClass(builder, item.getKey(), (Document) item.getValue());
                }
            });

            this.write(builder.build());
        });
    }

    private void write(TypeSpec type) {
        JavaFile javaFile = JavaFile.builder(this.packageName, type).build();
        try {
            javaFile.writeTo(new File("src/main/java"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TypeSpec.Builder mainClass(String className) {
        return TypeSpec.classBuilder(WordUtils.capitalize(className))
                .addModifiers(Modifier.PUBLIC)
                .superclass(ModBase.class)
                .addJavadoc("Generated by the Light platform. Do not manually modify the code.\n");
    }

    private void subClass(TypeSpec.Builder spec, String name, Document define) {

        // Add the field
        Type type = regular(define.getString("type"));
        if (type != List.class && type != Object.class) {
            this.append(spec, name, this.type(type));
            return;
        }

        // If the subtype is not defined
        Document items = (Document) define.get("contents");
        if (items == null) {
            this.append(spec, name, this.type(type));
            return;
        }

        this.append(spec, name, this.type(define.getString("type"), WordUtils.capitalize(name)));

        AnnotationSpec annotation = AnnotationSpec.builder(JsonIgnoreProperties.class)
                .addMember("ignoreUnknown", "$L", Boolean.TRUE)
                .build();

        TypeSpec.Builder builder = TypeSpec.classBuilder(WordUtils.capitalize(name))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                .addAnnotation(annotation);

        items.entrySet().forEach((item) -> {
            this.subClass(builder, item.getKey(), (Document) item.getValue());
        });

        spec.addType(builder.build());
    }


    private TypeName type(Type name) {
        return TypeName.get(name);
    }

    private TypeName type(String name, String template) {
        Type type = regular(name);

        if (type == List.class && template != null) {
            return ParameterizedTypeName.get(ClassName.get("java.util", "List"), ClassName.get("", template));
        }

        return TypeName.get(type);
    }

    private void append(TypeSpec.Builder builder, String name, TypeName type) {
        builder.addField(this.field(name, type));
        builder.addMethod(this.getter(name, type));
        builder.addMethod(this.setter(name, type));
    }

    private FieldSpec field(String name, TypeName type) {

        FieldSpec.Builder builder = FieldSpec.builder(type, reserved.contains(name) ? name + "_" : name)
                .addModifiers(Modifier.PRIVATE);

        if (reserved.contains(name)) {
            builder.addAnnotation(AnnotationSpec.builder(JsonProperty.class).addMember("value", "$S", name).build());
        }

        return builder.build();
    }

    private MethodSpec getter(String name, TypeName type) {

        String reservedName = reserved.contains(name) ? name + "_" : name;
        return MethodSpec.methodBuilder("get" + WordUtils.capitalize(reservedName))
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement(String.format("return this.%s", reservedName))
                .build();
    }

    private MethodSpec setter(String name, TypeName type) {

        String reservedName = reserved.contains(name) ? name + "_" : name;
        return MethodSpec.methodBuilder("set" + WordUtils.capitalize(reservedName))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(type, reservedName)
                .addStatement(String.format("this.%s = %s", reservedName, reservedName))
                .build();
    }

    static Type regular(String type) {

        type = type.toLowerCase().trim();

        if ("string".equals(type)) {
            return String.class;
        }

        if ("date".equals(type)) {
            return Date.class;
        }

        if ("int".equals(type)) {
            return Integer.class;
        }

        if ("number".equals(type)) {
            return Long.class;
        }

        if ("array".equals(type)) {
            return List.class;
        }

        if ("boolean".equals(type)) {
            return Boolean.class;
        }

        if ("objectid".equals(type)) {
            return ObjectId.class;
        }

        if ("object".equals(type)) {
            return Object.class;
        }

        return null;
    }

    private final static List<String> lightReserved = Arrays.asList(
            "_id",
            "createAt",
            "updateAt",
            "createBy",
            "updateBy",
            "valid"
    );

    private final static List<String> reserved = Arrays.asList(
            "abstract",
            "continue",
            "for",
            "new",
            "switch",
            "assert",
            "default",
            "goto",
            "package",
            "synchronized",
            "boolean",
            "do",
            "if",
            "private",
            "this",
            "break",
            "double",
            "implements",
            "protected",
            "throw",
            "byte",
            "else",
            "import",
            "public",
            "throws",
            "case",
            "enum",
            "instanceof",
            "return",
            "transient",
            "catch",
            "extends",
            "int",
            "short",
            "try",
            "char",
            "final",
            "interface",
            "static",
            "void",
            "class",
            "finally",
            "long",
            "strictfp",
            "volatile",
            "const",
            "float",
            "native",
            "super",
            "while"
    );

}
