package com.github.larva.zhang.problems;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlASTVisitorAdapter;
import com.alibaba.druid.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * SimpleAppendUpdateTimeVisitor 简单的druid visitor实现，仅仅在insert on duplicate key update缺少设置更新时间字段时自动加上设置更新时间为当前时间
 *
 * @author zhanghan
 * @date 2019/12/12
 * @since 1.0
 */
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
