package cn.alphabets.light.validator;

import cn.alphabets.light.Constant;
import cn.alphabets.light.Environment;
import cn.alphabets.light.entity.ModValidator;
import cn.alphabets.light.http.Context;
import cn.alphabets.light.http.Params;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * RuleTest
 * Created by lilin on 2017/7/11.
 */
public class RuleTest {

    @Before
    public void setUp() {
        Environment.instance().args.local = true;
    }

    //@Test
    public void testIsValid() {
        Document json = new Document();
        Context handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        List<Document> errors = Rule.isValid(handler, "aaa");
        System.out.println(errors);
    }

    //@Test
    public void testRequired() {

        Rule rule = new Rule();

        ModValidator validator = new ModValidator();
        validator.setKey("data.name");
        validator.setMessage("Name is required");

        // 值为null
        Document json = new Document();
        Context handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        Document result = rule.required(handler, validator);
        Assert.assertEquals(result.getString("message"), "Name is required");

        // 值为空字符串
        json = new Document("data", new Document("name", ""));
        handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        result = rule.required(handler, validator);
        Assert.assertEquals(result.getString("message"), "Name is required");

        // 有值
        json = new Document("data", new Document("name", "alphabets"));
        handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        result = rule.required(handler, validator);
        Assert.assertNull(result);
    }

    //@Test
    public void testUnique() {
        Rule rule = new Rule();

        // 用户表里是已经存在
        List<Document> optionConditions = new ArrayList<>();
        Document conditions = new Document();
        conditions.put("parameter", "name");
        conditions.put("value", "$data.name");
        optionConditions.add(conditions);

        Document option = new Document();
        option.put("schema", "user");
        option.put("conditions", optionConditions);

        ModValidator validator = new ModValidator();
        validator.setKey("data.name");
        validator.setMessage("The name already exists");
        validator.setOption(option);
        validator.setStrict(true);

        Document json = new Document("data", new Document("name", "admin"));
        Context handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        Document result = rule.unique(handler, validator);
        Assert.assertEquals(result.getString("message"), "The name already exists");

        // 用户表里不存在
        json = new Document("data", new Document("name", "empty"));
        handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        result = rule.unique(handler, validator);
        Assert.assertNull(result);
    }

    //@Test
    public void testNumeric() {
        Rule rule = new Rule();

        // 非数字
        ModValidator validator = new ModValidator();
        validator.setKey("data.count");
        validator.setMessage("Non-numeric");
        validator.setStrict(true);

        Document json = new Document("data", new Document("count", "a"));
        Context handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        Document result = rule.numeric(handler, validator);
        Assert.assertEquals(result.getString("message"), "Non-numeric");

        // 数字
        json = new Document("data", new Document("count", "10"));
        handler = new Context(new Params(json), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX, null);

        result = rule.numeric(handler, validator);
        Assert.assertNull(result);
    }

}
