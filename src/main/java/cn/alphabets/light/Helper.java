package cn.alphabets.light;

import io.vertx.core.http.HttpServerRequest;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.bson.Document;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.Environment;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.environment.EnvironmentFactory;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;
import org.jtwig.resource.reference.ResourceReference;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper
 */
public class Helper {

    public static final SimpleDateFormat ISO_FORMATTER;

    static {
        ISO_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ISO_FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String toUTCString(Date date) {
        return ISO_FORMATTER.format(date);
    }

    public static Date fromUTCString(String date) {
        try {
            return ISO_FORMATTER.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Speculate Content-Type from a file
     *
     * @param stream file stream
     * @return content type
     */
    public static String getContentType(InputStream stream) {

        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();
        try {
            parser.parse(stream, new BodyContentHandler(), metadata, new ParseContext());
        } catch (IOException | SAXException | TikaException e) {
            return null;
        }

        return metadata.get(Metadata.CONTENT_TYPE);
    }

    /**
     * Set the system environment variables
     *
     * @param newEnv environment
     */
    @SuppressWarnings("unchecked")
    public static void setEnv(Map<String, String> newEnv) {
        Class[] classes = Collections.class.getDeclaredClasses();
        Map<String, String> env = System.getenv();
        for (Class cl : classes) {
            if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                try {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Map<String, String> map = (Map<String, String>) field.get(env);
                    map.clear();
                    map.putAll(newEnv);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void setEnv(String key, String value) {
        Map<String, String> env = new ConcurrentHashMap<String, String>() {{
            put(key, value);
        }};

        Helper.setEnv(env);
    }

    /**
     * Use Json Path to set the value of the Json object. Supports embedded objects
     *
     * @param source target json object
     * @param path   json path
     * @param val    value
     */
    @SuppressWarnings("unchecked")
    public static void setValueByJsonPath(Document source, List<String> path, Object val) {

        Object parent = source;

        for (int i = 0; i < path.size(); i++) {

            String key = path.get(i);
            boolean isLast = (i == path.size() - 1);

            if (isLast) {
                boolean isList = key.equals("") || NumberUtils.isDigits(key);
                if (isList) {
                    ((List<Object>) parent).add(val);
                } else {
                    ((Document) parent).put(key, val);
                }
                return;
            }

            boolean isListValue = path.get(i + 1).equals("") || NumberUtils.isDigits(path.get(i + 1));
            boolean isObjectValue = !isListValue;

            if (NumberUtils.isDigits(key)) {

                if (((List<?>) parent).size() > Integer.parseInt(key)) {
                    parent = ((List<?>) parent).get(Integer.parseInt(key));
                } else {
                    if (isListValue) {
                        ((List<Object>) parent).add(new ArrayList<>());
                    }
                    if (isObjectValue) {
                        ((List<Document>) parent).add(new Document());
                    }
                    parent = ((List<?>) parent).get(Integer.parseInt(key));
                }
            } else {

                if (((Document) parent).containsKey(key)) {
                    parent = ((Document) parent).get(key);
                } else {
                    if (isListValue) {
                        ((Document) parent).put(key, new ArrayList<>());
                    }
                    if (isObjectValue) {
                        ((Document) parent).put(key, new Document());
                    }
                    parent = ((Document) parent).get(key);
                }
            }
        }
    }

    /**
     * Determines whether the current runtime environment is JUnit
     *
     * @return true is JUnit
     */
    public static boolean isJUnitTest() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<StackTraceElement> list = Arrays.asList(stackTrace);
        for (StackTraceElement element : list) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether the request comes to the browser
     *
     * @param request request
     * @return true is browser
     */
    public static boolean isBrowser(HttpServerRequest request) {

        String ua = request.getHeader("user-agent");
        return ua != null && ua.toLowerCase().matches("mozilla.*");
    }

    /**
     * Deserializes the JSON generated by JQuery.param() method
     *
     * @param url full request url
     * @return Json Object
     */
    public static Document unParam(String url) {

        String decoded;
        Pattern pattern = Pattern.compile("\\[([^\\]]*)\\]");

        try {
            url = url.contains("?") ? url.substring(url.indexOf("?") + 1) : url;
            decoded = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        final Document json = new Document();

        Arrays.stream(decoded.split("&")).forEach(keyVal -> {

            String[] splitted = keyVal.split("=");
            String key = splitted[0];
            String val = splitted.length > 1 ? splitted[1] : "";

            List<String> path = new ArrayList<>();
            path.add(key.indexOf("[") > 0 ? key.substring(0, key.indexOf("[")) : key);

            Matcher m = pattern.matcher(key);
            while (m.find()) {
                path.add(m.group(1));
            }

            Helper.setValueByJsonPath(json, path, val);
        });

        return json;
    }

    /**
     * Use the template file to generate a string
     *
     * @param name     resource name
     * @param model    parameters
     * @param function custom functions
     * @return The resulting string
     */
    public static String loadTemplate(String name, Map<String, Object> model, List<SimpleJtwigFunction> function) {

        EnvironmentConfiguration configuration = EnvironmentConfigurationBuilder
                .configuration()
                .functions()
                .add(function)
                .and()
                .parser()
                .syntax()
                .withStartCode("<%~").withEndCode("%>")
                .withStartOutput("<%=").withEndOutput("%>")
                .withStartComment("<#").withEndComment("#>")
                .and()
                .and()
                .build();

        Environment environment = new EnvironmentFactory().create(configuration);
        ResourceReference resource = new ResourceReference(ResourceReference.CLASSPATH, name);

        return new JtwigTemplate(environment, resource).render(JtwigModel.newModel(model));
    }

    public static class StringFunction extends SimpleJtwigFunction {

        private String name;
        Function<List<Object>, String> function;

        public StringFunction(String name, Function<List<Object>, String> function) {
            this.name = name;
            this.function = function;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public Object execute(FunctionRequest request) {
            return this.function.apply(request.getArguments());
        }
    }

    public static class ListFunction extends SimpleJtwigFunction {

        private String name;
        Function<List<Object>, List<String>> function;

        public ListFunction(String name, Function<List<Object>, List<String>> function) {
            this.name = name;
            this.function = function;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public Object execute(FunctionRequest request) {
            return this.function.apply(request.getArguments());
        }
    }

    public static class MapFunction extends SimpleJtwigFunction {

        private String name;
        Function<List<Object>, Map<String, String>> function;

        public MapFunction(String name, Function<List<Object>, Map<String, String>> function) {
            this.name = name;
            this.function = function;
        }

        @Override
        public String name() {
            return this.name;
        }

        @Override
        public Object execute(FunctionRequest request) {
            return this.function.apply(request.getArguments());
        }
    }
}
