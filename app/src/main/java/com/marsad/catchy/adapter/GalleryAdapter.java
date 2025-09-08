package com.marsad.catchy.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.marsad.catchy.R;
import com.marsad.catchy.model.GalleryImages;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryHolder> {

    // Renamed interface and variable
    OnImageSelectedListener onImageSelectedListener;
    List<GalleryImages> list;

    public GalleryAdapter(List<GalleryImages> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public GalleryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_items, parent, false);
        return new GalleryHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull GalleryHolder holder, int position) {

        Glide.with(holder.itemView.getContext())
                .load(list.get(position).getPicUri())
                // Add a placeholder and error drawable for better UX
                .placeholder(R.drawable.ic_launcher_background) // Replace with your actual placeholder
                .error(R.drawable.ic_launcher) // Replace with your actual error image
                .into(holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            if (onImageSelectedListener != null) {
                // Ensure position is valid, especially if items can be removed rapidly
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onImageSelectedListener.onImageSelected(list.get(currentPosition).getPicUri());
                }
            }
        });

    }

    // This private method is no longer needed as the logic is inlined in setOnClickListener
    // private void chooseImage(Uri picUri) {
    //    if (onImageSelectedListener != null) {
    //        onImageSelectedListener.onImageSelected(picUri);
    //    }
    // }


    @Override
    public int getItemCount() {
        return list.size();
    }

    // Renamed method and interface
    public void setOnImageSelectedListener(OnImageSelectedListener listener) {
        this.onImageSelectedListener = listener;
    }

    public interface OnImageSelectedListener {
        void onImageSelected(Uri picUri);
    }

    static class GalleryHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public GalleryHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

}
