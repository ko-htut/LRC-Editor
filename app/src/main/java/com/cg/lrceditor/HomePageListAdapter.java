package com.cg.lrceditor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.util.LinkedList;

public class HomePageListAdapter extends RecyclerView.Adapter<HomePageListAdapter.item> {
    private LinkedList<File> mFileList;
    private LayoutInflater mInflator;

    private LyricFileSelectListener mClickListener;

    public HomePageListAdapter(Context context, LinkedList<File> fileList) {
        mInflator = LayoutInflater.from(context);
        this.mFileList = fileList;
    }

    class item extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final TextView songName;
        final HomePageListAdapter mAdapter;

        public item(View itemView, HomePageListAdapter adapter) {
            super(itemView);
            songName = itemView.findViewById(R.id.song_textview);
            this.mAdapter = adapter;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            String song_name = mFileList.get(getLayoutPosition()).getName();
            if(mClickListener != null) mClickListener.fileSelected(song_name);
        }
    }

    @NonNull
    @Override
    public HomePageListAdapter.item onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflator.inflate(R.layout.lyric_item, parent, false);
        return new item(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull HomePageListAdapter.item holder, int position) {
        String mCurrent = mFileList.get(position).getName();
        holder.songName.setText(mCurrent);
    }

    @Override
    public int getItemCount() {
        return mFileList.size();
    }

    void setClickListener(LyricFileSelectListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface LyricFileSelectListener {
        void fileSelected(String fileName);
    }
}

