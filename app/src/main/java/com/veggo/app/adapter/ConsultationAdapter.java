package com.veggo.app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.veggo.app.R;
import com.veggo.app.assets.AssetModels;

import java.util.ArrayList;
import java.util.List;

public class ConsultationAdapter extends RecyclerView.Adapter<ConsultationAdapter.ViewHolder> {

    private List<AssetModels.Question> questions = new ArrayList<>();

    public void setQuestions(List<AssetModels.Question> questions) {
        this.questions = new ArrayList<>(questions);
        notifyDataSetChanged();
    }

    public void addQuestion(AssetModels.Question question) {
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
        this.questions.add(0, question); // Thêm vào đầu danh sách
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
        AssetModels.Question question = questions.get(position);
        holder.bind(question);
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

        public void bind(AssetModels.Question question) {
            tvUserName.setText(question.customerName != null ? question.customerName : "Khách hàng");
            tvQuestionTime.setText(question.createdAt != null ? question.createdAt : "Vừa xong");
            tvQuestionContent.setText(question.question);
            tvAnswerContent.setText(question.answer != null ? question.answer : "Chúng tôi sẽ sớm phản hồi câu hỏi của bạn.");
            // In a real app, you'd handle "helpful" count from data
        }
    }
}
