package com.yuanseen.shuati.ui8;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfoManager;
import com.yuanseen.shuati.ui2.Question;
import com.yuanseen.shuati.ui2.QuizHomeActivity;

import java.util.ArrayList;
import java.util.List;

public class QuestionDetailFragment extends Fragment {
    private TextView questionTypeText;
    private TextView questionContentView;
    private RecyclerView optionsRecyclerView;
    private TextView questionAnswerView;
    private OptionAdapter optionAdapter;

    private ImageButton favoriteButton;
    private ImageButton wrongQuestionButton;

    private BankInfoManager bankInfoManager;
    private String currentBankId;
    private String currentQuestionType;
    private int currentQuestionId;
    private boolean isFavorite;
    private boolean isWrong;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bankInfoManager = new BankInfoManager(requireContext());
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_question_detail8, container, false);

        // 初始化视图
        questionTypeText = view.findViewById(R.id.question_type_text8);
        questionContentView = view.findViewById(R.id.question_content_text);
        optionsRecyclerView = view.findViewById(R.id.question_options_recycler);
        questionAnswerView = view.findViewById(R.id.question_answer_text);
        favoriteButton = view.findViewById(R.id.btn_favorite);
        wrongQuestionButton = view.findViewById(R.id.btn_wrong_question);

        favoriteButton.setOnClickListener(v -> toggleFavoriteStatus());
        wrongQuestionButton.setOnClickListener(v -> toggleWrongQuestionStatus());

        // 设置RecyclerView
        optionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        optionAdapter = new OptionAdapter();
        optionsRecyclerView.setAdapter(optionAdapter);

        return view;
    }

    public void displayQuestionDetails(@Nullable Question question, boolean isFavorite, boolean isWrong, boolean isNext) {
        if (!isAdded() || getView() == null) {
            // 如果Fragment未附加或视图未初始化，延迟处理
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded() && getView() != null) {
                    displayQuestionDetails(question, isFavorite, isWrong, isNext);
                }
            });
            return;
        }

        // 更新题目内容
        updateQuestionContent(question, isFavorite, isWrong);
        Log.e("22",question.toString());

    }

            // 设置题型和题号
            private void updateQuestionContent(Question question, boolean isFavorite, boolean isWrong) {

                // 添加空检查
                if (questionTypeText == null || questionContentView == null ||
                        optionsRecyclerView == null || questionAnswerView == null) {
                    return;
                }

                if (question == null) {
                    questionTypeText.setText("");
                    questionContentView.setText("暂无题目内容");
                    optionAdapter.setOptions(new ArrayList<>(), "");
                    questionAnswerView.setText("");
                    return;
                }

                currentBankId = question.getBankId();
                currentQuestionType = question.getType().name();
                currentQuestionId = question.getNumber();
                this.isFavorite = isFavorite;
                this.isWrong = isWrong;


                // 设置题型和题号
                String typeText = "";
                switch (question.getType()) {
                    case SINGLE_CHOICE:
                        typeText = "单选题 #" + currentQuestionId;
                        break;
                    case MULTI_CHOICE:
                        typeText = "多选题 #" + currentQuestionId;
                        break;
                    case TRUE_FALSE:
                        typeText = "判断题 #" + currentQuestionId;
                        break;
                }
                questionTypeText.setText(typeText);

                // 设置题干
                questionContentView.setText(question.getContent());

                // 设置选项
                List<OptionItem> options = new ArrayList<>();
                if (question.getType() != Question.Type.TRUE_FALSE) {
                    String[] optionLines = question.getOptions().split("\n");
                    for (String line : optionLines) {
                        if (line.length() > 2) {
                            options.add(new OptionItem(line.substring(0, 1), line.substring(2)));
                        }
                    }
                } else {
                    // 判断题特殊处理
                    options.add(new OptionItem("A", "正确"));
                    options.add(new OptionItem("B", "错误"));
                }
                optionAdapter.setOptions(options, question.getAnswer());

                // 设置答案
                String answerText = "答案: ";
                switch (question.getType()) {
                    case SINGLE_CHOICE:
                        answerText += question.getAnswer();
                        break;
                    case MULTI_CHOICE:
                        StringBuilder sb = new StringBuilder();
                        for (char c : question.getAnswer().toCharArray()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(c);
                        }
                        answerText += sb.toString();
                        break;
                    case TRUE_FALSE:
                        answerText += question.getAnswer().equals("A") ? "正确" : "错误";
                        break;
                }
                questionAnswerView.setText(answerText);

                updateButtonStates();
            }

    private void updateButtonStates() {
        // 更新收藏按钮状态
        favoriteButton.setImageResource(
                isFavorite ? R.drawable.tar_duotone_icon : R.drawable.star_half_duotone_icon
        );

        // 更新错题按钮状态
        wrongQuestionButton.setImageResource(
                isWrong ? R.drawable.calendar_x_duotone_icon : R.drawable.calendar_duotone_icon
        );
    }

    private void toggleFavoriteStatus() {
        String typeKey = getTypeKey(currentQuestionType);
        if (isFavorite) {
            bankInfoManager.removeFavorite(currentBankId, typeKey, currentQuestionId);
            Toast.makeText(getContext(), "已移出收藏", Toast.LENGTH_SHORT).show();
        } else {
            bankInfoManager.addFavorite(currentBankId, typeKey, currentQuestionId);
            Toast.makeText(getContext(), "已加入收藏", Toast.LENGTH_SHORT).show();
        }
        isFavorite = !isFavorite;
        updateButtonStates();
    }

    private void toggleWrongQuestionStatus() {
        String typeKey = getTypeKey(currentQuestionType);
        if (isWrong) {
            bankInfoManager.removeWrongQuestion(currentBankId, typeKey, currentQuestionId);
            Toast.makeText(getContext(), "已移出错题本", Toast.LENGTH_SHORT).show();
        } else {
            bankInfoManager.addWrongQuestion(currentBankId, typeKey, currentQuestionId);
            Toast.makeText(getContext(), "已加入错题本", Toast.LENGTH_SHORT).show();
        }
        isWrong = !isWrong;
        updateButtonStates();
    }
    private String getTypeKey(String questionType) {
        switch (questionType) {
            case "SINGLE_CHOICE": return "danxuan";
            case "MULTI_CHOICE": return "duoxuan";
            case "TRUE_FALSE": return "panduan";
            default: return "";
        }
    }

    // 选项适配器
    private static class OptionItem {
        String key;
        String value;
        OptionItem(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private class OptionAdapter extends RecyclerView.Adapter<OptionAdapter.OptionViewHolder> {
        private List<OptionItem> options = new ArrayList<>();
        private String correctAnswer = "";

        void setOptions(List<OptionItem> options, String correctAnswer) {
            this.options = options;
            this.correctAnswer = correctAnswer;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_option, parent, false);
            return new OptionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
            OptionItem item = options.get(position);
            holder.optionKey.setText(item.key + ".");
            holder.optionValue.setText(item.value);

            // 高亮显示正确答案
            if (correctAnswer.contains(item.key)) {
                holder.optionKey.setTextColor(getResources().getColor(R.color.teal_700));
                holder.optionValue.setTextColor(getResources().getColor(R.color.teal_700));
            } else {
                holder.optionKey.setTextColor(getResources().getColor(R.color.black));
                holder.optionValue.setTextColor(getResources().getColor(R.color.black));
            }
        }

        @Override
        public int getItemCount() {
            return options.size();
        }

        class OptionViewHolder extends RecyclerView.ViewHolder {
            TextView optionKey;
            TextView optionValue;

            OptionViewHolder(View itemView) {
                super(itemView);
                optionKey = itemView.findViewById(R.id.option_key);
                optionValue = itemView.findViewById(R.id.option_value);
            }
        }
    }
}