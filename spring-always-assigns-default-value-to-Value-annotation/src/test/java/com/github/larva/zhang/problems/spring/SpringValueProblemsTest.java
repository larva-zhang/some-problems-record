package com.github.larva.zhang.problems.spring;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * SpringValueProblemsTest
 *
 * @see <a href="https://jira.spring.io/browse/SPR-9989">SPR-9989</a>
 * @author zhanghan
 * @date 2018/11/23
 * @since 1.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration({"classpath:spring-value-problems-test.xml"})
public class SpringValueProblemsTest {

    @Value("${prop1}")
    private String prop1Value;

    @Value("${prop2}")
    private String prop2Value;

    @Value("${prop1:value1default}")
    private String prop1ValueWithDefault;

    @Value("${prop2:value2default}")
    private String prop2ValueWithDefault;

    @Test
    public void test() {
        Assert.assertEquals(prop1Value, prop1ValueWithDefault);
        Assert.assertEquals(prop2Value, prop2ValueWithDefault); // Fails here, default value has been used instead of
    }
}
