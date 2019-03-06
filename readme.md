# 说明
    
    说明操作工具类 DbUtil
    
# 工具方法（DbUtil）

- public static <T> int deleteMulti(JdbcTemplate jdbcTemplate, T[] ids, Class<?> cls)

    根据ID数组批量删除实体，会级联删除多对多关联表，如果是树形结构会删除子元素
    
- public static <T extends UuidEntity> List<T> findByArray(JdbcTemplate jdbcTemplate, Class<T> cls, String fieldName, String[] fieldValues)

    根据指定字段以及指定的多个值查询列表，不关联查询
    
- public static <T> DbRes<T> findByExample(JdbcTemplate jdbcTemplate, T t, PageParam pageParam)

    根据指定的样例及分页参数查询实体列表，并关联查询
    
- public static <T> T findObject(JdbcTemplate jdbcTemplate, String sql, Class<T> cls, Object... params)

    根据指定的Sql及参数查询指定的实体，唯一实体
    
- public static <T> T findOne(JdbcTemplate jdbcTemplate, String s, Class<T> aClass, Object... params)

    根据指定的Sql及参数查询第一个实体
    
- public static <T extends UuidEntity> T get(JdbcTemplate jdbcTemplate, Class<T> cls, String targetId)

    查询主键是指定值的实体
    
- public static String getTableName(Class<?> cls) 

    获取指定实体对应的表名
    
- public static <T> int insert(JdbcTemplate jdbcTemplate, T t)

    插入指定的实体到数据库
    
- public static <T> int update(JdbcTemplate jdbcTemplate, T t) 

    更新指定的实体
    
- public static <T extends UuidEntity> int insertOrUpdate(JdbcTemplate jdbcTemplate, T t)

    插入或者更新指定的实体，如果uuid为空则插入，否则更新
    
# 注解

- @Table

- @DbField

- @EmptySearch

- @FieldIgnore

- @OneToMany

- @TableId

- @UnionField