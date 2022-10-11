package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;

import org.json.JSONException;
import org.w3c.dom.Text;

import java.util.HashMap;

public class RegistrationScreen extends AppCompatActivity {

    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 32;
    private static final String API_URL = "https://boiling-temple-46468.herokuapp.com";

    private boolean checkEmail(String email)
    {
        if (email.lastIndexOf('@') < 0) return false;

        // Return true if . exists and it's not the last char, otherwise return false
        int periodLocation = email.lastIndexOf('.');
        return periodLocation >= 0 && email.length() - 1 != periodLocation;
    }

    private boolean compareText(String current, String previous)
    {
        return current.equals(previous);
    }

    private boolean meetsPasswordRequirements(String pass)
    {
        int length = pass.length();
        if (length < PASSWORD_MIN) return false; // too short
        if (length > PASSWORD_MAX) return false; // too long
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_screen);

        Button profileActivity = (Button)findViewById(R.id.SubmitButton);
        EditText FirstNameEditText = (EditText)findViewById(R.id.FirstNameEditText);
        EditText LastNameEditText = (EditText)findViewById(R.id.LastNameEditText);
        EditText UsernameEditText = (EditText)findViewById(R.id.UsernameEditText);
        EditText EmailEditText = (EditText)findViewById(R.id.EmailEditText);
        EditText EmailConfirmEditText = (EditText)findViewById(R.id.EmailConfirmEditText);
        EditText PasswordEditText = (EditText)findViewById(R.id.PasswordEditText);
        EditText PasswordConfirmEditText = (EditText)findViewById(R.id.PasswordConfirmEditText);
        SharedPreferences sp = getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        EmailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean areEqual = compareText(s.toString(), EmailEditText.getText().toString());
                if (!areEqual) {
                    EmailConfirmEditText.setTextColor(Color.RED);
                }
                else
                {
                    EmailConfirmEditText.setTextColor(Color.BLACK);
                }
            }

        });
        EmailConfirmEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean areEqual = compareText(s.toString(), EmailEditText.getText().toString());
                if (!areEqual) {
                    EmailConfirmEditText.setTextColor(Color.RED);
                }
                else
                {
                    EmailConfirmEditText.setTextColor(Color.BLACK);
                }
            }
        });
        PasswordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean areEqual = compareText(s.toString(), PasswordConfirmEditText.getText().toString());
                if (!areEqual) {
                    PasswordConfirmEditText.setTextColor(Color.RED);
                }
                else
                {
                    PasswordConfirmEditText.setTextColor(Color.BLACK);
                }
            }
        });
        PasswordConfirmEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean areEqual = compareText(s.toString(), PasswordEditText.getText().toString());
                if (!areEqual) {
                    PasswordConfirmEditText.setTextColor(Color.RED);
                }
                else
                {
                    PasswordConfirmEditText.setTextColor(Color.BLACK);
                }
            }
        });

        profileActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                boolean confirmEmail = compareText(EmailEditText.getText().toString(), EmailConfirmEditText.getText().toString());
                boolean confirmPassword = compareText(PasswordEditText.getText().toString(), PasswordConfirmEditText.getText().toString());
                boolean passMeets = meetsPasswordRequirements(PasswordEditText.getText().toString());
                boolean emailFormat = checkEmail(EmailEditText.getText().toString());


                if (confirmEmail && confirmPassword && passMeets && emailFormat)
                {
                    profileActivity.setEnabled(false);

                    // API Call URL
                    String createUsers = "/users/createUsers";
                    String checkUnique = "/users/checkUniqueUsername";

                    // API Call Parameters
                    HashMap<String, String> obj = new HashMap<String, String>();
                    HashMap<String, String> checkObj = new HashMap<String, String>();

                    String email = EmailEditText.getText().toString();

                    obj.put("email", email);
                    obj.put("username", UsernameEditText.getText().toString());
                    checkObj = obj;
                    obj.put("firstName", FirstNameEditText.getText().toString());
                    obj.put("lastName", LastNameEditText.getText().toString());
                    obj.put("password", PasswordEditText.getText().toString());
                    SharedPreferences.Editor editor = sp.edit();


                    APIDataService apiCaller = new APIDataService(RegistrationScreen.this);

                    apiCaller.callAPIURL(API_URL + checkUnique, checkObj, Request.Method.GET, response -> {
                        try {
                            String foundEmail = response.getString("foundEmail");
                            String foundUsername = response.getString("foundUsername");
                            boolean failed = false;
                            if (foundEmail.equals("true"))
                            {
                                failed = true;
                                Toast.makeText(RegistrationScreen.this, "Email has already been used on OddJobs. Try another one!", Toast.LENGTH_SHORT).show();
                            }
                            if (foundUsername.equals("true"))
                            {
                                failed = true;
                                Toast.makeText(RegistrationScreen.this, "Username has already been used on OddJobs. Try another one!", Toast.LENGTH_SHORT).show();
                            }
                            if (!failed)
                            {
                                apiCaller.callAPIJSON(API_URL + createUsers, obj, Request.Method.POST, response1 -> {
                                    profileActivity.setEnabled(false);
                                    try{
                                        String success = response1.getString("success");
                                        // If success is true, move to Profile page
                                        if (success.equals("true")){
                                            editor.putString("email", email);
                                            editor.commit();
                                            Intent startIntent = new Intent(getApplicationContext(), ProfileScreen.class);
                                            startActivity(startIntent);
                                        } else {
                                            Toast.makeText(RegistrationScreen.this, "Registration failed", Toast.LENGTH_SHORT).show();
                                        }

                                    } catch(JSONException e){
                                        e.printStackTrace();
                                    }
                                });
                            }
                            failed = false;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                }
                else if (!confirmEmail)
                {
                    Toast.makeText(RegistrationScreen.this, "Confirmed Email does not match.", Toast.LENGTH_LONG).show();
                }
                else if (!confirmPassword)
                {
                    Toast.makeText(RegistrationScreen.this, "Confirmed Password does not match.", Toast.LENGTH_LONG).show();
                }
                else if (!emailFormat)
                {
                    Toast.makeText(RegistrationScreen.this, "Email must be a real address.", Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(RegistrationScreen.this, "Password must be greater than " + PASSWORD_MIN + " characters and less than " + PASSWORD_MAX + " characters.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}