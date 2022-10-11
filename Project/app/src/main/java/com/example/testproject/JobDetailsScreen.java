package com.example.testproject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

public class JobDetailsScreen extends AppCompatActivity {

    ListView workerListView;
    TextView JobTitle, jobDescription, jobTags, jobCategories, JobUsername, jobReward, jobDate;
    Button ViewProfileButton, acceptJobButton, deleteJobButton, toggleJobButton, submitJobButton;

    SharedPreferences sp;

    public static final String API_URL = "https://boiling-temple-46468.herokuapp.com";

    // Call to populate a linear layout with workers of a job
    // Deletes all children in layout and populates with new children
    private void populateWorkers(ListView workerView, SharedPreferences sharedPreferences, APIDataService service, String jobid)
    {
        HashMap<String, String> params = new HashMap<>();
        params.put("jobid", jobid);

        ArrayList<Worker> arrayOfWorkers = new ArrayList<Worker>();

        service.callAPIURL(API_URL + "/jobWorkers/readWorker", params, Request.Method.GET, response -> {
            try {
                int workerCount = response.getInt("numworkers");

                for (int i = 0; i < workerCount; ++i)
                {
                    int finalI = i;
                    Worker worker = new Worker(response.getString("email" + i), response.getString("worker" + i), new View.OnClickListener(){
                    @Override
                    public void onClick(View v)
                    {
                        try {
                            String clickedEmail = response.getString("email" + finalI);
                            if (clickedEmail.equals(sp.getString("email", "")))
                            {
                                Intent startIntent = new Intent(getApplicationContext(), ProfileScreen.class);
                                startActivity(startIntent);
                            }
                            else
                            {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("viewedEmail", response.getString("email" + finalI));
                                editor.commit();

                                Intent startIntent = new Intent(getApplicationContext(), OtherProfileScreen.class);
                                startActivity(startIntent);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    });
                    arrayOfWorkers.add(worker);
                    WorkerAdapter workerAdapter = new WorkerAdapter(this, arrayOfWorkers);
                    workerView.setAdapter(workerAdapter);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_details_screen);

        Bundle bundle = getIntent().getExtras();

        workerListView = findViewById(R.id.workerListView);

        JobTitle = findViewById(R.id.jobDetails_jobTitle);
        jobDescription = findViewById(R.id.jobDetails_jobDescription);
        jobTags = findViewById(R.id.jobDetails_jobTags);
        jobCategories = findViewById(R.id.jobDetails_jobCategories);
        JobUsername = findViewById(R.id.jobDetails_jobUsername);
        jobReward = findViewById(R.id.jobDetails_jobReward);
        jobDate = findViewById(R.id.jobDetails_jobDate);
        acceptJobButton = findViewById(R.id.AcceptJobButton);
        deleteJobButton = findViewById(R.id.DeleteJobButton);
        toggleJobButton = findViewById(R.id.ToggleJobButton);
        submitJobButton = findViewById(R.id.SubmitButton);


        ViewProfileButton = findViewById(R.id.ViewProfileButton);

        String username = bundle.getString("user");
        String jobid = bundle.getString("jobid");
        String email = bundle.getString("email");
        String title = bundle.getString("title");
        String description = bundle.getString("description");
        String categories = bundle.getString("categories");
        String tags = bundle.getString("tags");
        String reward = bundle.getString("reward");
        String date = bundle.getString("date");

        JobUsername.setText(username);
        JobTitle.setText(title);
        jobDescription.setText(description);
        jobCategories.setText(categories);
        jobTags.setText(tags);
        jobReward.setText(reward);
        jobDate.setText(date);

        String currentStatus = "";

        sp = getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        APIDataService service = new APIDataService(JobDetailsScreen.this);

        HashMap<String, String> params = new HashMap<>();
        params.put("email", sp.getString("email", ""));
        params.put("jobid", jobid);

        populateWorkers(workerListView, sp, service, jobid);

        if (sp.getString("email", "").equals(email)) {
            acceptJobButton.setEnabled(false);
            deleteJobButton.setVisibility(View.VISIBLE);
            deleteJobButton.setEnabled(true);


            toggleJobButton.setVisibility(View.VISIBLE);

            submitJobButton.setVisibility(View.VISIBLE);

            service.callAPIURL(API_URL + "/job/checkStatus", params, Request.Method.GET, response -> {
                try {
                    String status = response.getString("status");
                    if (status.equals("0")) {
                        toggleJobButton.setText(R.string.looking_for_workers);
                    } else if (status.equals("1")) {
                        toggleJobButton.setText(R.string.job_in_progress);
                    } else if (status.equals("2")) {
                        toggleJobButton.setText(R.string.job_completed);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                toggleJobButton.setEnabled(true);
            });
        } else {
            service.callAPIURL(API_URL + "/job/checkStatus", params, Request.Method.GET, response -> {
                try {
                    String hasJob = response.getString("worker");
                    int status = response.getInt("status");

                    if (status == 0) {
                        acceptJobButton.setEnabled(true);
                    }

                    if (hasJob.equals("false")) {
                        acceptJobButton.setText(R.string.acceptJob);
                    } else if (hasJob.equals("true")) {
                        acceptJobButton.setText(R.string.leaveJob);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }

        LinearLayout jobDetails = findViewById(R.id.jobDetails);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            jobDetails.setOrientation(LinearLayout.VERTICAL);
        }
        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            jobDetails.setOrientation(LinearLayout.HORIZONTAL);
        }

        ViewProfileButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String clickedEmail = bundle.getString("email");
                if (clickedEmail.equals(sp.getString("email", "")))
                {
                    Intent startIntent = new Intent(getApplicationContext(), ProfileScreen.class);
                    startActivity(startIntent);
                }
                else
                {
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("viewedEmail", clickedEmail);
                    editor.commit();

                    Intent startIntent = new Intent(getApplicationContext(), OtherProfileScreen.class);
                    startActivity(startIntent);
                }
            }
        });
        acceptJobButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                acceptJobButton.setEnabled(false);
                if (acceptJobButton.getText().equals("Accept Job"))
                {
                    service.callAPIJSON(API_URL + "/job/acceptJob", params, Request.Method.POST, response -> {
                        try {
                            String success = response.getString("success");
                            if (success.equals("true"))
                            {
                                Toast.makeText(JobDetailsScreen.this, "Accepted job successfully", Toast.LENGTH_SHORT).show();
                                acceptJobButton.setText(R.string.leaveJob);
                            }
                            acceptJobButton.setEnabled(true);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Service call happens asynchronously, so duplicated code to provide accurate result
                        populateWorkers(workerListView, sp, service, jobid);
                    });
                }
                else
                {
                    service.callAPIURL(API_URL + "/jobWorkers/deleteWorker", params, Request.Method.DELETE, response -> {
                        try {
                            String success = response.getString("success");
                            if (success.equals("true")) {
                                Toast.makeText(JobDetailsScreen.this, "Left Job", Toast.LENGTH_SHORT).show();
                                acceptJobButton.setText(R.string.acceptJob);
                            }
                            acceptJobButton.setEnabled(true);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        // Service call happens asynchronously, so duplicated code to provide accurate result
                        populateWorkers(workerListView, sp, service, jobid);
                    });
                }
            }
        });
        deleteJobButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteJobButton.setEnabled(false);
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                service.callAPIURL(API_URL + "/job/deleteJob", params, Request.Method.DELETE, response->{
                                    try {
                                        String success = response.getString("success");
                                        if (success.equals("true"))
                                        {
                                            Toast.makeText(JobDetailsScreen.this, "Job Deleted", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                        else
                                        {
                                            Toast.makeText(JobDetailsScreen.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                });
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                Toast.makeText(JobDetailsScreen.this, "Job not deleted.", Toast.LENGTH_SHORT).show();
                                deleteJobButton.setEnabled(true);
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(JobDetailsScreen.this);
                builder.setMessage("Are you sure you'd like to delete this job?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
            }
        });

        toggleJobButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleJobButton.setEnabled(false);

                if (toggleJobButton.getText().toString().equals(getString(R.string.looking_for_workers)))
                {
                    toggleJobButton.setText(R.string.job_in_progress);
                }
                else if (toggleJobButton.getText().toString().equals(getString(R.string.job_in_progress)))
                {
                    toggleJobButton.setText(R.string.job_completed);
                }
                else if (toggleJobButton.getText().toString().equals(getString(R.string.job_completed)))
                {
                    toggleJobButton.setText(R.string.looking_for_workers);
                }
                submitJobButton.setEnabled(true);
                toggleJobButton.setEnabled(true);
            }
        });

        submitJobButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitJobButton.setEnabled(false);

                HashMap<String, String> changeParams = new HashMap<>(params);

                // Default to 0, check if it should be something else
                String status = "0";

                if (toggleJobButton.getText().toString().equals(getString(R.string.job_in_progress)))
                {
                    status = "1";
                }
                else if (toggleJobButton.getText().toString().equals(getString(R.string.job_completed)))
                {
                    status = "2";
                }

                changeParams.put("status", status);

                service.callAPIJSON(API_URL + "/job/changeStatus", changeParams, Request.Method.PUT, response->{
                    try {
                        String success = response.getString("success");
                        if (success.equals("true"))
                        {
                            Toast.makeText(JobDetailsScreen.this, "Updated Status Successfully", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(JobDetailsScreen.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}
