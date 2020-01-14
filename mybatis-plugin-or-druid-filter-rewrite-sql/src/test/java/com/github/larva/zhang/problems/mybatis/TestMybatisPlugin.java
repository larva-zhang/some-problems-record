package com.github.larva.zhang.problems.mybatis;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.github.larva.zhang.problems.BaseTest;

/**
 * TestMybatisPlugin
 *
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
@ContextConfiguration(classes = TestMybatisPluginRewriteSqlConfig.class)
@ActiveProfiles("test-mybatis-plugin")
public class TestMybatisPlugin extends BaseTest {

    @Test
    public void testMybatisPluginRewriteWithoutUpdateTimeSql() {
        Date today = new Date();
        Date yesterday = DateUtils.addDays(new Date(), -1);
        // insert duplicate key期望被Mybatis Plugin改写
        userDao.insertWithUpdateTime("test1", "test1", 1, true, yesterday, yesterday);
        userDao.insertWithoutUpdateTime("test1", "test1", 1, true, yesterday);
        Date date = userDao.selectUpdateTimeByAccount("test1");
        Assert.assertFalse("insert duplicate key without update time, expect mybatis plugin rewrite sql but not",
                DateUtils.isSameDay(yesterday, date));
        Assert.assertTrue("insert duplicate key without update time, expect mybatis plugin rewrite sql but not",
                DateUtils.isSameDay(today, date));

    }

    @Test
    public void testMybatisPluginNotRewriteWithUpdateTimeSql() {
        Date today = new Date();
        Date yesterday = DateUtils.addDays(new Date(), -1);
        // insert duplicate key期望不被Mybatis Plugin改写
        userDao.insertWithUpdateTime("test1", "test1", 1, true, yesterday, yesterday);
        userDao.insertWithUpdateTime("test1", "test1", 1, true, yesterday, yesterday);
        Date date = userDao.selectUpdateTimeByAccount("test1");
        Assert.assertTrue("insert duplicate key with update time, expect mybatis plugin not rewrite sql but did",
                DateUtils.isSameDay(yesterday, date));
        Assert.assertFalse("insert duplicate key with update time, expect mybatis plugin not rewrite sql but did",
                DateUtils.isSameDay(today, date));
    }
}
