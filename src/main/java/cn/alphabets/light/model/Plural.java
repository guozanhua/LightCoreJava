package cn.alphabets.light.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * Plural 返回多个值
 * <p>
 * Created by lilin on 2016/11/13.
 */
@JsonPropertyOrder(alphabetic = true)
public class Plural<T extends ModCommon> {

    public Long totalItems;
    public List<T> items;
    public Document options;

    public Plural(List<T> items) {
        this((long) items.size(), items, null);
    }

    public Plural(Long totalItems, List<T> items) {
        this(totalItems, items, null);
    }

    public Plural(Long totalItems, List<T> items, Document options) {
        this.totalItems = totalItems;
        this.items = items;
        this.options = options;
    }
}
