package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import org.json.JSONException;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class EditProfileScreen extends AppCompatActivity {

    public static  final String API_URL = "https://boiling-temple-46468.herokuapp.com";

    EditText FirstNameEditText;
    EditText LastNameEditText;
    EditText BioEditText;
    EditText PasswordEditText;
    Button SubmitButton;
    TextView ErrorTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_screen);


        FirstNameEditText = findViewById(R.id.FirstNameEditText);
        LastNameEditText = findViewById(R.id.LastNameEditText);
        BioEditText = findViewById(R.id.BioEditText);
        PasswordEditText = findViewById(R.id.PasswordEditText);

        SubmitButton = (Button)findViewById(R.id.SubmitButton);
        ErrorTextView = findViewById(R.id.ErrorTextView);


        SharedPreferences sp = getApplicationContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        APIDataService service = new APIDataService(EditProfileScreen.this);
        String tempURL = "https://boiling-temple-46468.herokuapp.com/profile/readProfile";
        String mainURL = "https://boiling-temple-46468.herokuapp.com/profile/updateProfile";
        String checkURL = API_URL + "/users/checkpass";

        AtomicReference<Float> employerrating = new AtomicReference<>((float) 0);
        AtomicReference<Float> workerrating = new AtomicReference<>((float) 0);

        HashMap<String, String> obj = new HashMap<>();
        obj.put("email", sp.getString("email",""));

        //  puts what is already there in the textboxes
        service.callAPIURL(tempURL, obj, Request.Method.GET, response -> {
            try {
                BioEditText.setText(response.getString("bio"));
                FirstNameEditText.setText(response.getString("firstname"));
                LastNameEditText.setText(response.getString("lastname"));
                employerrating.set(Float.valueOf(response.getString("employerrating")));
                workerrating.set(Float.valueOf(response.getString("workerrating")));

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(EditProfileScreen.this, "Error loading editor.", Toast.LENGTH_LONG).show();
            }
        });


        SubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SubmitButton.setEnabled(false);
                //checkpass
                HashMap<String, String> checkpassParams = new HashMap<>();
                checkpassParams.put("email", sp.getString("email", ""));
                checkpassParams.put("password", PasswordEditText.getText().toString());

                service.callAPIURL(checkURL, checkpassParams, Request.Method.GET, response -> {
                    try
                    {
                        String checkpasssuccess = response.getString("success");
                        // If success is true, move to Profile page
                        if (checkpasssuccess.equals("true")) {
                            HashMap<String, String> uploadParams = new HashMap<>();

                            uploadParams.put("email", sp.getString("email", ""));

                            uploadParams.put("username", sp.getString("username", ""));
                            uploadParams.put("firstname", FirstNameEditText.getText().toString());
                            uploadParams.put("lastname", LastNameEditText.getText().toString());
                            uploadParams.put("bio", BioEditText.getText().toString());
                            uploadParams.put("employerrating", employerrating.toString());
                            uploadParams.put("workerrating", workerrating.toString());

                            service.callAPIJSON(mainURL, uploadParams, Request.Method.PUT, responseUpload -> {
                                SubmitButton.setEnabled(true);
                                try {
                                    String success = responseUpload.getString("success");
                                    if (success.equals("true")) {
                                        Toast.makeText(EditProfileScreen.this, "Profile Edit Successful", Toast.LENGTH_SHORT).show();
                                        Intent startIntent = new Intent(getApplicationContext(), ProfileScreen.class);
                                        startActivity(startIntent);
                                    } else {
                                        Toast.makeText(EditProfileScreen.this, "Error updating profile", Toast.LENGTH_LONG).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }


                            });
                        }
                        else
                        {
                            SubmitButton.setEnabled(true);
                            ErrorTextView.setText("Invalid Password");
                            ErrorTextView.setTextColor(Color.RED);
                            ErrorTextView.setVisibility(View.VISIBLE);
                        }
                    }
                    catch (JSONException e)
                    {
                        e.printStackTrace();
                    }
                });

            }
        });
    }
}