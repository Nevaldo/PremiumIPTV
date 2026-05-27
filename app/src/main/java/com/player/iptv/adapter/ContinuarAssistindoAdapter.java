package com.player.iptv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.player.iptv.R;
import com.player.iptv.model.Historico;

import java.util.ArrayList;
import java.util.List;

public class ContinuarAssistindoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_CARD = 0;
    public static final int TYPE_LIST = 1;

    private final int viewType;
    private final List<Historico> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Historico item, int position);
    }

    public ContinuarAssistindoAdapter(int viewType) {
        this.viewType = viewType;
    }

    public void setItems(List<Historico> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CARD) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_continuar_assistindo, parent, false);
            return new RecenteViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_continuar_list, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Historico item = items.get(position);
        if (holder instanceof RecenteViewHolder) {
            RecenteViewHolder h = (RecenteViewHolder) holder;
            h.txtTitulo.setText(item.getTitulo());
            h.txtInfo.setText("00:00");
            int progress = item.getDuration() > 0 ? (int) (item.getPosition() * 100 / item.getDuration()) : 0;
            h.progressBar.setProgress(progress);
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(h.itemView.getContext())
                    .load(imageUrl)
                    .transform(new CenterCrop(), new RoundedCorners(5))
                    .placeholder(R.color.bg_surface)
                    .error(R.color.bg_surface)
                    .into(h.imgBanner);
            } else {
                h.imgBanner.setImageResource(R.color.bg_surface);
            }
        } else {
            CardViewHolder h = (CardViewHolder) holder;
            h.txtTitulo.setText(item.getTitulo());
            // Exibir subtítulo (ex: T7 • E4) se disponível, senão deixa vazio
            String sub = item.getSubtitle();
            h.txtInfo.setText(sub != null && !sub.isEmpty() ? sub : "");
            int progress = item.getDuration() > 0 ? (int) (item.getPosition() * 100L / item.getDuration()) : 0;
            h.progressBar.setProgress(progress);
            // Exibir percentual concluído
            h.txtSubtitle.setText(progress + "% concluído");
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(h.itemView.getContext())
                    .load(imageUrl)
                    .transform(new CenterCrop(), new RoundedCorners(6))
                    .placeholder(R.color.bg_surface)
                    .error(R.color.bg_surface)
                    .into(h.imgBanner);
            } else {
                h.imgBanner.setImageResource(R.color.bg_surface);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class CardViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgBanner;
        final TextView txtTitulo;
        final TextView txtInfo;
        final TextView txtSubtitle;
        final ProgressBar progressBar;

        CardViewHolder(View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            txtSubtitle = itemView.findViewById(R.id.txtSubtitle);
            progressBar = itemView.findViewById(R.id.progressBar);
            itemView.setFocusable(true);
            itemView.setClickable(true);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick(items.get(pos), pos);
                    }
                }
            });
        }
    }

    class RecenteViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgBanner;
        final TextView txtTitulo;
        final TextView txtInfo;
        final ProgressBar progressBar;

        RecenteViewHolder(View itemView) {
            super(itemView);
            imgBanner = itemView.findViewById(R.id.imgBanner);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtInfo = itemView.findViewById(R.id.txtInfo);
            progressBar = itemView.findViewById(R.id.progressBar);
            itemView.setFocusable(true);
            itemView.setClickable(true);
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        listener.onItemClick(items.get(pos), pos);
                    }
                }
            });
        }
    }
}
