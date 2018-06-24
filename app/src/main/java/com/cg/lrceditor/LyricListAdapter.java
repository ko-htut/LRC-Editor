package com.cg.lrceditor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class LyricListAdapter extends RecyclerView.Adapter<LyricListAdapter.LyricViewHolder> {
    public final List<ItemData> lyricData;
    public boolean[] timestampVisible;
    private LayoutInflater mInflator;

    private ItemClickListener mClickListener;

    LyricListAdapter(Context context, List lyricData) {
        mInflator = LayoutInflater.from(context);
        this.lyricData = lyricData;
        timestampVisible = new boolean[this.lyricData.size()];

        for (int i = 0, len = this.lyricData.size(); i < len; i++) {
            if (this.lyricData.get(i).getTimestamp() != null) {
                timestampVisible[i] = true;
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull LyricListAdapter.LyricViewHolder holder, int position) {
        String mCurrent = lyricData.get(position).getLyric();
        holder.itemTextview.setText(mCurrent);

        if (timestampVisible[position]) {
            holder.itemTimeControls.setVisibility(View.VISIBLE);
            holder.itemplay.setEnabled(true);
            holder.itemTimeview.setText(lyricData.get(position).getTimestamp());
        } else {
            holder.itemTimeControls.setVisibility(View.INVISIBLE);
            holder.itemplay.setEnabled(false);
        }
    }

    @NonNull
    @Override
    public LyricListAdapter.LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View mItemView = mInflator.inflate(R.layout.lyriclist_item, parent, false);
        return new LyricViewHolder(mItemView, this);
    }

    @Override
    public int getItemCount() {
        return lyricData.size();
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onAddButtonClick(int position);

        void onRemoveButtonClick(int position);

        void onPlayButtonClick(int position);

        void onIncreaseTimeClick(int position);

        void onDecreaseTimeClick(int position);

        void onLongPressIncrTime(int position);

        void onLongPressDecrTime(int position);

    }

    class LyricViewHolder extends RecyclerView.ViewHolder {
        private final TextView itemTextview;
        private final LinearLayout itemTimeControls;
        private final TextView itemTimeview;
        private final Button itemplay;
        private final Button itemadd;
        final LyricListAdapter mAdapter;

        LyricViewHolder(View itemView, LyricListAdapter adapter) {
            super(itemView);
            itemTextview = itemView.findViewById(R.id.item_text);
            itemadd = itemView.findViewById(R.id.item_add);
            itemTimeControls = itemView.findViewById(R.id.item_time_controls);
            itemTimeview = itemView.findViewById(R.id.item_time);
            itemplay = itemView.findViewById(R.id.item_play);
            this.mAdapter = adapter;

            itemadd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    timestampVisible[getAdapterPosition()] = true;
                    if (mClickListener != null)
                        mClickListener.onAddButtonClick(getAdapterPosition());
                }
            });

            itemplay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickListener != null)
                        mClickListener.onPlayButtonClick(getAdapterPosition());
                }
            });

            Button incrTime = itemView.findViewById(R.id.increase_time_button);
            Button decrTime = itemView.findViewById(R.id.decrease_time_button);

            incrTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickListener != null)
                        mClickListener.onIncreaseTimeClick(getAdapterPosition());
                }
            });

            incrTime.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mClickListener != null) {
                        mClickListener.onLongPressIncrTime(getAdapterPosition());
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }
                    return false;
                }
            });

            decrTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickListener != null)
                        mClickListener.onDecreaseTimeClick(getAdapterPosition());
                }
            });

            decrTime.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (mClickListener != null) {
                        mClickListener.onLongPressDecrTime(getAdapterPosition());
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }
                    return false;
                }
            });

        }

    }
}
