package com.cg.lrceditor;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class FinalizeActivity extends AppCompatActivity {

    private static final int WRITE_EXTERNAL_REQUEST = 1;

    private LinkedList<String> mLyricList;
    private String[] timestamps;
    private Uri uri;

    private EditText songName;
    private EditText artistName;
    private EditText albumName;
    private EditText composerName;

    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finalize);

        Intent intent = getIntent();
        mLyricList = new LinkedList<>((List<String>) intent.getSerializableExtra("LYRICS"));
        timestamps = intent.getStringArrayExtra("TIMESTAMPS");
        uri = intent.getParcelableExtra("URI");

        songName = findViewById(R.id.songName_edittext);
        artistName = findViewById(R.id.artistName_edittext);
        albumName = findViewById(R.id.albumName_edittext);
        composerName = findViewById(R.id.composer_edittext);

        resultTextView = findViewById(R.id.result_textview);

        if (Build.VERSION.SDK_INT >= 23) /* 23 = Marshmellow */
            grantPermission();

        if (uri != null) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(this, uri);

            songName.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
            albumName.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
            artistName.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            composerName.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER));
        }
    }

    public void saveLyrics(View view) {
        if (!isExternalStorageWritable()) {
            Toast.makeText(this, "ERROR: Storage unavailable/busy", Toast.LENGTH_LONG).show();
            return;
        }

        resultTextView.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= 23) /* 23 = Marshmellow */
            grantPermission();

        try (FileWriter writer = new FileWriter(new File(Environment.getExternalStorageDirectory().getPath() + "/Lyrics",
                songName.getText().toString() + ".lrc"))) {
            writer.write("[ar: " + artistName.getText().toString().trim()   + "]\n" +
                             "[al: " + albumName.getText().toString().trim()    + "]\n" +
                             "[ti: " + songName.getText().toString().trim()     + "]\n" +
                             "[au: " + composerName.getText().toString().trim() + "]\n" +
                             "\n" +
                             "[re:" + getString(R.string.app_name) + " - Android app" + "]\n" +
                             "[ve:" + getString(R.string.version_string) + "]\n" +
                             "\n");
            writer.write("[00:00.00]\n");
            for(int i = 0, len = timestamps.length; i < len; i++) {
                if(timestamps[i] != null) {
                    String lyric = mLyricList.get(i);
                    writer.write("[" + timestamps[i] + "]" + lyric + "\n");
                }
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            resultTextView.setTextColor(Color.rgb(198, 27, 27));
            resultTextView.setText(String.format(Locale.getDefault(), "Whoops! An Error Ocurred!\n%s", e));
            return;
        }

        resultTextView.setTextColor(Color.rgb(45, 168, 26));
        resultTextView.setText(String.format(Locale.getDefault(), "Successfully wrote the lyrics file at %s",
                Environment.getExternalStorageDirectory()
                + "/Lyrics/"
                + songName.getText().toString()
                        + ".lrc"));
    }

    private void grantPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            displayDialog();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_REQUEST);
        }
    }

    private void displayDialog() {

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                throw new RuntimeException();
            }
        };

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("This app needs the write permission for saving the lyric files");
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
                        Button button = findViewById(R.id.done_button);
                        button.performClick();
                        return;
                    } else {
                        Toast.makeText(this, "Cannot save the file without the storage permission", Toast.LENGTH_LONG).show();
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
}
