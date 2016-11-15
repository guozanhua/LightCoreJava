package cn.alphabets.light.model;

import cn.alphabets.light.Constant;
import cn.alphabets.light.Environment;
import cn.alphabets.light.cache.CacheManager;
import cn.alphabets.light.db.mongo.Controller;
import cn.alphabets.light.entity.ModUser;
import cn.alphabets.light.http.Context;
import cn.alphabets.light.mock.MockRoutingContext;
import org.bson.Document;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * DataRiderTest
 * Created by lilin on 2016/11/13.
 */
public class DataRiderTest {

    private Context handler;

    @Before
    public void setUp() {
        Environment.instance().args.local = true;
        CacheManager.INSTANCE.setUp(Constant.SYSTEM_DB);
        handler = new Context(new MockRoutingContext(), Constant.SYSTEM_DB, Constant.SYSTEM_DB_PREFIX);
    }

    @Test
    public void testList() {

        handler.params.setCondition(new Document("keyword", "admin"));
        Plural<ModUser> result = new DataRider(User.class).list(handler);
        List<ModUser> user = result.getItems();

        Assert.assertEquals("admin", user.get(0).getName());
    }

    @Test
    public void testGet() {

        handler.params.setId("000000000000000000000001");
        ModUser user = new DataRider(User.class).get(handler);

        Assert.assertEquals("admin", user.getName());
    }

    @Test
    public void testAdd() {

        DataRider rider = new DataRider(User.class);

        ModUser user = new ModUser();
        user.setName("test user name");

        // add user
        handler.params.setData(user);
        String result = rider.add(handler);
        Assert.assertNotNull(result);

        // delete test user
        handler.params.setId(result);
        Long count = new Controller(handler, User.class).delete();
        Assert.assertTrue(1L == count);
    }

    @Test
    public void testCall() {

        handler.params.setCondition(new Document("id", "admin"));
        ModUser user = (ModUser)new DataRider(User.class).call(handler, "get");

        Assert.assertEquals("admin", user.getName());
    }
}
