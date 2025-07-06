package com.yuanseen.shuati.ui2;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yuanseen.shuati.R;

import java.util.List;

public class QuestionGridAdapter extends RecyclerView.Adapter<QuestionGridAdapter.QuestionViewHolder> {
    private List<Integer> questionNumbers;
    private String questionType;
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    private int lastReadQuestionId = -1;

    public interface OnItemClickListener {
        void onItemClick(String type, int number);
    }

    public QuestionGridAdapter(List<Integer> questionNumbers, String questionType) {
        this.questionNumbers = questionNumbers;
        this.questionType = questionType;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_number, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, @SuppressLint("RecyclerView") int position) {
        int questionNumber = questionNumbers.get(position);
        holder.numberText.setText(String.valueOf(questionNumber));

        // 设置选中状态 - 当前选中或上次阅读的题目
        boolean isSelected = position == selectedPosition ||
                (selectedPosition == -1 && questionNumber == lastReadQuestionId);
        holder.itemView.setSelected(isSelected);

        // 如果是上次阅读的题目且没有当前选中项，自动设置选中位置
        if (questionNumber == lastReadQuestionId && selectedPosition == -1) {
            selectedPosition = position;
        }
        // 设置选中状态
//        holder.itemView.setSelected(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            // 更新选中位置
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // 通知前一个选中项和当前选中项更新
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onItemClick(questionType, questionNumber);
            }
        });
    }

    public void setLastReadQuestionId(int questionId) {
        this.lastReadQuestionId = questionId;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return questionNumbers.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView numberText;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            numberText = itemView.findViewById(R.id.question_number);
        }
    }
}