package cn.alphabets.light.entity;

import cn.alphabets.light.model.ModBase;
import cn.alphabets.light.model.deserializer.LongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.lang.Long;
import java.lang.String;

/**
 * Generated by the Light platform. Do not manually modify the code.
 */
public class ModPlace extends ModBase {
  @JsonDeserialize(
      using = LongDeserializer.class
  )
  private Long invisible;

  private String name;

  private String parent;

  @JsonDeserialize(
      using = LongDeserializer.class
  )
  private Long sort;

  public Long getInvisible() {
    return this.invisible;
  }

  public void setInvisible(Long invisible) {
    this.invisible = invisible;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getParent() {
    return this.parent;
  }

  public void setParent(String parent) {
    this.parent = parent;
  }

  public Long getSort() {
    return this.sort;
  }

  public void setSort(Long sort) {
    this.sort = sort;
  }
}
