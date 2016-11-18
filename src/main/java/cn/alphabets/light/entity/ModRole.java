package cn.alphabets.light.entity;

import cn.alphabets.light.model.ModBase;
import java.lang.String;
import java.util.List;

/**
 * Generated by the Light platform. Do not manually modify the code.
 */
public class ModRole extends ModBase {
  private String name;

  private String description;

  private List authority;

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

  public List getAuthority() {
    return this.authority;
  }

  public void setAuthority(List authority) {
    this.authority = authority;
  }
}
