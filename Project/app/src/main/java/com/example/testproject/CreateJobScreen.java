package com.example.testproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;

import org.json.JSONException;
import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CreateJobScreen extends AppCompatActivity {

    public static final String CATEGORY_NOT_SELECTED = "category";

    // Ensure that requirements for posting a job are met
    // Passing strings not textviews for hopefully less tightly coupled
    private boolean checkJobTitle(String title)
    {
        // Ensure a title is entered
        if (title.length() == 0) return false;

        return true;
    }

    private boolean checkJobCategory(String category)
    {
        // Ensure a category is selected
        if (category.equals(MainActivity.categoryStrings[0])) return false;

        return true;
    }

    private boolean checkJobTags(String tags)
    {
        // Ensure a tag is found in tags string
        int found = tags.indexOf('#');

        // If didn't find a hashtag and the length is greater than 0, return false
        if (found < 0 && tags.length() > 0) return false;

        return true;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_job_screen);

        TextView JobName = (EditText)findViewById(R.id.jobname);
        Spinner CategorySpinner = (Spinner) findViewById(R.id.categoryS);

        ArrayAdapter<String>adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,MainActivity.categoryStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        CategorySpinner.setAdapter(adapter);

        TextView Tags = (EditText)findViewById(R.id.tags);
        TextView Description = (EditText)findViewById(R.id.description);
        TextView Reward = (EditText)findViewById(R.id.reward);

        TextView Address = (EditText)findViewById(R.id.address);


        Button acceptButton = findViewById(R.id.acceptbutton);
        Button acceptButton2 = findViewById(R.id.acceptbutton2);

        DatePicker jobDatePicker = findViewById(R.id.jobDatePicker);

        //Preventing earlier dates than the current date
        jobDatePicker.setMinDate(System.currentTimeMillis());

        APIDataService service = new APIDataService(CreateJobScreen.this);
        String tempURL = "https://boiling-temple-46468.herokuapp.com/job/createJobLocation";

        SharedPreferences sp = getApplicationContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);



        //LOCATION ACCEPT
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptButton.setEnabled(false);
                acceptButton2.setEnabled(false);

                HashMap<String,String> paramsPass = new HashMap<>();

                String title = JobName.getText().toString();
                String categories = CategorySpinner.getSelectedItem().toString();
                String tags = Tags.getText().toString();

                if (!checkJobTitle(title))
                {
                    Toast.makeText(CreateJobScreen.this, "Please enter a title for your job.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }
                if (!checkJobCategory(categories))
                {
                    Toast.makeText(CreateJobScreen.this, "Jobs are required to have a category, select one from the drop down menu.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }
                if (!checkJobTags(tags))
                {
                    Toast.makeText(CreateJobScreen.this, "Any entered tags must have a '#' i.e. '#mowing'.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }

                paramsPass.put("email", sp.getString("email",""));
                paramsPass.put("username", sp.getString("username", ""));
                paramsPass.put("description", Description.getText().toString());
                paramsPass.put("tags", Tags.getText().toString());
                paramsPass.put("categories", categories);
                paramsPass.put("jobName", JobName.getText().toString());
                paramsPass.put("reward", Reward.getText().toString());
                paramsPass.put("dayOfJob", String.valueOf(jobDatePicker.getDayOfMonth()));
                paramsPass.put("monthOfJob", String.valueOf(jobDatePicker.getMonth()));
                paramsPass.put("yearOfJob", String.valueOf(jobDatePicker.getYear()));

                if(MainActivity.currentLatLng != null) {
                    paramsPass.put("latitude", Double.toString(MainActivity.currentLatLng.latitude));
                    paramsPass.put("longitude", Double.toString(MainActivity.currentLatLng.longitude));
                }
                else
                {
                    Toast.makeText(CreateJobScreen.this, "Unable to locate device, please use custom address.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }

                service.callAPIJSON(tempURL, paramsPass, Request.Method.POST, response -> {
                    try {
                        String success = response.getString("success");
                        if(success.equals("true")){
                            Toast.makeText(CreateJobScreen.this, "Job Posted Successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else {
                            Toast.makeText(CreateJobScreen.this, "Error Posting Job... try again later.", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                });
            }
        });

        //ADDRESS ACCEPT
        acceptButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptButton.setEnabled(false);
                acceptButton2.setEnabled(false);
                HashMap<String,String> paramsPass = new HashMap<>();

                String title = JobName.getText().toString();
                String categories = CategorySpinner.getSelectedItem().toString();
                String tags = Tags.getText().toString();
                String addresst = Address.getText().toString();

                if (!checkJobTitle(title))
                {
                    Toast.makeText(CreateJobScreen.this, "Please enter a title for your job.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }
                if (!checkJobTitle(addresst))
                {
                    Toast.makeText(CreateJobScreen.this, "Please enter an address for your job.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }
                if (!checkJobCategory(categories))
                {
                    Toast.makeText(CreateJobScreen.this, "Jobs are required to have a category, select one from the drop down menu.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }
                if (!checkJobTags(tags))
                {
                    Toast.makeText(CreateJobScreen.this, "Any entered tags must have a '#' i.e. '#mowing'.", Toast.LENGTH_SHORT).show();
                    acceptButton2.setEnabled(true);
                    acceptButton.setEnabled(true);
                    return;
                }

                paramsPass.put("email", sp.getString("email",""));
                paramsPass.put("username", sp.getString("username", ""));
                paramsPass.put("description", Description.getText().toString());
                paramsPass.put("tags", Tags.getText().toString());
                paramsPass.put("categories",categories);
                paramsPass.put("jobName", JobName.getText().toString());
                paramsPass.put("reward", Reward.getText().toString());
                paramsPass.put("dayOfJob", String.valueOf(jobDatePicker.getDayOfMonth()));
                paramsPass.put("monthOfJob", String.valueOf(jobDatePicker.getMonth()));
                paramsPass.put("yearOfJob", String.valueOf(jobDatePicker.getYear()));

                //ACCEPT USING ADDRESS
                Geocoder coder = new Geocoder(CreateJobScreen.this, Locale.getDefault());
                List<android.location.Address> address;
                String strAddress = Address.getText().toString();
                try {
                    address = coder.getFromLocationName(strAddress, 5);
                    if (address == null) {
                        Toast.makeText(CreateJobScreen.this, "Invalid Address.", Toast.LENGTH_SHORT).show();
                        acceptButton2.setEnabled(true);
                        acceptButton.setEnabled(true);
                        return;
                    }
                    Address location = address.get(0);

                    paramsPass.put("latitude", Double.toString(location.getLatitude()));
                    paramsPass.put("longitude", Double.toString(location.getLongitude()));
                }
                catch (IOException e) {
                    Toast.makeText(CreateJobScreen.this, "Error with geocoder.", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

                service.callAPIJSON(tempURL, paramsPass, Request.Method.POST, response -> {
                    try {
                        String success = response.getString("success");
                        if(success.equals("true")){
                            Toast.makeText(CreateJobScreen.this, "Job Posted Successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        else {
                            Toast.makeText(CreateJobScreen.this, "Error Posting Job... try again later.", Toast.LENGTH_LONG).show();
                        }
                        acceptButton2.setEnabled(true);
                        acceptButton.setEnabled(true);

                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}
