package com.cg.lrceditor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class CreateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);
    }


    public void startEditor(View view) {
        EditText editText = findViewById(R.id.lyrics_textbox);
        String data = editText.getText().toString().trim();

        if(data.isEmpty()) {
            Toast.makeText(this, "You haven't typed/pasted any lyrics", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EditorActivity.class);
        intent.putExtra("LYRICS", data.split("\\n"));
        startActivity(intent);
    }
}
