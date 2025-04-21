package com.receparslan.artbook;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.receparslan.artbook.databinding.ActivityAddingBinding;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;

public class AddingActivity extends AppCompatActivity {

    private ActivityAddingBinding binding; // View binding

    // Smaller image for save the database
    private Bitmap smallerImage;

    private Art art; // New art

    // Activity result launcher for gallery
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            Intent intentFromResult = result.getData();
            if (intentFromResult != null) {
                Uri imageUri = intentFromResult.getData();
                if (imageUri != null) {
                    try {
                        // Check the sdk version for ImageDecoder
                        if (Build.VERSION.SDK_INT >= 28) {
                            // Return the image uri to the image bitmap with decoder
                            ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), imageUri);
                            art.setImage(ImageDecoder.decodeBitmap(source));
                            binding.artImageView.setImageBitmap(art.getImage());
                        } else {
                            // Return the image uri to the image bitmap with old way
                            ContentResolver contentResolver = getContentResolver();
                            try (InputStream inputStream = contentResolver.openInputStream(imageUri)) {
                                art.setImage(BitmapFactory.decodeStream(inputStream));
                                binding.artImageView.setImageBitmap(art.getImage());
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }
    });
    // Request permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new RequestPermission(), isGranted -> {
        // Check the permission
        if (isGranted) {
            // Permission granted and open the gallery
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        } else {
            // Permission denied and show a toast message
            Toast.makeText(this, "Permission needed to access the gallery", Toast.LENGTH_SHORT).show();
        }
    });
    private String query; // Query for database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityAddingBinding.inflate(getLayoutInflater()); // Initialize view binding
        setContentView(binding.getRoot()); // Set the content view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set the custom action bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);

        art = new Art(); // Initialize the new art

        // Set the art details when the edit button is clicked
        if (Objects.requireNonNull(getIntent().getStringExtra("key")).equals("edit")) {
            try (SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
                 Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ? ", new String[]{String.valueOf(getIntent().getIntExtra("artID", -1))})) {

                // Get the indexes
                int artNameIdx = cursor.getColumnIndex("name");
                int artistNameIdx = cursor.getColumnIndex("artist");
                int dateIdx = cursor.getColumnIndex("date");
                int imageIdx = cursor.getColumnIndex("image");

                // Set the art details
                if (cursor.moveToNext()) {
                    art.setId(getIntent().getIntExtra("artID", -1));
                    art.setName(cursor.getString(artNameIdx));
                    art.setArtistName(cursor.getString(artistNameIdx));
                    art.setDate(cursor.getString(dateIdx));
                    art.setImage(BitmapFactory.decodeByteArray(cursor.getBlob(imageIdx), 0, cursor.getBlob(imageIdx).length));

                    binding.artNameEditText.setText(art.getName());
                    binding.artistEditText.setText(art.getArtistName());
                    binding.artTimeEditText.setText(art.getDate());
                    binding.artImageView.setImageBitmap(art.getImage());
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        // Open the gallery when the art image is clicked and the permission is granted
        binding.artImageView.setOnClickListener(view -> {
            // Check the sdk version for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermission(view, Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                requestPermission(view, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        // Save the art to the database and go back to the home page when the save button is clicked
        binding.saveButton.setOnClickListener(view -> {
            // Set the art properties from the inputs
            art.setName(binding.artNameEditText.getText().toString());
            art.setArtistName(binding.artistEditText.getText().toString());
            art.setDate(binding.artTimeEditText.getText().toString());

            // Check the image is selected
            if (art.getImage() == null) {
                binding.artImageView.callOnClick();
            } else {
                // Make the selected image smaller and compress it
                ByteArrayOutputStream compressedImage = new ByteArrayOutputStream();
                smallerImage = makeSmallerImage(art.getImage(), 300);
                smallerImage.compress(Bitmap.CompressFormat.PNG, 50, compressedImage);

                // Set the query for the database
                if (Objects.requireNonNull(getIntent().getStringExtra("key")).equals("edit"))
                    query = "UPDATE  arts SET name = ?, artist = ?, date = ?, image = ? WHERE id = ?";
                else
                    query = "INSERT INTO arts (name, artist, date, image) VALUES (?,?,?,?)";

                // Save the art to the database
                try (SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)) {
                    SQLiteStatement sqLiteStatement = database.compileStatement(query);
                    sqLiteStatement.bindString(1, art.getName());
                    sqLiteStatement.bindString(2, art.getArtistName());
                    sqLiteStatement.bindString(3, art.getDate());
                    sqLiteStatement.bindBlob(4, compressedImage.toByteArray());
                    if (Objects.requireNonNull(getIntent().getStringExtra("key")).equals("edit"))
                        sqLiteStatement.bindLong(5, art.getId());
                    sqLiteStatement.execute();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

                // Finish the activity and go back to the home page
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the activity stack
                startActivity(intent);
            }
        });
    }

    // Request permission
    public void requestPermission(View view, String permission) {
        // Check the permission
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // Permission denied
            Snackbar.make(view, "Permission needed to access the gallery", Snackbar.LENGTH_INDEFINITE).setAction("Allow", v -> requestPermissionLauncher.launch(permission)).show();
        } else {
            // Request permission
            requestPermissionLauncher.launch(permission);
        }
    }

    // Make the image smaller
    public Bitmap makeSmallerImage(@NonNull Bitmap image, int maximumSize) {
        int width = image.getWidth(); // Image width
        int height = image.getHeight(); // Image height
        float bitmapRatio = (float) width / (float) height; // Image ratio

        // Check the image orientation
        if (bitmapRatio > 1) {
            //Landscape Image
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {
            //Portrait Image
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }
}