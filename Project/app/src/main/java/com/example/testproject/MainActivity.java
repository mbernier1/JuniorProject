package com.example.testproject;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.model.LatLng;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MainActivity extends AppCompatActivity
{
    private static final float MIN_DISTANCE_FOR_UPDATE_METERS = 5.0f;
    private static final long MIN_UPDATE_LOCATION_TIME_MS = 1000;
    static LocationManager locationManager;
    static LocationListener locationListener;
    static Location gpsLocation;
    static LatLng currentLatLng;
    static float gdistrange = 0.0050f;
    public static final String[] categoryStrings = {"-Categories-","Yardwork","Carpentry","Automotive","Transportation","Plumbing","Art","Child Care", "Other"};

    public static void main(String[] args)
    {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startLocationUpdates();

        SharedPreferences sp = getApplicationContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("viewPostedJobs", true);
        editor.commit();

        Button signInActivity = (Button)findViewById(R.id.signInButton);
        signInActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent startIntent = new Intent(getApplicationContext(), LoginScreen.class);
                startActivity(startIntent);
            }
        });

        Button registerActivity = (Button)findViewById(R.id.registerButton);
        registerActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent startIntent = new Intent(getApplicationContext(), RegistrationScreen.class);
                startActivity(startIntent);
            }
        });

        Button settingsActivity = (Button)findViewById(R.id.settingsButton);
        settingsActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Intent startIntent = new Intent(getApplicationContext(), AboutScreen.class);
                startActivity(startIntent);
            }
        });
    }

    protected void startLocationUpdates() {
        //no perms
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(MainActivity.this, "Please Allow Location Tracking.", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
        }
        //perms granted
        else
        {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationListener = new LocationListener()
            {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    // Set gpsLocation Lat/Long to the updated version
                    gpsLocation.setLatitude(location.getLatitude());
                    gpsLocation.setLongitude(location.getLongitude());
                    // Set latLng to the updated version for the new marker
                    currentLatLng = new LatLng(gpsLocation.getLatitude(), gpsLocation.getLongitude());
                }
            };

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_UPDATE_LOCATION_TIME_MS, MIN_DISTANCE_FOR_UPDATE_METERS, locationListener);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 200: {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent mStartActivity = new Intent(MainActivity.this, MainActivity.class);
                    int mPendingIntentId = 123456;
                    PendingIntent mPendingIntent = PendingIntent.getActivity(MainActivity.this, mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager)MainActivity.this.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                    System.exit(0);
                }
            }
        }
    }
}