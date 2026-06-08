package mdad.networkdata.lostfoundapp;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
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

import org.json.JSONObject;

public class ItemDetailsActivity extends AppCompatActivity {

    // UI components for showing item info + letting user submit a claim
    TextView tvTitle, tvInfo, tvItemStatus;
    EditText etClaimMessage;
    Button btnSubmitClaim;

    ImageView ivItemPhoto;


    // itemId comes from previous screen (ItemListActivity) using Intent extra
    String itemId;

    // default status in case Intent didn't pass it properly
    String itemStatus = "open";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_details);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // link UI from XML so we can set text / read message input
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        tvItemStatus = (TextView) findViewById(R.id.tvItemStatus);
        etClaimMessage = (EditText) findViewById(R.id.etClaimMessage);
        btnSubmitClaim = (Button) findViewById(R.id.btnSubmitClaim);
        ivItemPhoto = (ImageView) findViewById(R.id.ivItemPhoto);


        // read item details passed from previous screen
        itemId = getIntent().getStringExtra("item_id");
        String title = getIntent().getStringExtra("title");
        String type = getIntent().getStringExtra("type");
        String location = getIntent().getStringExtra("location");
        String status = getIntent().getStringExtra("status");

        // use status from intent if it exists (open/claimed/closed etc.)
        if (status != null) {
            itemStatus = status;
        }

        String imageBase64 = getIntent().getStringExtra("image");
        // DEBUG: verify image extra length
        // Toast.makeText(this,
        //        "image extra len = " + (imageBase64 == null ? "null" : imageBase64.length()),
        //        Toast.LENGTH_LONG).show();


        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            Bitmap bmp = decodeBase64ToBitmap(imageBase64);
            if (bmp != null) {
                ivItemPhoto.setImageBitmap(bmp);
                ivItemPhoto.setVisibility(View.VISIBLE);  // show only if valid
            }
        }


        // display item info on screen
        tvTitle.setText(title);
        tvInfo.setText(type + " | " + location);
        tvItemStatus.setText("Status: " + itemStatus.toUpperCase());

        // check role from session (admin shouldn't be able to submit claim)
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        String role = sp.getString("role", "user");

        if (role.equals("admin")) {
            btnSubmitClaim.setEnabled(false);
            btnSubmitClaim.setText("Admin Cannot Claim");
        }

        // if item already closed/claimed, disable claim button immediately
        if (!"open".equals(itemStatus)) {
            btnSubmitClaim.setEnabled(false);
            btnSubmitClaim.setText("Not Available");
        } else {
            // item is open, but still need to check if user is allowed to claim
            // (like for example, already submitted a pending claim before)
            checkClaimAllowed();
        }

        // submit claim when user clicks the button
        btnSubmitClaim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitClaim();
            }
        });
    }

    private void checkClaimAllowed() {

        // get user_id from session so backend knows who is claiming
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        int userId = sp.getInt("user_id", -1);

        // if user not logged in, prevent claim submission
        if (userId == -1) {
            btnSubmitClaim.setEnabled(false);
            btnSubmitClaim.setText("Login Required");
            return;
        }

        // build GET url with query params so PHP can check conditions
        String url = ApiConfig.CHECK_CLAIM_ALLOWED + "?item_id=" + itemId + "&user_id=" + userId;

        RequestQueue queue = Volley.newRequestQueue(ItemDetailsActivity.this);

        // send GET request to check if user can claim this item
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        // backend returns success + allowed + reason (if not allowed)
                        int success = response.optInt("success", 0);

                        if (success == 1) {
                            int allowed = response.optInt("allowed", 1);
                            String reason = response.optString("reason", "");

                            if (allowed == 0) {
                                // not allowed -> disable button and optionally show reason
                                btnSubmitClaim.setEnabled(false);
                                btnSubmitClaim.setText("Cannot Claim");
                                if (!reason.equals("")) {
                                    Toast.makeText(ItemDetailsActivity.this, reason, Toast.LENGTH_LONG).show();
                                }
                            } else {
                                // allowed -> user can submit claim normally
                                btnSubmitClaim.setEnabled(true);
                                btnSubmitClaim.setText("Submit Claim");
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // if this fails, i let backend handle the final validation later
                    }
                }
        );

        // this actually sends the request
        queue.add(req);
    }

    private void submitClaim() {

        // claim message is required so admin can review properly
        String message = etClaimMessage.getText().toString().trim();
        if (message.equals("")) {
            Toast.makeText(this, "Please enter a claim message", Toast.LENGTH_SHORT).show();
            return;
        }

        // get user_id from session again to send to backend
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        int userId = sp.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            // pack claim data into JSON body (this is what PHP expects)
            JSONObject body = new JSONObject();
            body.put("item_id", Integer.parseInt(itemId));
            body.put("user_id", userId);
            body.put("message", message);

            RequestQueue queue = Volley.newRequestQueue(this);

            // send POST request to submit claim for admin review
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    ApiConfig.SUBMIT_CLAIM,
                    body,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            // backend returns success + message
                            int success = response.optInt("success", 0);
                            String msg = response.optString("message", "");

                            if (success == 1) {
                                Toast.makeText(ItemDetailsActivity.this, "Claim submitted for review", Toast.LENGTH_SHORT).show();
                                finish(); // go back after submission
                            } else {
                                Toast.makeText(ItemDetailsActivity.this, "Failed: " + msg, Toast.LENGTH_LONG).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(ItemDetailsActivity.this, "Server error", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

            // sends the POST request
            queue.add(req);

        } catch (Exception e) {
            // covers JSON build issues / parse errors etc.
            Toast.makeText(this, "Error submitting claim", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        try {
            byte[] bytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }


}
