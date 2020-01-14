package com.github.larva.zhang.problems;

import javax.annotation.Resource;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * BaseTest
 *
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {ApplicationTest.class})
@Rollback
@Transactional
@EnableTransactionManagement
public class BaseTest {

    @Resource
    protected UserDao userDao;

}
