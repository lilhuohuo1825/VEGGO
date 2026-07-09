package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.core.utils.ReviewMediaHelper;
import com.veggo.app.core.utils.UserAvatarHelper;
import com.veggo.app.domain.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    public interface ActionListener {
        void onToggleLike(Review review);

        void onLoginRequired();
    }

    private List<Review> reviews = new ArrayList<>();
    @Nullable
    private String currentCustomerId;
    @Nullable
    private ActionListener actionListener;

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews != null ? new ArrayList<>(reviews) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setCurrentCustomerId(@Nullable String currentCustomerId) {
        this.currentCustomerId = currentCustomerId;
    }

    public void setActionListener(@Nullable ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        holder.bind(reviews.get(position));
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {
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
        private final LinearLayout btnHelpful;
        private final ImageView ivHelpfulIcon;
        private final TextView tvHelpfulReview;

        ReviewViewHolder(@NonNull View itemView) {
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
            btnHelpful = itemView.findViewById(R.id.btnHelpful);
            ivHelpfulIcon = itemView.findViewById(R.id.ivHelpfulIcon);
            tvHelpfulReview = itemView.findViewById(R.id.tvHelpfulReview);
        }

        void bind(Review review) {
            tvReviewerName.setText(review.getReviewerName());
            tvReviewTime.setText(review.getReviewTime());
            rbReviewRating.setRating(review.getRating());
            tvReviewContent.setText(review.getContent());
            bindHelpfulState(review);
            UserAvatarHelper.bind(ivAvatar, review.getAvatarUrl());

            List<String> images = review.getImageUrls();
            if (images != null && !images.isEmpty()) {
                List<String> validMedia = new ArrayList<>();
                for (String url : images) {
                    if (url != null && !url.trim().isEmpty()) {
                        validMedia.add(url);
                    }
                }

                if (!validMedia.isEmpty()) {
                    ReviewMediaHelper.bindMediaCard(cvReviewImage1, ivReviewImage1, validMedia.get(0));
                } else {
                    cvReviewImage1.setVisibility(View.GONE);
                }

                if (validMedia.size() > 1) {
                    ReviewMediaHelper.bindMediaCard(cvReviewImage2, ivReviewImage2, validMedia.get(1));
                } else {
                    cvReviewImage2.setVisibility(View.GONE);
                }

                llReviewImages.setVisibility(validMedia.isEmpty() ? View.GONE : View.VISIBLE);
            } else {
                llReviewImages.setVisibility(View.GONE);
            }

            btnHelpful.setOnClickListener(v -> {
                if (actionListener == null || review.getId() == null || review.getId().isEmpty()) {
                    return;
                }
                if (!isLoggedIn()) {
                    actionListener.onLoginRequired();
                    return;
                }
                if (isOwnReview(review)) {
                    return;
                }
                actionListener.onToggleLike(review);
            });
        }

        private void bindHelpfulState(Review review) {
            boolean isOwnReview = isOwnReview(review);
            boolean canLike = review.getId() != null && !review.getId().isEmpty() && !isOwnReview;
            btnHelpful.setVisibility(review.getId() == null || review.getId().isEmpty() ? View.GONE : View.VISIBLE);
            btnHelpful.setEnabled(canLike);
            btnHelpful.setClickable(canLike);

            boolean liked = review.isLikedBy(currentCustomerId);
            tvHelpfulReview.setText(itemView.getContext().getString(
                    R.string.helpful_format, review.getHelpfulCount()));
            if (liked) {
                ivHelpfulIcon.setImageResource(R.drawable.ic_heart_filled_green);
                ivHelpfulIcon.clearColorFilter();
                tvHelpfulReview.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_main));
            } else {
                ivHelpfulIcon.setImageResource(R.drawable.ic_heart_outline_grey);
                ivHelpfulIcon.clearColorFilter();
                tvHelpfulReview.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.neutral_60));
            }
        }

        private boolean isOwnReview(Review review) {
            return currentCustomerId != null
                    && review.getCustomerId() != null
                    && currentCustomerId.equals(review.getCustomerId());
        }

        private boolean isLoggedIn() {
            return currentCustomerId != null && !currentCustomerId.trim().isEmpty();
        }
    }
}
