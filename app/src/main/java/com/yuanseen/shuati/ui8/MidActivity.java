package com.yuanseen.shuati.ui8;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yuanseen.shuati.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class MidActivity extends AppCompatActivity {

    private EditText searchEditText;
    private LinearLayout resultsContainer;
    private String questionBankId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 获取题库ID
        questionBankId = getIntent().getStringExtra("question_bank_id");
        if (questionBankId == null) {
            Toast.makeText(this, "题库信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图
        searchEditText = findViewById(R.id.search_edit_text);
        resultsContainer = findViewById(R.id.results_container);

        // 设置搜索监听
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuestions(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 边缘处理
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void searchQuestions(String keyword) {
        resultsContainer.removeAllViews();
        if (keyword.isEmpty()) return;

        try {
            File bankFile = new File(getFilesDir(), "question_banks/" + questionBankId + ".json");
            if (!bankFile.exists()) {
                Toast.makeText(this, "题库文件不存在", Toast.LENGTH_SHORT).show();
                return;
            }

            String bankJsonStr = new String(Files.readAllBytes(bankFile.toPath()));
            JSONObject bankJson = new JSONObject(bankJsonStr);
            JSONObject tiku = bankJson.getJSONObject("tiku");

            List<JSONObject> results = new ArrayList<>();
            searchInQuestionArray(tiku.getJSONArray("danxuan"), keyword, results);
            searchInQuestionArray(tiku.getJSONArray("duoxuan"), keyword, results);
            searchInQuestionArray(tiku.getJSONArray("panduan"), keyword, results);

            if (results.isEmpty()) {
                TextView noResult = new TextView(this);
                noResult.setText("没有找到匹配的题目");
                noResult.setTextSize(16);
                noResult.setPadding(16, 16, 16, 16);
                resultsContainer.addView(noResult);
            } else {
                for (JSONObject question : results) {
                    addQuestionToView(question);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void searchInQuestionArray(JSONArray questions, String keyword, List<JSONObject> results) {
        try {
            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                String stem = question.getString("stem").toLowerCase();
                String answer = question.getString("answer").toLowerCase();
                if (stem.contains(keyword.toLowerCase()) || answer.contains(keyword.toLowerCase())) {
                    results.add(question);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addQuestionToView(JSONObject question) {
        try {
            View questionView = getLayoutInflater().inflate(R.layout.item_question_result, null);

            TextView typeView = questionView.findViewById(R.id.question_type);
            TextView stemView = questionView.findViewById(R.id.question_stem);
            TextView answerView = questionView.findViewById(R.id.question_answer);

            String type = question.getString("type");
            int id = question.getInt("id");
            typeView.setText(getTypeName(type) + " #" + id);
            stemView.setText(question.getString("stem"));
            answerView.setText("答案: " + question.getString("answer"));

            // 添加点击监听器
            questionView.setOnClickListener(v -> {
//                String message = "题目类型: " + getTypeName(type) + ", ID: " + id;
                // In your previous activity where you want to launch the quiz:
                Intent intent = new Intent(this, QuizHomeActivity.class);
                intent.putExtra("question_bank_id", questionBankId);
                intent.putExtra("question_type", getTypeName2(type)); // or "MULTI_CHOICE" or "TRUE_FALSE"
                intent.putExtra("question_id", id); // the question ID you want to display
                startActivity(intent);
//                Toast.makeText(MidActivity.this, message, Toast.LENGTH_SHORT).show();
            });

            resultsContainer.addView(questionView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getTypeName(String type) {
        switch (type) {
            case "danxuan": return "单选题";
            case "duoxuan": return "多选题";
            case "panduan": return "判断题";
            default: return type;
        }
    }

    private String getTypeName2(String type) {
        switch (type) {
            case "danxuan": return "SINGLE_CHOICE";
            case "duoxuan": return "MULTI_CHOICE";
            case "panduan": return "TRUE_FALSE";
            default: return type;
        }
    }
}