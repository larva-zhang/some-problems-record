package com.zxl.problems;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LambdaCollectorsToMapNullKvTest
 *
 * @author zhanghan
 * @date 2018/12/10
 * @since 1.0
 */
@RunWith(JUnit4.class)
public class LambdaCollectorsToMapNullKvTest {

    @Test
    public void testPutNullKey() {
        Map<Object, Object> map = new HashMap<>();
        try {
            map.put(null, new Object());
            throw new RuntimeException();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
        }
    }

    @Test
    public void testPutNullValue() {
        Map<Object, Object> map = new HashMap<>();
        try {
            map.put(new Object(), null);
            throw new RuntimeException();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
        }
    }

    @Test
    public void testLambdaCollectorsToMapNullKey() {
        List<Object> list = new ArrayList<>();
        list.add(new Object());
        try {
            Map<Object, Object> map = list.stream().collect(Collectors.toMap(key -> null, ele -> ele));
            throw new RuntimeException();
        } catch (Exception e) {
            Assert.assertEquals(RuntimeException.class, e.getClass());
        }
    }

    @Test
    public void testLambdaCollectorsToMapNullValue() {
        List<Object> list = new ArrayList<>();
        list.add(null);
        try {
            Map<Object, Object> map = list.stream().collect(Collectors.toMap(key -> new Object(), ele -> ele));
            throw new RuntimeException();
        } catch (Exception e) {
            Assert.assertEquals(NullPointerException.class, e.getClass());
        }
    }
}
