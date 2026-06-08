package mdad.networkdata.lostfoundapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

public class MyClaimsActivity extends AppCompatActivity {

    // ListView to display user's claim history
    ListView lvMyClaims;

    // data source for ListView (each row stored as key-value pair)
    ArrayList<HashMap<String, String>> myClaimList = new ArrayList<HashMap<String, String>>();
    SimpleAdapter adapter;

    // mapping keys to row_my_claims.xml TextViews
    String[] from = {"title", "status"};
    int[] to = {R.id.tvMyClaimTitle, R.id.tvMyClaimStatus};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_claims);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // link ListView from XML
        lvMyClaims = (ListView) findViewById(R.id.lvMyClaims);

        // connect data list to ListView using SimpleAdapter
        adapter = new SimpleAdapter(
                MyClaimsActivity.this,
                myClaimList,
                R.layout.row_my_claims,
                from,
                to
        );

        lvMyClaims.setAdapter(adapter);

        // load user's claims when page first opens
        loadMyClaims();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // refresh claims when user comes back to this screen
        loadMyClaims();
    }

    private void loadMyClaims() {

        // get logged-in user_id from session
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        int userId = sp.getInt("user_id", -1);

        // if no valid session, stop and close page
        if (userId == -1) {
            Toast.makeText(MyClaimsActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // send user_id as query parameter to fetch only this user's claims
        String url = ApiConfig.FETCH_MY_CLAIMS + "?user_id=" + userId;

        RequestQueue queue = Volley.newRequestQueue(MyClaimsActivity.this);

        // GET request to retrieve claim history
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        int success = response.optInt("success", 0);

                        // clear old list before adding updated data
                        myClaimList.clear();

                        if (success == 1) {

                            JSONArray claims = response.optJSONArray("claims");

                            // loop through each claim returned by server
                            for (int i = 0; i < claims.length(); i++) {

                                JSONObject obj = claims.optJSONObject(i);

                                String itemTitle = obj.optString("title");
                                String type = obj.optString("type");
                                String location = obj.optString("location");
                                String claimStatus = obj.optString("status"); // pending/approved/rejected

                                // format display text for ListView row
                                String title = type.toUpperCase() + ": " + itemTitle + " (" + location + ")";
                                String status = "Claim Status: " + claimStatus.toUpperCase();

                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("title", title);
                                map.put("status", status);

                                myClaimList.add(map);
                            }

                            // refresh ListView after updating data
                            adapter.notifyDataSetChanged();

                        } else {
                            adapter.notifyDataSetChanged();

                            Toast.makeText(MyClaimsActivity.this,
                                    response.optString("message", "No claims found"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // handle network/server errors
                        Toast.makeText(MyClaimsActivity.this, "Error loading claims", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // send request to server
        queue.add(req);
    }
}
