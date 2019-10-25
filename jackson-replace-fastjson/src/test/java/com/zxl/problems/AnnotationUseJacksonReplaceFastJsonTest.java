package com.zxl.problems;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

/**
 * AnnotationUseJacksonReplaceFastJsonTest
 *
 * @author zhanghan
 * @date 2019/10/22
 * @since 1.0
 */
@RunWith(JUnit4.class)
@SuppressWarnings("WeakerAccess")
public class AnnotationUseJacksonReplaceFastJsonTest {

    @Test
    public void testJSONFieldUnwrapped() {
        Person person = new Person();
        person.name = "abc";
        person.age = 123;
        UnwrappedIsFalseBean unwrappedIsFalseBean = new UnwrappedIsFalseBean();
        unwrappedIsFalseBean.person = person;
        Assert.assertEquals("{\"person\":{\"name\":\"abc\",\"age\":123}}", JSON.toJSONString(unwrappedIsFalseBean));
        UnwrappedIsTrueBean unwrappedIsTrueBean = new UnwrappedIsTrueBean();
        unwrappedIsTrueBean.person = person;
        Assert.assertEquals("{\"name\":\"abc\",\"age\":123}", JSON.toJSONString(unwrappedIsTrueBean));
    }

    @JsonDeserialize
    public static class UnwrappedIsFalseBean {
        @JSONField(unwrapped = false)
        public Person person;
    }

    public static class UnwrappedIsTrueBean {
        @JSONField(unwrapped = true)
        public Person person;
    }

    public static class Person {
        @JSONField(ordinal = 0)
        public String name;
        @JSONField(ordinal = 1)
        public Integer age;
    }
}
