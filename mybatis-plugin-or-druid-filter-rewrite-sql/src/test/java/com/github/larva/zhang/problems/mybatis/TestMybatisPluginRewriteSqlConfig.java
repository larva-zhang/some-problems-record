package com.github.larva.zhang.problems.mybatis;

import java.sql.SQLException;
import java.util.Collections;

import javax.sql.DataSource;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.alibaba.druid.pool.DruidDataSource;
import com.github.larva.zhang.problems.SimpleRewriteSqlMybatisPlugin;

/**
 * MybatisPluginRewriteSqlConfig 使用Mybatis Plugin机制改写SQL
 *
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
@Profile("test-mybatis-plugin")
@Configuration
@MapperScan(value = "com.github.larva.zhang.problems", sqlSessionFactoryRef = "sqlSessionFactory")
public class TestMybatisPluginRewriteSqlConfig {

    @Bean(initMethod = "init", destroyMethod = "close")
    public DruidDataSource dataSource(@Value("${spring.datasource.url}") String url,
                                      @Value("${spring.datasource.username}") String username,
                                      @Value("${spring.datasource.password}") String password) throws SQLException {
        DruidDataSource druidDataSource = new DruidDataSource();
        // 基本属性 url、user、password
        druidDataSource.setUrl(url);
        druidDataSource.setUsername(username);
        druidDataSource.setPassword(password);
        // 配置初始化大小、最小、最大
        druidDataSource.setInitialSize(2);
        druidDataSource.setMinIdle(2);
        druidDataSource.setMaxActive(200);
        // 配置获取连接等待超时的时间
        druidDataSource.setMaxWait(5000);
        // 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒
        druidDataSource.setTimeBetweenEvictionRunsMillis(20000);
        // 配置一个连接在池中最小生存的时间，单位是毫秒
        druidDataSource.setMinEvictableIdleTimeMillis(300000);
        druidDataSource.setValidationQuery("SELECT 'x'");
        druidDataSource.setTestWhileIdle(true);
        druidDataSource.setTestOnBorrow(false);
        druidDataSource.setTestOnReturn(false);
        // 打开PSCache，并且指定每个连接上PSCache的大小
        druidDataSource.setPoolPreparedStatements(true);
        druidDataSource.setMaxPoolPreparedStatementPerConnectionSize(20);
        // 设置H2 Schema
        druidDataSource.setConnectionInitSqls(Collections.singletonList("set schema PUBLIC;"));
        // 配置日志的filters
        druidDataSource.addFilters("slf4j");
        return druidDataSource;
    }

    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public org.apache.ibatis.session.Configuration mybatisConfiguration() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setCacheEnabled(false);
        configuration.setLazyLoadingEnabled(false);
        configuration.setAggressiveLazyLoading(false);
        configuration.setMultipleResultSetsEnabled(true);
        configuration.setUseColumnLabel(true);
        configuration.setUseGeneratedKeys(true);
        configuration.setAutoMappingBehavior(AutoMappingBehavior.FULL);
        configuration.setDefaultExecutorType(ExecutorType.SIMPLE);
        configuration.setDefaultStatementTimeout(25000);
        configuration.setLogImpl(Slf4jImpl.class);
        configuration.setUseActualParamName(false);
        configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
        // 使用Mybatis Plugin机制改写SQL
        configuration.addInterceptor(mybatisPlugin());
        return configuration;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource,
            org.apache.ibatis.session.Configuration mybatisConfiguration) {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setConfiguration(mybatisConfiguration);
        sqlSessionFactoryBean.setDataSource(dataSource);
        return sqlSessionFactoryBean;
    }

    @Bean
    public SimpleRewriteSqlMybatisPlugin mybatisPlugin() {
        return new SimpleRewriteSqlMybatisPlugin();
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
