package com.fesi.deadlinemate.global.monitoring;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

public final class QueryCountingConnectionHandler implements InvocationHandler {

    private final Connection delegate;

    public QueryCountingConnectionHandler(Connection delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object result;
        try {
            result = method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }

        String name = method.getName();
        if ("createStatement".equals(name) && result instanceof Statement stmt) {
            return wrapStatement(stmt, Statement.class, null);
        }
        if ("prepareStatement".equals(name) && result instanceof PreparedStatement ps) {
            String sql = extractSql(args);
            return wrapStatement(ps, PreparedStatement.class, sql);
        }
        if ("prepareCall".equals(name) && result instanceof CallableStatement cs) {
            String sql = extractSql(args);
            return wrapStatement(cs, CallableStatement.class, sql);
        }
        return result;
    }

    private String extractSql(Object[] args) {
        if (args != null && args.length > 0 && args[0] instanceof String s) return s;
        return null;
    }

    private <T> T wrapStatement(T stmt, Class<T> iface, String sql) {
        return iface.cast(Proxy.newProxyInstance(
                stmt.getClass().getClassLoader(),
                new Class<?>[]{iface},
                new QueryCountingStatementHandler(stmt, sql)
        ));
    }
}
