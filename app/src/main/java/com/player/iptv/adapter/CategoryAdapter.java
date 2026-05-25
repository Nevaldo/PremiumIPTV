package com.player.iptv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.R;
import com.player.iptv.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<Category> items = new ArrayList<>();
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    public interface OnItemClickListener {
        void onItemClick(Category category, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category item = items.get(position);
        holder.tvName.setText(item.getCategoryName());
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
            notifyItemChanged(pos);

            if (listener != null) {
                listener.onItemClick(item, pos);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (selectedPosition >= 0 && selectedPosition < items.size()) {
                    items.get(selectedPosition).setSelected(false);
                    notifyItemChanged(selectedPosition);
                }
                selectedPosition = holder.getAdapterPosition();
                if (selectedPosition >= 0 && selectedPosition < items.size()) {
                    items.get(selectedPosition).setSelected(true);
                    notifyItemChanged(selectedPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            itemView.setFocusable(true);
            itemView.setClickable(true);
        }
    }
}
