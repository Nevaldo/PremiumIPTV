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
import com.player.iptv.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MovieFilterAdapter extends RecyclerView.Adapter<MovieFilterAdapter.ViewHolder> {

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final List<Movie> movies = new ArrayList<>();
    private OnMovieClickListener listener;

    public void setOnMovieClickListener(OnMovieClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Movie> list) {
        movies.clear();
        if (list != null) {
            movies.addAll(list);
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
        Movie movie = movies.get(position);

        String name = movie.getName() != null ? movie.getName() : movie.getTitle();
        holder.tvTitle.setText(name);

        String poster = movie.getStreamIcon();
        if (poster == null && movie.getInfo() != null) {
            poster = movie.getInfo().getMovieImage();
        }
        if (poster != null && !poster.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(poster)
                    .transform(new CenterCrop())
                    .placeholder(R.color.bg_surface)
                    .error(R.color.bg_surface)
                    .into(holder.ivPoster);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMovieClick(movie);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
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
