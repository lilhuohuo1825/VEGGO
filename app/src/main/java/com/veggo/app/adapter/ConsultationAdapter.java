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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.Consultation;
import com.veggo.app.domain.model.ConsultationReply;
import com.veggo.app.presentation.product.ConsultationAvatarHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        List<Consultation> newList = questions != null ? new ArrayList<>(questions) : new ArrayList<>();
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ConsultationDiffCallback(this.questions, newList, currentCustomerId));
        this.questions = newList;
        diffResult.dispatchUpdatesTo(this);
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

    private static final class ConsultationDiffCallback extends DiffUtil.Callback {
        private final List<Consultation> oldList;
        private final List<Consultation> newList;
        @Nullable
        private final String currentCustomerId;

        ConsultationDiffCallback(List<Consultation> oldList, List<Consultation> newList,
                                   @Nullable String currentCustomerId) {
            this.oldList = oldList;
            this.newList = newList;
            this.currentCustomerId = currentCustomerId;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return Objects.equals(oldList.get(oldItemPosition).getId(), newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Consultation oldItem = oldList.get(oldItemPosition);
            Consultation newItem = newList.get(newItemPosition);
            return Objects.equals(oldItem.getQuestion(), newItem.getQuestion())
                    && Objects.equals(oldItem.getAnswer(), newItem.getAnswer())
                    && Objects.equals(oldItem.getStatus(), newItem.getStatus())
                    && oldItem.getHelpfulCount() == newItem.getHelpfulCount()
                    && oldItem.getReplies().size() == newItem.getReplies().size()
                    && oldItem.isLikedBy(currentCustomerId) == newItem.isLikedBy(currentCustomerId)
                    && Objects.equals(oldItem.getCreatedAt(), newItem.getCreatedAt());
        }
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
                if (isOwnQuestion(question)) {
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
                if (isOwnQuestion(question)) {
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
            boolean isOwnQuestion = isOwnQuestion(question);
            if (isOwnQuestion) {
                llActions.setVisibility(View.GONE);
                return;
            }

            llActions.setVisibility(View.VISIBLE);
            btnLike.setVisibility(View.VISIBLE);
            btnReply.setVisibility(View.VISIBLE);

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
        }

        private void bindReplyInput(Consultation question) {
            if (isOwnQuestion(question)) {
                llReplyInput.setVisibility(View.GONE);
                return;
            }
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
            if (isOwnQuestion(question)) {
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

        private boolean isOwnQuestion(Consultation question) {
            return currentCustomerId != null
                    && question.getCustomerId() != null
                    && currentCustomerId.equals(question.getCustomerId());
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
