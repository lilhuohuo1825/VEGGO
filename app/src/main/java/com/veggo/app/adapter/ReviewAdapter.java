package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.veggo.app.R;
import com.veggo.app.domain.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviews = new ArrayList<>();

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivAvatar;
        private final TextView tvReviewerName;
        private final TextView tvReviewTime;
        private final RatingBar rbReviewRating;
        private final TextView tvReviewContent;
        private final ImageView ivReviewImage1;
        private final ImageView ivReviewImage2;
        private final View cvReviewImage1;
        private final View cvReviewImage2;
        private final View llReviewImages;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvReviewerName = itemView.findViewById(R.id.tvReviewerName);
            tvReviewTime = itemView.findViewById(R.id.tvReviewTime);
            rbReviewRating = itemView.findViewById(R.id.rbReviewRating);
            tvReviewContent = itemView.findViewById(R.id.tvReviewContent);
            ivReviewImage1 = itemView.findViewById(R.id.ivReviewImage1);
            ivReviewImage2 = itemView.findViewById(R.id.ivReviewImage2);
            cvReviewImage1 = itemView.findViewById(R.id.cvReviewImage1);
            cvReviewImage2 = itemView.findViewById(R.id.cvReviewImage2);
            llReviewImages = itemView.findViewById(R.id.llReviewImages);
        }

        public void bind(Review review) {
            tvReviewerName.setText(review.getReviewerName());
            tvReviewTime.setText(review.getReviewTime());
            rbReviewRating.setRating(review.getRating());
            tvReviewContent.setText(review.getContent());

            if (review.getAvatarUrl() != null && !review.getAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext()).load(review.getAvatarUrl()).into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.color.neutral_40);
            }

            List<String> images = review.getImageUrls();
            if (images != null && !images.isEmpty()) {
                // Clean up empty strings or nulls
                List<String> validImages = new ArrayList<>();
                for (String url : images) {
                    if (url != null && !url.trim().isEmpty()) {
                        validImages.add(url);
                    }
                }

                if (validImages.size() > 0) {
                    cvReviewImage1.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext()).load(validImages.get(0)).into(ivReviewImage1);
                } else {
                    cvReviewImage1.setVisibility(View.GONE);
                }
                
                if (validImages.size() > 1) {
                    cvReviewImage2.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext()).load(validImages.get(1)).into(ivReviewImage2);
                } else {
                    cvReviewImage2.setVisibility(View.GONE);
                }
                
                llReviewImages.setVisibility(validImages.size() > 0 ? View.VISIBLE : View.GONE);
            } else {
                llReviewImages.setVisibility(View.GONE);
            }
        }
    }
}