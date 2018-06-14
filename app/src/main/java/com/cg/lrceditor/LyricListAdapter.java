package com.cg.lrceditor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;

public class LyricListAdapter extends RecyclerView.Adapter<LyricListAdapter.LyricViewHolder> {
    private final LinkedList<String> mLyricList;
    public boolean[] item_visible;
    public String[] lyric_times;
    private LayoutInflater mInflator;

    private ItemClickListener mClickListener;

    LyricListAdapter(Context context, LinkedList<String> lyricList) {
        mInflator = LayoutInflater.from(context);
        this.mLyricList = lyricList;
        item_visible = new boolean[this.mLyricList.size()];
        lyric_times = new String[this.mLyricList.size()];
    }

    class LyricViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemTextview;
        private final LinearLayout itemTimeControls;
        private final TextView itemTimeview;
        private final Button itemplay;
        private final Button itembutton;
        final LyricListAdapter mAdapter;

        LyricViewHolder(View itemView, LyricListAdapter adapter) {
            super(itemView);
            itemTextview = itemView.findViewById(R.id.item_text);
            itembutton = itemView.findViewById(R.id.item_button);
            itemTimeControls = itemView.findViewById(R.id.item_time_controls);
            itemTimeview = itemView.findViewById(R.id.item_time);
            itemplay = itemView.findViewById(R.id.item_play);
            this.mAdapter = adapter;

            itembutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    item_visible[getAdapterPosition()] = true;
                    if (mClickListener != null) mClickListener.onAddButtonClick(getAdapterPosition());
                }
            });

            itemplay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mClickListener != null) mClickListener.onPlayButtonClick(getAdapterPosition());
                }
            });

            Button incrTime = itemView.findViewById(R.id.increase_time_button);
            Button decrTime = itemView.findViewById(R.id.decrease_time_button);

            incrTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mClickListener != null) mClickListener.onIncreaseTimeClick(getAdapterPosition());
                }
            });

            decrTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mClickListener != null) mClickListener.onDecreaseTimeClick(getAdapterPosition());
                }
            });
        }

    }

    @NonNull
    @Override
    public LyricListAdapter.LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflator.inflate(R.layout.lyriclist_item, parent, false);
        return new LyricViewHolder(mItemView, this);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricListAdapter.LyricViewHolder holder, int position) {
        String mCurrent = mLyricList.get(position);
        holder.itemTextview.setText(mCurrent);
        if(item_visible[position]) {
            holder.itemTimeControls.setVisibility(View.VISIBLE);
            holder.itemplay.setVisibility(View.VISIBLE);
            holder.itemTimeview.setText(lyric_times[position]);
        }
        else {
            holder.itemTimeControls.setVisibility(View.INVISIBLE);
            holder.itemplay.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mLyricList.size();
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }


    public interface ItemClickListener {
        void onAddButtonClick(int position);
        void onPlayButtonClick(int position);
        void onIncreaseTimeClick(int position);
        void onDecreaseTimeClick(int position);
    }
}
