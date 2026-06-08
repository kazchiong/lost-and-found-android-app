package mdad.networkdata.lostfoundapp;

import android.app.DatePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddItemActivity extends AppCompatActivity {

    // input fields for item details
    EditText etTitle, etDesc, etLocation, etDate;
    Button btnSubmit;

    // camera preview + capture button
    ImageView ivItemPreview;
    Button btnCapturePhoto;

    // radio buttons to choose lost or found
    RadioGroup rgType;
    RadioButton rbLost, rbFound;

    // camera constants / state
    private static final int REQ_CAPTURE = 101;
    private String currentPhotoPath = null;
    private String imageBase64 = ""; // this is what we send to PHP in JSON

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_item);     // shows the screen layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // check if user is logged in (session stored during login)
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        int userId = sp.getInt("user_id", -1);

        // if no valid session, redirect back to main page
        if (userId == -1) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(AddItemActivity.this, MainActivity.class));
            finish();
            return;
        }

        // link UI components from XML
        ivItemPreview = (ImageView) findViewById(R.id.ivItemPreview);
        btnCapturePhoto = (Button) findViewById(R.id.btnCapturePhoto);

        etTitle = (EditText) findViewById(R.id.etTitle);
        etDesc = (EditText) findViewById(R.id.etDesc);
        etLocation = (EditText) findViewById(R.id.etLocation);
        etDate = (EditText) findViewById(R.id.etDate);
        btnSubmit = (Button) findViewById(R.id.btnSubmit);

        rgType = (RadioGroup) findViewById(R.id.rgType);
        rbLost = (RadioButton) findViewById(R.id.rbLost);
        rbFound = (RadioButton) findViewById(R.id.rbFound);

        // when user clicks date field, show date picker dialog
        etDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        // when user clicks capture, open camera
        btnCapturePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        // when submit button is clicked, send data to server
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitItem();
            }
        });
    }

    private void showDatePicker() {

        // get current date as default selection
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dp = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int y, int m, int d) {

                        // show user-friendly format (DD-MM-YYYY)
                        String displayDate = String.format(Locale.US, "%02d-%02d-%04d", d, (m + 1), y);
                        etDate.setText(displayDate);

                        // store MySQL format separately (YYYY-MM-DD) using tag
                        // this keeps display format and database format separate
                        String mysqlDate = String.format(Locale.US, "%04d-%02d-%02d", y, (m + 1), d);
                        etDate.setTag(mysqlDate);
                    }
                },
                year, month, day
        );

        dp.show();
    }


    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
            return;
        }

        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri photoUri = FileProvider.getUriForFile(
                this,
                "mdad.networkdata.lostfoundapp",
                photoFile
        );

        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        // grant permissions to the camera app
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("photo", photoUri));

        startActivityForResult(intent, REQ_CAPTURE);
    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "LF_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(fileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CAPTURE && resultCode == RESULT_OK) {
            if (currentPhotoPath == null) return;

            File imgFile = new File(currentPhotoPath);
            // DEBUG: verify camera wrote file
            // Toast.makeText(this, "File size: " + imgFile.length() + " bytes", Toast.LENGTH_LONG).show();

            if (!imgFile.exists()) return;

            // 1) preview on screen
            ivItemPreview.setImageURI(Uri.fromFile(imgFile));

            // 2) encode to base64 for JSON upload
            imageBase64 = encodeImageToBase64(currentPhotoPath);

            // DEBUG: verify base64 was created
            // Toast.makeText(this, "Image bytes (base64 len): " + imageBase64.length(), Toast.LENGTH_LONG).show();

            if (imageBase64.isEmpty()) {
                Toast.makeText(this, "Photo captured but failed to encode", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Photo attached", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String encodeImageToBase64(String path) {
        try {
            Bitmap bmp = BitmapFactory.decodeFile(path);
            if (bmp == null) return "";

            // Compress to reduce JSON payload size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, baos);

            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }




    private void submitItem() {

        // read input values from form
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String dateDisplay = etDate.getText().toString().trim();
        String dateMysql = (etDate.getTag() != null) ? etDate.getTag().toString() : "";

        // determine item type based on selected radio button
        String selectedType = rbLost.isChecked() ? "lost" : "found";

        // ensure date is selected properly
        if (dateDisplay.equals("") || dateMysql.equals("")) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show();
            return;
        }

        // basic validation to ensure no empty fields
        if (title.equals("") || desc.equals("") ||
                location.equals("") || dateMysql.equals("")) {

            Toast.makeText(AddItemActivity.this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // retrieve logged-in user_id from session again (extra safety)
        SharedPreferences sp = getSharedPreferences("session", MODE_PRIVATE);
        int userId = sp.getInt("user_id", -1);

        if (userId == -1) {
            Toast.makeText(AddItemActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // build JSON body to send to PHP backend
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("user_id", userId);
            jsonBody.put("type", selectedType);
            jsonBody.put("title", title);
            jsonBody.put("description", desc);
            jsonBody.put("location", location);
            jsonBody.put("date_reported", dateMysql);
            jsonBody.put("image", imageBase64);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // create Volley request queue
        RequestQueue queue = Volley.newRequestQueue(AddItemActivity.this);

        // send POST request to add item endpoint (no lambda, use anonymous classes)
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                ApiConfig.ADD_ITEM,
                jsonBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        int success = response.optInt("success", 0);
                        String message = response.optString("message", "");

                        // int hasKey = response.optInt("debug_has_image_key", -999);
                        // int imgLen = response.optInt("debug_image_len", -999);

                        if (success == 1) {
                            Toast.makeText(
                                    AddItemActivity.this,
                                    "Item added successfully.", //  hasImageKey=" + hasKey + " len=" + imgLen
                                    Toast.LENGTH_LONG
                            ).show();
                            finish();
                        } else {
                            Toast.makeText(AddItemActivity.this, "Failed: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        // handle network/server errors
                        String msg = "Volley error";

                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            msg = new String(error.networkResponse.data);
                        } else if (error.getMessage() != null) {
                            msg = error.getMessage();
                        }

                        Toast.makeText(AddItemActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                }
        );

        // this actually sends the request
        queue.add(request);
    }
}
