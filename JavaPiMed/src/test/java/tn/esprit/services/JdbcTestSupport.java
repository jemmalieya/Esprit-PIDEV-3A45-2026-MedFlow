package tn.esprit.services;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class JdbcTestSupport {

    private JdbcTestSupport() {
    }

    static <T> T newServiceInstance(Class<T> type) {
        try {
            Unsafe unsafe = getUnsafe();
            @SuppressWarnings("unchecked")
            T instance = (T) unsafe.allocateInstance(type);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create test instance for " + type.getName(), e);
        }
    }

    static void injectConnection(Object target, Connection connection) {
        try {
            Field field = target.getClass().getDeclaredField("cn");
            field.setAccessible(true);
            field.set(target, connection);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to inject connection", e);
        }
    }

    static Connection connection(PreparedStatement preparedStatement) {
        return connection(preparedStatement, null);
    }

    static Connection connection(PreparedStatement preparedStatement, Statement statement) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> handleConnection(method, preparedStatement, statement)
        );
    }

    static PreparedStatement preparedStatement(MockPreparedStatementState state) {
        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                new PreparedStatementHandler(state)
        );
    }

    static Statement statement(MockResultSet resultSet) {
        return (Statement) Proxy.newProxyInstance(
                Statement.class.getClassLoader(),
                new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("executeQuery")) {
                        return resultSet.proxy();
                    }
                    if (name.equals("close") || name.equals("closeOnCompletion")) {
                        return null;
                    }
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    static MockResultSet resultSet(List<Map<String, Object>> rows) {
        return new MockResultSet(rows);
    }

    static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new HashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            row.put((String) keyValues[index], keyValues[index + 1]);
        }
        return row;
    }

    static final class MockPreparedStatementState {
        private final String expectedSql;
        private final MockResultSet queryResultSet;
        private final MockResultSet generatedKeysResultSet;
        private final int updateCount;
        private final Map<Integer, Object> parameters = new HashMap<>();

        MockPreparedStatementState(String expectedSql, MockResultSet queryResultSet, MockResultSet generatedKeysResultSet, int updateCount) {
            this.expectedSql = expectedSql;
            this.queryResultSet = queryResultSet;
            this.generatedKeysResultSet = generatedKeysResultSet;
            this.updateCount = updateCount;
        }

        Map<Integer, Object> getParameters() {
            return parameters;
        }
    }

    static final class MockResultSet {
        private final List<Map<String, Object>> rows;
        private int index = -1;

        MockResultSet(List<Map<String, Object>> rows) {
            this.rows = new ArrayList<>(rows);
        }

        ResultSet proxy() {
            return (ResultSet) Proxy.newProxyInstance(
                    ResultSet.class.getClassLoader(),
                    new Class<?>[]{ResultSet.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if (name.equals("next")) {
                            index++;
                            return index < rows.size();
                        }
                        if (name.equals("getInt")) {
                            return asNumber(value(args)).intValue();
                        }
                        if (name.equals("getString")) {
                            Object value = value(args);
                            return value == null ? null : value.toString();
                        }
                        if (name.equals("getTimestamp")) {
                            return (Timestamp) value(args);
                        }
                        if (name.equals("getObject")) {
                            return value(args);
                        }
                        if (name.equals("close")) {
                            return null;
                        }
                        if (method.getReturnType().equals(boolean.class)) {
                            return false;
                        }
                        if (method.getReturnType().equals(int.class)) {
                            return 0;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private Object value(Object[] args) {
            if (index < 0 || index >= rows.size()) {
                throw new IllegalStateException("ResultSet cursor is not positioned on a row");
            }
            Object key = args[0];
            if (key instanceof String columnName) {
                return rows.get(index).get(columnName);
            }
            if (key instanceof Integer columnIndex) {
                return rows.get(index).values().toArray()[columnIndex - 1];
            }
            throw new IllegalArgumentException("Unsupported column key: " + key);
        }

        private Number asNumber(Object value) {
            if (value instanceof Number number) {
                return number;
            }
            if (value == null) {
                return 0;
            }
            return Integer.parseInt(value.toString());
        }
    }

    private static class PreparedStatementHandler implements InvocationHandler {
        private final MockPreparedStatementState state;

        PreparedStatementHandler(MockPreparedStatementState state) {
            this.state = state;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if (name.equals("setInt") || name.equals("setString") || name.equals("setTimestamp") || name.equals("setObject")) {
                state.getParameters().put((Integer) args[0], args[1]);
                return null;
            }
            if (name.equals("executeUpdate")) {
                return state.updateCount;
            }
            if (name.equals("executeQuery")) {
                return state.queryResultSet == null ? resultSet(List.of()).proxy() : state.queryResultSet.proxy();
            }
            if (name.equals("getGeneratedKeys")) {
                return state.generatedKeysResultSet == null ? resultSet(List.of()).proxy() : state.generatedKeysResultSet.proxy();
            }
            if (name.equals("close")) {
                return null;
            }
            if (name.equals("toString")) {
                return "PreparedStatement[sql=" + state.expectedSql + "]";
            }
            if (method.getReturnType().equals(boolean.class)) {
                return false;
            }
            if (method.getReturnType().equals(int.class)) {
                return 0;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static Object handleConnection(Method method, PreparedStatement preparedStatement, Statement statement) {
        String name = method.getName();
        if (name.equals("prepareStatement")) {
            return preparedStatement;
        }
        if (name.equals("createStatement")) {
            return statement;
        }
        if (name.equals("close")) {
            return null;
        }
        if (name.equals("isClosed")) {
            return false;
        }
        if (method.getReturnType().equals(boolean.class)) {
            return false;
        }
        if (method.getReturnType().equals(int.class)) {
            return 0;
        }
        return defaultValue(method.getReturnType());
    }

    private static Unsafe getUnsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}