package com.wk.middleware.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wk.middleware.annotations.*;
import com.wk.middleware.entities.Base;
import com.wk.middleware.entities.UuidEntity;
import com.wk.middleware.exceptions.MddbException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Created by jince on 2018/11/21.
 */
public final class DBUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBUtil.class);

    private static final Cache<Class, List<Field>> FIELD_CACHE = CacheBuilder.newBuilder()
            .maximumSize(2000)
            .build();

    private DBUtil() {
    }

    public static <T> int update(JdbcTemplate jdbcTemplate, T t) {

        SqlInfo sqlInfo = genUpdateSqlInfo(t);

        return jdbcTemplate.update(sqlInfo.sql, sqlInfo.getParams());
    }

    private static <T> SqlInfo genUpdateSqlInfo(T t) {

        String tableName = getTableName(t.getClass());

        List<Field> fields = getObjectFields(t.getClass());

        StringBuilder sb = new StringBuilder("update " + tableName + " set ");
        List<Object> ps = new ArrayList<>();

        String idName = null;
        Object idVal = null;
        for (Field field : fields) {

            if (!isFieldEditable(field)) {
                continue;
            }
            String fieldName = getDbFieldName(field);
            try {
                field.setAccessible(true);
                Object fieldVal = field.get(t);
                if (field.getAnnotation(TableId.class) != null) {
                    idName = fieldName;
                    idVal = fieldVal;
                } else {
                    sb.append(fieldName).append("=?,");
                    ps.add(fieldVal);
                }
            } catch (Exception e) {
                LOGGER.error("get field " + fieldName + " value error", e);
            }
        }
        if (idName == null) {
            throw new RuntimeException("class " + t.getClass() + " have no field annotated by TableId");
        }

        int sbLen = sb.length();
        if (sb.lastIndexOf(",") == sbLen - 1) {
            sb.delete(sbLen - 1, sbLen);
        }

        sb.append(" where ").append(idName).append("=?");
        ps.add(idVal);

        return new SqlInfo(sb.toString(), ps.toArray(new Object[0]));
    }

    private static boolean isFieldEditable(Field field) {

        DbField dbField = field.getAnnotation(DbField.class);
        return !(dbField != null && !dbField.editable());
    }

    private static <T> List<Field> getObjectFields(Class<T> t) {

        try {
            return FIELD_CACHE.get(t, () -> {
                Class c = t;

                List<Field> fields = new ArrayList<>();
                do {
                    fields.addAll(Arrays.stream(c.getDeclaredFields()).filter(f -> !ignoreField(f)).collect(Collectors.toList()));
                    c = c.getSuperclass();
                } while (c != Object.class);
                return fields;
            });
        } catch (ExecutionException e) {
            throw new RuntimeException("getObjectFields error " + t, e);
        }
    }

    private static boolean ignoreField(Field f) {
        return f.getAnnotation(FieldIgnore.class) != null
                || Modifier.isStatic(f.getModifiers())
                || f.getAnnotation(UnionField.class) != null
                || f.getAnnotation(OneToMany.class) != null;
    }

    public static String getTableName(Class<?> cls) {

        Table table = cls.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("class " + cls + " have no annotation Table ");
        }
        return table.name();
    }

    public static <T> int insert(JdbcTemplate jdbcTemplate, T t) {

        if (t instanceof Base) {
            Base b = (Base) t;
            if (b.getCrtime() == null) {
                b.setCrtime(new Date());
            }
            if (StringUtils.isEmpty(b.getCruser())) {
                b.setCruser(WebUtil.getCurrentUsername());
            }
        }

        SqlInfo sqlInfo = genInsertSqlInfo(t);

        return jdbcTemplate.update(sqlInfo.sql, sqlInfo.params);
    }

    private static <T> SqlInfo genInsertSqlInfo(T t) {

        String tableName = getTableName(t.getClass());
        List<Field> fields = getObjectFields(t.getClass());

        StringBuilder sb = new StringBuilder("insert into " + tableName + " (");
        StringBuilder s1 = new StringBuilder();
        StringBuilder s2 = new StringBuilder();
        List<Object> params = new ArrayList<>();

        for (Field field : fields) {
            String fieldName = getDbFieldName(field);
            try {
                field.setAccessible(true);
                Object fieldVal = field.get(t);
                s1.append(",").append(fieldName);
                s2.append(",").append("?");
                params.add(fieldVal);
            } catch (Exception e) {
                LOGGER.error("get field " + fieldName + " value error", e);
            }
        }
        if (s1.length() > 0) {
            s1.delete(0, 1);
        }
        if (s2.length() > 0) {
            s2.delete(0, 1);
        }
        sb.append(s1).append(") values (").append(s2).append(")");
        return new SqlInfo(sb.toString(), params.toArray(new Object[0]));
    }

    public static <T> int deleteMulti(JdbcTemplate jdbcTemplate, T[] ids, Class<?> cls) {

        tryCascadeDelete(jdbcTemplate, ids, cls);

        String tableName = getTableName(cls);
        String idFieldName = getDbFieldName(getKeyField(cls));

        StringBuilder sb = new StringBuilder("delete from " + tableName + " where " + idFieldName + " in ( ");
        List<T> params = new ArrayList<>();
        for (T s : ids) {
            sb.append("?,");
            params.add(s);
        }

        int sbLen = sb.length();
        if (sb.lastIndexOf(",") == sbLen - 1) {
            sb.delete(sbLen - 1, sbLen);
        }

        sb.append(")");

        int r = jdbcTemplate.update(sb.toString(), params.toArray(new Object[0]));

        //  如果是树结构，则级联删除子集
        Field treeField = getTreeField(cls);
        if (treeField != null) {
            T[] childrenIds = findChildrenIds(jdbcTemplate, cls, treeField, ids);
            if (ArrayUtils.isNotEmpty(childrenIds)) {
                r += deleteMulti(jdbcTemplate, childrenIds, cls);
            }
        }
        return r;
    }

    private static <T> void tryCascadeDelete(JdbcTemplate jdbcTemplate, T[] ids, Class<?> cls) {

        if (ArrayUtils.isEmpty(ids)) return;

        List<Field> fields = getObjectUnionFields(cls);
        if (!CollectionUtils.isEmpty(fields)) {

            SqlInfo sqlInfo = genSqlInfoByArray(ids);
            if (sqlInfo == null) return;

            for (Field field : fields) {
                field.setAccessible(true);
                UnionField unionField = field.getAnnotation(UnionField.class);

                String tableName = unionField.tableName();
                String unionFieldName = unionField.unionField();

                int r = jdbcTemplate.update("DELETE FROM " + tableName + " WHERE " + unionFieldName + " in (" + sqlInfo.getSql() + ")", sqlInfo.getParams());
                LOGGER.info(">> 删除实体 {} 时级联删除关联表 {} 数据结果：{}", cls, tableName, r);
            }
        }
    }

    private static <T> T[] findChildrenIds(JdbcTemplate jdbcTemplate, Class<?> cls, Field treeField, T[] ids) {

        String tableName = getTableName(cls);
        String idFieldName = getDbFieldName(getKeyField(cls));
        String treeFieldName = getDbFieldName(treeField);
        SqlInfo sqlInfo = genSqlInfoByArray(ids);
        if (sqlInfo == null) return null;

        String sql = "select " + idFieldName + " from " + tableName + " where " + treeFieldName + " in (" + sqlInfo.getSql() + ") ";

        return (T[]) jdbcTemplate.queryForList(sql, sqlInfo.getParams())
                .stream()
                .map(m -> MapUtils.getObject(m, idFieldName)).toArray(Object[]::new);
    }

    private static Field getTreeField(Class<?> cls) {
        return getObjectFields(cls).stream()
                .filter(field -> {
                    DbField dbField = field.getAnnotation(DbField.class);
                    return dbField != null && dbField.tree();
                }).findFirst().orElse(null);
    }

    private static String getKeyFieldName(Class<?> cls) {


        Field f = getKeyField(cls);
        if (f == null) {
            throw new RuntimeException("class TableId has no field annotated by TableId");
        }
        return f.getName();
    }

    public static <T> DbRes<T> findByExample(JdbcTemplate jdbcTemplate, T t, PageParam pageParam) {
        DBUtil.WhereInfo whereInfo = DBUtil.genWhere(t);

        Class<T> cls = (Class<T>) t.getClass();
        String tableName = getTableName(cls);
//        String orderBy = getOrderBy(cls);
        PageParam.SortField sortField = parseSortField(pageParam, cls);

        long count = jdbcTemplate.queryForObject("select count(*) from " + tableName + " t where " + whereInfo.getWhere(), Long.class, whereInfo.getParams().toArray(new Object[0]));

        whereInfo.getParams().add((pageParam.getPageCurrent() - 1) * pageParam.getPageSize());
        whereInfo.getParams().add(pageParam.getPageSize());

        Object[] p = whereInfo.getParams().toArray(new Object[0]);

        List<T> list = jdbcTemplate.query("select * from " + tableName + " t where " + whereInfo.getWhere() +
                        (sortField != null ? (" order by " + sortField.getFieldName() + " " + (sortField.getSortType() != null && sortField.getSortType() != 1 ? "asc" : "desc")) : "") + " limit ?,?",
                new BeanPropertyRowMapper<>(cls), p);

        long allPage = count % pageParam.getPageSize() == 0 ? count / pageParam.getPageSize() : count / pageParam.getPageSize() + 1;

        pageParam.setPageCount((int) allPage);
        pageParam.setRecordCount(count);

        appendUnionField(jdbcTemplate, list);
        appenOneToManyField(jdbcTemplate, list);

        return new DbRes<>(list, pageParam);
    }

    private static <T> void appenOneToManyField(JdbcTemplate jdbcTemplate, List<T> list) {

        if (CollectionUtils.isEmpty(list)) return;

        Class<T> cls = (Class<T>) list.get(0).getClass();

        List<Field> fields = getOneToManyField(cls);

        if (!CollectionUtils.isEmpty(fields)) {

            Field keyField = getKeyField(cls);

            for (Field field : fields) {
                try {
                    field.setAccessible(true);

                    OneToMany oneToMany = field.getAnnotation(OneToMany.class);
                    String unionTableName = oneToMany.unionField();

                    Class otherClass = getRelationClass(field);
                    String otherTableName = getTableName(otherClass);

                    SqlInfo sqlInfo = genListInSqlInfo(list);
                    String sql = "SELECT * FROM " + otherTableName + " t WHERE " + unionTableName + " in (" + sqlInfo.getSql() + ")";

                    List l = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(otherClass), sqlInfo.getParams());

                    Field unionField = getFieldByName(otherClass, unionTableName);
                    unionField.setAccessible(true);

                    if (!CollectionUtils.isEmpty(l)) {

                        for (T t : list) {
                            Object val = getFieldValue(field, t);
                            if (val != null && val instanceof Collection) {

                                Object keyFieldValue = getFieldValue(keyField, t);

                                ((Collection) val).addAll((Set) l.stream().filter(o -> {
                                    Object unionFieldValue = getFieldValue(unionField, o);
                                    return (unionFieldValue == null && keyFieldValue == null) || (keyFieldValue != null && keyFieldValue.equals(unionFieldValue));
                                }).collect(Collectors.toSet()));
                            }
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("appenOneToManyField error", e);
                }
            }
        }
    }

    private static Field getFieldByName(Class otherClass, String unionTableName) {
        Class c = otherClass;

        do {
            Field f = Arrays.stream(c.getDeclaredFields()).filter(field -> field.getName().equals(unionTableName)).findFirst().orElse(null);
            if (f != null) return f;
            c = c.getSuperclass();
        } while (c != Object.class);

        return null;
    }

    private static <T> List<Field> getOneToManyField(Class<T> cls) {
        Class c = cls;

        List<Field> fields = new ArrayList<>();
        do {
            fields.addAll(Arrays.stream(c.getDeclaredFields()).filter(f -> f.getAnnotation(OneToMany.class) != null).collect(Collectors.toList()));
            c = c.getSuperclass();
        } while (c != Object.class);
        return fields;
    }

    private static <T> PageParam.SortField parseSortField(PageParam pageParam, Class<T> cls) {

        PageParam.SortField sortField = pageParam.getSort();

        if (sortField == null) {
            if (cls == null) return null;
            String field = getOrderBy(cls);
            if (StringUtils.isEmpty(field)) {
                return null;
            }
            String[] sortInfo = field.split(" ");
            return new PageParam.SortField(sortInfo[0], sortInfo.length == 2 && "asc".equalsIgnoreCase(sortInfo[1].trim()) ? 0 : 1);
        }
        return sortField;
    }

    private static <T> void appendUnionField(JdbcTemplate jdbcTemplate, List<T> list) {

        if (CollectionUtils.isEmpty(list)) return;

        Class<T> cls = (Class<T>) list.get(0).getClass();

        List<Field> fields = getUnionFields(cls);

        if (!CollectionUtils.isEmpty(fields)) {

            Field keyField = getKeyField(cls);

            for (Field field : fields) {

                try {
                    field.setAccessible(true);

                    UnionField unionField = field.getAnnotation(UnionField.class);
                    String unionTableName = unionField.tableName();
                    String unionFieldName = unionField.unionField();
                    String otherFieldName = unionField.otherField();
                    String order = unionField.order();

                    Class otherClass = getRelationClass(field);
                    String otherTableName = getTableName(otherClass);
                    String keyFieldName = getKeyFieldName(otherClass);

                    SqlInfo sqlInfo = genListInSqlInfo(list);
                    String sql = "select t." + unionFieldName + ",t1.* from " + unionTableName + " t left join " + otherTableName + " t1 on t." + otherFieldName + "=t1." + keyFieldName + " where t." + unionFieldName + " " +
                            "in (" + sqlInfo.getSql() + ") ";
                    if (!StringUtils.isEmpty(order)) {
                        sql += " order by t." + order;
                    }

                    List<Map<String, Object>> l = jdbcTemplate.queryForList(sql, sqlInfo.getParams());

                    if (!CollectionUtils.isEmpty(l)) {

                        for (Map<String, Object> map : l) {
                            String keyValue = MapUtils.getString(map, unionFieldName);

                            Object obj = map2Obj(map, otherClass);

                            for (T t : list) {
                                if (keyValue.equals(getFieldValue(keyField, t))) {
                                    Object v = getFieldValue(field, t);
                                    if (v != null && v instanceof Collection) {
                                        ((Collection) v).add(obj);
                                    }
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    LOGGER.error("appendUnionField error", e);
                }

            }
        }
    }

    private static Object map2Obj(Map<String, Object> map, Class otherClass) throws IllegalAccessException, InstantiationException, InvocationTargetException {

        if (MapUtils.isEmpty(map)) return null;

        Object o = otherClass.newInstance();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val == null) continue;

            try {
                Method setterMethod = getSetterMethod(otherClass, key, val.getClass());

                setterMethod.invoke(o, val);
            } catch (NoSuchMethodException e) {
                LOGGER.debug("class " + otherClass + " has no setter method of " + key);
            }
        }
        return o;
    }

    private static Method getSetterMethod(Class otherClass, String key, Class<?> valClass) throws NoSuchMethodException {

        return otherClass.getMethod(getSetterMethodName(key), valClass);
    }

    private static String getSetterMethodName(String key) {
        return "set" + key.substring(0, 1).toUpperCase() + key.substring(1);
    }

    private static <T> SqlInfo genListInSqlInfo(List<T> list) {

        if (!CollectionUtils.isEmpty(list)) {

            Class cls = list.get(0).getClass();
            StringBuilder sb = new StringBuilder();

            Object[] params = new Object[list.size()];
            int i = 0;

            for (T t : list) {
                sb.append(",?");
                params[i++] = getFieldValue(getKeyField(cls), t);
            }

            if (sb.length() > 0) {
                sb.delete(0, 1);
            }

            return new SqlInfo(sb.toString(), params);

        }
        throw new RuntimeException("list size is 0");
    }

    private static Class getRelationClass(Field field) throws ClassNotFoundException {
        Type type = field.getGenericType();
        if (type != null && type instanceof ParameterizedTypeImpl) {
            ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) type;
            Type[] types = parameterizedType.getActualTypeArguments();
            if (types.length > 0) {
                return Class.forName(types[0].getTypeName());
            }
        }
        return null;
    }

    private static String getRelationTableName(Field field) throws ClassNotFoundException {

        Class cls = getRelationClass(field);
        return cls == null ? null : getTableName(cls);
    }

    public static <T> List<Field> getUnionFields(Class<T> cls) {

        Class c = cls;

        List<Field> fields = new ArrayList<>();
        do {
            fields.addAll(Arrays.stream(c.getDeclaredFields()).filter(f -> f.getAnnotation(UnionField.class) != null).collect(Collectors.toList()));
            c = c.getSuperclass();
        } while (c != Object.class);
        return fields;
    }

    private static <T> String getOrderBy(Class<T> cls) {

        Table table = cls.getAnnotation(Table.class);

        if (table == null) {
            return null;
        }
        return table.orderBy();
    }

    private static <T> WhereInfo genWhere(T t) {

        StringBuilder sb = new StringBuilder(" 0=0 ");
        List<Object> params = new ArrayList<>();

        if (t == null) return new WhereInfo(sb.toString(), params);

        List<Field> fields = getObjectFields(t.getClass());

        for (Field field : fields) {
            String fieldName = field.getName();

            Class type = field.getType();
            try {
                field.setAccessible(true);
                Object fieldValue = field.get(t);

                if (!type.isPrimitive()) {
                    if (type == String.class) {
                        if (org.apache.commons.lang3.StringUtils.isNoneBlank(((String) fieldValue))) {
                            sb.append(" and ").append(fieldName).append(" like ? ");
                            params.add("%" + fieldValue + "%");
                        } else {
                            if (isEnableEmptySearchField(field)) {
                                sb.append(" and (").append(fieldName).append("='' or ").append(fieldName).append(" is NULL )");
                            }
                        }
                    } else {
                        if (fieldValue != null) {
                            sb.append(" and ").append(fieldName).append("=? ");
                            params.add(fieldValue);
                        } else if (isEnableEmptySearchField(field)) {
                            sb.append(" and (").append(fieldName).append("='' or ").append(fieldName).append(" is NULL )");
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.error("get field " + field + " value error", e);
            }
        }

        List<Field> unionFields = getObjectUnionFields(t.getClass());
        if (!CollectionUtils.isEmpty(unionFields)) {

            String keyField = getKeyFieldName(t.getClass());
            for (Field unionField : unionFields) {

                Object fieldVal = getFieldValue(unionField, t);
                if (fieldVal != null && fieldVal instanceof Collection && !((Collection) fieldVal).isEmpty()) {
                    Collection coll = (Collection) fieldVal;

                    UnionField uf = unionField.getAnnotation(UnionField.class);
                    String unionTableName = uf.tableName();
                    String unionFieldName = uf.unionField();
                    String otherFieldName = uf.otherField();

                    StringBuilder ssb = new StringBuilder();
                    Field otherKeyField = null;
                    for (Object o : coll) {
                        if (otherKeyField == null) {
                            otherKeyField = getKeyField(o.getClass());
                        }
                        Object otherKeyVal = getFieldValue(otherKeyField, o);
                        if (otherKeyVal != null) {
                            ssb.append(".?");
                            params.add(otherKeyVal);
                        }
                    }
                    if (ssb.length() > 0) {
                        ssb.delete(0, 1);
                    }

                    sb.append(" and exists (select 1 from ").append(unionTableName)
                            .append(" ut where ut.").append(otherFieldName).append(" in (").append(ssb.toString()).append(") and ").append(" ut.").append(unionFieldName).append("=t.").append(keyField).append(" ) ");
                }

            }
        }

        return new WhereInfo(sb.toString(), params);
    }

    private static List<Field> getObjectUnionFields(Class<?> aClass) {

        Class cls = aClass;
        List<Field> fields = new ArrayList<>();
        do {
            fields.addAll(Arrays.stream(cls.getDeclaredFields())
                    .filter(field -> field.getAnnotation(UnionField.class) != null).collect(Collectors.toList()));
        } while ((cls = cls.getSuperclass()) != Object.class);
        return fields;
    }

    private static boolean isEnableEmptySearchField(Field field) {
        return field.getAnnotation(EmptySearch.class) != null;
    }

    public static <T extends UuidEntity> int insertOrUpdate(JdbcTemplate jdbcTemplate, T t) {

        if (!StringUtils.isEmpty(t.getUuid())) {

            checkUniqueField(jdbcTemplate, t, SaveType.EDIT);

            T p = findByUuid(jdbcTemplate, (Class<T>) t.getClass(), t.getUuid());
            if (p == null) {
                throw new RuntimeException("uuid {" + t.getUuid() + "} 记录 {" + t.getClass() + "} 不存在");
            }

            copyNonNullFieldValue(t, p);

            return update(jdbcTemplate, p);
        } else {

            checkUniqueField(jdbcTemplate, t, SaveType.ADD);

            t.setUuid(UUID.randomUUID().toString());
            return insert(jdbcTemplate, t);
        }
    }

    private static <T> void copyNonNullFieldValue(T source, T target) {

        if (source == null || target == null) throw new NullPointerException();
        if (source.getClass() != target.getClass())
            throw new ClassCastException("类型不匹配 {" + source.getClass() + "}，{" + target.getClass() + "}");

        Class cls = source.getClass();
        List<Field> fields = getObjectFields(cls);
        if (!CollectionUtils.isEmpty(fields)) {

            for (Field field : fields) {

                Object fieldValue = getFieldValue(field, source);
                if (fieldValue != null) {
                    setFieldValue(field, target, fieldValue);
                }
            }
        }
    }

    private static <T> void setFieldValue(Field field, T target, Object fieldValue) {

        field.setAccessible(true);
        try {
            field.set(target, fieldValue);
        } catch (IllegalAccessException e) {
            LOGGER.error("setFieldValue error! {" + field + "} {" + target + "} {" + fieldValue + "}", e);
        }
    }

    private static <T extends UuidEntity> T findByUuid(JdbcTemplate jdbcTemplate, Class<T> aClass, String uuid) {
        String tableName = getTableName(aClass);
        return findOne(jdbcTemplate, "select * from " + tableName + " where uuid=? ", aClass, uuid);
    }

    private static <T extends UuidEntity> void checkUniqueField(JdbcTemplate jdbcTemplate, T obj, SaveType type) {

        List<Field> fields = getObjectFields(obj.getClass()).stream().filter(f -> {
            DbField dbField = f.getAnnotation(DbField.class);

            return dbField != null && dbField.unique();
        }).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(fields)) return;

        Field keyField = getKeyField(obj.getClass());
        if (keyField == null) throw new RuntimeException("no field annotated by TableId");

        String keyFieldDbName = getDbFieldName(keyField);
        Object keyFieldValue = getFieldValue(keyField, obj);

        String tableName = getTableName(obj.getClass());
        for (Field field : fields) {

            String dbFieldName = getDbFieldName(field);
            Object fieldValue = getFieldValue(field, obj);

            T t;
            if (type == SaveType.EDIT) {
                t = findOne(jdbcTemplate, "select * from " + tableName + " where " + dbFieldName + "= ? and " + keyFieldDbName + "<>? ", (Class<T>) obj.getClass(), fieldValue, keyFieldValue);
            } else {
                t = findOne(jdbcTemplate, "select * from " + tableName + " where " + dbFieldName + "= ?  ", (Class<T>) obj.getClass(), fieldValue);
            }

            if (t != null) throw new MddbException("字段[" + dbFieldName + "]值为[" + fieldValue + "]的记录已经存在！");
        }
    }

    private static <T> Field getKeyField(Class<T> cls) {
        return getObjectFields(cls).stream().filter(f -> f.getAnnotation(TableId.class) != null).findFirst().orElse(null);
    }

    public static <T> T findOne(JdbcTemplate jdbcTemplate, String s, Class<T> aClass, Object... params) {

        List<T> ts = jdbcTemplate.query(s, new BeanPropertyRowMapper<>(aClass), params);

        if (CollectionUtils.isEmpty(ts)) {
            return null;
        }
        return ts.get(0);
    }

    private static <T> Object getFieldValue(Field field, T role) {

        field.setAccessible(true);
        try {
            return field.get(role);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("get Field " + role + " " + field + " value error", e);
        }
    }

    private static String getDbFieldName(Field field) {

        DbField dbField = field.getAnnotation(DbField.class);
        return dbField == null || StringUtils.isEmpty(dbField.name()) ? field.getName() : dbField.name();
    }

    public static <T> SqlInfo genSqlInfoByArray(T[] ts) {

        if (ArrayUtils.isEmpty(ts)) return null;

        StringBuilder sb = new StringBuilder();
        Object[] params = new Object[ts.length];
        int i = 0;
        for (T t : ts) {
            sb.append(",?");
            params[i++] = t;
        }
        if (sb.length() > 0) {
            sb.delete(0, 1);
        }
        return new SqlInfo(sb.toString(), params);
    }

    public static <T extends UuidEntity> List<T> findByArray(JdbcTemplate jdbcTemplate, Class<T> cls, String fieldName, String[] fieldValues) {

        SqlInfo sqlInfo = genSqlInfoByArray(fieldValues);

        if (sqlInfo == null) throw new NullPointerException();
        String tableName = getTableName(cls);
        String sql = "select * from " + tableName + " where " + fieldName + " in (" + sqlInfo.getSql() + ") ";

        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(cls), sqlInfo.getParams());
    }

    public static <T> T findObject(JdbcTemplate jdbcTemplate, String sql, Class<T> cls, Object... params) {
        return jdbcTemplate.queryForObject(sql, cls, params);
    }

    public static <T extends UuidEntity> T get(JdbcTemplate jdbcTemplate, Class<T> cls, String targetId) {

        String tableName = getTableName(cls);
        String keyFieldName = getKeyFieldName(cls);
        return findOne(jdbcTemplate, "select * from " + tableName + " where " + keyFieldName + "=?", cls, targetId);
    }

//    public static DbRes findByTableName(JdbcTemplate adminJdbcTemplate, JdbcTemplate jdbcTemplate, String tableName, PageParam pageParam) {
//
//        DBUtil.WhereInfo whereInfo = genWhereParamByCondition(adminJdbcTemplate, pageParam.getConditions());
//
//        PageParam.SortField sortField = parseSortField(pageParam, null);
//
//        if (sortField != null) {
//            whereInfo.setWhere(whereInfo.getWhere() + " order by " + sortField.getFieldName() + " " + (sortField.getSortType() == null || sortField.getSortType() == 1 ? "asc" : "desc"));
//        }
//
//        long count = jdbcTemplate.queryForObject("select count(*) from " + tableName + " t where " + whereInfo.getWhere(), Long.class, whereInfo.getParams().toArray(new Object[0]));
//
//        whereInfo.getParams().add((pageParam.getPageCurrent() - 1) * pageParam.getPageSize());
//        whereInfo.getParams().add(pageParam.getPageSize());
//
//        Object[] p = whereInfo.getParams().toArray(new Object[0]);
//
//        List<Map<String, Object>> list = jdbcTemplate.query("select * from " + tableName + " t where " + whereInfo.getWhere() + " limit ?,?",
//                new ColumnMapRowMapper() {
//
//                    @Nullable
//                    @Override
//                    protected Object getColumnValue(ResultSet rs, int index) throws SQLException {
//                        Object val = super.getColumnValue(rs, index);
//
//                        if (val != null && val instanceof Date) {
//                            return val instanceof java.sql.Date ? DateUtil.toStr((Date) val, DateUtil.FMT_YYYY_MM_DD) :
//                                    DateUtil.toStr((Date) val, DateUtil.FMT_YYYY_MM_DD_HH_MM_SS);
//                        }
//
//                        return val;
//                    }
//                }, p);
//
//        long allPage = count % pageParam.getPageSize() == 0 ? count / pageParam.getPageSize() : count / pageParam.getPageSize() + 1;
//
//        pageParam.setPageCount((int) allPage);
//        pageParam.setRecordCount(count);
//
////        appendUnionField(jdbcTemplate, list);
//        return new DbRes<>(list, pageParam);
//    }

//    public static WhereInfo genWhereParamByCondition(JdbcTemplate jdbcTemplate, List<MetaCondition> conditions) {
//
//        StringBuilder sb = new StringBuilder(" 0=0 ");
//        List<Object> params = new ArrayList<>();
//        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(conditions)) {
//
//            SqlInfo sqlInfo = genSqlInfoByArray(conditions.stream().map(MetaCondition::getMetaId).toArray(String[]::new));
//            List<Meta> metas = jdbcTemplate.query("select * from sys_jds_meta where uuid in (" + sqlInfo.getSql() + ")", new BeanPropertyRowMapper<>(Meta.class), sqlInfo.getParams());
//
//            if (!CollectionUtils.isEmpty(metas)) {
//                for (MetaCondition condition : conditions) {
//
//                    try {
//                        Meta meta = metas.stream().filter(m -> m.getUuid().equals(condition.getMetaId())).findFirst().orElse(null);
//                        if (meta != null) {
//                            sb.append(" and ").append(meta.getName()).append(" ").append(determineOpr(condition.getOpr())).append(" ").append(determineVal(condition.getOpr(), condition.getValue(), meta.getFieldType(), params));
////                            params.add(condition.getValue());
//                        }
//                    } catch (Exception e) {
//                        LOGGER.error("genWhereParamByCondition error ", e);
//                    }
//                }
//            }
//        }
//        return new WhereInfo(sb.toString(), params);
//    }

//    public static WhereInfo genWhereParamByConditionAndSearchField(JdbcTemplate jdbcTemplate, List<MetaCondition> conditions, List<SearchField> searchFields) {
//        WhereInfo whereInfo = genWhereParamByCondition(jdbcTemplate, conditions);
//        if (!CollectionUtils.isEmpty(searchFields)) {
//            String where = whereInfo.getWhere();
//            List<Object> params = whereInfo.getParams();
//            where += searchFields.stream().map(field -> {
//                String fieldUnion = field.getFieldUnion();
//                String connectMark = StringUtils.isEmpty(fieldUnion) ? SqlConstant.AND : (StringUtils.equalsIgnoreCase(fieldUnion, "and") ? SqlConstant.AND : SqlConstant.OR);
//                return connectMark + FieldOprsEnum.getWhereWithQuestionMarkBySearchField(field);
//            }).collect(Collectors.joining());
//            searchFields.forEach(field -> {
//                params.addAll(FieldOprsEnum.getWhereParamsBySearchField(field));
//            });
//            return new WhereInfo(where, params);
//        } else {
//            return whereInfo;
//        }
//    }

//    private static String determineVal(String opr, String value, String fieldType, List<Object> params) {
//
//
//        Assert.notNull(opr, "determineVal opr");
//        Assert.notNull(value, "determineVal value");
//        Assert.notNull(fieldType, "determineVal fieldType");
//        Assert.notNull(params, "determineVal params");
//
//        opr = opr.toUpperCase();
//
//        StringBuilder sb = new StringBuilder();
//        switch (opr) {
//            case "EQ":
//            case "NEQ":
//            case "GT":
//            case "LT":
//            case "GTE":
//            case "LTE":
//                sb.append("?");
//
//                params.add(isFieldIntType(fieldType) ? Integer.parseInt(value) : value);
//                break;
//            case "IN":
//                String[] sArr = value.split(",");
//                List<Object> objects = isFieldIntType(fieldType) ? Arrays.stream(sArr).map(Integer::parseInt).collect(Collectors.toList()) :
//                        Arrays.stream(sArr).collect(Collectors.toList());
//
//                sb.append("(").append(StringUtils.join(objects.stream().map(s -> "?").toArray(), ",")).append(")");
//
//                params.addAll(objects);
//                break;
//            case "LIKE":
//                sb.append("?");
//                params.add("%" + value + "%");
//                break;
//            default:
//                throw new RuntimeException("不支持的类型 " + opr);
//        }
//
//        return sb.toString();
//    }

//    public static boolean isFieldIntType(String fieldType) {
//        return "int".equalsIgnoreCase(fieldType) || "decimal".equalsIgnoreCase(fieldType);
//    }
//
//    public static String determineOpr(String opr) {
//
//        if (opr == null) throw new NullPointerException();
//
//        opr = opr.toUpperCase();
//        switch (opr) {
//            case "EQ":
//                return "=";
//            case "NEQ":
//                return "<>";
//            case "GT":
//                return ">";
//            case "LT":
//                return "<";
//            case "GTE":
//                return ">=";
//            case "LTE":
//                return "<=";
//            case "IN":
//                return "in";
//            case "LIKE":
//                return "like";
//            default:
//                throw new RuntimeException("不支持的类型 " + opr);
//        }
//
//    }

    public static class SqlInfo {

        String sql;
        Object[] params;

        public SqlInfo() {
        }

        public SqlInfo(String s, Object[] objects) {
            this.sql = s;
            this.params = objects;
        }

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public Object[] getParams() {
            return params;
        }

        public void setParams(Object[] params) {
            this.params = params;
        }
    }

    public static class WhereInfo {
        String where;
        List<Object> params = new ArrayList<>();

        public WhereInfo() {
        }

        public WhereInfo(String s, List<Object> params) {
            this.where = s;
            this.params = params;
        }

        public String getWhere() {
            return where;
        }

        public void setWhere(String where) {
            this.where = where;
        }

        public List<Object> getParams() {
            return params;
        }

        public void setParams(List<Object> params) {
            this.params = params;
        }
    }

    static enum SaveType {
        ADD, EDIT
    }
}
