package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;

import java.util.ArrayList;
import java.util.List;

public class ReviewPhotoAdapter extends RecyclerView.Adapter<ReviewPhotoAdapter.PhotoViewHolder> {

    private List<String> photos = new ArrayList<>();

    public void setPhotos(List<String> photos) {
        this.photos = photos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(photos.get(position));
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imvReviewPhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imvReviewPhoto = itemView.findViewById(R.id.imvReviewPhoto);
        }

        public void bind(String url) {
            if (url != null && !url.isEmpty()) {
                Glide.with(itemView.getContext()).load(url).into(imvReviewPhoto);
            }
        }
    }
}
