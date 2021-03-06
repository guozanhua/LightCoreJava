package cn.alphabets.light.model.datarider;


import cn.alphabets.light.Constant;
import cn.alphabets.light.db.mysql.Controller;
import cn.alphabets.light.entity.ModBoard;
import cn.alphabets.light.entity.ModStructure;
import cn.alphabets.light.exception.DataRiderException;
import cn.alphabets.light.http.Context;
import cn.alphabets.light.http.Params;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQLRider
 * <p>
 * Created by lilin on 2016/11/12.
 */
public class SQLRider extends Rider {

    private static final io.vertx.core.logging.Logger logger = LoggerFactory.getLogger(SQLRider.class);

    /**
     * invoke controller method to perform db operation
     *
     * @param board  board info
     * @param params DBParams
     * @return db operation result
     */
    public Object call(Context handler, Class clazz, ModBoard board, Params params) {

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

    Params adaptToBoard(Context handler, Class clazz, ModBoard board, Params params) {

        String parent = getStruct(board.getSchema()).getParent();

        // 继承表，没有指定type那么添加当前表名为默认的type
        if (!StringUtils.isEmpty(parent)) {
            if (!params.getCondition().containsKey("type")) {
                params.getCondition().put("type", board.getSchema());
            }
            if (!params.getData().containsKey("type")) {
                params.getData().put("type", board.getSchema());
            }
        }

        String script = buildScript(handler, board, params, parent);
        Params newParams = Params.clone(params, script, board.getSchema(), clazz);

        // 进行类型转换（MySQL不支持多层嵌套的数据，所以只简单的对第一层数据进行转换）
        parseType(handler, board, newParams);

        return newParams;
    }

    private void parseType(Context handler, ModBoard board, Params params) {

        TypeConverter converter = new TypeConverter(handler);
        ModStructure struct = getStruct(board.getSchema());

        Document condition = params.getCondition();
        board.getFilters().forEach(filter -> {

            String parameter = filter.getKey();
            String key = filter.getParameter();
            String type = detectValueType(struct, parameter);

            if (condition.containsKey(parameter)) {
                condition.put(key, converter.convert(type, condition.get(key)));
            }
        });

    }

    private String buildScript(Context handler, ModBoard board, Params params, String parent) {

        if (!StringUtils.isEmpty(board.getScript())) {
            return board.getScript();
        }

        String schema = StringUtils.isEmpty(parent) ? board.getSchema() : parent;

        // SELECT TODO: 支持参数中的select
        List<String> selects = new ArrayList<>();
        board.getSelects().forEach(item -> {
            if (item.getSelect()) {
                selects.add(String.format("`%s`.`%s`", schema, item.getKey()));
            }
        });

        // SORT TODO: 支持参数中的sort
        List<String> sorts = new ArrayList<>();
        board.getSorts().stream()
                .sorted(Comparator.comparingInt(item -> Integer.parseInt(item.getOrder())))
                .forEach(item ->
                        sorts.add(String.format("`%s`.`%s` %s", schema, item.getKey(), item.getOrder())));

        // WHERE
        List<List<String>> where = new ArrayList<>();

        Map<String, List<ModBoard.Filters>> group = board.getFilters()
                .stream()
                .filter(item -> params.getCondition().containsKey(item.getParameter()))
                .collect(Collectors.groupingBy(ModBoard.Filters::getGroup));

        group.values().forEach(item -> {
            List<String> and = new ArrayList<>();
            item.forEach(i -> and.add(compiler(schema, i.getKey(), i.getOperator(), i.getParameter())));
            where.add(and);
        });

        // 生成SQL语句
        if (board.getType() == Constant.API_TYPE_LIST || board.getType() == Constant.API_TYPE_GET) {
            return selectStatement(params, handler.getDomain(), parent, schema, selects, where, sorts);
        }

        if (board.getType() == Constant.API_TYPE_COUNT) {
            return selectStatement(params, handler.getDomain(), parent, schema, null, where, null);
        }

        if (board.getType() == Constant.API_TYPE_ADD) {
            return insertStatement(params, handler.getDomain(), parent, board.getSchema());
        }

        if (board.getType() == Constant.API_TYPE_UPDATE) {
            return updateStatement(params, handler.getDomain(), parent, schema, where);
        }

        if (board.getType() == Constant.API_TYPE_REMOVE) {
            return deleteStatement(params, handler.getDomain(), parent, schema, where);
        }

        logger.warn("Type is not recognized");
        return "";
    }

    private String selectStatement(
            Params params, String db, String parent, String schema,
            List<String> selects, List<List<String>> where, List<String> sorts) {

        StringBuilder builder = new StringBuilder();

        builder.append("SELECT ");

        // 没有指定select项目，则通过count(1)获取件数
        if (selects != null && selects.size() > 0) {
            builder.append(StringUtils.join(selects, ","));
        } else {
            builder.append(" COUNT(1) AS COUNT ");
        }

        builder.append(String.format(" FROM `%s`.`%s`", db, schema));
        builder.append(getWhere(params, parent, schema, where));

        // 排序
        if (sorts != null && sorts.size() > 0) {
            builder.append(" ORDER BY ");
            builder.append(StringUtils.join(sorts, ","));
        }

        // 行数限制
        if (params.getSkip() > 0 || params.getLimit() > 0) {
            builder.append(String.format(" LIMIT %d OFFSET %d ",
                    params.getLimit() <= 0 ? Integer.MAX_VALUE : params.getLimit(),
                    params.getSkip() < 0 ? 0 : params.getSkip()));
        }

        return builder.toString();
    }

    private String getWhere(Params params, String parent, String schema, List<List<String>> where) {

        StringBuilder builder = new StringBuilder();

        // 没有指定where，尝试使用_id检索
        if (where == null || where.size() <= 0) {
            builder.append(" WHERE ");

            // 只获取有效的项目
            List<String> list = new ArrayList<>();
            list.add(String.format("`%s`.`valid` = 1", schema));

            // 添加_id条件
            if (params.getId() != null) {
                list.add(String.format("`%s`.`_id` = <%%= condition._id %%>", schema));
            }

            builder.append(StringUtils.join(list, " AND "));
        }

        // 没有OR条件，所有项目用 AND 连接
        if (where != null && where.size() == 1) {
            builder.append(" WHERE ");

            // 只获取有效的项目
            List<String> list = where.get(0);
            list.add(String.format("`%s`.`valid` = 1", schema));

            // 有父表，添加type条件
            if (!StringUtils.isEmpty(parent)) {
                list.add(String.format("`%s`.`type` = <%%= condition.type %%>", schema));
            }

            builder.append(StringUtils.join(list, " AND "));
        }

        // 有OR条件，所有项目先用AND连接，然后再用OR连接
        if (where != null && where.size() > 1) {
            List<String> or = where.stream()
                    .map(item -> {

                        List<String> list = new ArrayList<>(item);

                        // 只获取有效的项目
                        list.add(String.format("`%s`.`valid` = 1", schema));

                        // 有父表，添加type条件
                        if (!StringUtils.isEmpty(parent)) {
                            list.add(String.format("`%s`.`type` = <%%= condition.type %%>", schema));
                        }

                        return StringUtils.join(list, " AND ");
                    })
                    .collect(Collectors.toList());

            builder.append(" WHERE ");
            builder.append(StringUtils.join(or, " OR "));
        }

        return builder.toString();
    }

    private String insertStatement(Params params, String db, String parent, String schema) {

        ModStructure structure = getStruct(schema);
        Map<String, Map<String, String>> items = ((Map<String, Map<String, String>>) structure.getItems());
        Set<String> keys = items.keySet();

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("INSERT INTO `%s`.`%s` (", db, StringUtils.isEmpty(parent) ? schema : parent));

        // INSERT语句 字段定义 （只做成字段值在data里存在，并且字段不等于_id的项目）
        final List<String> column = new ArrayList<>();
        column.add("`_id`");
        column.add("`createAt`");
        column.add("`createBy`");
        column.add("`updateAt`");
        column.add("`updateBy`");
        column.add("`valid`");

        keys.stream()
                .filter(item -> params.getData().containsKey(item))
                .forEach(item -> column.add(String.format("`%s`", item)));
        builder.append((StringUtils.join(column, ",")));

        builder.append(") VALUES (");

        // INSERT语句 值定义
        final List<String> value = new ArrayList<>();
        value.add("<%= data._id %>");
        value.add("<%= data.createAt %>");
        value.add("<%= data.createBy %>");
        value.add("<%= data.updateAt %>");
        value.add("<%= data.updateBy %>");
        value.add("<%= data.valid %>");

        keys.stream()
                .filter(item -> params.getData().containsKey(item))
                .forEach(item -> value.add(String.format("<%%= data.%s %%>", item)));

        builder.append(StringUtils.join(value, ","));
        builder.append(")");

        return builder.toString();
    }

    private String updateStatement(Params params, String db, String parent, String schema, List<List<String>> where) {

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("UPDATE `%s`.`%s` SET ", db, schema));

        // UPDATE语句 字段定义 （只做成字段值在data里存在，并且字段不等于_id的项目）
        final List<String> column = new ArrayList<>();
        column.add("`updateAt` = <%= data.updateAt %>");
        column.add("`updateBy` = <%= data.updateBy %>");

        // TODO: 应该像INSERT一样使用schema的字段生成更新项，否则data里如果存在不相关的数据时会构建出错误的SQL
        params.getData().keySet()
                .stream().filter(item -> params.getData().get(item) != null)
                .forEach(item -> column.add(String.format("`%s` = <%%= data.%s %%>", item, item)));
        builder.append((StringUtils.join(column, ",")));

        builder.append(getWhere(params, parent, schema, where));
        return builder.toString();
    }

    private String deleteStatement(Params params, String db, String parent, String schema, List<List<String>> where) {

        // return String.format("DELETE FROM `%s`.`%s` ", db, schema) + getWhere(params, schema, where);

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("UPDATE `%s`.`%s` SET ", db, schema));

        // UPDATE语句 字段定义 （只做成字段值在data里存在，并且字段不等于_id的项目）
        final List<String> column = Arrays.asList(
                "`updateAt` = <%= data.updateAt %>",
                "`updateBy` = <%= data.updateBy %>",
                "`valid` = <%= data.valid %>"
        );

        builder.append((StringUtils.join(column, ",")));
        builder.append(getWhere(params, parent, schema, where));
        return builder.toString();
    }

    private String compiler(String schema, String key, String operator, String value) {

        switch (operator) {
            case "$eq":
                return String.format("`%s`.`%s` = <%%= condition.%s %%>", schema, key, value);
            case "$ne":
                return String.format("`%s`.`%s` <> <%%= condition.%s %%>", schema, key, value);
            case "$gt":
                return String.format("`%s`.`%s`> <%%= condition.%s %%>", schema, key, value);
            case "$gte":
                return String.format("`%s`.`%s` >= <%%= condition.%s %%>", schema, key, value);
            case "$lt":
                return String.format("`%s`.`%s` < <%%= condition.%s %%>", schema, key, value);
            case "$lte":
                return String.format("`%s`.`%s` <= <%%= condition.%s %%>", schema, key, value);
            case "$regex":
                return String.format("`%s`.`%s` REGEXP <%%= condition.%s %%>", schema, key, value);
            case "$in":
                return String.format("`%s`.`%s` IN <%%= condition.%s %%>", schema, key, value);
            case "$nin":
                return String.format("`%s`.`%s` NOT IN <%%= condition.%s %%>", schema, key, value);
            case "$exists":
                return String.format("`%s`.`%s` IS NOT NULL", schema, key);
        }

        throw new RuntimeException("Core has not yet supported the operator.");
    }
}
