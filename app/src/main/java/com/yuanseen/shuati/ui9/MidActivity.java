package com.yuanseen.shuati.ui9;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui5.QuizHomeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MidActivity extends AppCompatActivity {

    private String questionBankId;
    private RecyclerView papersRecyclerView;
    private TextView emptyView;
    private List<PaperInfo> paperInfoList = new ArrayList<>();
    private PaperAdapter paperAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mid3);

        // 获取题库ID
        questionBankId = getIntent().getStringExtra("question_bank_id");
        if (questionBankId == null) {
            Toast.makeText(this, "题库信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化视图
        papersRecyclerView = findViewById(R.id.papers_recycler_view);
        emptyView = findViewById(R.id.empty_view);

        // 设置RecyclerView布局管理器为2列的网格布局
        papersRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        paperAdapter = new PaperAdapter();
        papersRecyclerView.setAdapter(paperAdapter);


        // 加载试卷数据
        loadPapersData();
    }

    private void loadPapersData() {
        try {
            // 读取题库信息文件
            File infoFile = new File(getFilesDir(), "question_banks/" + questionBankId + "_info.json");
            if (!infoFile.exists()) {
                showEmptyView();
                return;
            }

            String infoJsonStr = new String(Files.readAllBytes(infoFile.toPath()));
            JSONObject infoJson = new JSONObject(infoJsonStr);

            // 获取生成的试卷列表
            JSONArray generatedPapers = infoJson.optJSONArray("generatedPapers");
            if (generatedPapers == null || generatedPapers.length() == 0) {
                showEmptyView();
                return;
            }

            paperInfoList.clear();

            // 遍历所有试卷
            for (int i = 0; i < generatedPapers.length(); i++) {
                String paperId = generatedPapers.getString(i);
                File paperFile = new File(getFilesDir(), "question_banks/" + paperId + ".json");

                if (paperFile.exists()) {
                    String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                    JSONObject paperJson = new JSONObject(paperJsonStr);

                    PaperInfo paperInfo = new PaperInfo();
                    paperInfo.paperId = paperId;
                    paperInfo.startTime = paperJson.optString("kaikaoshijian", "未知时间");

                    // 获取分数，如果不存在则计算
                    if (paperJson.has("defen") && paperJson.getInt("defen") != -1) {
                        paperInfo.score = paperJson.getInt("defen");
                    } else {
                        paperInfo.score = calculateExamScore(getFilesDir(), paperId);
                        // 更新试卷文件中的分数
                        paperJson.put("defen", paperInfo.score);
                        Files.write(paperFile.toPath(), paperJson.toString().getBytes());
                    }

                    paperInfoList.add(paperInfo);
                }
            }

            if (paperInfoList.isEmpty()) {
                showEmptyView();
            } else {
                hideEmptyView();
                paperAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e("MidActivity", "加载试卷数据失败", e);
            showEmptyView();
        }
    }

    private void showEmptyView() {
        papersRecyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText("暂无试卷记录");
    }

    private void hideEmptyView() {
        papersRecyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
    }

    private class PaperInfo {
        String paperId;
        String startTime;
        int score;
    }

    private class PaperAdapter extends RecyclerView.Adapter<PaperAdapter.PaperViewHolder> {

        @Override
        public PaperViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_paper_card, parent, false);
            return new PaperViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PaperViewHolder holder, int position) {
            PaperInfo paperInfo = paperInfoList.get(position);

            // 设置分数
            holder.scoreText.setText(String.valueOf(paperInfo.score));

            // 设置开始时间
            holder.timeText.setText(paperInfo.startTime);

            // 设置点击事件
            holder.cardView.setOnClickListener(v -> {
                // 点击进入考试详情或重新考试
                Intent intent = new Intent(MidActivity.this, PaperDetailActivity.class);
                intent.putExtra("question_bank_id", questionBankId);
                intent.putExtra("paper_id", paperInfo.paperId);
                intent.putExtra("exam_mode", true);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return paperInfoList.size();
        }

        class PaperViewHolder extends RecyclerView.ViewHolder {
            CardView cardView;
            TextView scoreText;
            TextView timeText;

            PaperViewHolder(View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.paper_card);
                scoreText = itemView.findViewById(R.id.score_text);
                timeText = itemView.findViewById(R.id.time_text);
            }
        }
    }

    // 保留原有的静态方法
    public static void finishExamPaper(File filesDir, String paperId) {
        try {
            File paperFile = new File(filesDir, "question_banks/" + paperId + ".json");
            if (!paperFile.exists()) {
                return;
            }

            String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(paperJsonStr);
            paperJson.put("shifoukaoshijieshu", true);

            Files.write(paperFile.toPath(), paperJson.toString().getBytes());
        } catch (Exception e) {
            Log.e("MidActivity", "结束考试失败", e);
        }
    }

    public static int calculateExamScore(File filesDir, String paperId) {
        try {
            File paperFile = new File(filesDir, "question_banks/" + paperId + ".json");
            if (!paperFile.exists()) {
                return -1;
            }

            String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(paperJsonStr);
            JSONObject examConfig = paperJson.getJSONObject("yushelianxi");
            JSONObject questions = paperJson.getJSONObject("timu");

            int totalScore = 0;

            // 计算单选题分数
            if (questions.has("danxuan")) {
                JSONArray danxuanQuestions = questions.getJSONArray("danxuan");
                int danxuanScorePerQuestion = examConfig.getJSONArray("danxuan").getInt(1);

                for (int i = 0; i < danxuanQuestions.length(); i++) {
                    JSONObject question = danxuanQuestions.getJSONObject(i);
                    if (!question.isNull("yourans") && question.getBoolean("duicuo")) {
                        totalScore += danxuanScorePerQuestion;
                    }
                }
            }

            // 计算多选题分数
            if (questions.has("duoxuan")) {
                JSONArray duoxuanQuestions = questions.getJSONArray("duoxuan");
                int duoxuanScorePerQuestion = examConfig.getJSONArray("duoxuan").getInt(1);

                for (int i = 0; i < duoxuanQuestions.length(); i++) {
                    JSONObject question = duoxuanQuestions.getJSONObject(i);
                    if (!question.isNull("yourans") && question.getBoolean("duicuo")) {
                        totalScore += duoxuanScorePerQuestion;
                    }
                }
            }

            // 计算判断题分数
            if (questions.has("panduan")) {
                JSONArray panduanQuestions = questions.getJSONArray("panduan");
                int panduanScorePerQuestion = examConfig.getJSONArray("panduan").getInt(1);

                for (int i = 0; i < panduanQuestions.length(); i++) {
                    JSONObject question = panduanQuestions.getJSONObject(i);
                    if (!question.isNull("yourans") && question.getBoolean("duicuo")) {
                        totalScore += panduanScorePerQuestion;
                    }
                }
            }

            return totalScore;
        } catch (Exception e) {
            Log.e("MidActivity", "计算试卷分数失败", e);
            return -1;
        }
    }
}