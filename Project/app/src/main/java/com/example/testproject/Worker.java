package com.example.testproject;

import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Worker implements Serializable {

    protected String m_email;
    protected String m_username;
    protected View.OnClickListener m_click;

    public Worker(String email, String username, View.OnClickListener click){
        m_email = email;
        m_username = username;
        m_click = click;
    }

    public String getEmail() { return m_email; }

    public String getUsername() {
        return m_username;
    }

    public View.OnClickListener getClick() {
        return m_click;
    }

}
