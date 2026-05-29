package com.player.iptv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.R;
import com.player.iptv.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Category> items = new ArrayList<>();
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    public static final int VIEW_TYPE_LIST = 0;
    public static final int VIEW_TYPE_CARD = 1;

    private int viewType = VIEW_TYPE_LIST;

    public interface OnItemClickListener {
        void onItemClick(Category category, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public int getViewType() {
        return viewType;
    }

    public void submitList(List<Category> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
        if (!items.isEmpty() && selectedPosition == -1) {
            selectedPosition = 0;
            items.get(0).setSelected(true);
            notifyItemChanged(0);
        }
    }

    public Category getItem(int position) {
        return items.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CARD) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
            return new CardViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_category, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Category item = items.get(position);
        if (holder instanceof CardViewHolder) {
            CardViewHolder card = (CardViewHolder) holder;
            card.txtCategory.setText(item.getCategoryName());
            if (item.getImageResId() != 0) {
                card.imgCategory.setImageResource(item.getImageResId());
                card.imgIcon.setImageResource(item.getImageResId());
            }
        } else if (holder instanceof ListViewHolder) {
            ListViewHolder list = (ListViewHolder) holder;
            list.tvName.setText(item.getCategoryName());
            if (item.getImageResId() != 0) {
                list.ivIcon.setImageResource(item.getImageResId());
            }
            list.itemView.setSelected(item.isSelected());
        }
        holder.itemView.setSelected(item.isSelected());

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            if (selectedPosition == pos) return;

            if (selectedPosition >= 0 && selectedPosition < items.size()) {
                items.get(selectedPosition).setSelected(false);
                notifyItemChanged(selectedPosition);
            }
            selectedPosition = pos;
            items.get(pos).setSelected(true);
            //notifyItemChanged(pos);

            if (listener != null) {
                listener.onItemClick(item, pos);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (selectedPosition >= 0 && selectedPosition < items.size()) {
                    items.get(selectedPosition).setSelected(false);
                    //notifyItemChanged(selectedPosition);
                }
                selectedPosition = holder.getAdapterPosition();
                if (selectedPosition >= 0 && selectedPosition < items.size()) {
                    items.get(selectedPosition).setSelected(true);
                    //notifyItemChanged(selectedPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ListViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final ImageView ivIcon;
        ListViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            ivIcon = itemView.findViewById(R.id.ivCategoryIcon);
            itemView.setFocusable(true);
            itemView.setClickable(true);
        }
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgCategory;
        final ImageView imgIcon;
        final TextView txtCategory;
        final TextView txtCount;
        CardViewHolder(View itemView) {
            super(itemView);
            imgCategory = itemView.findViewById(R.id.imgCategory);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            txtCategory = itemView.findViewById(R.id.txtCategory);
            txtCount = itemView.findViewById(R.id.txtCount);
            itemView.setFocusable(true);
            itemView.setClickable(true);
        }
    }
}
