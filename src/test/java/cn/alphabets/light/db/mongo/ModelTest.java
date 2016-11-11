package cn.alphabets.light.db.mongo;

import cn.alphabets.light.Config;
import cn.alphabets.light.model.I18n;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * ModelTest
 */
public class ModelTest {

    @Before
    public void setUp() {
        Config.instance().args.local = true;
    }

    @Test
    public void testGetBy() {

        Model model = new Model(Config.CONSTANT.SYSTEM_DB, Config.CONSTANT.SYSTEM_DB_PREFIX, "i18n");

        List<I18n> i18n = model.list();

        Assert.assertTrue(i18n.size() > 0);
    }

    @Test
    public void testGet() {

        Model model = new Model(Config.CONSTANT.SYSTEM_DB, Config.CONSTANT.SYSTEM_DB_PREFIX, "i18n");

        I18n i18n = model.get();

        Assert.assertNotNull(i18n);
    }
}
