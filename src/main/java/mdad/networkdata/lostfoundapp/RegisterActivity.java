package mdad.networkdata.lostfoundapp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONException;
import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    // input fields for user registration
    EditText etName, etEmail, etPassword;
    Button btnRegister;
    TextView tvPasswordHint; // small hint text to guide user for stronger password

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // link UI from XML to Java
        etName = (EditText) findViewById(R.id.etName);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnRegister = (Button) findViewById(R.id.btnRegister);
        tvPasswordHint = (TextView) findViewById(R.id.tvPasswordHint);

        // when register button is clicked, validate and send to server
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // read user input
                String name = etName.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString();

                // check password strength first (shows hint text if weak)
                if (!isPasswordValid(password)) {
                    tvPasswordHint.setVisibility(View.VISIBLE);
                    Toast.makeText(RegisterActivity.this,
                            "Please use a stronger password",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // basic empty field validation
                if (name.equals("") || email.equals("") || password.equals("")) {
                    Toast.makeText(RegisterActivity.this,
                            "Please fill in all fields",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // build JSON body to send to PHP backend
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("name", name);
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // create volley queue (handles request sending)
                RequestQueue queue = Volley.newRequestQueue(RegisterActivity.this);

                // POST request to register endpoint (converted lambdas -> anonymous classes)
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        ApiConfig.REGISTER,
                        jsonBody,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {

                                // read server response
                                int success = response.optInt("success", 0);
                                String message = response.optString("message");

                                if (success == 1) {
                                    Toast.makeText(RegisterActivity.this,
                                            "Registered successfully",
                                            Toast.LENGTH_SHORT).show();
                                    finish(); // go back to previous page (login)
                                } else {
                                    // backend message usually says email exists / error etc.
                                    Toast.makeText(RegisterActivity.this,
                                            message,
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // handles no internet / server down / wrong url etc.
                                Toast.makeText(RegisterActivity.this,
                                        "Error connecting to server",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                );

                // this actually sends the request
                queue.add(request);
            }
        });

        // live password hint while typing (so user knows immediately if password is weak)
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePasswordHint(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

    }

    private boolean isPasswordValid(String password) {
        // simple password rules: length + uppercase + lowercase + number
        if (password.length() < 8) return false;
        if (!password.matches(".*[A-Z].*")) return false;
        if (!password.matches(".*[a-z].*")) return false;
        if (!password.matches(".*[0-9].*")) return false;
        return true;
    }

    private void updatePasswordHint(String password) {
        // hide hint if empty (so it doesn’t look “angry” when user hasn’t typed yet)
        if (password.isEmpty()) {
            tvPasswordHint.setVisibility(View.GONE);
            return;
        }

        // show hint only when password is not meeting the rules
        if (isPasswordValid(password)) {
            tvPasswordHint.setVisibility(View.GONE);
        } else {
            tvPasswordHint.setVisibility(View.VISIBLE);
        }
    }

}
