package com.cg.lrceditor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST = 1;
    private SharedPreferences settings;
    private TextView saveLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        saveLocation = findViewById(R.id.save_location);
        settings = getSharedPreferences("LRC Editor Preferences", MODE_PRIVATE);

        String location = settings.getString("saveLocation", Environment.getExternalStorageDirectory().getPath() + "/Lyrics/");
        saveLocation.setText(location);
    }

    public void changeSaveLocation(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        startActivityForResult(intent, LOCATION_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == LOCATION_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                if (uri != null) {
                    SharedPreferences.Editor editor = settings.edit();

                    String realPath = FileUtil.getFullPathFromTreeUri(uri, this);

                    editor.putString("saveLocation", realPath);
                    try {
                        if (!(uri.getPath().split("/")[2].startsWith("primary")))
                            editor.putString("saveUri", uri.toString());
                        else
                            editor.putString("saveUri", null);
                    } catch (ArrayIndexOutOfBoundsException ignored) {
                        editor.putString("saveUri", null);
                    }
                    editor.apply();
                    saveLocation.setText(realPath);
                }
            }
        }
    }
}
