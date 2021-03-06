package cn.alphabets.light.entity;

import cn.alphabets.light.model.ModCommon;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.String;

/**
 * Generated by the Light platform. Do not manually modify the code.
 */
public class ModRoute extends ModCommon {
  private String url;

  private String template;

  private String action;

  @JsonProperty("class")
  private String class_;

  private String description;

  private String version;

  public String getUrl() {
    return this.url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTemplate() {
    return this.template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }

  public String getAction() {
    return this.action;
  }

  public void setAction(String action) {
    this.action = action;
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

  public String getVersion() {
    return this.version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
