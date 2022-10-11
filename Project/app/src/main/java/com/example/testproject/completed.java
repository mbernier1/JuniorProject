package com.example.testproject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link completed#newInstance} factory method to
 * create an instance of this fragment.
 */
public class completed extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "url";

    private static final String URL_ACCEPTED_JOBS = "https://boiling-temple-46468.herokuapp.com/job/getUserJobs";
    private static final String URL_POSTED_JOBS = "https://boiling-temple-46468.herokuapp.com/job/getEmployerJobs";

    // Status of returned jobs interested in
    private static final int STATUS = 2;

    // TODO: Rename and change types of parameters
    private ListView listView;
    private String url;
    private TextView numJobs;

    private HashMap<String, String> params;

    SharedPreferences sp;
    private APIDataService service = new APIDataService(getContext());
    private ArrayList<Job> arrayOfJobs = new ArrayList<>();

    public completed() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment completed.
     */
    // TODO: Rename and change types and number of parameters
    public static completed newInstance(String param1) {
        completed fragment = new completed();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_completed, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        listView = getView().findViewById(R.id.allJobs);
        numJobs = getView().findViewById(R.id.numJobs);

        sp = getContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        if (sp.getBoolean("viewPostedJobs",true))
        {
            url = URL_POSTED_JOBS;
        }
        else {
            url = URL_ACCEPTED_JOBS;
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
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

                Intent startIntent = new Intent(getContext() , JobDetailsScreen.class);

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

        //sp = getContext().getSharedPreferences("OddJobsUser", Context.MODE_PRIVATE);

        // Add shared preferences bool to decide which URL to use
        params = new HashMap<>();
        params.put("email", sp.getString("email", ""));
        service.callAPIURLArray(url, params, Request.Method.GET, response1 -> {
            JSONObject object;

            try {
                JSONArray jsonArray = response1.getJSONArray("PostedJobs");

                for (int i = 1; i < Integer.parseInt(jsonArray.getJSONObject(0).getString("numJobs")) + 1; i++) {

                    object = jsonArray.getJSONObject(i);
                    Job newJob = new Job(object);
                    if (newJob.getStatus() == STATUS)
                        arrayOfJobs.add(newJob);
                }
            } catch (JSONException e) {

                e.printStackTrace();
            }

            try
            {
                JobAdapter job_adapter = new JobAdapter(getContext(), arrayOfJobs);
                listView.setAdapter(job_adapter);
                String numJobsText = getString(R.string.found_jobs) + " " + arrayOfJobs.size();
                numJobs.setText(numJobsText);
            } catch (java.lang.NullPointerException e) {
                e.printStackTrace();
            }
        });
    }
}