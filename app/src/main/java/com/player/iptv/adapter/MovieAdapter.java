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
import com.player.iptv.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_SMALL = 1;

    private final List<Movie> items = new ArrayList<>();
    private OnItemClickListener listener;
    private int selectedPosition = -1;
    private int viewType = TYPE_NORMAL;

    public interface OnItemClickListener {
        void onItemClick(Movie movie, int position);
    }

    public MovieAdapter() {}

    public MovieAdapter(boolean useSmallLayout) {
        this.viewType = useSmallLayout ? TYPE_SMALL : TYPE_NORMAL;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Movie> list) {
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
        int layout = viewType == TYPE_SMALL ? R.layout.item_movie_small : R.layout.item_movie;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Movie movie = items.get(position);
        String title = movie.getName() != null ? movie.getName() : movie.getTitle();
        holder.tvTitle.setText(title);
        holder.itemView.setSelected(position == selectedPosition);

        // Badge NOVO — exibir nos 3 primeiros itens como exemplo
        if (holder.tvBadge != null) {
            if (position < 3) {
                holder.tvBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvBadge.setVisibility(View.GONE);
            }
        }

        // Info: ano e duração
        if (holder.tvInfo != null) {
            String info = buildMovieInfo(movie);
            holder.tvInfo.setText(info);
        }

        // Rating
        if (holder.tvRating != null) {
            String rating = movie.getInfo() != null && movie.getInfo().getRating() != null
                    ? movie.getInfo().getRating()
                    : "8.5";
            holder.tvRating.setText(rating);
        }

        String imageUrl = movie.getStreamIcon();
        if (imageUrl == null && movie.getInfo() != null) {
            imageUrl = movie.getInfo().getMovieImage();
        }

        Glide.with(holder.itemView.getContext())
            .load(imageUrl)
            .transform(new CenterCrop(), new RoundedCorners(8))
            .placeholder(R.color.bg_surface)
            .error(R.color.bg_surface)
            .into(holder.ivPoster);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            if (selectedPosition >= 0 && selectedPosition < items.size()) {
                notifyItemChanged(selectedPosition);
            }
            selectedPosition = pos;
            notifyItemChanged(pos);

            if (listener != null) {
                listener.onItemClick(movie, pos);
            }
        });
    }

    private String buildMovieInfo(Movie movie) {
        StringBuilder sb = new StringBuilder();
        if (movie.getInfo() != null && movie.getInfo().getReleaseDate() != null) {
            String year = movie.getInfo().getReleaseDate();
            if (year.length() >= 4) year = year.substring(0, 4);
            sb.append(year);
        }
        if (movie.getInfo() != null && movie.getInfo().getDurationSecs() != null) {
            if (sb.length() > 0) sb.append(" • ");
            // Convertendo segundos se possível, senão apenas adiciona
            try {
                int secs = Integer.parseInt(movie.getInfo().getDurationSecs());
                int mins = secs / 60;
                if (mins > 60) {
                    sb.append(mins / 60).append("h ").append(mins % 60).append("m");
                } else {
                    sb.append(mins).append("m");
                }
            } catch (Exception e) {
                sb.append(movie.getInfo().getDurationSecs());
            }
        }
        return sb.toString();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivPoster;
        final TextView tvTitle;
        final TextView tvBadge;
        final TextView tvInfo;
        final TextView tvRating;

        ViewHolder(View itemView) {
            super(itemView);
            ivPoster = itemView.findViewById(R.id.ivMoviePoster);
            tvTitle = itemView.findViewById(R.id.tvMovieTitle);
            tvBadge = itemView.findViewById(R.id.tvMovieBadge);
            tvInfo = itemView.findViewById(R.id.tvMovieInfo);
            tvRating = itemView.findViewById(R.id.tvMovieRating);
            itemView.setFocusable(true);
            itemView.setClickable(true);
        }
    }
}
