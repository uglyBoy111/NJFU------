package com.yuanseen.shuati.ui9;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui5.Question;
import com.yuanseen.shuati.ui8.QuizHomeActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PaperDetailActivity extends AppCompatActivity {

    private String questionBankId;
    private String paperId;
    private MaUtil maUtil;
    private RecyclerView questionsRecyclerView;
    private QuestionsAdapter questionsAdapter;
    private List<QuestionItem> questionItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_detail);

        // 获取从MidActivity传递过来的题库ID和试卷ID
        questionBankId = getIntent().getStringExtra("question_bank_id");
        paperId = getIntent().getStringExtra("paper_id");

        if (questionBankId == null || paperId == null) {
            finish();
            return;
        }

        // 初始化MaUtil
        maUtil = new MaUtil(questionBankId, paperId, getFilesDir());

        // 初始化RecyclerView
        questionsRecyclerView = findViewById(R.id.questions_recycler_view);
        questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        questionsAdapter = new QuestionsAdapter();
        questionsRecyclerView.setAdapter(questionsAdapter);


        // 加载题目数据
        loadQuestionsData();
    }

    private void loadQuestionsData() {
        try {
            // 清空现有数据
            questionItems.clear();

            // 从MaUtil获取题目信息
            Map<String, List<Integer>> questionMap = maUtil.loadFavoritesFromInfoFile();

            // 处理单选题
            if (questionMap.containsKey("danxuan")) {
                for (int questionNumber : questionMap.get("danxuan")) {
                    Question question = maUtil.loadQuestionFromFile(questionBankId, "SINGLE_CHOICE", questionNumber);
                    if (question != null) {
                        String savedAnswer = maUtil.getSavedAnswerFromPaper("SINGLE_CHOICE", questionNumber);
                        boolean isCorrect = maUtil.checkAnswerCorrectness("SINGLE_CHOICE", questionNumber, savedAnswer);

                        QuestionItem item = new QuestionItem();
                        item.question = question;
                        item.userAnswer = savedAnswer;
                        item.isCorrect = isCorrect;
                        item.questionType = "单选题";
                        questionItems.add(item);
                    }
                }
            }

            // 处理多选题
            if (questionMap.containsKey("duoxuan")) {
                for (int questionNumber : questionMap.get("duoxuan")) {
                    Question question = maUtil.loadQuestionFromFile(questionBankId, "MULTI_CHOICE", questionNumber);
                    if (question != null) {
                        String savedAnswer = maUtil.getSavedAnswerFromPaper("MULTI_CHOICE", questionNumber);
                        boolean isCorrect = maUtil.checkAnswerCorrectness("MULTI_CHOICE", questionNumber, savedAnswer);

                        QuestionItem item = new QuestionItem();
                        item.question = question;
                        item.userAnswer = savedAnswer;
                        item.isCorrect = isCorrect;
                        item.questionType = "多选题";
                        questionItems.add(item);
                    }
                }
            }

            // 处理判断题
            if (questionMap.containsKey("panduan")) {
                for (int questionNumber : questionMap.get("panduan")) {
                    Question question = maUtil.loadQuestionFromFile(questionBankId, "TRUE_FALSE", questionNumber);
                    if (question != null) {
                        String savedAnswer = maUtil.getSavedAnswerFromPaper("TRUE_FALSE", questionNumber);
                        boolean isCorrect = maUtil.checkAnswerCorrectness("TRUE_FALSE", questionNumber, savedAnswer);

                        QuestionItem item = new QuestionItem();
                        item.question = question;
                        item.userAnswer = savedAnswer;
                        item.isCorrect = isCorrect;
                        item.questionType = "判断题";
                        questionItems.add(item);
                    }
                }
            }

            // 更新UI
            questionsAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class QuestionItem {
        Question question;
        String userAnswer;
        boolean isCorrect;
        String questionType;
    }

    private class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder> {

        @Override
        public QuestionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_question_card, parent, false);
            return new QuestionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(QuestionViewHolder holder, int position) {
            QuestionItem item = questionItems.get(position);

            // 设置题目类型和编号
            holder.questionNumberText.setText(item.questionType + " #" + item.question.getNumber());

            // 设置题目内容
            holder.questionContentText.setText(item.question.getContent());

            // 设置用户答案和正确答案
            String userAnswer = item.userAnswer != null ? item.userAnswer : "未作答";
            String correctAnswer = item.question.getAnswer();

            holder.userAnswerText.setText("你的答案: " + userAnswer);
            holder.correctAnswerText.setText("正确答案: " + correctAnswer);

            // 根据回答是否正确设置颜色
            int textColor = item.isCorrect ? getResources().getColor(R.color.teal_700) : getResources().getColor(R.color.red_700);
            holder.userAnswerText.setTextColor(textColor);

            // 设置选项（如果有）
            if (!item.question.getOptions().isEmpty()) {
                holder.optionsText.setText(item.question.getOptions());
                holder.optionsText.setVisibility(View.VISIBLE);
            } else {
                holder.optionsText.setVisibility(View.GONE);
            }

            // 添加点击事件
            holder.cardView.setOnClickListener(v -> {
                // 根据题目类型设置对应的类型字符串
                String questionType;
                switch (item.questionType) {
                    case "单选题":
                        questionType = "SINGLE_CHOICE";
                        break;
                    case "多选题":
                        questionType = "MULTI_CHOICE";
                        break;
                    case "判断题":
                        questionType = "TRUE_FALSE";
                        break;
                    default:
                        questionType = "SINGLE_CHOICE"; // 默认值
                }

                // 启动 QuizHomeActivity
                Intent intent = new Intent(PaperDetailActivity.this, QuizHomeActivity.class);
                intent.putExtra("question_bank_id", questionBankId);
                intent.putExtra("question_type", questionType);
                intent.putExtra("question_id", item.question.getNumber());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return questionItems.size();
        }

        class QuestionViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView questionNumberText;
            TextView questionContentText;
            TextView optionsText;
            TextView userAnswerText;
            TextView correctAnswerText;

            QuestionViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.question_card);
                questionNumberText = itemView.findViewById(R.id.question_number_text);
                questionContentText = itemView.findViewById(R.id.question_content_text);
                optionsText = itemView.findViewById(R.id.options_text);
                userAnswerText = itemView.findViewById(R.id.user_answer_text);
                correctAnswerText = itemView.findViewById(R.id.correct_answer_text);
            }
        }
    }
}