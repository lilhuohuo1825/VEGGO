package com.veggo.app.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.Consultation;
import com.veggo.app.domain.model.ConsultationReply;
import com.veggo.app.presentation.product.ConsultationAvatarHelper;

import java.util.ArrayList;
import java.util.List;

public class ConsultationAdapter extends RecyclerView.Adapter<ConsultationAdapter.ViewHolder> {

    public interface ActionListener {
        void onToggleLike(Consultation question);

        void onSubmitReply(Consultation question, String content);

        void onLoginRequired();
    }

    private List<Consultation> questions = new ArrayList<>();
    @Nullable
    private String currentCustomerId;
    @Nullable
    private ActionListener actionListener;
    @Nullable
    private String expandedReplyQuestionId;

    public void setQuestions(List<Consultation> questions) {
        this.questions = questions != null ? new ArrayList<>(questions) : new ArrayList<>();
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
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_consultation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(questions.get(position));
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivUserAvatar;
        private final ImageView ivAdminAvatar;
        private final ImageView ivLikeIcon;
        private final TextView tvUserName;
        private final TextView tvQuestionTime;
        private final TextView tvQuestionContent;
        private final TextView tvAnswerBadge;
        private final TextView tvAnswerContent;
        private final TextView tvPendingBadge;
        private final TextView tvHelpful;
        private final LinearLayout llAnswerContainer;
        private final LinearLayout llRepliesContainer;
        private final LinearLayout llActions;
        private final LinearLayout llReplyInput;
        private final LinearLayout btnLike;
        private final LinearLayout btnReply;
        private final EditText edtReply;
        private final ImageView btnSendReply;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            ivAdminAvatar = itemView.findViewById(R.id.ivAdminAvatar);
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvQuestionTime = itemView.findViewById(R.id.tvQuestionTime);
            tvQuestionContent = itemView.findViewById(R.id.tvQuestionContent);
            tvAnswerBadge = itemView.findViewById(R.id.tvAnswerBadge);
            tvAnswerContent = itemView.findViewById(R.id.tvAnswerContent);
            tvPendingBadge = itemView.findViewById(R.id.tvPendingBadge);
            tvHelpful = itemView.findViewById(R.id.tvHelpful);
            llAnswerContainer = itemView.findViewById(R.id.llAnswerContainer);
            llRepliesContainer = itemView.findViewById(R.id.llRepliesContainer);
            llActions = itemView.findViewById(R.id.llActions);
            llReplyInput = itemView.findViewById(R.id.llReplyInput);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnReply = itemView.findViewById(R.id.btnReply);
            edtReply = itemView.findViewById(R.id.edtReply);
            btnSendReply = itemView.findViewById(R.id.btnSendReply);
        }

        void bind(Consultation question) {
            String customerName = ConsultationAvatarHelper.displayName(question.getCustomerName(), false);
            tvUserName.setText(customerName);
            ConsultationAvatarHelper.bindUserAvatar(ivUserAvatar, question.getCustomerAvatarUrl());
            tvQuestionTime.setText(!TextUtils.isEmpty(question.getCreatedAt())
                    ? question.getCreatedAt()
                    : itemView.getContext().getString(R.string.consultation_time_now));
            tvQuestionContent.setText(question.getQuestion());

            boolean answered = isAnswered(question);
            if (answered) {
                llAnswerContainer.setVisibility(View.VISIBLE);
                tvPendingBadge.setVisibility(View.GONE);
                ConsultationAvatarHelper.bindAdminAvatar(ivAdminAvatar);
                tvAnswerBadge.setText(itemView.getContext().getString(R.string.consultation_admin_name));
                tvAnswerContent.setText(question.getAnswer());
            } else {
                llAnswerContainer.setVisibility(View.GONE);
                tvPendingBadge.setVisibility(View.VISIBLE);
            }

            bindReplies(question.getReplies());
            bindLikeState(question);
            bindReplyInput(question);

            btnLike.setOnClickListener(v -> {
                if (actionListener == null) {
                    return;
                }
                if (!isLoggedIn()) {
                    actionListener.onLoginRequired();
                    return;
                }
                if (currentCustomerId != null
                        && currentCustomerId.equals(question.getCustomerId())) {
                    return;
                }
                actionListener.onToggleLike(question);
            });

            btnReply.setOnClickListener(v -> {
                if (actionListener == null) {
                    return;
                }
                if (!isLoggedIn()) {
                    actionListener.onLoginRequired();
                    return;
                }
                expandedReplyQuestionId = question.getId().equals(expandedReplyQuestionId)
                        ? null
                        : question.getId();
                notifyDataSetChanged();
            });

            btnSendReply.setOnClickListener(v -> submitReply(question));
            edtReply.setOnEditorActionListener((textView, actionId, event) -> {
                submitReply(question);
                return true;
            });
        }

        private void bindReplies(List<ConsultationReply> replies) {
            llRepliesContainer.removeAllViews();
            if (replies == null || replies.isEmpty()) {
                llRepliesContainer.setVisibility(View.GONE);
                return;
            }
            llRepliesContainer.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            for (ConsultationReply reply : replies) {
                View replyView = inflater.inflate(R.layout.item_consultation_reply, llRepliesContainer, false);
                ImageView avatar = replyView.findViewById(R.id.ivReplyAvatar);
                TextView author = replyView.findViewById(R.id.tvReplyAuthor);
                TextView time = replyView.findViewById(R.id.tvReplyTime);
                TextView content = replyView.findViewById(R.id.tvReplyContent);

                author.setText(ConsultationAvatarHelper.displayName(reply.getCustomerName(), reply.isAdmin()));
                time.setText(!TextUtils.isEmpty(reply.getCreatedAt()) ? reply.getCreatedAt() : "");
                content.setText(reply.getContent());
                if (reply.isAdmin()) {
                    ConsultationAvatarHelper.bindAdminAvatar(avatar);
                } else {
                    ConsultationAvatarHelper.bindUserAvatar(avatar, reply.getCustomerAvatarUrl());
                }
                llRepliesContainer.addView(replyView);
            }
        }

        private void bindLikeState(Consultation question) {
            boolean isOwnQuestion = currentCustomerId != null
                    && question.getCustomerId() != null
                    && currentCustomerId.equals(question.getCustomerId());
            btnLike.setVisibility(isOwnQuestion ? View.GONE : View.VISIBLE);

            boolean liked = question.isLikedBy(currentCustomerId);
            tvHelpful.setText(itemView.getContext().getString(
                    R.string.helpful_format, question.getHelpfulCount()));
            if (liked) {
                ivLikeIcon.setImageResource(R.drawable.ic_heart_filled_green);
                ivLikeIcon.clearColorFilter();
                tvHelpful.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_main));
            } else {
                ivLikeIcon.setImageResource(R.drawable.ic_heart_outline_green);
                ivLikeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.neutral_60));
                tvHelpful.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.neutral_60));
            }
            llActions.setVisibility(View.VISIBLE);
        }

        private void bindReplyInput(Consultation question) {
            boolean expanded = question.getId() != null && question.getId().equals(expandedReplyQuestionId);
            llReplyInput.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded && edtReply.getText().length() == 0) {
                edtReply.requestFocus();
            }
        }

        private void submitReply(Consultation question) {
            if (actionListener == null) {
                return;
            }
            if (!isLoggedIn()) {
                actionListener.onLoginRequired();
                return;
            }
            String content = edtReply.getText().toString().trim();
            if (content.isEmpty()) {
                return;
            }
            actionListener.onSubmitReply(question, content);
            edtReply.setText("");
            expandedReplyQuestionId = null;
        }

        private boolean isLoggedIn() {
            return currentCustomerId != null && !currentCustomerId.trim().isEmpty();
        }

        private boolean isAnswered(Consultation question) {
            return "answered".equalsIgnoreCase(question.getStatus())
                    && question.getAnswer() != null
                    && !question.getAnswer().trim().isEmpty();
        }
    }
}
