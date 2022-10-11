package com.example.testproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class JobAdapter extends ArrayAdapter<Job> {

    public JobAdapter(Context context, ArrayList<Job> Jobs){
        super(context, 0, Jobs);
    }

    TextView jobTitle, username, category, tags, reward, status, description, jobdate;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Job job = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.single_job, parent, false);
        }

        jobTitle = convertView.findViewById(R.id.jobTitle);
        username = convertView.findViewById(R.id.username);
        category = convertView.findViewById(R.id.category);
        tags = convertView.findViewById(R.id.tags);
        reward = convertView.findViewById(R.id.reward);
        status = convertView.findViewById(R.id.status);
        description = convertView.findViewById(R.id.description);
        jobdate = convertView.findViewById(R.id.JobDate);

        // Populate the data into the template view using the data object
        if(job.jobTitle == null){
            jobTitle.setText("");
        }
        else{
            jobTitle.setText(job.jobTitle);
        }
        if(job.username == null){
            username.setText("");
        }
        else {
            username.setText(job.username);
        }
        if(job.category == null){
            category.setText("");
        }
        else{
            category.setText(job.category);
        }
        if(job.tags == null){
            tags.setText("");
        }
        else{
            tags.setText(job.tags);
        }
        if(job.reward == null){
            reward.setText("");
        }
        else{
            reward.setText(job.reward);
        }
        if (job.status == 0)
        {
            status.setText(R.string.looking_for_workers);
        }
        else if (job.status == 1)
        {
            status.setText(R.string.job_in_progress);
        }
        else if (job.status == 2)
        {
            status.setText(R.string.job_completed);
        }
        if(job.description == null)
        {
            description.setText("");
        }
        else
        {
            description.setText(job.description);
        }
        jobdate.setText(job.getDate());

        // Return the completed view to render on screen
        return convertView;
    }
}
