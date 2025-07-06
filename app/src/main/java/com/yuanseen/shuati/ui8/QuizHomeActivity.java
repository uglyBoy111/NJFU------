package com.yuanseen.shuati.ui8;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfo;
import com.yuanseen.shuati.ui.gallery.addqes.bankinfo.BankInfoManager;
import com.yuanseen.shuati.ui2.Question;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;

public class QuizHomeActivity extends AppCompatActivity {
    private QuestionDetailFragment questionDetailFragment;
    private String currentQuestionType;
    private int currentQuestionId;
    private String currentBankId;

    private BankInfoManager bankInfoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_home8);

        // 获取参数
        currentBankId = getIntent().getStringExtra("question_bank_id");
        currentQuestionType = getIntent().getStringExtra("question_type");
        currentQuestionId = getIntent().getIntExtra("question_id", -1);
        // 初始化BankInfoManager
        bankInfoManager = new BankInfoManager(this);

        // 初始化 Fragment（兼容动态添加）
        questionDetailFragment = (QuestionDetailFragment) getSupportFragmentManager()
                .findFragmentById(R.id.question_detail_fragment8);

        if (questionDetailFragment == null) {
            questionDetailFragment = new QuestionDetailFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.question_detail_fragment8, questionDetailFragment)
                    .commitNow(); // 使用 commitNow() 确保立即生效
        }

        // 延迟加载题目，等待 Fragment 视图就绪
        new Handler().post(() -> {
            if (currentQuestionType != null && currentQuestionId != -1) {
                displayQuestion(currentQuestionType, currentQuestionId);
            }
        });
    }

    private void displayQuestion(String questionType, int questionNumber) {
        // 主线程显示当前题目
        runOnUiThread(() -> {
            Question question = loadQuestionFromFile(currentBankId, questionType, questionNumber);
            if (question != null && questionDetailFragment != null) {
                BankInfo bankInfo = bankInfoManager.getBankInfo(currentBankId);
                boolean isFavorite = bankInfo.getFavorites().get(getTypeKey(questionType)).contains(questionNumber);
                boolean isWrong = bankInfo.getWrongQuestions().get(getTypeKey(questionType)).contains(questionNumber);

                questionDetailFragment.displayQuestionDetails(question, isFavorite, isWrong, questionNumber > currentQuestionId);

                currentQuestionType = questionType;
                currentQuestionId = questionNumber;
                Log.e("11",questionType+questionNumber);
                Log.e("11",question.toString());
            }
        });
    }

    private Question loadQuestionFromFile(String bankId, String type, int number) {
        File bankFile = new File(getFilesDir(), "question_banks/" + bankId + ".json");
        if (!bankFile.exists()) {
            Log.e("QuizHomeActivity", "题库文件不存在: " + bankId);
            return null;
        }

        try {
            String jsonStr = new String(Files.readAllBytes(bankFile.toPath()));
            JSONObject bankJson = new JSONObject(jsonStr);
            JSONObject tiku = bankJson.getJSONObject("tiku");

            String typeKey = getTypeKey(type);
            JSONArray questions = tiku.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject q = questions.getJSONObject(i);
                if (q.getInt("id") == number) {
                    String content = q.getString("stem");
                    String answer = q.getString("answer");
                    String options = "";

                    if (!type.equals("TRUE_FALSE")) {
                        StringBuilder sb = new StringBuilder();
                        char optionChar = 'A';
                        for (int j = 0; j < 6; j++) {
                            String opKey = "op" + (char)('a' + j);
                            if (q.has(opKey) && !q.getString(opKey).isEmpty()) {
                                sb.append(optionChar++).append(". ").append(q.getString(opKey)).append("\n");
                            }
                        }
                        options = sb.toString().trim();
                    }

                    return new Question(
                            bankId,
                            Question.Type.valueOf(type),
                            number,
                            content,
                            options,
                            answer
                    );
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载题目失败", e);
        }
        return null;
    }

    private String getTypeKey(String questionType) {
        switch (questionType) {
            case "SINGLE_CHOICE": return "danxuan";
            case "MULTI_CHOICE": return "duoxuan";
            case "TRUE_FALSE": return "panduan";
            default: return "";
        }
    }
}