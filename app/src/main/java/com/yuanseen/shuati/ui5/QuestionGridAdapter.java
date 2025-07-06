package com.yuanseen.shuati.ui5;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.yuanseen.shuati.R;

import java.util.ArrayList;
import java.util.List;

public class QuestionGridAdapter extends RecyclerView.Adapter<QuestionGridAdapter.QuestionViewHolder> {
    private List<Integer> questionNumbers;
    private String questionType;
    private OnItemClickListener listener;
    private int selectedPosition = -1;

    private int lastReadQuestionId = -1;

    private static int currentSelectedId = -1; // 静态变量保存当前选中ID

    public interface OnItemClickListener {
        void onItemClick(String type, int number);
    }

    public QuestionGridAdapter(List<Integer> questions, String questionType) {
        this.questionNumbers = questions != null ? questions : new ArrayList<>();
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

        // 设置选中状态 - 当前题目或上次阅读的题目
        boolean isSelected = questionNumber == currentSelectedId || questionNumber == lastReadQuestionId;
        holder.itemView.setSelected(isSelected);



        holder.itemView.setOnClickListener(v -> {
            // 更新选中状态
            currentSelectedId = questionNumber;
            lastReadQuestionId = questionNumber;
            notifyDataSetChanged();

            if (listener != null) {
                listener.onItemClick(questionType, questionNumber);
            }
        });
    }

    public void setLastReadQuestionId(int questionId) {
        this.lastReadQuestionId = questionId;
        notifyDataSetChanged();
    }

    // 添加新方法清除选中状态
    public static void clearSelection() {
        currentSelectedId = -1;
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