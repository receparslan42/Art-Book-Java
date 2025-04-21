package com.receparslan.artbook;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.receparslan.artbook.databinding.ActivityDetailBinding;

import java.util.Objects;

public class DetailActivity extends AppCompatActivity {

    ActivityDetailBinding binding; // View

    Art art; // Selected art

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDetailBinding.inflate(getLayoutInflater()); // Initialize view binding
        setContentView(binding.getRoot()); // Set the content view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set the custom action bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);

        // Get the selected art
        art = new Art();
        art.setId(getIntent().getIntExtra("artID", -1));

        // Get the art details
        try (SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)) {
            try (Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id = ? ", new String[]{String.valueOf(art.getId())})) {
                // Get the indexes
                int artNameIdx = cursor.getColumnIndex("name");
                int artistNameIdx = cursor.getColumnIndex("artist");
                int dateIdx = cursor.getColumnIndex("date");
                int imageIdx = cursor.getColumnIndex("image");

                // Set the art details
                if (cursor.moveToNext()) {
                    art.setName(cursor.getString(artNameIdx));
                    art.setArtistName(cursor.getString(artistNameIdx));
                    art.setDate(cursor.getString(dateIdx));
                    art.setImage(BitmapFactory.decodeByteArray(cursor.getBlob(imageIdx), 0, cursor.getBlob(imageIdx).length));
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Set the art details
        binding.artImageView.setImageBitmap(art.getImage());
        binding.artNameTextView.setText(art.getName());
        binding.artistTextView.setText(art.getArtistName());
        binding.artTimeTextView.setText(art.getDate());

        // Edit the selected art
        binding.editButton.setOnClickListener(view -> {
            Intent intent = new Intent(DetailActivity.this, AddingActivity.class);
            intent.putExtra("key", "edit");
            intent.putExtra("artID", art.getId());
            startActivity(intent);
        });

        // Delete the selected art
        binding.deleteButton.setOnClickListener(view -> {
            AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
            confirmDialog.setMessage("Are you sure want to delete?");
            confirmDialog.setPositiveButton("Yes", (dialogInterface, i) -> {
                String query = "DELETE FROM arts WHERE id = ?";

                // Delete the art
                try (SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
                     SQLiteStatement statement = database.compileStatement(query)) {
                    statement.bindLong(1, art.getId());
                    statement.execute();
                }
                // Go to the home page
                Intent intent = new Intent(DetailActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
            confirmDialog.setNegativeButton("No", null);
            confirmDialog.show();
        });
    }
}