package com.github.larva.zhang.problems;

import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.util.JdbcConstants;

import lombok.extern.slf4j.Slf4j;

/**
 * SimpleRewriteSqlMybatisPlugin
 *
 * Mybatis Plugin参考<a href="https://blog.csdn.net/zsj777/article/details/81986096">mybatis拦截器，动态修改sql语句</a> <a href=
 * "https://github.com/pagehelper/Mybatis-PageHelper/blob/master/src/main/java/com/github/pagehelper/QueryInterceptor.java">Mybatis-PageHelper</a>
 * 
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
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
