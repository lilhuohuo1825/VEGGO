package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.core.utils.ReviewMediaHelper;

import java.util.ArrayList;
import java.util.List;

public class ReviewPhotoAdapter extends RecyclerView.Adapter<ReviewPhotoAdapter.PhotoViewHolder> {

    private List<String> photos = new ArrayList<>();

    public void setPhotos(List<String> photos) {
        this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
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
        private final View card;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView;
            imvReviewPhoto = itemView.findViewById(R.id.imvReviewPhoto);
        }

        public void bind(String url) {
            ReviewMediaHelper.bindMediaCard(card, imvReviewPhoto, url);
        }
    }
}
