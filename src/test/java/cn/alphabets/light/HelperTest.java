package cn.alphabets.light;

import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HelperTest
 */
public class HelperTest {

    @Test
    public void testGetMimeType() throws FileNotFoundException {

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream inputStream;

        inputStream = classLoader.getResourceAsStream("tmp/logo.png");
        Assert.assertEquals("image/png", Helper.getContentType(inputStream));

        inputStream = classLoader.getResourceAsStream("view/accounts.html");
        Assert.assertEquals("text/html", Helper.getContentType(inputStream));

        inputStream = classLoader.getResourceAsStream("config.yml");
        Assert.assertEquals("text/plain", Helper.getContentType(inputStream));

        inputStream = classLoader.getResourceAsStream("log4j2.xml");
        Assert.assertEquals("application/xml", Helper.getContentType(inputStream));
    }

    @Test
    public void testSetEnv() {
        Map<String, String> env = new ConcurrentHashMap<String, String>() {{
            put("key1", "val1");
            put("key2", "val2");
        }};
        Helper.setEnv(env);

        Assert.assertEquals(System.getenv("key1"), "val1");
        Assert.assertEquals(System.getenv("key2"), "val2");
    }

    @Test
    public void testUnParam() {

        String slimjson = "[ \"]*";
        Document source = Helper.unParam("s=0&c[m][]=a&c[m][]=b&c[p][p1][p11]=d&c[p][p1][p12][]=1&c[p][p1][p12][]=2");
        String json = source.toJson().replaceAll(slimjson, "");

        Assert.assertEquals(json, "{s:0,c:{m:[a,b],p:{p1:{p11:d,p12:[1,2]}}}}");
    }

    @Test
    public void testLookup() {

        String slimjson = "[ \"]*";

        Document source = new Document();
        Helper.setValueByJsonPath(source, Arrays.asList("key", ""), 1);
        Helper.setValueByJsonPath(source, Arrays.asList("key", ""), 2);
        Assert.assertEquals(source.toJson().replaceAll(slimjson, ""), "{key:[1,2]}");

        source = new Document();
        Helper.setValueByJsonPath(source, Arrays.asList("key", "son"), 1);
        Assert.assertEquals(source.toJson().replaceAll(slimjson, ""), "{key:{son:1}}");

        source = new Document();
        Helper.setValueByJsonPath(source, Arrays.asList("key", "0", "son"), "value1");
        Helper.setValueByJsonPath(source, Arrays.asList("key", "1", "son"), "value2");
        Assert.assertEquals(source.toJson().replaceAll(slimjson, ""), "{key:[{son:value1},{son:value2}]}");
    }

    @Test
    public void testLoadTemplate() throws IOException {

        Helper.StringFunction dynamic = new Helper.StringFunction("dynamic", (x) -> x.get(0) + " : dynamic");
        Helper.StringFunction i = new Helper.StringFunction("i", (x) -> x.get(0) + " : i");

        Map<String, Object> map = new ConcurrentHashMap<String, Object>() {{
            put("conf", Environment.instance());
            put("state", Boolean.TRUE);
        }};

        String result = Helper.loadTemplate("view/accounts.html", map, Arrays.asList(dynamic, i));
        Assert.assertTrue(result.contains("Host : 127.0.0.1"));
        Assert.assertTrue(result.contains("Hello : dynamic"));
        Assert.assertTrue(result.contains("Sub"));
    }

}
