package com.cg.lrceditor;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EditorActivity extends AppCompatActivity implements LyricListAdapter.ItemClickListener, MediaPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener,
        MediaPlayer.OnCompletionListener {

    private static final int FILE_REQUEST = 1;

    private final LinkedList<String> mLyricList = new LinkedList<>();
    private String[] timestamps;
    private RecyclerView mRecyclerView;
    private LyricListAdapter mAdapter;

    private boolean isPlaying = false;
    private boolean updateBusy = false;
    private boolean playerPrepared = false;
    private boolean stopUpdating = false;
    private boolean firstStart = true;
    private boolean changedTimestamp = false;

    private Uri uri = null;

    private Handler songTimeUpdater = new Handler();
    private SeekBar seekbar;
    private MediaPlayer player;

    private TextView startText, endText;
    private TextView titleText;
    private Button play_pause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Toolbar toolbar = findViewById(R.id.Editortoolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        if (intent.getData() != null) {
            LyricReader r = new LyricReader(intent.getData(), this);
            if (!r.readLyrics()) {
                Toast.makeText(this, r.getErrorMsg(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            populateLyrics(r.getLyrics(), r.getTimestamps()[0]);
            this.timestamps = r.getTimestamps();
        } else {
            this.timestamps = intent.getStringArrayExtra("TIMESTAMPS");
            if (this.timestamps != null)
                populateLyrics(intent.getStringArrayExtra("LYRICS"), this.timestamps[0]);
            else
                populateLyrics(intent.getStringArrayExtra("LYRICS"), null);
        }

        mRecyclerView = findViewById(R.id.recyclerview);
        mAdapter = new LyricListAdapter(this, mLyricList);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        populateTimestamps(this.timestamps);
        mAdapter.notifyDataSetChanged();

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);

        seekbar = findViewById(R.id.seekBar);
        startText = findViewById(R.id.startText);
        endText = findViewById(R.id.endText);
        play_pause = findViewById(R.id.play_pause);
        titleText = findViewById(R.id.titleText);

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        seekbar.setOnSeekBarChangeListener(this);
    }

    private void populateLyrics(String[] lyrics, String firstTimeStamp) {
        for (String s : lyrics) {
            if (!(mLyricList.size() == 0 && s.equals("") &&
                    firstTimeStamp != null && timeToMilli(firstTimeStamp) == 0))
                mLyricList.addLast(s.trim());
        }

        if (!mLyricList.getLast().equals(""))
            mLyricList.addLast("");
    }

    private void populateTimestamps(String[] timestamps) {
        if (timestamps != null) {
            int offset = 0;
            if (timestamps[0].equals("00:00.00") || timestamps[0].equals("00:00:00"))
                offset++;
            for (int i = offset, len = timestamps.length; i < len; i++) {
                mAdapter.lyric_times[i - offset] = timestamps[i];
                mAdapter.item_visible[i - offset] = true;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isPlaying) {
            playPause(null);
        }
    }

    @Override
    public void onAddButtonClick(int position) {
        double pos;
        if (playerPrepared)
            pos = player.getCurrentPosition();
        else
            pos = 0;
        
        changedTimestamp = true;

        mAdapter.lyric_times[position] = String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(pos), getSeconds(pos), getMilli(pos));
        mAdapter.notifyItemChanged(position);
        mRecyclerView.smoothScrollToPosition(position + 1);
    }

    @Override
    public void onPlayButtonClick(int position) {
        if (!playerPrepared) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String time = mAdapter.lyric_times[position];
        player.seekTo(timeToMilli(time));
        if (!isPlaying) {
            play_pause.setText(R.string.pause_text);
            player.start();
            isPlaying = true;
        }
        songTimeUpdater.post(updateSongTime);
    }

    @Override
    public void onIncreaseTimeClick(int position) {
        String time = mAdapter.lyric_times[position];
        long milli = timeToMilli(time);
        milli += 100;
        mAdapter.lyric_times[position] = String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli));
        mAdapter.notifyItemChanged(position);
    }

    @Override
    public void onDecreaseTimeClick(int position) {
        String time = mAdapter.lyric_times[position];
        long milli = timeToMilli(time);
        milli -= 100;
        if (milli < 0)
            milli = 0;
        mAdapter.lyric_times[position] = String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli));
        mAdapter.notifyItemChanged(position);
    }

    private long getMinutes(double time) {
        return TimeUnit.MILLISECONDS.toMinutes((long) time);
    }

    private long getSeconds(double time) {
        return TimeUnit.MILLISECONDS.toSeconds((long) time) - TimeUnit.MINUTES.toSeconds(getMinutes(time));
    }

    private long getMilli(double time) {

        return ((long) (time - TimeUnit.SECONDS.toMillis(getSeconds(time)) - TimeUnit.MINUTES.toMillis(getMinutes(time)))) / 10;
    }

    private void readyMediaPlayer(Uri songUri) {
        try {
            player.setDataSource(this, songUri);
        } catch (IOException | IllegalArgumentException e) {
            Toast.makeText(this, "Whoops " + e, Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
        playerPrepared = false;
        uri = songUri;

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this, uri);
        titleText.setText(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));

        player.prepareAsync();
    }

    private Runnable updateSongTime = new Runnable() {
        @Override
        public void run() {
            if (stopUpdating) {
                return;
            }

            if (!updateBusy) {
                seekbar.setProgress(player.getCurrentPosition());
            }

            songTimeUpdater.postDelayed(this, 1000);
        }
    };


    public void playPause(View view) {
        if (!playerPrepared) {
            if (view != null) {
                Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (isPlaying) {
            play_pause.setText(R.string.play_text);
            player.pause();
        } else {
            play_pause.setText(R.string.pause_text);
            player.start();
        }
        isPlaying = !isPlaying;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playerPrepared = true;
        firstStart = false;
        stopUpdating = false;
        startText.setText(getString(R.string.default_timetext));
        double duration = player.getDuration();
        endText.setText(String.format(Locale.getDefault(), "%02d:%02d", getMinutes(duration), getSeconds(duration)));
        seekbar.setMax((int) duration);
        seekbar.setProgress(0);
        songTimeUpdater.post(updateSongTime);
    }

    public void rewind10(View view) {
        if (!playerPrepared) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        player.seekTo(player.getCurrentPosition() - 10 * 1000);
        songTimeUpdater.post(updateSongTime);
    }

    public void forward10(View view) {
        if (!playerPrepared) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        player.seekTo(player.getCurrentPosition() + 10 * 1000);
        songTimeUpdater.post(updateSongTime);
    }

    private int timeToMilli(String time) {
        try {
            return (int) TimeUnit.MINUTES.toMillis(Integer.parseInt(time.substring(0, 2)))
                    + (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(time.substring(3, 5)))
                    + (int) (TimeUnit.MILLISECONDS.toMillis(Integer.parseInt(time.substring(6, 8))) * 10);
        } catch (IndexOutOfBoundsException e) {
            return (int) TimeUnit.MINUTES.toMillis(Integer.parseInt(time.substring(0, 2)))
                    + (int) TimeUnit.SECONDS.toMillis(Integer.parseInt(time.substring(3, 5)));
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        startText.setText(String.format(Locale.getDefault(), "%02d:%02d", getMinutes(progress), getSeconds(progress)));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        updateBusy = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        player.seekTo(timeToMilli(startText.getText().toString()));
        updateBusy = false;
        songTimeUpdater.post(updateSongTime);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        play_pause.setText(R.string.play_text);
        isPlaying = false;
    }

    public void selectSong(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, FILE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == FILE_REQUEST && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                Uri uri = resultData.getData();

                play_pause.setText(R.string.play_text);
                isPlaying = false;

                player.reset();
                if (!firstStart) {
                    stopUpdating = true;
                    songTimeUpdater.post(updateSongTime);
                }
                readyMediaPlayer(uri);
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_editoractivity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_done:
                Intent intent = new Intent(this, FinalizeActivity.class);
                intent.putExtra("TIMESTAMPS", mAdapter.lyric_times);
                intent.putExtra("URI", uri);
                intent.putExtra("LYRICS", mLyricList);

                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (changedTimestamp) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("You'll lose your timestamps if you go back. Are you sure you want to go back?")
                    .setPositiveButton("Go Back", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            reset();
                            EditorActivity.super.onBackPressed();
                        }
                    })
                    .setNegativeButton("Stay here", null)
                    .show();
        } else {
            reset();
            EditorActivity.super.onBackPressed();
        }
    }

    private void reset() {
        stopUpdating = true;
        player.stop();
        play_pause.setText(R.string.play_text);
        isPlaying = false;
        playerPrepared = false;
        player.release();
    }
}
