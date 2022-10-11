package com.example.testproject;

import android.content.Context;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class APIDataService {

    Context contex;

    public APIDataService(Context contex) {
        this.contex = contex;
    }

    public interface VolleyResponseListener{
        void onResponse(JSONObject response);
    }

    // Should be able to handle all GET requests assuming params is correctly formatted. URL is used since GET cannot receive JSONObject body.
    public void callAPIURL(String url, HashMap<String, String> params, int method, VolleyResponseListener volleyResponseListener) {
        // Populate the URL

        Set<String> keys = params.keySet();
        Iterator<String> iter = keys.iterator();
        if (iter.hasNext())
            url += "?";
        while (iter.hasNext()){
            String key = iter.next();
            url += key + "=" + params.get(key);
            if (iter.hasNext())
                url += "&";
        }

        final JsonObjectRequest request = new JsonObjectRequest(method, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String username = "unupdated"; // empty string to put a value in related to a specific key

                volleyResponseListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(contex, "Something went wrong", Toast.LENGTH_SHORT).show();
            }
        });

        // may need to implement something like this to increase timeout on certain requests
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueueSingleton.getInstance(contex).addToRequestQueue(request);
    }
    public void callAPIURLArray(String url, HashMap<String, String> params, int method, VolleyResponseListener volleyResponseListener) {
        // Populate the URL

        Set<String> keys = params.keySet();
        Iterator<String> iter = keys.iterator();
        if (iter.hasNext())
            url += "?";
        while (iter.hasNext()){
            String key = iter.next();
            url += key + "=" + params.get(key);
            if (iter.hasNext())
                url += "&";
        }

        final JsonArrayRequest request = new JsonArrayRequest(method, url, null, response -> {
            String username = "unupdated"; // empty string to put a value in related to a specific key

            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("PostedJobs", response);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            volleyResponseListener.onResponse(jsonObject);
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        RequestQueueSingleton.getInstance(contex).addToRequestQueue(request);
    }

    // Should be able to handle all POST requests. Params is passed to the API method unlike in GET
    public void callAPIJSON(String url, HashMap<String, String> params, int method, VolleyResponseListener volleyResponseListener) {

        final JsonObjectRequest request = new JsonObjectRequest(method, url, new JSONObject(params), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String username = "unupdated"; // empty string to put a value in related to a specific key

                volleyResponseListener.onResponse(response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(contex, "Something went wrong", Toast.LENGTH_SHORT).show();
                //volleyResponseListener.onError("Something Wrong");
            }
        });

        // may need to implement something like this to increase timeout on certain requests
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        RequestQueueSingleton.getInstance(contex).addToRequestQueue(request);
    }
}
