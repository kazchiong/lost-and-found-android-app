package mdad.networkdata.lostfoundapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UserHomeActivity extends AppCompatActivity {

    TextView tvWelcome;
    Button btnViewItems, btnAddItem, btnMyClaims, btnAdminClaims, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_home);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // link buttons/textview from XML so we can use them in Java
        tvWelcome = (TextView) findViewById(R.id.tvWelcome);
        btnViewItems = (Button) findViewById(R.id.btnViewItems);
        btnAddItem = (Button) findViewById(R.id.btnAddItem);
        btnMyClaims = (Button) findViewById(R.id.btnMyClaims);
        btnAdminClaims = (Button) findViewById(R.id.btnAdminClaims);  // only for admin role
        btnLogout = (Button) findViewById(R.id.btnLogout);

        // read session info saved during login (who is the user + what role)
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        int userId = sp.getInt("user_id", -1);
        String name = sp.getString("name", "User");
        String role = sp.getString("role", "user");

        // safety check: if no session, user should not be here, send back to main page
        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // simple welcome message using stored user name
        tvWelcome.setText("Welcome " + name + "!");

        // opens list of items (shows all items)
        btnViewItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(UserHomeActivity.this, ItemListActivity.class));
            }
        });

        // opens add item page (user posts lost/found item)
        btnAddItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(UserHomeActivity.this, AddItemActivity.class));
            }
        });

        // My Claims button is only meant for normal users (admins don't need it)
        if (!role.equals("admin")) {
            btnMyClaims.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(UserHomeActivity.this, MyClaimsActivity.class);
                    startActivity(i);
                }
            });
        } else {
            // hide button so admin UI doesn't show irrelevant options
            btnMyClaims.setVisibility(View.GONE);
        }

        // Admin Claims button is only for admin role
        if (role.equals("admin")) {
            btnAdminClaims.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(UserHomeActivity.this, AdminClaimsActivity.class));
                }
            });
        } else {
            // hide it from normal users so they can't access admin feature
            btnAdminClaims.setVisibility(View.GONE);
        }

        // logout clears session and brings user back to MainActivity
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sp.edit().clear().apply(); // remove saved user_id/name/role
                Toast.makeText(UserHomeActivity.this, "Logged out", Toast.LENGTH_SHORT).show();

                // clear task stack so user cannot press back to return to logged-in pages
                Intent i = new Intent(UserHomeActivity.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        });

    }
}
