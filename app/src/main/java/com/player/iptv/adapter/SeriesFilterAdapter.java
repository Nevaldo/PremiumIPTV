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
import com.player.iptv.R;
import com.player.iptv.model.Series;

import java.util.ArrayList;
import java.util.List;

public class SeriesFilterAdapter extends RecyclerView.Adapter<SeriesFilterAdapter.ViewHolder> {

    public interface OnSeriesClickListener {
        void onSeriesClick(Series series);
    }

    private final List<Series> seriesList = new ArrayList<>();
    private OnSeriesClickListener listener;

    public void setOnSeriesClickListener(OnSeriesClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Series> list) {
        seriesList.clear();
        if (list != null) {
            seriesList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_small, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Series series = seriesList.get(position);

        String name = series.getName() != null ? series.getName() : "";
        holder.tvTitle.setText(name);

        String cover = series.getCover();
        if (cover == null && series.getInfo() != null) {
            cover = series.getInfo().getCover();
        }
        if (cover != null && !cover.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(cover)
                    .transform(new CenterCrop())
                    .placeholder(R.color.bg_surface)
                    .error(R.color.bg_surface)
                    .into(holder.ivPoster);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSeriesClick(series);
            }
        });
    }

    @Override
    public int getItemCount() {
        return seriesList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPoster;
        final TextView tvTitle;

        ViewHolder(View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivMoviePoster);
            tvTitle = itemView.findViewById(R.id.tvMovieTitle);
        }
    }
}
