package cn.alphabets.light.entity;

import cn.alphabets.light.model.Entity;
import cn.alphabets.light.model.ModCommon;
import cn.alphabets.light.model.deserializer.DateDeserializer;
import cn.alphabets.light.model.deserializer.LongDeserializer;
import cn.alphabets.light.model.serializer.DateSerializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.Boolean;
import java.lang.Long;
import java.lang.String;
import java.util.Date;
import java.util.List;

/**
 * Generated by the Light platform. Do not manually modify the code.
 */
public class ModTest extends ModCommon {
  private String name;

  private String description;

  @JsonDeserialize(
      using = LongDeserializer.class
  )
  private Long age;

  @JsonDeserialize(
      using = DateDeserializer.class
  )
  @JsonSerialize(
      using = DateSerializer.class
  )
  private Date birthday;

  private Boolean developer;

  @JsonProperty("extends")
  private List<Extends> extends_;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getAge() {
    return this.age;
  }

  public void setAge(Long age) {
    this.age = age;
  }

  public Date getBirthday() {
    return this.birthday;
  }

  public void setBirthday(Date birthday) {
    this.birthday = birthday;
  }

  public Boolean getDeveloper() {
    return this.developer;
  }

  public void setDeveloper(Boolean developer) {
    this.developer = developer;
  }

  public List<Extends> getExtends_() {
    return this.extends_;
  }

  public void setExtends_(List<Extends> extends_) {
    this.extends_ = extends_;
  }

  @JsonIgnoreProperties(
      ignoreUnknown = true
  )
  public static final class Extends extends Entity {
    private String address;

    public String getAddress() {
      return this.address;
    }

    public void setAddress(String address) {
      this.address = address;
    }
  }
}
