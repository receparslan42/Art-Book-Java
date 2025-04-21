package com.receparslan.artbook;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.receparslan.artbook.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding; // View binding

    RecyclerView recyclerView; // Recycler view

    ArrayList<Art> artList; // List of arts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater()); // Initialize view binding
        setContentView(binding.getRoot()); // Set the content view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set the custom action bar
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar);

        // Get the arts from the database
        try (SQLiteDatabase database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null)) {
            database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, name VARCHAR, artist VARCHAR, date VARCHAR, image BLOB)");

            try (Cursor cursor = database.rawQuery("SELECT * FROM arts", null)) {
                // Get the indexes
                int artIDIdx = cursor.getColumnIndex("id");
                int artNameIdx = cursor.getColumnIndex("name");
                int artistNameIdx = cursor.getColumnIndex("artist");
                int dateIdx = cursor.getColumnIndex("date");
                int imageIdx = cursor.getColumnIndex("image");

                // Initialize the list of arts
                artList = new ArrayList<>();
                while (cursor.moveToNext()) {
                    Art art = new Art();
                    art.setId(cursor.getInt(artIDIdx));
                    art.setName(cursor.getString(artNameIdx));
                    art.setArtistName(cursor.getString(artistNameIdx));
                    art.setDate(cursor.getString(dateIdx));
                    art.setImage(BitmapFactory.decodeByteArray(cursor.getBlob(imageIdx), 0, cursor.getBlob(imageIdx).length));
                    artList.add(art);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // Set the recycler view
        recyclerView = binding.recyclerView;
        recyclerView.setAdapter(new RecyclerAdapter(artList));
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getApplicationContext()));
    }

    // Inflate the options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // Handle the options menu item clicks
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addAnArt) {
            Intent intent = new Intent(MainActivity.this, AddingActivity.class);
            intent.putExtra("key", "add");
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}