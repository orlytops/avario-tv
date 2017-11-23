package com.avariohome.avario.apiretro.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by orly on 9/6/17.
 */

public class User {

  @SerializedName("username")
  private String userName;

  @SerializedName("bidperiod")
  private int bidPeriod;

  @SerializedName("about")
  private String about;

  @SerializedName("skills")
  private List<Skill> skills;

  @SerializedName("exams")
  private List<Exam> exams;

  @SerializedName("avatar")
  private String avatar;

  /**
   * @return The user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @return The bid period
   */
  public int getBidPeriod() {
    return bidPeriod;
  }

  /**
   * @return skills
   */
  public List<Skill> getSkills() {
    return skills;
  }

  /**
   * @return the exams
   */
  public List<Exam> getExams() {
    return exams;
  }

  /*
  * return the user avatar
  * */
  public String getAvatar() {
    return avatar;
  }

  public String getAbout() {
    return about;
  }
}
