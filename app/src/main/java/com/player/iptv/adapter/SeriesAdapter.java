package com.player.iptv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.player.iptv.R;
import com.player.iptv.model.Series;

import java.util.ArrayList;
import java.util.List;

public class SeriesAdapter extends RecyclerView.Adapter<SeriesAdapter.ViewHolder> {

    public static final int TYPE_NORMAL   = 0;
    public static final int TYPE_FEATURED = 1;
    public static final int TYPE_NEW      = 2;
    public static final int TYPE_CONTINUE = 3;
    public static final int TYPE_SMALL    = 4;

    private final List<Series> items = new ArrayList<>();
    private OnItemClickListener listener;
    private int selectedPosition = -1;
    private int viewType = TYPE_NORMAL;

    public interface OnItemClickListener {
        void onItemClick(Series series, int position);
    }

    public SeriesAdapter() {}

    public SeriesAdapter(int viewType) {
        this.viewType = viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Series> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case TYPE_FEATURED: layout = R.layout.item_series_featured; break;
            case TYPE_NEW:      layout = R.layout.item_series_new;      break;
            case TYPE_CONTINUE: layout = R.layout.item_series_continue; break;
            case TYPE_SMALL:    layout = R.layout.item_series_small;    break;
            default:            layout = R.layout.item_series;          break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Series series = items.get(position);
        if (holder.tvTitle != null) {
            holder.tvTitle.setText(series.getName());
        }
        holder.itemView.setSelected(position == selectedPosition);

        String imageUrl = series.getCover();
        if (imageUrl == null && series.getInfo() != null) {
            imageUrl = series.getInfo().getCover();
        }

        if (holder.ivCover != null) {
            Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .transform(new CenterCrop(), new RoundedCorners(12))
                .placeholder(R.color.bg_surface)
                .error(R.color.bg_surface)
                .into(holder.ivCover);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            if (selectedPosition >= 0 && selectedPosition < items.size()) {
                notifyItemChanged(selectedPosition);
            }
            selectedPosition = pos;
            notifyItemChanged(pos);

            if (listener != null) {
                listener.onItemClick(series, pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivCover;
        final TextView tvTitle;

        ViewHolder(View itemView) {
            super(itemView);

            ImageView tempCover = itemView.findViewById(R.id.ivCover);
            if (tempCover == null) {
                tempCover = itemView.findViewById(R.id.ivSeriesCover);
            }
            ivCover = tempCover;

            TextView tempTitle = itemView.findViewById(R.id.tvTitle);
            if (tempTitle == null) {
                tempTitle = itemView.findViewById(R.id.tvSeriesTitle);
            }
            tvTitle = tempTitle;

            itemView.setFocusable(true);
            itemView.setClickable(true);
        }
    }
}
