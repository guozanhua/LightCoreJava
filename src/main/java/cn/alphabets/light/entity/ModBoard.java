package cn.alphabets.light.entity;

import cn.alphabets.light.model.Entity;
import cn.alphabets.light.model.ModBase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.Boolean;
import java.lang.Long;
import java.lang.String;
import java.util.List;

/**
 * Generated by the Light platform. Do not manually modify the code.
 */
public class ModBoard extends ModBase {
  private String action;

  private String path;

  private List<Filters> filters;

  private Long kind;

  private Long type;

  private List<Selects> selects;

  private List<Sorts> sorts;

  private String api;

  @JsonProperty("class")
  private String class_;

  private String description;

  private String reserved;

  private String schema;

  private String script;

  public String getAction() {
    return this.action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getPath() {
    return this.path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<Filters> getFilters() {
    return this.filters;
  }

  public void setFilters(List<Filters> filters) {
    this.filters = filters;
  }

  public Long getKind() {
    return this.kind;
  }

  public void setKind(Long kind) {
    this.kind = kind;
  }

  public Long getType() {
    return this.type;
  }

  public void setType(Long type) {
    this.type = type;
  }

  public List<Selects> getSelects() {
    return this.selects;
  }

  public void setSelects(List<Selects> selects) {
    this.selects = selects;
  }

  public List<Sorts> getSorts() {
    return this.sorts;
  }

  public void setSorts(List<Sorts> sorts) {
    this.sorts = sorts;
  }

  public String getApi() {
    return this.api;
  }

  public void setApi(String api) {
    this.api = api;
  }

  public String getClass_() {
    return this.class_;
  }

  public void setClass_(String class_) {
    this.class_ = class_;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getReserved() {
    return this.reserved;
  }

  public void setReserved(String reserved) {
    this.reserved = reserved;
  }

  public String getSchema() {
    return this.schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getScript() {
    return this.script;
  }

  public void setScript(String script) {
    this.script = script;
  }

  @JsonIgnoreProperties(
      ignoreUnknown = true
  )
  public static final class Filters extends Entity {
    @JsonProperty("default")
    private String default_;

    private String group;

    private String key;

    private String operator;

    private String parameter;

    private String type;

    public String getDefault_() {
      return this.default_;
    }

    public void setDefault_(String default_) {
      this.default_ = default_;
    }

    public String getGroup() {
      return this.group;
    }

    public void setGroup(String group) {
      this.group = group;
    }

    public String getKey() {
      return this.key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getOperator() {
      return this.operator;
    }

    public void setOperator(String operator) {
      this.operator = operator;
    }

    public String getParameter() {
      return this.parameter;
    }

    public void setParameter(String parameter) {
      this.parameter = parameter;
    }

    public String getType() {
      return this.type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

  @JsonIgnoreProperties(
      ignoreUnknown = true
  )
  public static final class Selects extends Entity {
    private String key;

    private String link;

    private String option;

    private Boolean select;

    private String alias;

    private List fields;

    private String format;

    private String type;

    private Boolean readonly;

    private Boolean display;

    private Long reserved;

    public String getKey() {
      return this.key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getLink() {
      return this.link;
    }

    public void setLink(String link) {
      this.link = link;
    }

    public String getOption() {
      return this.option;
    }

    public void setOption(String option) {
      this.option = option;
    }

    public Boolean getSelect() {
      return this.select;
    }

    public void setSelect(Boolean select) {
      this.select = select;
    }

    public String getAlias() {
      return this.alias;
    }

    public void setAlias(String alias) {
      this.alias = alias;
    }

    public List getFields() {
      return this.fields;
    }

    public void setFields(List fields) {
      this.fields = fields;
    }

    public String getFormat() {
      return this.format;
    }

    public void setFormat(String format) {
      this.format = format;
    }

    public String getType() {
      return this.type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Boolean getReadonly() {
      return this.readonly;
    }

    public void setReadonly(Boolean readonly) {
      this.readonly = readonly;
    }

    public Boolean getDisplay() {
      return this.display;
    }

    public void setDisplay(Boolean display) {
      this.display = display;
    }

    public Long getReserved() {
      return this.reserved;
    }

    public void setReserved(Long reserved) {
      this.reserved = reserved;
    }
  }

  @JsonIgnoreProperties(
      ignoreUnknown = true
  )
  public static final class Sorts extends Entity {
    private String key;

    private String order;

    private String dynamic;

    private Long index;

    public String getKey() {
      return this.key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getOrder() {
      return this.order;
    }

    public void setOrder(String order) {
      this.order = order;
    }

    public String getDynamic() {
      return this.dynamic;
    }

    public void setDynamic(String dynamic) {
      this.dynamic = dynamic;
    }

    public Long getIndex() {
      return this.index;
    }

    public void setIndex(Long index) {
      this.index = index;
    }
  }
}
