package com.fesi.deadlinemate.global.monitoring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public final class QueryCountingStatementHandler implements InvocationHandler {

    private static final Set<String> EXECUTE_METHODS = Set.of(
            "execute", "executeQuery", "executeUpdate",
            "executeBatch", "executeLargeBatch", "executeLargeUpdate"
    );

    private final Object delegate;
    private final String sql;

    public QueryCountingStatementHandler(Object delegate, String sql) {
        this.delegate = delegate;
        this.sql = sql;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (!EXECUTE_METHODS.contains(method.getName())) {
            return invokeDelegate(method, args);
        }

        QueryCountContext ctx = QueryCountContext.current();
        if (ctx == null) {
            return invokeDelegate(method, args);
        }

        long start = System.currentTimeMillis();
        try {
            return invokeDelegate(method, args);
        } finally {
            ctx.recordSql(resolveSql(method, args), System.currentTimeMillis() - start);
        }
    }

    private String resolveSql(Method method, Object[] args) {
        if (sql != null) return sql;
        if (args != null && args.length > 0 && args[0] instanceof String s) return s;
        return method.getName();
    }

    private Object invokeDelegate(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
