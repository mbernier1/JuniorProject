package com.example.testproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;


public class SearchScreen  extends AppCompatActivity {

    ListView postedJobs;
    EditText search;
    final String FILTERS = MainActivity.categoryStrings[0];
    final static float range = MainActivity.gdistrange;

    String URL = "https://boiling-temple-46468.herokuapp.com/job/readJobs";
    String URL_refreshL = "https://boiling-temple-46468.herokuapp.com/job/getCategoryLocation";
    String URL_searchL = "https://boiling-temple-46468.herokuapp.com/job/getTagsLocation";

    private TextView dist;
    ArrayList<Job> arrayOfJobs = new ArrayList<Job>();
    HashMap<String, String> obj = new HashMap<String, String>();
    boolean resume = false;

    private boolean checkTagSearch(String search)
    {
        // True if found, false otherwise
        if (search.length() == 0) return true;
        return search.indexOf('#') >= 0;
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        resume = true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (resume)
        {
            arrayOfJobs.clear();
            APIDataService service = new APIDataService(SearchScreen.this);
            service.callAPIURLArray(URL, obj, Request.Method.GET, response1 -> {
                JSONObject object = null;
                try {
                    JSONArray jsonArray = response1.getJSONArray("PostedJobs");

                    for (int i = 1; i < Integer.parseInt(jsonArray.getJSONObject(0).getString("numJobs")) + 1; i++) {

                        object = jsonArray.getJSONObject(i);
                        Job newJob = new Job(object);
                        arrayOfJobs.add(newJob);
                    }
                } catch (JSONException e) {

                    e.printStackTrace();
                }
                JobAdapter job_adapter = new JobAdapter(SearchScreen.this, arrayOfJobs);
                postedJobs.setAdapter(job_adapter);
            });
            resume = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_screen);

        search = findViewById(R.id.searchbar);
        postedJobs = findViewById(R.id.allJobs);
        dist = findViewById(R.id.searchdistance);
        dist.setText("Searching for jobs in a radius of about " + ((range*100) / 2) * 61 + " miles.");

        BottomNavigationView bottomNavigationView = findViewById(R.id.BottomNavigationView);
        bottomNavigationView.setBackgroundColor(0);
        bottomNavigationView.setSelectedItemId(R.id.Nav_search);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        bottomNavigationView.setOnItemSelectedListener(item ->  {

                switch (item.getItemId()){
                    case R.id.Nav_map:
                        startActivity(new Intent(getApplicationContext(), JobSearchMap.class));
                        overridePendingTransition(0,0);
                        return true;

                    case R.id.Nav_search:
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

        search.setOnKeyListener((v, keyCode, event) -> {
            // If the event is a key-down event on the "enter" button
            if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                // Perform action on key press
                HashMap<String, String> search_list = new HashMap<>();
                String search_word = search.getText().toString();

                if (!checkTagSearch(search_word))
                {
                    Toast.makeText(this, "Search tags need a # like '#mowing'!", Toast.LENGTH_SHORT).show();
                    return false;
                }

                // Encoding the tags for URL
                search_word = search_word.replace("#", "%23");
                search_list.put("tags", search_word);

                if(MainActivity.currentLatLng != null) {
                    search_list.put("latitude", Double.toString(MainActivity.currentLatLng.latitude));
                    search_list.put("longitude", Double.toString(MainActivity.currentLatLng.longitude));
                    search_list.put("range", String.valueOf(range));
                }
                else
                {
                    Toast.makeText(this, "Error, Please enable location services.", Toast.LENGTH_SHORT).show();
                    //I think we should return false in this case.
                    return false;
                }

                APIDataService service = new APIDataService(SearchScreen.this);
                service.callAPIURLArray(URL_searchL, search_list, Request.Method.GET, response1 -> {
                    JSONObject object = null;

                    arrayOfJobs.clear();
                    try {
                        JSONArray jsonArray = response1.getJSONArray("PostedJobs");
                        for (int i = 1; i < Integer.parseInt(jsonArray.getJSONObject(0).getString("numJobs")) + 1; i++) {
                            object = jsonArray.getJSONObject(i);
                            Job newJob = new Job(object);
                            arrayOfJobs.add(newJob);
                        }
                    } catch (JSONException e) {

                        e.printStackTrace();
                    }
                    JobAdapter job_adapter = new JobAdapter(SearchScreen.this, arrayOfJobs);
                    postedJobs.setAdapter(job_adapter);
                });
                return true;
            }
            return false;
        });
        Spinner filters = findViewById(R.id.filters);

        //create an adapter to describe how the items are displayed
        ArrayAdapter<String> filter_adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MainActivity.categoryStrings);

        //set the spinners adapter to the previously created one.
        filters.setAdapter(filter_adapter);

        filters.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String filter_search_word = MainActivity.categoryStrings[position];
                HashMap<String, String> refresh_list = new HashMap<>();
                refresh_list.put("category", filter_search_word);
                if(MainActivity.currentLatLng != null) {
                    refresh_list.put("latitude", Double.toString(MainActivity.currentLatLng.latitude));
                    refresh_list.put("longitude", Double.toString(MainActivity.currentLatLng.longitude));
                    refresh_list.put("range", "5");
                }
                else
                {
                    Toast.makeText(SearchScreen.this, "Error, Please enable location services.", Toast.LENGTH_SHORT).show();
                    //I think we should just return in this case.
                    return;
                }

                if(filter_search_word != FILTERS) {
                APIDataService service = new APIDataService(SearchScreen.this);

                    service.callAPIURLArray(URL_refreshL, refresh_list, Request.Method.GET, response1 -> {
                        JSONObject object = null;


                        arrayOfJobs.clear();
                        try {
                            JSONArray jsonArray = response1.getJSONArray("PostedJobs");

                            for (int i = 1; i < Integer.parseInt(jsonArray.getJSONObject(0).getString("numJobs")) + 2; i++) {

                                object = jsonArray.getJSONObject(i);
                                Job newJob = new Job(object);
                                arrayOfJobs.add(newJob);
                            }
                        } catch (JSONException e) {

                            e.printStackTrace();
                        }
                        JobAdapter job_adapter = new JobAdapter(SearchScreen.this, arrayOfJobs);
                        postedJobs.setAdapter(job_adapter);
                    });
                }
                else
                {
                    arrayOfJobs.clear();
                    APIDataService service = new APIDataService(SearchScreen.this);
                    service.callAPIURLArray(URL, obj, Request.Method.GET, response1 -> {
                        JSONObject object = null;
                        try {
                            JSONArray jsonArray = response1.getJSONArray("PostedJobs");

                            for (int i = 1; i < Integer.parseInt(jsonArray.getJSONObject(0).getString("numJobs")) + 1; i++) {

                                object = jsonArray.getJSONObject(i);
                                Job newJob = new Job(object);
                                arrayOfJobs.add(newJob);
                            }
                        } catch (JSONException e) {

                            e.printStackTrace();
                        }
                        JobAdapter job_adapter = new JobAdapter(SearchScreen.this, arrayOfJobs);
                        postedJobs.setAdapter(job_adapter);
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        postedJobs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String user = arrayOfJobs.get(position).getUsername();
                String jobid = arrayOfJobs.get(position).getJobId();
                String email = arrayOfJobs.get(position).getEmail();
                String title = arrayOfJobs.get(position).getJobTitle();
                String description = arrayOfJobs.get(position).getDescription();
                String tags = arrayOfJobs.get(position).getTags();
                String categories = arrayOfJobs.get(position).getCategory();
                String reward = arrayOfJobs.get(position).getReward();
                String date = arrayOfJobs.get(position).getDate();


                Intent startIntent = new Intent(getBaseContext() , JobDetailsScreen.class);

                startIntent.putExtra("user", user);
                startIntent.putExtra("jobid", jobid);
                startIntent.putExtra("email", email);
                startIntent.putExtra("title", title);
                startIntent.putExtra("description", description);
                startIntent.putExtra("tags", tags);
                startIntent.putExtra("categories", categories);
                startIntent.putExtra("reward", reward);
                startIntent.putExtra("date", date);
                startActivity(startIntent);
            }
        });
    }
}