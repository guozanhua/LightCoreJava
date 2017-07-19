package cn.alphabets.light.model.datarider;


import cn.alphabets.light.Constant;
import cn.alphabets.light.Environment;
import cn.alphabets.light.cache.CacheManager;
import cn.alphabets.light.db.mysql.Controller;
import cn.alphabets.light.entity.ModBoard;
import cn.alphabets.light.entity.ModStructure;
import cn.alphabets.light.exception.DataRiderException;
import cn.alphabets.light.http.Context;
import cn.alphabets.light.http.Params;
import cn.alphabets.light.model.Entity;
import cn.alphabets.light.model.ModCommon;
import cn.alphabets.light.model.Plural;
import cn.alphabets.light.model.Singular;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.bson.Document;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.alphabets.light.Constant.MODEL_PREFIX;
import static cn.alphabets.light.Constant.VALID;

/**
 * SQLRider
 * <p>
 * Created by lilin on 2016/11/12.
 */
public class SQLRider {

    private static final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(SQLRider.class);
    private static final List<String> METHOD = Arrays.asList(
            "", "add", "update", "remove", "list", "search", "get", "count"
    );

    public static Object call(Context handler, Class clazz, String boardMethod) throws DataRiderException {
        return call(handler, clazz, boardMethod, null);
    }

    public static Object call(Context handler, Class clazz, String boardMethod, Params params) throws DataRiderException {

        ModBoard board = getBoard(clazz, boardMethod);
        if (board == null) {
            throw DataRiderException.BoardNotFound("unknown api");
        }

        Object result = call(handler, clazz, board, params);
        return OptionsBuilder.fetchOptions(handler, result, board);
    }


    /**
     * invoke controller method to perform db operation
     *
     * @param board  board info
     * @param params DBParams
     * @return db operation result
     */
    private static Object call(Context handler, Class clazz, ModBoard board, Params params) {

        Params newParams = adaptToBoard(handler, clazz, board, params == null ? handler.params : params);
        Controller controller = new Controller(handler, newParams);
        String methodName = METHOD.get(board.getType().intValue());

        try {
            return controller.getClass().getMethod(methodName).invoke(controller);
        } catch (InvocationTargetException e) {
            throw DataRiderException.ControllerMethodCallFailed(methodName, e.getTargetException());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw DataRiderException.ControllerMethodCallFailed(methodName, e);
        }
    }

    public static Params adaptToBoard(Context handler, Class clazz, ModBoard board, Params params) {

        String script = buildScript(handler, board, params);

        System.out.println(script);

        return Params.clone(params, script, board.getSchema(), clazz);
    }

    /**
     * 通过查找 structure 中的定义，来识别给定字段的类型
     *
     * @param structure ModStructure
     * @param parameter 要识别类型的字段名
     * @return 类型名称
     */
    private static String detectValueType(ModStructure structure, String parameter) {

        Map<String, Map> items = (Map<String, Map>) structure.getItems();

        // 多层结构的数据，可以包含.标识符，如 address.city
        if (parameter.contains(".")) {

            String[] array = parameter.split("\\.");
            Map<String, Map> subTypeInfo = (Map<String, Map>) items.get(array[0]).get("contents");

            return subTypeInfo.get(array[1]).get("type").toString().toLowerCase();
        } else {
            return items.get(parameter).get("type").toString().trim().toLowerCase();
        }
    }

    private static String buildScript(Context handler, ModBoard board, Params params) {

        if (!StringUtils.isEmpty(board.getScript())) {
            return board.getScript();
        }

        List<String> selects = new ArrayList<>();
        board.getSelects().forEach(item -> {
            if (item.getSelect()) {
                selects.add(String.format("`%s`.`%s`", board.getSchema(), item.getKey()));
            }
        });

        List<String> sorts = new ArrayList<>();
        board.getSorts().stream()
                .sorted(Comparator.comparingInt(item -> Integer.parseInt(item.getOrder())))
                .forEach(item ->
                        sorts.add(String.format("`%s`.`%s` %s", board.getSchema(), item.getKey(), item.getOrder())));


        Map<String, List<ModBoard.Filters>> group = board.getFilters()
                .stream()
                .collect(Collectors.groupingBy(ModBoard.Filters::getGroup));

        List<List<String>> where = new ArrayList<>();

        group.values().forEach(item -> {
            List<String> and = new ArrayList<>();
            item.forEach(i -> and.add(compiler(i.getKey(), i.getOperator(), i.getParameter())));
            where.add(and);
        });

        if (board.getType() == Constant.API_TYPE_LIST || board.getType() == Constant.API_TYPE_GET) {
            return selectStatement(params, handler.getDomain(), board.getSchema(), selects, where, sorts);
        }

        if (board.getType() == Constant.API_TYPE_COUNT) {
            return selectStatement(params, handler.getDomain(), board.getSchema(), null, where, null);
        }

        return "";
    }

    private static String selectStatement(
            Params params, String db, String schema,
            List<String> selects, List<List<String>> where, List<String> sorts) {

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        if (selects != null && selects.size() > 0) {
            builder.append(StringUtils.join(selects, ","));
        } else {
            builder.append(" COUNT(1) ");
        }

        builder.append(String.format(" FROM `%s`.`%s`", db, schema));

        // 没有指定where，尝试使用_id检索
        if (where == null || where.size() <= 0) {
            builder.append(" WHERE ");

            // 只获取有效的项目
            List<String> list = new ArrayList<>();
            list.add(String.format("`%s`.`valid` = 1", schema));

            // 添加_id条件
            if (params.getId() != null) {
                list.add(String.format("`%s`.`_id` = <%%= _id %%>", schema));
            }

            builder.append(StringUtils.join(list, " AND "));
        }

        if (where != null && where.size() == 1) {
            builder.append(" WHERE ");

            // 只获取有效的项目
            List<String> list = where.get(0);
            list.add(String.format("`%s`.`valid` = 1", schema));

            builder.append(StringUtils.join(list, " AND "));
        }

        if (where != null && where.size() > 1) {
            List<String> or = where.stream()
                    .map(item -> {
                        List<String> list = new ArrayList<>(item);
                        list.add(String.format("`%s`.`valid` = 1", schema));
                        return StringUtils.join(list, " AND ");
                    })
                    .collect(Collectors.toList());

            builder.append(" WHERE ");
            builder.append(StringUtils.join(or, " OR "));
        }

        if (sorts != null && sorts.size() > 0) {
            builder.append(" ORDER BY ");
            builder.append(StringUtils.join(sorts, ","));
        }

        return builder.toString();
    }

    private String insertStatement(String db, String schema) {

        StringBuilder builder = new StringBuilder();

        builder.append(String.format("INSERT INTO `%s`.`%s` (", db, schema));
        builder.append(") VALUES (");
        builder.append(")");

        return builder.toString();
    }

    private static String compiler(String key, String operator, String value) {

        switch (operator) {
            case "$eq":
                return String.format("`%s` = %s", key, value);
            case "$ne":
                return String.format("`%s` <> %s", key, value);
            case "$gt":
                return String.format("`%s`> %s", key, value);
            case "$gte":
                return String.format("`%s` >= %s", key, value);
            case "$lt":
                return String.format("`%s` < %s", key, value);
            case "$lte":
                return String.format("`%s` <= %s", key, value);
        }

        throw new RuntimeException("Core has not yet supported the operator.");
    }

    private static Object reserved(Context handler, String keyword) {

        if ("$uid".equals(keyword)) {
            return handler.uid();
        }

        if ("$corp".equals(keyword)) {
            return handler.code();
        }

        if ("$sysdate".equals(keyword)) {
            return new Date();
        }

        if ("$systime".equals(keyword)) {
            return new Date();
        }

        return null;
    }


    public static <T extends ModCommon> Plural<T> list(Context handler, Class clazz) {
        return list(handler, clazz, null);
    }

    public static <T extends ModCommon> Plural<T> list(Context handler, Class clazz, Params params) {
        return (Plural<T>) SQLRider.call(handler, clazz, "list", params);
    }

    public static <T extends ModCommon> Singular<T> add(Context handler, Class clazz) {
        return add(handler, clazz, null);
    }

    public static <T extends ModCommon> Singular<T> add(Context handler, Class clazz, Params params) {
        return (Singular<T>) SQLRider.call(handler, clazz, "add", params);
    }

    public static <T extends ModCommon> Singular<T> get(Context handler, Class clazz) {
        return get(handler, clazz, null);
    }

    public static <T extends ModCommon> Singular<T> get(Context handler, Class clazz, Params params) {
        return (Singular<T>) SQLRider.call(handler, clazz, "get", params);
    }

    public static Long remove(Context handler, Class clazz) {
        return remove(handler, clazz, null);
    }

    public static Long remove(Context handler, Class clazz, Params params) {
        return (Long) SQLRider.call(handler, clazz, "remove", params);
    }

    public static <T extends ModCommon> Singular<T> update(Context handler, Class clazz) {
        return update(handler, clazz, null);
    }

    public static <T extends ModCommon> Singular<T> update(Context handler, Class clazz, Params params) {
        return (Singular<T>) SQLRider.call(handler, clazz, "update", params);
    }

    public static Long count(Context handler, Class clazz) {
        return count(handler, clazz, null);
    }

    public static Long count(Context handler, Class clazz, Params params) {
        return (Long) SQLRider.call(handler, clazz, "count", params);
    }

    public static <T extends ModCommon> Plural<T> search(Context handler, Class clazz) {
        return search(handler, clazz, null);
    }

    public static <T extends ModCommon> Plural<T> search(Context handler, Class clazz, Document params) {
        throw new UnsupportedOperationException("rider search");
    }

    /**
     * find board by mod class & board method (eg : get,remove etc.)
     *
     * @param clazz  Mod class
     * @param method board method
     * @return board
     */
    private static ModBoard getBoard(Class clazz, String method) {

        // TODO: 支持URL参数，如 /api/user/:id

        String api = String.format("/api/%s/%s",
                WordUtils.uncapitalize(clazz.getSimpleName().replace(MODEL_PREFIX, "")),
                method);

        for (ModBoard board : CacheManager.INSTANCE.getBoards()) {
            if (board.getApi().toLowerCase().equals(api.toLowerCase())) {
                return board;
            }
        }

        throw DataRiderException.BoardNotFound(api);
    }

    private static ModStructure getStruct(String schema) {
        return CacheManager.INSTANCE.getStructures()
                .stream()
                .filter(s -> s.getSchema().equals(schema))
                .findFirst()
                .get();
    }

    /**
     * 获取Entity的类型
     * - name为系统表，或kind为系统api时，返回cn.alphabets.light.entity包下的类型
     * - 否则返回用户包下的类型
     *
     * @param name 表名称
     * @param kind api类型
     * @return Entity类型
     */
    public static Class getEntityType(String name, Long kind) {

        boolean usingLightEntity = Entity.system.contains(name) || Constant.KIND_BOARD_SYSTEM_DATA.equals(kind);

        String packageName = usingLightEntity
                ? Constant.DEFAULT_PACKAGE_NAME + ".entity"
                : Environment.instance().getPackages() + ".entity";

        String className = Constant.MODEL_PREFIX + WordUtils.capitalize(name);

        try {
            return Class.forName(packageName + "." + className);
        } catch (ClassNotFoundException e) {
            try {
                return Class.forName(Constant.DEFAULT_PACKAGE_NAME + ".entity." + className);
            } catch (ClassNotFoundException e1) {
                throw DataRiderException.EntityClassNotFound(packageName + "." + className);
            }
        }
    }

}
