package com.example.testproject;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;

import java.util.HashMap;

public class OtherProfileScreen extends AppCompatActivity
{
    public static  final String API_URL = "https://boiling-temple-46468.herokuapp.com";

    TextView NameTextView;
    TextView UsernameTextView;
    TextView EmailTextView;
    TextView BioBodyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_profile_screen);

        BottomNavigationView bottomNavigationView = findViewById(R.id.BottomNavigationView);
        bottomNavigationView.setBackgroundColor(0);
        bottomNavigationView.setSelectedItemId(R.id.space);
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
                    startActivity(new Intent(getApplicationContext(), MyJobsScreen.class));
                    overridePendingTransition(0,0);
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

        SharedPreferences sp = getApplicationContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        NameTextView = findViewById(R.id.NameTextView);
        UsernameTextView = findViewById(R.id.UsernameTextView);
        EmailTextView = findViewById(R.id.EmailTextView);
        BioBodyTextView = findViewById(R.id.BioBodyTextView);

        APIDataService service = new APIDataService(OtherProfileScreen.this);

        String tempURL = API_URL + "/profile/readProfile";

        HashMap<String, String> obj = new HashMap<String, String>();

        obj.put("email", sp.getString("viewedEmail",""));

        service.callAPIURL(tempURL, obj, Request.Method.GET, response -> {
            try {
                NameTextView.setText(response.getString("firstname") + ' ' + response.getString("lastname"));
                UsernameTextView.setText(response.getString("username"));
                BioBodyTextView.setText(response.getString("bio"));
                EmailTextView.setText(sp.getString("viewedEmail",""));
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(OtherProfileScreen.this, "Error occurred loading profile", Toast.LENGTH_SHORT).show();

                Intent startIntent = new Intent(getApplicationContext(), SearchScreen.class);
                startActivity(startIntent);
            }
        });
    }
}
