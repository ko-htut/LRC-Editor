package com.cg.lrceditor;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LyricListAdapter extends RecyclerView.Adapter<LyricListAdapter.LyricViewHolder> {
    public final List<ItemData> lyricData;
    private boolean[] timestampVisible;
    private LayoutInflater mInflator;
    private SparseBooleanArray selectedItems;

    private ItemClickListener mClickListener;

    LyricListAdapter(Context context, List<ItemData> lyricData) {
        mInflator = LayoutInflater.from(context);
        this.lyricData = lyricData;
        timestampVisible = new boolean[this.lyricData.size()];
        selectedItems = new SparseBooleanArray();

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

        holder.itemView.setActivated(selectedItems.get(position, false));

        applyClickEvents(holder, position);
    }

    private void applyClickEvents(LyricListAdapter.LyricViewHolder holder, final int position) {
        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mClickListener.onLyricItemClicked(position);
            }
        });

        holder.linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mClickListener.onLyricItemSelected(position);
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                return true;
            }
        });
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

    public void timestampSet(int position) {
        timestampVisible[position] = true;
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
        }

        notifyItemChanged(pos);
    }

    public void selectAll() {
        for (int i = 0; i < getItemCount(); i++)
            selectedItems.put(i, true);
        notifyDataSetChanged();
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public void notifyNewTimestamp(int position) {
        boolean[] temp = new boolean[this.lyricData.size()];
        System.arraycopy(this.timestampVisible, 0, temp, 0, position);
        temp[position] = false;
        System.arraycopy(this.timestampVisible, position + 1 - 1, temp, position + 1, this.lyricData.size() - (position + 1));
        this.timestampVisible = temp;
    }


    public void removeLyrics(int position) {
        lyricData.get(position).setTimestamp(null);
        timestampVisible[position] = false;
    }

    public int getSelectionCount() {
        return selectedItems.size();
    }

    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onAddButtonClick(int position);

        void onPlayButtonClick(int position);

        void onIncreaseTimeClick(int position);

        void onDecreaseTimeClick(int position);

        void onLongPressIncrTime(int position);

        void onLongPressDecrTime(int position);

        void onLyricItemSelected(int position);

        void onLyricItemClicked(int position);

    }

    class LyricViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener,
            View.OnClickListener {
        private final LinearLayout linearLayout;

        private final TextView itemTextview;
        private final LinearLayout itemTimeControls;
        private final TextView itemTimeview;
        private final Button itemplay;
        private final Button itemadd;
        final LyricListAdapter mAdapter;

        LyricViewHolder(View itemView, LyricListAdapter adapter) {
            super(itemView);

            linearLayout = itemView.findViewById(R.id.item_parent_linearlayout);

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
                    mClickListener.onAddButtonClick(getAdapterPosition());
                }
            });

            itemplay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onPlayButtonClick(getAdapterPosition());
                }
            });

            Button incrTime = itemView.findViewById(R.id.increase_time_button);
            Button decrTime = itemView.findViewById(R.id.decrease_time_button);

            incrTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onIncreaseTimeClick(getAdapterPosition());
                }
            });

            incrTime.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mClickListener.onLongPressIncrTime(getAdapterPosition());
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    return false;
                }
            });

            decrTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onDecreaseTimeClick(getAdapterPosition());
                }
            });

            decrTime.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mClickListener.onLongPressDecrTime(getAdapterPosition());
                    v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    return false;
                }
            });

        }

        @Override
        public boolean onLongClick(View v) {
            mClickListener.onLyricItemSelected(getAdapterPosition());
            v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return false;
        }

        @Override
        public void onClick(View v) {
            mClickListener.onLyricItemClicked(getAdapterPosition());
        }
    }
}
