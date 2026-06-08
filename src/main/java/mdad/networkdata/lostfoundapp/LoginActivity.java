package mdad.networkdata.lostfoundapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {

    // input fields for login
    EditText etEmail, etPassword;
    Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // link XML components to Java
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);

        // when login button is clicked, attempt authentication
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // get user input
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                // basic validation to ensure fields are not empty
                if (email.equals("") || password.equals("")) {
                    Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    // build JSON body to send login details to backend
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);

                    // create Volley request queue
                    RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);

                    // send POST request to login endpoint (no lambda, use anonymous classes)
                    JsonObjectRequest req = new JsonObjectRequest(
                            Request.Method.POST,
                            ApiConfig.LOGIN,
                            jsonBody,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {

                                    // check success flag returned from server
                                    int success = response.optInt("success", 0);

                                    if (success == 1) {

                                        // get user details from response
                                        JSONObject user = response.optJSONObject("user");
                                        int userId = user.optInt("id");
                                        String name = user.optString("name");
                                        String role = user.optString("role");

                                        // save login session using SharedPreferences
                                        // this lets other activities know who is logged in
                                        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
                                        sp.edit()
                                                .putInt("user_id", userId)
                                                .putString("name", name)
                                                .putString("role", role)
                                                .apply();

                                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                                        // go to home page after login
                                        Intent i = new Intent(LoginActivity.this, UserHomeActivity.class);
                                        startActivity(i);
                                        finish();

                                    } else {
                                        // show message returned from server if login fails
                                        Toast.makeText(LoginActivity.this,
                                                response.optString("message", "Login failed"),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    // handles network/server connection errors
                                    Toast.makeText(LoginActivity.this, "Error connecting to server", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );

                    // this actually sends the request
                    queue.add(req);

                } catch (Exception e) {
                    // catch any unexpected errors during JSON creation
                    Toast.makeText(LoginActivity.this, "Login error", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
