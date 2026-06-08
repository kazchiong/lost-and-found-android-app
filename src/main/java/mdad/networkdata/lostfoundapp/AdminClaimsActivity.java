package mdad.networkdata.lostfoundapp;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class AdminClaimsActivity extends AppCompatActivity {

    ListView lvClaims;

    // stores key-value claims like in matrix
    ArrayList<HashMap<String, String>> claimList = new ArrayList<HashMap<String, String>>();
    SimpleAdapter adapter;      // bridge for claimList to listView

    // these keys are used in HashMap for SimpleAdapter display
    String[] from = {"title", "info"};      // the keys in each HashMap that will be displayed, they taking "title" and "info" from the HashMap
    int[] to = {R.id.tvClaimTitle, R.id.tvClaimInfo};       // puts the TextViews into row layout; "title" into tvClaimTitle and "info" into tvClaimInfo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_claims);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // finds ListView from the xml layout so that java can use it
        lvClaims = (ListView) findViewById(R.id.lvClaims);

        // creates the adapter then uses claimList as data, row_claim.xml as row design and from[] and to[] to match data to UI
        adapter = new SimpleAdapter(AdminClaimsActivity.this, claimList, R.layout.row_claim, from, to);
        lvClaims.setAdapter(adapter);       // connects adapter to the ListView, now the list can show data


        // tap a row -> show AlertDialog with Approve/Reject options
        lvClaims.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {

                HashMap<String, String> selected = claimList.get(position);

                // extracts values from the clicked claim row, reads stored data, no web server call yet
                final String claimId = selected.get("claim_id");
                final String itemTitle = selected.get("item_title");
                final String claimantName = selected.get("claimant_name");
                final String claimantEmail = selected.get("claimant_email");
                final String message = selected.get("message");

                String dialogMsg =
                        "Item: " + itemTitle + "\n\n" +
                                "Claimant: " + claimantName + " (" + claimantEmail + ")\n\n" +
                                "Message:\n" + message + "\n\n";

                AlertDialog.Builder builder = new AlertDialog.Builder(AdminClaimsActivity.this);
                builder.setTitle("Review Claim");
                builder.setMessage(dialogMsg);

                builder.setPositiveButton("Approve", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // when admin clicks approve, call this method (updateClaimStatus)
                        // passes the 2 piece of data (claimId [ID of the claim] and "approved" [status i want to set])
                        updateClaimStatus(claimId, "approved");
                    }
                });

                builder.setNegativeButton("Reject", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateClaimStatus(claimId, "rejected");
                    }
                });

                // this adds a cancel button to the dialog popup, "null" is the action,
                // what you want it to do, which is nothing, close the dialog
                builder.setNeutralButton("Cancel", null);

                builder.show();
            }
        });


        // gets access to a saved storage space called "session", contains saved values like role, user_id, etc. from login;
        // protect page access and not just button access to page, so when user logs out, it kicks user out
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        String role = sp.getString("role", "user");     // reads the saved value with "role" key, if theres no role, then "user" is the default

        // this checks if the role is NOT admin,
        // if no, then condition is true and runs the next block of code
        if (!"admin".equals(role)) {
            Toast.makeText(AdminClaimsActivity.this, "Access restricted to admin users", Toast.LENGTH_SHORT).show();
            finish();
        }

        // calls for the fetchPendingClaims() method, the volley GET request one
        fetchPendingClaims();
    }

    @Override
    // this method runs when user left the screen adn come back again
    protected void onResume() {
        super.onResume();       // runs the android's normal resume behaviour that is inherited from AppCompatActivity
        fetchPendingClaims();   // calls this method, meaning it reloads the page everytime you come back
    }

    private void fetchPendingClaims() {
        // creates a volley "queue", RequestQueue is a manager that handles network request,
        // doesnt send anything yet, just creates the system that will handle all the network request
        RequestQueue queue = Volley.newRequestQueue(AdminClaimsActivity.this);


        // creates the volley request that expects the response to be JSONObject
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                ApiConfig.FETCH_PENDING_CLAIMS,
                null,
                // this defines what to do when the server responds successfully with JSON
                new Response.Listener<JSONObject>() {
                    @Override
                    // this runs when the GET request returns JSON from the php
                    public void onResponse(JSONObject response) {
                        // reads "success" from the response, if its not "success", uses 0 as default
                        int success = response.optInt("success", 0);

                        // clears the old list to to later replace with the latest claims on the list from the server
                        claimList.clear();

                        if (success == 1) {
                            // if PHP say success, then reads "claims" array from the JSON response
                            JSONArray claims = response.optJSONArray("claims");

                            // loops through every claim in teh claims array
                            for (int i = 0; i < claims.length(); i++) {
                                // gets one claim (a JSON object) from the claims array
                                JSONObject obj = claims.optJSONObject(i);

                                // then extracts values from the JSON claim object that the PHP returned
                                String claimId = obj.optString("claim_id");
                                String itemId = obj.optString("item_id");
                                String itemTitle = obj.optString("title");
                                String claimantName = obj.optString("claimant_name");
                                String claimantEmail = obj.optString("claimant_email");
                                String message = obj.optString("message");

                                // display text in row_claim.xml
                                String title = "Claim: " + itemTitle;
                                String info = claimantName + " (" + claimantEmail + ")\n"
                                        + "Msg: " + message;

                                // stores everything we need for dialog + update as a key-value map
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("claim_id", claimId);
                                map.put("item_id", itemId);
                                map.put("item_title", itemTitle);
                                map.put("claimant_name", claimantName);
                                map.put("claimant_email", claimantEmail);
                                map.put("message", message);

                                // stores the keys used for SimpleAdapter display
                                map.put("title", title);
                                map.put("info", info);

                                // adds this claim record into the list used by the adapter
                                claimList.add(map);
                            }

                            // tells the adapter, hey data is cahnged, redraw the ListView
                            adapter.notifyDataSetChanged();

                        } else {
                            // if success != 1 (meaning PHP said no claims or failed),
                            // still refreshes the UI, even though list is empty
                            adapter.notifyDataSetChanged();
                            Toast.makeText(AdminClaimsActivity.this,
                                    response.optString("message", "No pending claims"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                // defines what happens if request fails
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AdminClaimsActivity.this,
                                "Error loading claims",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        queue.add(req);     // sends the request
    }

    private void updateClaimStatus(final String claimId, final String status) {

        // i need admin_id to send to the PHP, because the server records who approved/rejected (for accountability)
        // i stored user_id during login inside SharedPreferences "session"
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);

        // read the saved user_id (admin id). if it doesn’t exist, default = -1
        int adminId = sp.getInt("user_id", -1);

        // safety check: if adminId is missing, it means session is broken / user not logged in properly
        // so i stop here instead of sending a request with invalid data
        if (adminId == -1) {
            Toast.makeText(AdminClaimsActivity.this, "Please login again", Toast.LENGTH_SHORT).show();
            finish();   // kicks user out of this page (no point staying because cannot perform admin action)
            return;     // important: stops running the rest of this method
        }

        try {
            // build the JSON request body (this is what my PHP expects in POST)
            // basically im packing all the data into a JSON object to send to server
            JSONObject body = new JSONObject();

            // claimId is stored as String in my HashMap, but DB expects number, so i convert to int first
            body.put("claim_id", Integer.parseInt(claimId));

            // status is either "approved" or "rejected" depending what button admin clicked
            body.put("status", status);

            // admin_id is to let backend know which admin did the action
            body.put("admin_id", adminId);

            // create a Volley request queue again (the manager that handles the request)
            RequestQueue queue = Volley.newRequestQueue(AdminClaimsActivity.this);

            // create POST request that sends JSON body and expects JSON response back
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.UPDATE_CLAIM_STATUS,
                    body,

                    // this runs when server returns a response successfully (meaning request reached PHP and PHP replied)
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            // read the success flag from PHP response
                            // if PHP didn't include it, default to 0 so it counts as failure
                            int success = response.optInt("success", 0);

                            if (success == 1) {
                                // success means claim status updated in database
                                // show feedback so admin knows action worked
                                Toast.makeText(AdminClaimsActivity.this,
                                        "Claim " + status,
                                        Toast.LENGTH_SHORT).show();

                                // refresh the pending claim list so the approved/rejected claim disappears
                                // (because after update, it’s no longer pending)
                                fetchPendingClaims();

                            } else {
                                // if backend says fail, show message from server to help debug
                                Toast.makeText(AdminClaimsActivity.this,
                                        "Failed: " + response.optString("message", ""),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    },

                    // this runs when the request fails before server returns JSON
                    // (could be no internet, wrong url, server down, timeout, etc.)
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(AdminClaimsActivity.this,
                                    "Server error updating claim",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            // THIS is the part that actually sends the request out to the server
            queue.add(req);

        } catch (Exception e) {
            // this catch is for errors before network call even happens
            // e.g. JSON building fails, claimId parse fails, etc.
            Toast.makeText(AdminClaimsActivity.this,
                    "Error preparing request",
                    Toast.LENGTH_SHORT).show();
        }
    }
}