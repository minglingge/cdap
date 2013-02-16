package com.continuuity.passport.core.meta;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 */
public class VPC {

  private final int vpcId ;

  private final String vpcName;

  public VPC(String vpcName) {
    this(-1,vpcName);
  }
  public VPC(int vpcId, String vpcName) {
    this.vpcId = vpcId;
    this.vpcName = vpcName;
  }

  public int getVpcId() {
    return vpcId;
  }

  public String getVpcName() {
    return vpcName;
  }
  @Override
  public String toString() {
    Gson gson = new GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .create();
    return (gson.toJson(this));
  }
}
