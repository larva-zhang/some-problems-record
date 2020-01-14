package com.github.larva.zhang.problems.druid;

import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.github.larva.zhang.problems.BaseTest;

/**
 * TestDruidFilter
 *
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
@ContextConfiguration(classes = DruidFilterRewriteSqlConfig.class)
@ActiveProfiles("test-druid-filter")
public class TestDruidFilter extends BaseTest {

    @Test
    public void testDruidFilterRewriteWithoutUpdateTimeSql() {
        Date today = new Date();
        Date yesterday = DateUtils.addDays(new Date(), -1);
        // insert duplicate key期望被Druid Filter改写
        userDao.insertWithUpdateTime("test1", "test1", 1, true, yesterday, yesterday);
        userDao.insertWithoutUpdateTime("test1", "test1", 1, true, yesterday);
        Date date = userDao.selectUpdateTimeByAccount("test1");
        Assert.assertFalse("insert duplicate key without update time, expect druid filter rewrite sql but not",
                DateUtils.isSameDay(yesterday, date));
        Assert.assertTrue("insert duplicate key without update time, expect druid filter rewrite sql but not",
                DateUtils.isSameDay(today, date));
    }

    @Test
    public void testDruidFilterNotRewriteWithUpdateTimeSql() {
        Date today = new Date();
        Date yesterday = DateUtils.addDays(new Date(), -1);
        // insert duplicate key期望不被Druid Filter改写
        userDao.insertWithUpdateTime("test1", "test1", 1, true, yesterday, yesterday);
        userDao.insertWithUpdateTime("test1", "test1", 1, true, yesterday, yesterday);
        Date date = userDao.selectUpdateTimeByAccount("test1");
        Assert.assertTrue("insert duplicate key with update time, expect druid filter not rewrite sql but did",
                DateUtils.isSameDay(yesterday, date));
        Assert.assertFalse("insert duplicate key with update time, expect druid filter not rewrite sql but did",
                DateUtils.isSameDay(today, date));
    }
}
