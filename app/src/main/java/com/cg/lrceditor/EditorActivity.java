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
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EditorActivity extends AppCompatActivity implements LyricListAdapter.ItemClickListener,
        MediaPlayer.OnPreparedListener,
        SeekBar.OnSeekBarChangeListener,
        MediaPlayer.OnCompletionListener {

    private static final int FILE_REQUEST = 1;

    private ArrayList<ItemData> lyricData;

    private RecyclerView mRecyclerView;
    private LyricListAdapter mAdapter;

    private boolean isPlaying = false;
    private boolean updateBusy = false;
    private boolean playerPrepared = false;
    private boolean stopUpdating = false;
    private boolean firstStart = true;
    private boolean changedData = false;

    private Uri uri = null;

    private ActionModeCallback actionModeCallback;
    private ActionMode actionMode;

    private Handler songTimeUpdater = new Handler();
    private SeekBar seekbar;
    private MediaPlayer player;

    private Handler timestampUpdater = new Handler();
    private int longPressed = 0;
    private int longPressedPos = -1;

    private TextView startText, endText;
    private TextView titleText;
    private Button play_pause;

    private Runnable updateTimestamp = new Runnable() {
        @Override
        public void run() {
            if (longPressedPos == -1)
                return;

            String time = mAdapter.lyricData.get(longPressedPos).getTimestamp();
            long milli = timeToMilli(time);

            if (longPressed == 1) {
                milli += 100;
            } else if (longPressed == -1) {
                milli -= 100;
            }

            if (milli < 0)
                milli = 0;

            mAdapter.lyricData.get(longPressedPos).setTimestamp(
                    String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
            mAdapter.notifyItemChanged(longPressedPos);

            if (longPressed != 0 && milli != 0)
                timestampUpdater.postDelayed(this, 50);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Toolbar toolbar = findViewById(R.id.Editortoolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();

        if (intent.getData() != null) {                /* LRC File opened from elsewhere */
            LyricReader r = new LyricReader(intent.getData(), this);
            if (!r.readLyrics()) {
                Toast.makeText(this, r.getErrorMsg(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String[] lyrics = r.getLyrics();
            String[] timestamps = r.getTimestamps();
            populateDataSet(lyrics, timestamps);

        } else {
            String[] lyrics = intent.getStringArrayExtra("LYRICS");
            String[] timestamps = intent.getStringArrayExtra("TIMESTAMPS");
            populateDataSet(lyrics, timestamps);
        }

        mRecyclerView = findViewById(R.id.recyclerview);
        mAdapter = new LyricListAdapter(this, lyricData);
        mAdapter.setClickListener(this);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);

        actionModeCallback = new ActionModeCallback();

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

    private void populateDataSet(String[] lyrics, String[] timestamps) {
        lyricData = new ArrayList<>();

        if (!lyrics[0].trim().isEmpty())
            lyricData.add(new ItemData("", "00:00.00"));

        for (int i = 0, len = lyrics.length; i < len; i++)
            try {
                lyricData.add(new ItemData(lyrics[i].trim(), timestamps[i].trim()));
            } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
                lyricData.add(new ItemData(lyrics[i].trim(), null));
            }

        if (!lyrics[lyrics.length - 1].trim().isEmpty())
            lyricData.add(new ItemData("", null));
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

        changedData = true;

        mAdapter.lyricData.get(position).setTimestamp(
                String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(pos), getSeconds(pos), getMilli(pos)));
        mAdapter.notifyItemChanged(position);
        mRecyclerView.smoothScrollToPosition(position + 1);
    }

    @Override
    public void onPlayButtonClick(int position) {
        if (!playerPrepared) {
            Toast.makeText(this, "Player not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        String time = mAdapter.lyricData.get(position).getTimestamp();
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
        longPressed = 0;
        longPressedPos = -1;

        String time = mAdapter.lyricData.get(position).getTimestamp();
        long milli = timeToMilli(time);
        milli += 100;
        mAdapter.lyricData.get(position).setTimestamp(
                String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
        mAdapter.notifyItemChanged(position);

        changedData = true;

        if (playerPrepared) {
            player.seekTo((int) milli);
            play_pause.setText(R.string.pause_text);
            player.start();
            isPlaying = true;
            songTimeUpdater.post(updateSongTime);
        }
    }

    @Override
    public void onDecreaseTimeClick(int position) {
        longPressed = 0;
        longPressedPos = -1;

        String time = mAdapter.lyricData.get(position).getTimestamp();
        long milli = timeToMilli(time);
        milli -= 100;
        if (milli < 0)
            milli = 0;
        mAdapter.lyricData.get(position).setTimestamp(
                String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
        mAdapter.notifyItemChanged(position);

        changedData = true;

        if (playerPrepared) {
            player.seekTo((int) milli);
            play_pause.setText(R.string.pause_text);
            player.start();
            isPlaying = true;
            songTimeUpdater.post(updateSongTime);
        }
    }

    @Override
    public void onLongPressIncrTime(int position) {
        longPressedPos = position;
        longPressed = 1;

        timestampUpdater.post(updateTimestamp);

        changedData = true;
    }

    @Override
    public void onLongPressDecrTime(int position) {
        longPressedPos = position;
        longPressed = -1;

        timestampUpdater.post(updateTimestamp);

        changedData = true;
    }

    @Override
    public void onLyricItemSelected(int position) {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback);
        }

        toggleSelection(position);
    }

    @Override
    public void onLyricItemClicked(int position) {
        if (actionMode == null)
            return;

        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectionCount();

        if (count == 0) {
            actionMode.finish();
            actionMode = null;
        } else {
            Menu menu = actionMode.getMenu();
            MenuItem itemEdit = menu.findItem(R.id.action_edit);
            MenuItem lyric_before = menu.findItem(R.id.action_add_before);
            MenuItem lyric_after = menu.findItem(R.id.action_add_after);
            if (count >= 2) {
                itemEdit.setVisible(false);
                lyric_before.setVisible(false);
                lyric_after.setVisible(false);
            } else {
                itemEdit.setVisible(true);
                lyric_before.setVisible(true);
                lyric_after.setVisible(true);
            }

            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
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
            Toast.makeText(this, "Whoops " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void selectAll() {
        mAdapter.selectAll();
        int count = mAdapter.getSelectionCount();

        actionMode.setTitle(String.valueOf(count));
        actionMode.invalidate();
    }

    private void removeLyrics() {
        List<Integer> selectedItemPositions =
                mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            mAdapter.removeLyrics(selectedItemPositions.get(i));
        }
        mAdapter.notifyDataSetChanged();

        actionMode = null;
    }

    private void offsetTimestamps(int milli) {
        List<Integer> selectedItemPositions = mAdapter.getSelectedItems();

        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            String timestamp = mAdapter.lyricData.get(selectedItemPositions.get(i)).getTimestamp();
            if (timestamp == null) {
                timestamp = "00:00.00";
            }
            int time = timeToMilli(timestamp) + milli;
            if (time < 0)
                time = 0;
            timestamp = String.format(Locale.getDefault(), "%02d:%02d.%02d",
                    getMinutes(time), getSeconds(time), getMilli(time));
            mAdapter.lyricData.get(selectedItemPositions.get(i)).setTimestamp(timestamp);
            mAdapter.timestampSet(selectedItemPositions.get(i));
        }

        mAdapter.notifyDataSetChanged();
    }

    private void edit_lyric_data(final int lyric_change) {
        final int position = mAdapter.getSelectedItems().get(0);

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_layout, null);
        final EditText editText = view.findViewById(R.id.dialog_edittext);
        TextView textView = view.findViewById(R.id.dialog_prompt);

        String hint = null, positive_button_text = null;

        if (lyric_change == 1) {         /* Add before */

            textView.setText(getString(R.string.add_before_prompt));
            positive_button_text = getString(R.string.add_before_positive_button_text);
            hint = getString(R.string.add_before_hint);

        } else if (lyric_change == 2) {  /* Edit selected lyric */

            textView.setText(getString(R.string.edit_prompt));
            editText.setText(mAdapter.lyricData.get(position).getLyric());

            positive_button_text = getString(R.string.edit_positive_button_text);
            hint = getString(R.string.edit_lyrics_hint);

        } else if (lyric_change == 3) {  /* Add after */

            textView.setText(getString(R.string.add_after_prompt));
            positive_button_text = getString(R.string.add_after_positive_button_text);
            hint = getString(R.string.add_after_hint);
        }

        editText.setHint(hint);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(positive_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changedData = true;

                        if (lyric_change == 1) {         /* Add before */

                            mAdapter.lyricData.add(position,
                                    new ItemData(editText.getText().toString(), null));
                            mAdapter.notifyNewTimestamp(position);
                            mAdapter.notifyDataSetChanged();

                        } else if (lyric_change == 2) {  /* Edit selected lyric */

                            mAdapter.lyricData.get(position).setLyric(editText.getText().toString());
                            mAdapter.notifyItemChanged(position);

                        } else if (lyric_change == 3) {  /* Add after */

                            mAdapter.lyricData.add(position + 1,
                                    new ItemData(editText.getText().toString(), null));
                            mAdapter.notifyNewTimestamp(position + 1);
                            mAdapter.notifyDataSetChanged();

                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void batch_edit_lyrics() {

        LayoutInflater inflater = this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.batch_edit_dialog, null);
        final TextView batchTimestamp = view.findViewById(R.id.batch_item_time);

        final Handler batchTimestampUpdater = new Handler();
        final int[] longPressed = {0};
        final boolean[] batchTimeNegative = {false};

        final Runnable updateBatchTimestamp = new Runnable() {
            @Override
            public void run() {
                String time = batchTimestamp.getText().toString();

                if (batchTimeNegative[0])
                    time = time.substring(1);

                long milli = timeToMilli(time);

                if (longPressed[0] == 1) {
                    if (!batchTimeNegative[0]) {
                        milli += 100;
                    } else {
                        milli -= 100;
                    }

                    if (batchTimeNegative[0]) {
                        batchTimeNegative[0] = !(milli <= 0);
                        if (milli < 0)
                            milli = -milli;
                    }
                } else if (longPressed[0] == -1) {
                    if (!batchTimeNegative[0]) {
                        milli -= 100;
                    } else {
                        milli += 100;
                    }

                    if (!batchTimeNegative[0]) {
                        batchTimeNegative[0] = milli < 0;
                        if (milli < 0)
                            milli = -milli;
                    }
                }

                if (!batchTimeNegative[0] || milli == 0) {
                    batchTimestamp.setText(
                            String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
                } else {
                    batchTimestamp.setText(
                            String.format(Locale.getDefault(), "-%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
                }

                if (longPressed[0] != 0)
                    timestampUpdater.postDelayed(this, 50);
            }
        };


        Button increase = view.findViewById(R.id.batch_increase_time_button);
        increase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                longPressed[0] = 0;

                String time = batchTimestamp.getText().toString();

                if (batchTimeNegative[0])
                    time = time.substring(1);

                long milli = timeToMilli(time);
                if (!batchTimeNegative[0]) {
                    milli += 100;
                } else {
                    milli -= 100;
                }

                if (batchTimeNegative[0]) {
                    batchTimeNegative[0] = !(milli <= 0);
                    if (milli < 0)
                        milli = -milli;
                }

                if (!batchTimeNegative[0]) {
                    batchTimestamp.setText(
                            String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
                } else {
                    batchTimestamp.setText(
                            String.format(Locale.getDefault(), "-%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
                }
            }
        });

        increase.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                longPressed[0] = 1;

                batchTimestampUpdater.post(updateBatchTimestamp);
                return false;
            }
        });

        Button decrease = view.findViewById(R.id.batch_decrease_time_button);
        decrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                longPressed[0] = 0;

                String time = batchTimestamp.getText().toString();

                if (batchTimeNegative[0])
                    time = time.substring(1);

                long milli = timeToMilli(time);
                if (!batchTimeNegative[0]) {
                    milli -= 100;
                } else {
                    milli += 100;
                }

                if (!batchTimeNegative[0]) {
                    batchTimeNegative[0] = milli < 0;
                    if (milli < 0)
                        milli = -milli;
                }

                if (!batchTimeNegative[0] || milli == 0) {
                    batchTimestamp.setText(
                            String.format(Locale.getDefault(), "%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
                } else {
                    batchTimestamp.setText(
                            String.format(Locale.getDefault(), "-%02d:%02d.%02d", getMinutes(milli), getSeconds(milli), getMilli(milli)));
                }
            }
        });

        decrease.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                longPressed[0] = -1;

                batchTimestampUpdater.post(updateBatchTimestamp);
                return false;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Batch Edit")
                .setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        changedData = true;

                        if (batchTimeNegative[0]) {
                            String timestamp = batchTimestamp.getText().toString().substring(1);
                            offsetTimestamps(-timeToMilli(timestamp));
                        } else {
                            String timestamp = batchTimestamp.getText().toString();
                            offsetTimestamps(timeToMilli(timestamp));
                        }

                        actionMode.finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
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
                intent.putExtra("lyricData", this.lyricData);
                intent.putExtra("URI", uri);

                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (changedData) {
            new AlertDialog.Builder(this)
                    .setTitle("Warning")
                    .setMessage("You'll lose your modified data if you go back. Are you sure you want to go back?")
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

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_toolbar, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int lyric_change;
            switch (item.getItemId()) {
                case R.id.action_delete:
                    removeLyrics();
                    mode.finish();
                    return true;

                case R.id.action_edit:
                    lyric_change = 2;
                    edit_lyric_data(lyric_change);
                    mode.finish();
                    return true;

                case R.id.action_select_all:
                    selectAll();
                    return true;

                case R.id.action_add_before:
                    lyric_change = 1;
                    edit_lyric_data(lyric_change);
                    mode.finish();
                    return true;

                case R.id.action_add_after:
                    lyric_change = 3;
                    edit_lyric_data(lyric_change);
                    mode.finish();
                    return true;

                case R.id.action_batch_edit:
                    batch_edit_lyrics();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            actionMode = null;
        }
    }
}
