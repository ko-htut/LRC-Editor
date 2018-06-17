package com.cg.lrceditor;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;

public class HomePage extends AppCompatActivity implements HomePageListAdapter.LyricFileSelectListener {

    private static final int WRITE_EXTERNAL_REQUEST = 1;
    private boolean permissionAlreadyGranted = false;
    private boolean scannedOnce = false;

    private String saveLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(HomePage.this, CreateActivity.class);
                startActivity(intent);
            }
        });

        saveLocation = getSharedPreferences("LRC Editor Preferences", MODE_PRIVATE)
                .getString("saveLocation", Environment.getExternalStorageDirectory().getPath() + "/Lyrics");

        ready_fileIO();
        if (permissionAlreadyGranted) {
            scan_lyrics();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        saveLocation = getSharedPreferences("LRC Editor Preferences", MODE_PRIVATE)
                .getString("saveLocation", Environment.getExternalStorageDirectory().getPath() + "/Lyrics");
        if (permissionAlreadyGranted)
            scan_lyrics();
    }

    private void scan_lyrics() {
        File f = new File(saveLocation);

        TextView empty_textview = findViewById(R.id.empty_message_textview);
        RecyclerView r = findViewById(R.id.recyclerview);

        if (!f.exists()) {
            if (!f.mkdir()) {
                Toast.makeText(this, "Lyrics folder creation failed at " + f.getAbsolutePath(), Toast.LENGTH_LONG).show();
                Toast.makeText(this, "Make sure you have granted permissions", Toast.LENGTH_LONG).show();
                finish();
            }
            empty_textview.setVisibility(View.VISIBLE);
            r.setVisibility(View.GONE);
        } else {
            int count = f.listFiles().length;
            if (count > 0) {
                boolean lyricsAvailable = false;
                for (File file : f.listFiles())
                    if (file.getName().endsWith(".lrc")) {
                        lyricsAvailable = true;
                        break;
                    }

                if (lyricsAvailable) {
                    empty_textview.setVisibility(View.GONE);
                    r.setVisibility(View.VISIBLE);

                    ready_recyclerView(f);
                } else {
                    empty_textview.setVisibility(View.VISIBLE);
                    r.setVisibility(View.GONE);
                }
            }
        }
    }

    private void ready_recyclerView(File f) {
        RecyclerView recyclerView = findViewById(R.id.recyclerview);

        LinkedList<File> list = new LinkedList<>();
        for (File file : f.listFiles()) {
            if (file.getName().endsWith(".lrc")) {
                list.addLast(file);
            }
        }

        HomePageListAdapter adapter = new HomePageListAdapter(this, list);
        recyclerView.setAdapter(adapter);
        adapter.setClickListener(this);

        if (scannedOnce) {
            return;
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(dividerItemDecoration);

        scannedOnce = true;
    }

    private void ready_fileIO() {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "ERROR: Storage unavailable/busy", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= 23) /* 23 = Marshmellow */
            grantPermission();
    }

    private void grantPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            displayDialog();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_REQUEST);
        } else
            permissionAlreadyGranted = true;
    }

    private void displayDialog() {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("This app needs the read/write permission for viewing and saving the lyric files");
        dialog.setTitle("Need permissions");
        dialog.setCancelable(false);
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.sendMessage(handler.obtainMessage());
            }
        });
        dialog.show();

        try {
            Looper.loop(); /* Wait until the user acts upon the dialog */
        } catch (RuntimeException ignored) {
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_EXTERNAL_REQUEST) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        scan_lyrics();
                        return;
                    } else {
                        Toast.makeText(this, "LRC Editor cannot function without the storage permission", Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
            }
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_refresh:
                scan_lyrics();
                Toast.makeText(this, "List refreshed!", Toast.LENGTH_SHORT).show();
                return true;
            case R.id.action_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void fileSelected(String fileName) {
        LyricReader r = new LyricReader(saveLocation, fileName);
        if (!r.readLyrics()) {
            Toast.makeText(this, r.getErrorMsg(), Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("LYRICS", r.getLyrics());
        intent.putExtra("TIMESTAMPS", r.getTimestamps());

        startActivity(intent);
    }
}
