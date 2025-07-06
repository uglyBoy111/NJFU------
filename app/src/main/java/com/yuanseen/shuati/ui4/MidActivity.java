package com.yuanseen.shuati.ui4;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui4.QuizHomeActivity;

import java.util.ArrayList;

public class MidActivity extends AppCompatActivity {

    private CheckBox checkboxSingle;
    private CheckBox checkboxMulti;
    private CheckBox checkboxJudge;
    private String questionBankId;

    private TextView allTypesMaxStreak;

    private TextView selectedTypesMaxStreak;

    private String allTypesMax_pre = "all_types_max_streak";
    private String selectedTypesMax_pre = "selected_types_max_streak";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mid);

        // 获取题库ID
        questionBankId = getIntent().getStringExtra("question_bank_id");
        if (questionBankId == null) {
            Toast.makeText(this, "题库信息错误", Toast.LENGTH_SHORT).show();
            finish();
        }
        // 初始化连对量显示

        allTypesMaxStreak = findViewById(R.id.all_types_max_streak);
        selectedTypesMaxStreak = findViewById(R.id.selected_types_max_streak);

        // 从SharedPreferences获取数据
        SharedPreferences prefs = getSharedPreferences("AnswerStreakRecords", MODE_PRIVATE);
        int allTypesMax = prefs.getInt(allTypesMax_pre+questionBankId, 0);
        int selectedTypesMax = prefs.getInt(selectedTypesMax_pre+questionBankId, 0);

        // 更新UI
        allTypesMaxStreak.setText("全选题型历史最高连对: " + allTypesMax);
        selectedTypesMaxStreak.setText("部分题型历史最高连对: " + selectedTypesMax);
        // 初始化视图
        checkboxSingle = findViewById(R.id.checkbox_single);
        checkboxMulti = findViewById(R.id.checkbox_multi);
        checkboxJudge = findViewById(R.id.checkbox_judge);
        Button confirmButton = findViewById(R.id.confirm_button);

        // 确认按钮点击事件
        confirmButton.setOnClickListener(v -> {
            ArrayList<String> selectedTypes = new ArrayList<>();

            if (checkboxSingle.isChecked()) {
                selectedTypes.add("SINGLE_CHOICE");
            }
            if (checkboxMulti.isChecked()) {
                selectedTypes.add("MULTI_CHOICE");
            }
            if (checkboxJudge.isChecked()) {
                selectedTypes.add("TRUE_FALSE");
            }

            if (selectedTypes.isEmpty()) {
                Toast.makeText(this, "请至少选择一种题型", Toast.LENGTH_SHORT).show();
                return;
            }

            // 启动QuizHomeActivity并传递选择的题型
            Intent intent = new Intent(this, QuizHomeActivity.class);
            intent.putExtra("question_bank_id", questionBankId);
            intent.putExtra("random_mode", true);
            intent.putStringArrayListExtra("selected_types", selectedTypes);
            startActivity(intent);
            finish();
        });

        // 边缘处理
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}