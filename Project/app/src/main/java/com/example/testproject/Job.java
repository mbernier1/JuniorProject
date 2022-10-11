package com.example.testproject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Job implements Serializable {

    public String jobid;
    public String email;
    public String username;
    public String description;
    public String tags;
    public String category;
    public String reward;
    public String jobTitle;
    public int status;
    public int day;
    public int month;
    public int year;

    public Job(JSONObject object){
        try {
            jobid = object.getString("jobid");
            email = object.getString("email");
            username = object.getString("username");
            description = object.getString("description");
            tags = object.getString("tags");
            category = object.getString("category");
            reward = object.getString("reward");
            jobTitle = object.getString("jobTitle");
            status = object.getInt("status");
            day = object.getInt("day");
            month = object.getInt("month");
            year = object.getInt("year");
        }
     catch (JSONException e) {
        e.printStackTrace();
        }
    }

    public String getJobId() {
        return jobid;
    }

    public String getEmail() { return email; }

    public String getUsername() {
        return username;
    }

    public String getDescription() {
        return description;
    }

    public String getTags() {
        return tags;
    }

    public String getCategory() {
        return category;
    }

    public String getReward() {
        return reward;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public int getStatus() { return status; }

    public String getDate()
    {
        if (day == 0 || year == 0) return "No date";
        return (month+1) + "/" + day + "/" + year;
    }
}
