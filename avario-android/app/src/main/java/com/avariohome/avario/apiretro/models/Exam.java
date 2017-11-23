package com.avariohome.avario.apiretro.models;

import com.google.gson.annotations.SerializedName;

/**
 * Created by orly on 9/6/17.
 */

public class Exam {

  @SerializedName("name")
  private String name;

  @SerializedName("progress")
  private int progress;


  /*
  * return exam name
  * */
  public String getName() {
    return name;
  }

  /*
  * return exam progress
  * */
  public int getProgress() {
    return progress;
  }
}
