package com.cg.lrceditor;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.app_version);
        version.setText(String.format(Locale.getDefault(), "Version %s", BuildConfig.VERSION_NAME));
    }

    public void rate_and_review(View view) {
        Uri uri = Uri.parse("market://details?id=" + this.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
        }
    }

    public void send_feedback(View view) {
        String deviceInfo = "";
        deviceInfo += "\n OS Version: " + System.getProperty("os.version") + "(" + android.os.Build.VERSION.INCREMENTAL + ")";
        deviceInfo += "\n OS API Level: " + android.os.Build.VERSION.SDK_INT;
        deviceInfo += "\n Device: " + android.os.Build.DEVICE;
        deviceInfo += "\n Model and Product: " + android.os.Build.MODEL + " ("+ android.os.Build.PRODUCT + ")";

        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto",getString(R.string.dev_email), null));
        intent.putExtra(Intent.EXTRA_SUBJECT, "LRC Editor Feedback");
        intent.putExtra(Intent.EXTRA_TEXT, "Enter your feedback/bug report here\n\n" + deviceInfo);
        startActivity(Intent.createChooser(intent, "Send Feedback:"));
    }
}
