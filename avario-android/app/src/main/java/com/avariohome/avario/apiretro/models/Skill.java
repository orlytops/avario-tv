package com.avariohome.avario.apiretro.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 9/6/17.
 */

public class Skill {

  private boolean isChecked;

  @SerializedName("name")
  private String name;


  /*
  * return skill name
  * */
  public String getName() {
    return name;
  }

  public boolean isChecked() {
    return isChecked;
  }

  public void setChecked(boolean checked) {
    isChecked = checked;
  }
}
