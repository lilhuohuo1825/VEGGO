package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.domain.model.Consultation;

import java.util.ArrayList;
import java.util.List;

public class ConsultationAdapter extends RecyclerView.Adapter<ConsultationAdapter.ViewHolder> {

    private List<Consultation> questions = new ArrayList<>();

    public void setQuestions(List<Consultation> questions) {
        this.questions = new ArrayList<>(questions);
        notifyDataSetChanged();
    }

    public void addQuestion(Consultation question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.add(0, question);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_consultation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(questions.get(position));
    }

    @Override
    public int getItemCount() {
        return questions != null ? questions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvQuestionTime, tvQuestionContent, tvAnswerContent, tvHelpful;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvQuestionTime = itemView.findViewById(R.id.tvQuestionTime);
            tvQuestionContent = itemView.findViewById(R.id.tvQuestionContent);
            tvAnswerContent = itemView.findViewById(R.id.tvAnswerContent);
            tvHelpful = itemView.findViewById(R.id.tvHelpful);
        }

        public void bind(Consultation question) {
            tvUserName.setText(question.getCustomerName() != null ? question.getCustomerName() : "Khách hàng");
            tvQuestionTime.setText(question.getCreatedAt() != null && !question.getCreatedAt().isEmpty()
                    ? question.getCreatedAt() : "Vừa xong");
            tvQuestionContent.setText(question.getQuestion());
            tvAnswerContent.setText(question.getAnswer() != null && !question.getAnswer().isEmpty()
                    ? question.getAnswer()
                    : "Chúng tôi sẽ sớm phản hồi câu hỏi của bạn.");
        }
    }
}
