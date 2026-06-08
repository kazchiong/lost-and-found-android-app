package mdad.networkdata.lostfoundapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
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

public class ItemListActivity extends AppCompatActivity {

    // UI components
    ListView lvItems;
    Spinner spFilter;

    // list that stores item data retrieved from server
    ArrayList<HashMap<String, String>> itemList = new ArrayList<HashMap<String, String>>();

    // mapping keys to row_item.xml TextViews
    String[] from = {"title", "type", "location"};
    int[] to = {R.id.tvTitle, R.id.tvType, R.id.tvLocation};

    SimpleAdapter adapter;

    int userId = -1;
    int currentFilter = 0; // 0 = All Items, 1 = My Items

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_list);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // retrieve logged-in user session
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        userId = sp.getInt("user_id", -1);

        // if session invalid, redirect back to main page
        if (userId == -1) {
            Toast.makeText(ItemListActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ItemListActivity.this, MainActivity.class));
            finish();
            return;
        }

        // link UI elements
        spFilter = (Spinner) findViewById(R.id.spFilter);
        lvItems = (ListView) findViewById(R.id.lvItems);

        // adapter connects itemList data to ListView rows
        adapter = new SimpleAdapter(ItemListActivity.this, itemList, R.layout.row_item, from, to);
        lvItems.setAdapter(adapter);

        // spinner allows switching between all items and user’s own items
        String[] options = {"All Items", "My Items"};
        ArrayAdapter<String> spAdapter = new ArrayAdapter<String>(
                ItemListActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                options
        );
        spFilter.setAdapter(spAdapter);

        // when filter changes, reload data from correct endpoint
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentFilter = position;
                loadItemsByFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // clicking a row opens ItemDetailsActivity with selected item info
        lvItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {

                // get selected item from list
                HashMap<String, String> selected = itemList.get(position);

                String itemId = selected.get("id");
                String title = selected.get("title");
                String type = selected.get("type");
                String location = selected.get("location");
                String status = selected.get("status");
                String image = selected.get("image");

                // pass item data to details page using intent extras
                Intent i = new Intent(ItemListActivity.this, ItemDetailsActivity.class);
                i.putExtra("item_id", itemId);
                i.putExtra("title", title);
                i.putExtra("type", type);
                i.putExtra("location", location);
                i.putExtra("status", status);
                i.putExtra("image", image);

                startActivity(i);
            }
        });

        // first load will happen automatically from spinner selection
    }

    @Override
    protected void onResume() {
        super.onResume();
        // refresh list when returning from details page
        loadItemsByFilter();
    }

    private void loadItemsByFilter() {

        // decide which API endpoint to call based on selected filter
        if (currentFilter == 0) {
            fetchItems(ApiConfig.FETCH_ITEMS);
        } else {
            String url = ApiConfig.FETCH_MY_ITEMS + "?user_id=" + userId;
            fetchItems(url);
        }
    }

    private void fetchItems(String url) {

        // create Volley request queue
        RequestQueue queue = Volley.newRequestQueue(ItemListActivity.this);

        // GET request to fetch item list from server
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        int success = response.optInt("success", 0);

                        // clear old list before inserting updated data
                        itemList.clear();

                        if (success == 1) {

                            JSONArray items = response.optJSONArray("items");

                            // loop through JSON array returned from backend
                            for (int i = 0; i < items.length(); i++) {

                                JSONObject obj = items.optJSONObject(i);

                                String id = obj.optString("id", "");
                                String title = obj.optString("title", "No title");
                                String type = obj.optString("type", "");
                                String location = obj.optString("location", "");
                                String status = obj.optString("status", "open");
                                String image = obj.optString("image", "");

                                // store values in HashMap for adapter
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("id", id);
                                map.put("title", title);
                                map.put("type", type.toUpperCase());
                                // combine location and status for display
                                map.put("location", location + " (" + status.toUpperCase() + ")");
                                // keep raw status separately for details page logic
                                map.put("status", status);
                                map.put("image", image);

                                itemList.add(map);
                            }

                            // refresh ListView after data update
                            adapter.notifyDataSetChanged();

                        } else {
                            adapter.notifyDataSetChanged();
                            Toast.makeText(ItemListActivity.this,
                                    response.optString("message", "No items found"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        // handles network/server failure
                        Toast.makeText(ItemListActivity.this,
                                "Error connecting to server",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // send request to server
        queue.add(request);
    }
}
