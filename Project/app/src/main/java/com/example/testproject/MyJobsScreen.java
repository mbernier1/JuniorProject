package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;

public class MyJobsScreen extends AppCompatActivity {
    ListView postedJobs;

    BottomNavigationView topNavigationView;
    BottomNavigationView jobNavigationView;

    @Override
    protected void onResume()
    {
        super.onResume();
        topNavigationView.setSelectedItemId(topNavigationView.getSelectedItemId());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_jobs_screen);

        // sharedpreference to get the user email for the API call
        SharedPreferences sp = getApplicationContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        postedJobs = findViewById(R.id.allJobs);
        //Button acceptedJobsButton = findViewById(R.id.acceptedJobsButton);
        //Button postedJobsButton = findViewById(R.id.postedJobsButton);

        HashMap<String, String> my_accepted_jobs = new HashMap<>();
        my_accepted_jobs.put("email", sp.getString("email",""));



        topNavigationView = findViewById(R.id.topNavigationView);
        topNavigationView.setBackgroundColor(0);
        NavController navController = Navigation.findNavController(MyJobsScreen.this, R.id.fragment);
        NavigationUI.setupWithNavController(topNavigationView, navController);

        jobNavigationView = findViewById(R.id.jobTypeNavigationView);
        jobNavigationView.setBackgroundColor(0);
        jobNavigationView.setSelectedItemId(R.id.postedJobs);
        jobNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.postedJobs:
                    editor.putBoolean("viewPostedJobs", true);
                    editor.commit();
                    topNavigationView.setSelectedItemId(topNavigationView.getSelectedItemId());
                    return true;

                case R.id.acceptedJobs:
                    editor.putBoolean("viewPostedJobs", false);
                    editor.commit();
                    topNavigationView.setSelectedItemId(topNavigationView.getSelectedItemId());
                    return true;
            }
            return false;
        });


        BottomNavigationView bottomNavigationView = findViewById(R.id.BottomNavigationView);
        bottomNavigationView.setBackgroundColor(0);
        bottomNavigationView.setSelectedItemId(R.id.Nav_jobs);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            switch (item.getItemId()){
                case R.id.Nav_map:
                    startActivity(new Intent(getApplicationContext(), JobSearchMap.class));
                    overridePendingTransition(0,0);
                    return true;

                case R.id.Nav_search:
                    startActivity(new Intent(getApplicationContext(), SearchScreen.class));
                    overridePendingTransition(0,0);
                    return true;

                case R.id.Nav_jobs:
                    return true;

                case R.id.Nav_profile:
                    startActivity(new Intent(getApplicationContext(), ProfileScreen.class));
                    overridePendingTransition(0,0);
                    return true;
            }
            return false;
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), CreateJobScreen.class));
            }
        });

        /*acceptedJobsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptedJobsButton.setEnabled(false);
                postedJobsButton.setEnabled(true);

                editor.putBoolean("viewPostedJobs", false);
                editor.commit();
                topNavigationView.setSelectedItemId(topNavigationView.getSelectedItemId());
            }
        });
        postedJobsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptedJobsButton.setEnabled(true);
                postedJobsButton.setEnabled(false);

                editor.putBoolean("viewPostedJobs", true);
                editor.commit();
                topNavigationView.setSelectedItemId(topNavigationView.getSelectedItemId());
            }
        });*/
    }
}
