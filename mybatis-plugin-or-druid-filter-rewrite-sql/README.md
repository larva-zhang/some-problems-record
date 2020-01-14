# 背景
工作中偶尔会碰到需要统一修改SQL的情况，例如有以下表结构:
```mysql
CREATE TABLE `test_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `account` varchar(70) NOT NULL COMMENT '账号',
  `user_name` varchar(60) NOT NULL COMMENT '姓名',
  `age` int(11) NOT NULL COMMENT '年龄',
  `sex` bit(1) NOT NULL COMMENT '性别：0-男，1-女',
  `create_time` timestamp NOT NULL DEFAULT '2019-01-01 00:00:00' COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account` (`account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户信息表';
```
假设有如下Mapper SQL：
```mysql
insert into `test_user`(`account`, `user_name`, `age`, `sex`, `create_time`)
values ('test1', 'test_user_1', 1, 0, now())
on duplicate key update 
`user_name` = 'test_user_1', `age` = 1, `sex` = 0;
```
在Service层代码中通过判断Mapper返回的影响行数是否等于1来识别SQL是否执行成功。但假如`duplicate key update`设置的字段值和数据库中的记录值完全一致，则`mysql`不会执行update，因此在JDBC返回的影响行数会为0，导致Service层逻辑错误。

解决方法很简单，只需在`duplicate key update`中加上`update_time = now()`即可，但如果这种语句广泛存在，那么最简单的方法就是通过SQL Rewrite来实现。

# 设计 & 选型

## 何时修改SQL
系统使用[Mybatis](https://github.com/mybatis/mybatis-3)作为ORM，[alibaba druid](https://github.com/alibaba/druid)作为数据库连接池。

Mybatis提供了plugin机制来修改SQL，例如[Mybatis-PageHelper](https://github.com/pagehelper/Mybatis-PageHelper)就是使用plugin机制修改SQL添加分页和Count语句。

Druid提供了Filter机制来修改SQL，例如[EncodingConvertFilter](https://github.com/alibaba/druid/wiki/%E4%BD%BF%E7%94%A8EncodingConvertFilter)就是使用了Filter机制在实际执行前执行了编码转换。

既然以上两者都能做到修改SQL，那么我们该选择在什么时候执行修改呢？其实这两者并没有什么显著的优劣区别，我个人来看有以下两点区别：
1. 可移植性不同。比如JDBC连接池使用的是Hikari或者DBCP，这个时候更适合在Mybatis层修改，反过来如果ORM框架选择的是Hibernate则druid更适合。
2. 工作量不同。因为ORM和JDBC的代码抽象程度不同导致了在不同层面执行改写工作量有较大差异，基于Mybatis的ORM层进行改写时工作量远小于基于Druid的JDBC层改写，因为JDBC更底层，要考虑的更多，例如执行模式是PreparedStatment还是Statement，或者是CallableStatement等，改写时需要将这些全部覆盖到，而ORM层的改写则不用考虑这么细。

## SQL Parser选型
要改写SQL，首先得先解析SQL，分析SQL的语义来判断是否需要改写以及改写哪一部分，而词法分析历来是非常耗时的，因此SQL Parser框架很重要。Java生态中较为流行的SQL Parser有以下几种：
* [fdb-sql-parser](https://mvnrepository.com/artifact/com.foundationdb/fdb-sql-parser) 是FoundationDB在被Apple收购前开源的SQL Parser，目前已无人维护。
* [jsqlparser](http://jsqlparser.sourceforge.net/) 是基于[JavaCC](https://javacc.dev.java.net/)的开源SQL Parser，是[General SQL Parser](http://www.sqlparser.com/sql-parser-java.php?ref=jsqlparser)的Java实现版本。
* [Apache calcite](https://calcite.apache.org/) 是一款开源的动态数据管理框架，它具备SQL解析、SQL校验、查询优化、SQL生成以及数据连接查询等功能，常用于为大数据工具提供SQL能力，例如Hive、Flink等。calcite对标准SQL支持良好，但是对传统的关系型数据方言支持度较差。
* [alibaba druid](https://github.com/alibaba/druid) 是阿里巴巴开源的一款JDBC数据库连接池，但其为监控而生的理念让其天然具有了SQL Parser的能力。其自带的Wall Filer、StatFiler等都是基于SQL Parser解析的AST。并且支持多种数据库方言。

其实说到SQL Rewrite，我们很容易就想到数据库中间件的分库分表，因此我们在选择SQL Parser时完全可以参考那些知名的数据库中间件。[Apache Sharding Sphere(原当当Sharding-JDBC)](https://shardingsphere.apache.org/)、[Mycat](http://www.mycat.io/)都是国内目前大量使用的开源数据库中间件，这两者都使用了alibaba druid的SQL Parser模块，并且Mycat还开源了他们在选型时的对比分析[Mycat路由新解析器选型分析与结果.docx](https://github.com/MyCATApache/Mycat-doc/blob/master/%E8%BF%9B%E9%98%B6%E6%96%87%E6%A1%A3/Mycat%E8%B7%AF%E7%94%B1%E6%96%B0%E8%A7%A3%E6%9E%90%E5%99%A8%E9%80%89%E5%9E%8B%E5%88%86%E6%9E%90%E4%B8%8E%E7%BB%93%E6%9E%9C.docx)。
> 注意：[Apache Sharding Sphere](https://shardingsphere.apache.org/)在1.5.x版本后改用自己研发的SQL Parser，理由是因为Sharding Sphere并不需要完整的SQL AST，因此改用自研的SQL Parser以降低SQL解析完整性为代价提升分库分表效率，详见[深度认识 Sharding-JDBC：做最轻量级的数据库中间层](https://my.oschina.net/editorial-story/blog/888650)。

综上所述，我们可以放心的选用alibaba druid提供的SQL Parser，唯一的问题就是如何使用druid SQL Parser。druid官方并没有详细的关于SQL Parser和Visitor的API文档说明（再次吐槽一下国内开源项目在文档和代码注释上的不完善，druid源码基本没有注释），因此我们只能从其他相关文档，以及已有的Visitor中参考，以下是druid官方的全部关于SQL Parser和Visitor的文档：
* [SQL Parser](https://github.com/alibaba/druid/wiki/SQL-Parser)
* [MySQL SQL Parser](https://github.com/alibaba/druid/wiki/MySQL-SQL-Parser)
* [Druid_SQL_AST](https://github.com/alibaba/druid/wiki/Druid_SQL_AST)
* [WallVisitor](https://github.com/alibaba/druid/wiki/%E7%AE%80%E4%BB%8B_WallFilter)
* [配置—WallFilter](https://github.com/alibaba/druid/wiki/%E9%85%8D%E7%BD%AE-wallfilter)
* [EvalVisitor](https://github.com/alibaba/druid/wiki/EvalVisitor)
* [SchemaStatVisitor](https://github.com/alibaba/druid/wiki/SchemaStatVisitor)
* [ExportParameterVisitor_demo_cn](https://github.com/alibaba/druid/wiki/ExportParameterVisitor_demo_cn)
* [ParameterizedOutputVisitor](https://github.com/alibaba/druid/wiki/ParameterizedOutputVisitor)
* [SQL_Format](https://github.com/alibaba/druid/wiki/SQL_Format)
* [SQL_Parser_Demo_visitor(自定义Vistor)](https://github.com/alibaba/druid/wiki/SQL_Parser_Demo_visitor)
* [SQL_Parser_Parameterize](https://github.com/alibaba/druid/wiki/SQL_Parser_Parameterize)
* [SQL_RemoveCondition_demo](https://github.com/alibaba/druid/wiki/SQL_RemoveCondition_demo)
* [SQL_Schema_Repository](https://github.com/alibaba/druid/wiki/SQL_Schema_Repository)
* [TableMapping_cn](https://github.com/alibaba/druid/wiki/TableMapping_cn)
* [如何修改SQL添加条件](https://github.com/alibaba/druid/wiki/%E5%A6%82%E4%BD%95%E4%BF%AE%E6%94%B9SQL%E6%B7%BB%E5%8A%A0%E6%9D%A1%E4%BB%B6)

# Demo

在Demo中实现了Mybatis Plugin以及Druid Filter两种模式，实现的功能很简单，就是在开篇中的`insert ... on duplicate key update`sql中加上`update_time = now()`。

Demo地址为 [mybatis-plugin-or-druid-filter-rewrite-sql](https://github.com/larva-zhang/some-problems-record/tree/master/mybatis-plugin-or-druid-filter-rewrite-sql)。

在Demo中使用了H2模拟Mysql，H2的建表语句参考[`src/test/resources/schema-h2.sql`](https://github.com/larva-zhang/some-problems-record/blob/master/mybatis-plugin-or-druid-filter-rewrite-sql/src/test/resources/schema-h2.sql)。

## Druid Visitor
不论使用的是Mybatis Plugin还是Druid Filter，

## Mybatis plugin

Plugin代码是[`src/main/java/com/github/larva/zhang/problems/SimpleRewriteSqlMybatisPlugin.java`](https://github.com/larva-zhang/some-problems-record/blob/master/mybatis-plugin-or-druid-filter-rewrite-sql/src/main/java/com/github/larva/zhang/problems/SimpleRewriteSqlMybatisPlugin.java)。

```java
@Slf4j
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
public class SimpleRewriteSqlMybatisPlugin implements Interceptor {

    private final SimpleAppendUpdateTimeVisitor visitor = new SimpleAppendUpdateTimeVisitor();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement) args[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (sqlCommandType != SqlCommandType.INSERT) {
            // 只处理insert
            return invocation.proceed();
        }
        BoundSql boundSql = mappedStatement.getBoundSql(args[1]);
        String sql = boundSql.getSql();
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        if (CollectionUtils.isNotEmpty(sqlStatements)) {
            for (SQLStatement sqlStatement : sqlStatements) {
                sqlStatement.accept(visitor);
            }
        }
        if (visitor.getAndResetRewriteStatus()) {
            // 改写了SQL，需要替换MappedStatement
            String newSql = SQLUtils.toSQLString(sqlStatements, JdbcConstants.MYSQL);
            log.info("rewrite sql, origin sql: [{}], new sql: [{}]", sql, newSql);
            BoundSql newBoundSql = new BoundSql(mappedStatement.getConfiguration(), newSql,
                    boundSql.getParameterMappings(), boundSql.getParameterObject());
            // copy原始MappedStatement的各项属性
            MappedStatement.Builder builder =
                    new MappedStatement.Builder(mappedStatement.getConfiguration(), mappedStatement.getId(),
                            new WarpBoundSqlSqlSource(newBoundSql), mappedStatement.getSqlCommandType());
            builder.cache(mappedStatement.getCache()).databaseId(mappedStatement.getDatabaseId())
                    .fetchSize(mappedStatement.getFetchSize())
                    .flushCacheRequired(mappedStatement.isFlushCacheRequired())
                    .keyColumn(StringUtils.join(mappedStatement.getKeyColumns(), ','))
                    .keyGenerator(mappedStatement.getKeyGenerator())
                    .keyProperty(StringUtils.join(mappedStatement.getKeyProperties(), ','))
                    .lang(mappedStatement.getLang()).parameterMap(mappedStatement.getParameterMap())
                    .resource(mappedStatement.getResource()).resultMaps(mappedStatement.getResultMaps())
                    .resultOrdered(mappedStatement.isResultOrdered())
                    .resultSets(StringUtils.join(mappedStatement.getResultSets(), ','))
                    .resultSetType(mappedStatement.getResultSetType()).statementType(mappedStatement.getStatementType())
                    .timeout(mappedStatement.getTimeout()).useCache(mappedStatement.isUseCache());
            MappedStatement newMappedStatement = builder.build();
            // 将新生成的MappedStatement对象替换到参数列表中
            args[0] = newMappedStatement;
        }
        return invocation.proceed();
    }

    /**
     * 生成代理类然后添加到{@link InterceptorChain}中
     *
     * Mybatis的{@link Executor}依赖以下几个组件：
     * <ol>
     * <li>{@link StatementHandler} 负责创建JDBC {@link java.sql.Statement}对象</li>
     * <li>{@link ParameterHandler} 负责将实际参数填充到JDBC {@link java.sql.Statement}对象中</li>
     * <li>{@link ResultSetHandler} 负责JDBC {@link java.sql.Statement#execute(String)}
     * 后返回的{@link java.sql.ResultSet}的处理</li>
     * </ol>
     * 因为此Plugin只对Executor生效所以只代理{@link Executor}对象
     *
     * @param target
     * @return
     */
    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {

    }

    static class WarpBoundSqlSqlSource implements SqlSource {

        private final BoundSql boundSql;

        public WarpBoundSqlSqlSource(BoundSql boundSql) {
            this.boundSql = boundSql;
        }

        @Override
        public BoundSql getBoundSql(Object parameterObject) {
            return boundSql;
        }
    }
}
```

使用时只需声明Mybatis Configuration Bean时添加该Plugin实例到Interceptor列表中即可，参考[`src/test/java/com/github/larva/zhang/problems/mybatis/TestMybatisPluginRewriteSqlConfig.java`](https://github.com/larva-zhang/some-problems-record/blob/master/mybatis-plugin-or-druid-filter-rewrite-sql/src/test/java/com/github/larva/zhang/problems/mybatis/TestMybatisPluginRewriteSqlConfig.java)。

```java
    @Bean
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public org.apache.ibatis.session.Configuration mybatisConfiguration() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        // 各项属性设置
        ...
        // 使用Mybatis Plugin机制改写SQL
        configuration.addInterceptor(mybatisPlugin());
        return configuration;
    }
    
    @Bean
    public SimpleRewriteSqlMybatisPlugin mybatisPlugin() {
        return new SimpleRewriteSqlMybatisPlugin();
    }
```

## Druid Filter

Filter代码是[`src/main/java/com/github/larva/zhang/problems/SimpleRewriteSqlDruidFilter.java`](https://github.com/larva-zhang/some-problems-record/blob/master/mybatis-plugin-or-druid-filter-rewrite-sql/src/main/java/com/github/larva/zhang/problems/SimpleRewriteSqlDruidFilter.java)。

```java
@Slf4j
public class SimpleRewriteSqlDruidFilter extends FilterAdapter {

    private final SimpleAppendUpdateTimeVisitor visitor = new SimpleAppendUpdateTimeVisitor();

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        String dbType = chain.getDataSource().getDbType();
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, dbType);
        sqlStatements.forEach(sqlStatement -> sqlStatement.accept(visitor));
        if (visitor.getAndResetRewriteStatus()) {
            // 改写了SQL，需要替换
            String newSql = SQLUtils.toSQLString(sqlStatements, dbType);
            log.info("rewrite sql, origin sql: [{}], new sql: [{}]", sql, newSql);
            return super.statement_execute(chain, statement, newSql);
        }
        return super.statement_execute(chain, statement, sql);
    }

    @Override
    public PreparedStatementProxy connection_prepareStatement(FilterChain chain, ConnectionProxy connection, String sql, int autoGeneratedKeys) throws SQLException {
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, JdbcConstants.MYSQL);
        sqlStatements.forEach(sqlStatement -> sqlStatement.accept(visitor));
        if (visitor.getAndResetRewriteStatus()) {
            // 改写了SQL，需要替换
            String newSql = SQLUtils.toSQLString(sqlStatements, JdbcConstants.MYSQL);
            log.info("rewrite sql, origin sql: [{}], new sql: [{}]", sql, newSql);
            return super.connection_prepareStatement(chain, connection, newSql, autoGeneratedKeys);
        }
        return super.connection_prepareStatement(chain, connection, sql, autoGeneratedKeys);
    }
}
```
该Filter支持在`Statement`和`PreparedStatement`两种模式下执行的SQL Rewrite，但是缺少对其他类型的SQL的支持。

相较于Mybatis Plugin不好的一点是不论是什么SQL都需要先经过SQL Parser解析AST，当然这点也可以通过在`prepareStatement_execute`重写SQL而非`connection_prepareStatement`阶段。
> `prepareStatement_execute`阶段重写需要重新生成`PreparedStatementProxy`并且重设JdbcParameters，这点又比`connection_prepareStatement`阶段重写SQL要麻烦。

使用时只需在Druid DataSource实例声明时加入到Filter列表中即可，用法类型Druid的WallFilter。参考[`src/test/java/com/github/larva/zhang/problems/druid/DruidFilterRewriteSqlConfig.java`](https://github.com/larva-zhang/some-problems-record/blob/master/mybatis-plugin-or-druid-filter-rewrite-sql/src/test/java/com/github/larva/zhang/problems/druid/DruidFilterRewriteSqlConfig.java)。

```java
    @Bean(initMethod = "init", destroyMethod = "close")
    public DruidDataSource dataSource(@Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) throws SQLException {
        DruidDataSource druidDataSource = new DruidDataSource();
        // 各项属性设置
        ...
        // 添加改写SQL的Filter
        druidDataSource.setProxyFilters(Collections.singletonList(simpleRewriteSqlDruidFilter()));
        return druidDataSource;
    }
    
    @Bean
    public FilterAdapter simpleRewriteSqlDruidFilter() {
        return new SimpleRewriteSqlDruidFilter();
    }
```

## Druid Visitor
从上述的Plugin和Filter代码中都可以看到，实际的SQL改写是交给了[`src/main/java/com/github/larva/zhang/problems/SimpleAppendUpdateTimeVisitor.java`](https://github.com/larva-zhang/some-problems-record/blob/master/mybatis-plugin-or-druid-filter-rewrite-sql/src/main/java/com/github/larva/zhang/problems/SimpleAppendUpdateTimeVisitor.java)。

```java
@Slf4j
public class SimpleAppendUpdateTimeVisitor extends MySqlASTVisitorAdapter {

    private static final ThreadLocal<Boolean> REWRITE_STATUS_CACHE = new ThreadLocal<>();

    private static final String UPDATE_TIME_COLUMN = "update_time";

    @Override
    public boolean visit(MySqlInsertStatement x) {
        boolean hasUpdateTimeCol = false;
        // duplicate key update得到的都是SQLBinaryOpExpr
        List<SQLExpr> duplicateKeyUpdate = x.getDuplicateKeyUpdate();
        if (CollectionUtils.isNotEmpty(duplicateKeyUpdate)) {
            for (SQLExpr sqlExpr : duplicateKeyUpdate) {
                if (sqlExpr instanceof SQLBinaryOpExpr
                        && ((SQLBinaryOpExpr) sqlExpr).conditionContainsColumn(UPDATE_TIME_COLUMN)) {
                    hasUpdateTimeCol = true;
                    break;
                }
            }
            if (!hasUpdateTimeCol) {
                // append update time column
                String tableAlias = x.getTableSource().getAlias();
                StringBuilder setUpdateTimeBuilder = new StringBuilder();
                if (!StringUtils.isEmpty(tableAlias)) {
                    setUpdateTimeBuilder.append(tableAlias).append('.');
                }
                setUpdateTimeBuilder.append(UPDATE_TIME_COLUMN).append(" = now()");
                SQLExpr sqlExpr = SQLUtils.toMySqlExpr(setUpdateTimeBuilder.toString());
                duplicateKeyUpdate.add(sqlExpr);
                // 重写状态记录
                REWRITE_STATUS_CACHE.set(Boolean.TRUE);
            }
        }
        return super.visit(x);
    }

    /**
     * 返回重写状态并重置重写状态
     *
     * @return 重写状态，{@code true}表示已重写，{@code false}表示未重写
     */
    public boolean getAndResetRewriteStatus() {
        boolean rewriteStatus = Optional.ofNullable(REWRITE_STATUS_CACHE.get()).orElse(Boolean.FALSE);
        // reset rewrite status
        REWRITE_STATUS_CACHE.remove();
        return rewriteStatus;
    }
}
```