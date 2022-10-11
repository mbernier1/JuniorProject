package com.example.testproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

public class WorkerAdapter extends ArrayAdapter<Worker> {

    TextView workerUsername;
    Button profileButton;

    public WorkerAdapter(Context context, ArrayList<Worker> usernames){
        super(context, 0, usernames);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Worker worker = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.single_worker, parent, false);
        }

        workerUsername = convertView.findViewById(R.id.workerUsername);
        profileButton = convertView.findViewById(R.id.ViewProfileButton);

        // Populate the data into the template view using the data object
        workerUsername.setText(worker.getUsername());

        // Set Onclick
        profileButton.setOnClickListener(worker.getClick());

        // Return the completed view to render on screen
        return convertView;
    }
}
