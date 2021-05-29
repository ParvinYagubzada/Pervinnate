package az.code.mapper;

import az.code.annotations.Column;
import az.code.annotations.Entity;
import az.code.annotations.Id;
import az.code.exceptions.ClassNotMappable;
import az.code.exceptions.FieldlessClass;
import az.code.exceptions.NoMappableFieldsFound;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

public class Manager {
    private static final Path dataFolder = Path.of("src\\main\\resources\\META-INF");
    private static Connection connection;

    static {
        try {
            Properties properties = new Properties();
            properties.load(Files.newBufferedReader(dataFolder.resolve("app.config")));
            String connectionString = "jdbc:postgresql://" + properties.get("ip") +
                    "/" + properties.get("db") + "?user=" + properties.get("user") + "&password=" + properties.get("password");
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException | IOException throwable) {
            throwable.printStackTrace();
        }
    }

    public static <T> T find(int id, Class<T> reference) throws Exception, NoMappableFieldsFound {
        isMappable(reference);
        String table = reference.getAnnotation(Entity.class).name();
        Statement statement = connection.createStatement();
        String columnName = getIdColumnName(reference);
        ResultSet result = statement.executeQuery("SELECT * FROM " + table + " WHERE " + columnName + " = " + id);
        if (result.next()) {
            connection.close();
            return mapObject(result, reference);
        }
        connection.close();
        return null;
    }

    public static <T> T remove(T object) throws Exception, NoMappableFieldsFound {
        isMappable(object.getClass());
        String table = object.getClass().getAnnotation(Entity.class).name();
        Statement statement = connection.createStatement();
        MappableFields fields = getMappableFields(object.getClass().getDeclaredFields());
        String columnName = fields.idColumn;
        Field idField = fields.id;
        if (idField != null) {
            idField.setAccessible(true);
            Integer id = (Integer) idField.get(object);
            T find = (T) find(id, object.getClass());
            if (find != null && find.equals(object)) {
                statement.executeUpdate("DELETE FROM " + table + " WHERE " + columnName + " = " + id);
                return object;
            }
            return null;
        }
        return null;
    }

    public static <T> List<T> getObjects(int limit, Class<T> reference) throws Exception, NoMappableFieldsFound {
        isMappable(reference);
        String table = reference.getAnnotation(Entity.class).name();
        Statement statement = connection.createStatement();
        ResultSet result = statement.executeQuery("SELECT * FROM " + table + " LIMIT " + limit);
        List<T> objects = new LinkedList<>();
        while (result.next()) {
            objects.add(mapObject(result, reference));
        }
        connection.close();
        return objects;
    }

    public static <T> void merge(T object) throws Exception, NoMappableFieldsFound {
        isMappable(object.getClass());//TODO: Create table if it is not exists!
        MappableFields fields = getMappableFields(object.getClass().getDeclaredFields());
        Field idField = fields.id;
        if (idField != null) {
            idField.setAccessible(true);
            Integer id = (Integer) idField.get(object);
            if (id != null) {
                update(object, fields);
            } else {
                insert(object, fields);
            }
        }
    }

    private static <T> void update(T object, MappableFields fields) throws SQLException, IllegalAccessException {
        String columns = fields.id != null ? fields.idColumn : "";
        if (!columns.equals("") && fields.fieldList.size() > 0)
            columns += ", ";
        columns += fields.fieldList.stream().map(field -> field.getAnnotation(Column.class).name()).collect(Collectors.joining(", "));
        String table = object.getClass().getAnnotation(Entity.class).name();

        //language=
        String builder = "INSERT INTO %s(%s) VALUES(%s) ON CONFLICT (%s) DO UPDATE\nSET "
                .formatted(table, columns, repeat(fields.fieldList.size() + 1), fields.idColumn) + fields.fieldList.stream().map(field -> {
            String column = field.getAnnotation(Column.class).name();
            return column + "=excluded." + column;
        }).collect(Collectors.joining(", "));

        PreparedStatement statement = connection.prepareStatement(builder);
        statement.setInt(1, (Integer) fields.id.get(object));
        preparedStatement(object, statement, fields.fieldList, 2);
        System.out.println(statement);
        connection.close();
    }

    private static <T> void insert(T object, MappableFields fields) throws SQLException, IllegalAccessException {
        String columns = fields.fieldList.stream().map(field -> field.getAnnotation(Column.class).name()).collect(Collectors.joining(", "));
        String table = object.getClass().getAnnotation(Entity.class).name();
        PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + "(" + columns + ") VALUES(" + repeat(fields.fieldList.size()) + ")");
        System.out.println(statement);
        preparedStatement(object, statement, fields.fieldList, 1);
        connection.close();
    }

    private static <T> void preparedStatement(T object, PreparedStatement statement, List<Field> fields, int count) throws SQLException, IllegalAccessException {
        Object value;
        for (Field field : fields) {
            field.setAccessible(true);
            Class<?> type = field.getType();
            if (type.equals(Integer.class)) {
                value = field.get(object);
                if (value != null)
                    statement.setInt(count++, (Integer) value);
                else
                    statement.setNull(count++, 4);
            } else if (type.equals(Double.class)) {
                value = field.get(object);
                if (value != null)
                    statement.setDouble(count++, (Double) value);
                else
                    statement.setNull(count++, 8);
            } else if (type.getSimpleName().equals(Date.class.getSimpleName())) {
                value = field.get(object);
                if (value != null)
                    statement.setDate(count++, (Date) value);
                else
                    statement.setNull(count++, 91);
            } else if (type.equals(String.class)) {
                value = field.get(object);
                if (value != null) {
                    System.out.println(value);
                    statement.setString(count++, (String) value);
                } else
                    statement.setNull(count++, 12);
            }
        }
        System.out.println(statement);
        statement.execute();
        statement.close();
    }

    private static <T> T mapObject(ResultSet result, Class<T> reference) throws Exception, NoMappableFieldsFound {
        Constructor<T> constructor = reference.getConstructor();
        constructor.setAccessible(true);
        T object = constructor.newInstance();
        ResultSetMetaData metaData = result.getMetaData();
        Map<String, Integer> colNameIndex = new LinkedHashMap<>();
        for (int i = 1; i < metaData.getColumnCount() + 1; i++) {
            colNameIndex.put(metaData.getColumnName(i), i);
        }

        MappableFields mappableFields = getMappableFields(reference.getDeclaredFields());

        if (mappableFields.id != null) {
            mappableFields.id.setAccessible(true);
            mappableFields.id.set(object, result.getInt(mappableFields.idColumn));
        } else {
            throw new Exception("This object does not have any id!");
        }

        for (Field field : mappableFields.fieldList) {
            field.setAccessible(true);
            Integer columnIndex = colNameIndex.get(field.getAnnotation(Column.class).name());
            if (columnIndex == null)
                throw new Exception("This column does not exists.");
            field.set(object, getData(result, columnIndex, metaData.getColumnType(columnIndex)));
        }
        return object;
    }

    private static MappableFields getMappableFields(Field[] allFields) throws Exception, NoMappableFieldsFound {
        if (allFields.length == 0)
            throw new FieldlessClass();
        MappableFields fields = new MappableFields();
        boolean idFound = false;
        for (Field field : allFields) {
            if (field.isAnnotationPresent(Id.class)) {
                if (!idFound) {
                    if (field.isAnnotationPresent(Column.class)) {
                        fields.idColumn = field.getAnnotation(Column.class).name();
                    } else {
                        fields.idColumn = "id";
                    }
                    fields.id = field;
                    idFound = true;
                } else {
                    throw new Exception("You can't have 2 ids in 1 class");
                }
            } else if (field.isAnnotationPresent(Column.class))
                fields.fieldList.add(field);
        }
        if (fields.id == null && fields.fieldList.size() == 0)
            throw new NoMappableFieldsFound();
        return fields;
    }

    private static <T> String getIdColumnName(Class<T> reference) throws NoMappableFieldsFound, Exception {
        MappableFields fields = getMappableFields(reference.getDeclaredFields());
        Field idField = fields.id;
        if (idField == null)
            throw new Exception("This column does not exists.");
        return fields.idColumn;
    }

    private static String repeat(int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times - 1; i++) {
            builder.append("?").append(", ");
        }
        return builder.append("?").toString();
    }

    private static Object getData(ResultSet result, int columnIndex, int type) throws SQLException {
        return switch (type) {
            case 91 -> result.getDate(columnIndex);
            case 8 -> result.getDouble(columnIndex);
            case 4 -> result.getInt(columnIndex);
            case 12 -> result.getString(columnIndex);
            default -> null;
        };
    }

    private static <T> void isMappable(Class<T> reference) throws ClassNotMappable {
        if (!reference.isAnnotationPresent(Entity.class))
            throw new ClassNotMappable();
    }

    static class MappableFields {
        Field id = null;
        String idColumn = null;
        List<Field> fieldList = new LinkedList<>();
    }
}
