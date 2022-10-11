package com.example.testproject;

import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;


public class JobSearchMap extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap m_map;
    String URL = "https://boiling-temple-46468.herokuapp.com/job/readLocationJobs";
    private Marker currentLocation;
    private static float rangemin = MainActivity.gdistrange;
    private static float rangeboost = 0.030f;
    private int rangeMod;

    private ArrayList<Job> arrayOfJobs = new ArrayList<Job>();
    private HashMap<String, String> obj = new HashMap<String, String>();

    private SeekBar seekBar;
    private TextView distance;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_search_map);

        BottomNavigationView bottomNavigationView = findViewById(R.id.BottomNavigationView);
        bottomNavigationView.setBackgroundColor(0);
        bottomNavigationView.setSelectedItemId(R.id.Nav_map);
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
        FloatingActionButton floatingActionButton = findViewById(R.id.fab);
        bottomNavigationView.setOnItemSelectedListener(item-> {

                switch (item.getItemId()){
                    case R.id.Nav_map:
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
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        seekBar= findViewById(R.id.seekBar);
        distance= findViewById(R.id.distance);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rangeMod = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                localMapRefresh();
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        m_map = googleMap;
        m_map.setMinZoomPreference(12);
        m_map.getUiSettings().setZoomControlsEnabled(true);

        if(MainActivity.currentLatLng == null)
        {
            Toast.makeText(JobSearchMap.this, "NULL LOCATION.", Toast.LENGTH_SHORT).show();
        }
        else
        {
            localMapRefresh();
        }

        m_map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if(marker.equals(currentLocation)) {
                    Toast.makeText(JobSearchMap.this, "CURRENT LOCATION", Toast.LENGTH_SHORT).show();
                    return true;
                }
                else
                {
                    int offset = Integer.parseInt(marker.getTitle());
                    offset--;



                    Intent startIntent = new Intent(getBaseContext() , JobDetailsScreen.class);

                    startIntent.putExtra("user", arrayOfJobs.get(offset).getUsername());
                    startIntent.putExtra("jobid", arrayOfJobs.get(offset).getJobId());
                    startIntent.putExtra("email", arrayOfJobs.get(offset).getEmail());
                    startIntent.putExtra("title", arrayOfJobs.get(offset).getJobTitle());
                    startIntent.putExtra("description", arrayOfJobs.get(offset).getDescription());
                    startIntent.putExtra("tags", arrayOfJobs.get(offset).getTags());
                    startIntent.putExtra("categories", arrayOfJobs.get(offset).getCategory());
                    startIntent.putExtra("reward", arrayOfJobs.get(offset).getReward());

                    startActivity(startIntent);
                }
                return true;
            }
        });
    }

    //sets up map/refreshes map w/ new data.
    void localMapRefresh()
    {
        m_map.clear();
        //Posting user's location
        m_map.moveCamera(CameraUpdateFactory.newLatLng(MainActivity.currentLatLng));
        currentLocation = m_map.addMarker(new MarkerOptions()
                .position(MainActivity.currentLatLng)
                .title("Current Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        //Posting Job locations
        obj.clear();
        arrayOfJobs.clear();

        obj.put("latitude", Double.toString(MainActivity.currentLatLng.latitude));
        obj.put("longitude", Double.toString(MainActivity.currentLatLng.longitude));
        obj.put("range", Double.toString(rangeboost + (rangemin * (rangeMod+1))));

        String mid = Double.toString((rangeboost + (rangemin * (rangeMod+1)))/2 * 61);
        mid = mid.substring(0,Math.min(mid.length(),4));

        String d = "Searching for jobs in a radius of about " + mid + " miles.";

        distance.setText(d);

        APIDataService service = new APIDataService(JobSearchMap.this);
        service.callAPIURLArray(URL, obj, Request.Method.GET, response1 -> {
            JSONObject object = null;
            try {
                JSONArray jsonArray = response1.getJSONArray("PostedJobs");

                for (int i = 1; i < Integer.parseInt(jsonArray.getJSONObject(0).getString("numJobs")) + 1; i++) {
                    object = jsonArray.getJSONObject(i);
                    Job newJob = new Job(object);
                    arrayOfJobs.add(newJob);
                    m_map.addMarker(new MarkerOptions()
                            .position(new LatLng(object.getDouble("latitude"), object.getDouble("longitude")))
                            .title(Integer.toString(i))
                    );
                }
            } catch (JSONException e) {

                e.printStackTrace();
            }
        });
    }
}
