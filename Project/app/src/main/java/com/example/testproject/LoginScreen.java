package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import org.json.JSONException;

import java.util.HashMap;

// These are all new and are for the bad code

public class LoginScreen extends AppCompatActivity {

    //  Make this global or something
    public static final String API_URL = "https://boiling-temple-46468.herokuapp.com";

    Button loginButton;
    EditText userSignInName;
    EditText userPassword;
    TextView loginError;
    SharedPreferences sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        loginButton = findViewById(R.id.logInButton);
        userSignInName = findViewById(R.id.userSignInName);
        userPassword = findViewById(R.id.userPassword);
        loginError = findViewById(R.id.LoginError);

        APIDataService service = new APIDataService(LoginScreen.this);

        String tempURL2 = API_URL + "/users/checkpass";
        String tempURL = API_URL + "/profile/readProfile";

        sp = getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email;

                loginButton.setEnabled(false);

                // Populate the parameters of a request (GET/POST, same process)
                HashMap<String, String> obj = new HashMap<String, String>();
                email = userSignInName.getText().toString();
                obj.put("email", email);
                obj.put("password", userPassword.getText().toString());
                SharedPreferences.Editor editor = sp.edit();

                // Calls a GET request
                service.callAPIURL(tempURL2, obj, Request.Method.GET, response1 -> {
                    loginButton.setEnabled(true);
                    try{
                        String success = response1.getString("success");
                        // If success is true, move to Profile page
                        if (success.equals("true")){
                            editor.putString("email", email);
                            editor.commit();
                            service.callAPIURL(tempURL, obj, Request.Method.GET, response -> {
                                try {
                                    editor.putString("username", response.getString("username"));
                                    editor.commit();
                                    Intent startIntent = new Intent(getApplicationContext(), MyJobsScreen.class);
                                    startActivity(startIntent);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            loginError.setVisibility(View.VISIBLE);
                        }

                    } catch(JSONException e){
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}