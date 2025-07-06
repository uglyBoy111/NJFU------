package com.yuanseen.shuati.ui5;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.yuanseen.shuati.R;
import com.yuanseen.shuati.ui9.PaperDetailActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class MidActivity extends AppCompatActivity {

    private String questionBankId;

    private RadioGroup presetRadioGroup;
    //#1这里写一个是否正在考试的成员变量
    private boolean isInExam;
    //#2这里写一个上一场考试的试卷id
    private String lastExamPaperId;
    private SharedPreferences prefs;

    private JSONObject examPresets;

    // 在成员变量部分添加
    private ImageButton addPresetButton;
    private List<JSONObject> customPresets = new ArrayList<>();

    private LineChart scoreChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mid2);

        // 获取题库ID
        questionBankId = getIntent().getStringExtra("question_bank_id");
        if (questionBankId == null) {
            Toast.makeText(this, "题库信息错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

// 在onCreate()中:
        scoreChart = findViewById(R.id.scoreChart);
        setupScoreChart();
        scoreChart.setOnClickListener(v -> {
            Intent intent = new Intent(MidActivity.this, com.yuanseen.shuati.ui9.MidActivity.class);
            intent.putExtra("question_bank_id", questionBankId);
//            intent.putExtra("paper_id", paperInfo.paperId);
//            intent.putExtra("exam_mode", true);
            startActivity(intent);
        });

        presetRadioGroup = findViewById(R.id.preset_radio_group);
        Button confirmButton = findViewById(R.id.confirm_button);

        // 从SharedPreferences获取数据
        prefs = getSharedPreferences("AnswerStreakRecords", MODE_PRIVATE);
        //#3在这里也需要getSharedPreferences初始化成员变量：上一场考试的试卷id
        lastExamPaperId = prefs.getString("last_exam_paper_id", null);
        isInExam = checkIfInExam();
        int allTypesMax = prefs.getInt("all_types_max_streak", 0);
        int selectedTypesMax = prefs.getInt("selected_types_max_streak", 0);

        // 更新UI


        // 加载试卷预设
        loadExamPresets();
        loadCustomPresets(); // 加载已保存的配置

// 在onCreate方法中添加
        addPresetButton = findViewById(R.id.add_preset_button);
        addPresetButton.setOnClickListener(v -> showAddPresetDialog());
        // 确认按钮点击事件
        confirmButton.setOnClickListener(v -> {
            int selectedId = presetRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "请选择一个试卷预设", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            String presetName = selectedRadioButton.getText().toString();
            startExamWithPreset(presetName);
        });
// 修改RadioGroup的长按监听器
        presetRadioGroup.setOnLongClickListener(v -> {
            if (v instanceof RadioButton) {
                RadioButton radioButton = (RadioButton) v;
                String presetName = radioButton.getText().toString();

                if (!presetName.equals("默认预设")) {
                    new AlertDialog.Builder(this)
                            .setTitle("删除配置")
                            .setMessage("确定要删除配置 '" + presetName + "' 吗？")
                            .setPositiveButton("删除", (dialog, which) -> deletePreset(radioButton))
                            .setNegativeButton("取消", null)
                            .show();
                    return true;
                }
            }
            return false;
        });
        // 边缘处理
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // 添加设置图表的方法
    private void setupScoreChart() {
        // 准备图表数据
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        try {
            // 读取考试历史数据
            File infoFile = new File(getFilesDir(), "question_banks/" + questionBankId + "_info.json");
            if (infoFile.exists()) {
                String infoJsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                JSONObject infoJson = new JSONObject(infoJsonStr);
                JSONArray generatedPapers = infoJson.getJSONArray("generatedPapers");

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault());
                SimpleDateFormat displayFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());

                for (int i = 0; i < generatedPapers.length(); i++) {
                    String paperId = generatedPapers.getString(i);
                    File paperFile = new File(getFilesDir(), "question_banks/" + paperId + ".json");

                    if (paperFile.exists()) {
                        String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                        JSONObject paperJson = new JSONObject(paperJsonStr);

                        if (paperJson.has("defen") && paperJson.getInt("defen") != -1) {
                            int score = paperJson.getInt("defen");
                            String timeStr = paperJson.getString("kaikaoshijian");
                            Date date = sdf.parse(timeStr);

                            entries.add(new Entry(i, score));
                            labels.add(displayFormat.format(date));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MidActivity", "设置图表出错", e);
        }

        // 创建数据集
        LineDataSet dataSet = new LineDataSet(entries, "考试成绩");
        dataSet.setColor(Color.BLACK);
        dataSet.setCircleColor(Color.BLACK);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 设置图表数据
        LineData lineData = new LineData(dataSet);
        scoreChart.setData(lineData);

        // 自定义图表外观
        scoreChart.setBackgroundColor(Color.parseColor("#F5F5F5"));
        scoreChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        scoreChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        scoreChart.getXAxis().setGranularity(1f);
        scoreChart.getXAxis().setLabelCount(labels.size());
        scoreChart.getXAxis().setTextColor(Color.BLACK);
        scoreChart.getXAxis().setAxisLineColor(Color.BLACK);
        scoreChart.getXAxis().setGridColor(Color.parseColor("#E0E0E0"));

        scoreChart.getAxisLeft().setTextColor(Color.BLACK);
        scoreChart.getAxisLeft().setAxisLineColor(Color.BLACK);
        scoreChart.getAxisLeft().setGridColor(Color.parseColor("#E0E0E0"));
        scoreChart.getAxisRight().setEnabled(false);

        scoreChart.getDescription().setEnabled(false);
        scoreChart.getLegend().setEnabled(false);

        // 添加动画
        scoreChart.animateX(1500);
        scoreChart.animateY(1500);

        scoreChart.invalidate();
    }

//    #$#在这里获取分数和时间
    public void printExamScoresAndTimes(String questionBankId) {
        try {
            // 1. 读取题库信息文件
            File infoFile = new File(getFilesDir(), "question_banks/" + questionBankId + "_info.json");
            if (!infoFile.exists()) {
                System.out.println("题库信息文件不存在");
                return;
            }

            // 2. 解析题库信息文件
            String infoJsonStr = new String(Files.readAllBytes(infoFile.toPath()));
            JSONObject infoJson = new JSONObject(infoJsonStr);

            // 3. 获取生成的试卷列表
            JSONArray generatedPapers = infoJson.optJSONArray("generatedPapers");
            if (generatedPapers == null || generatedPapers.length() == 0) {
                System.out.println("该题库没有试卷记录");
                return;
            }

            // 4. 遍历所有试卷
            System.out.println("试卷ID\t\t分数\t\t开考时间");
            System.out.println("----------------------------------");

            for (int i = 0; i < generatedPapers.length(); i++) {
                String paperId = generatedPapers.getString(i);
                File paperFile = new File(getFilesDir(), "question_banks/" + paperId + ".json");

                if (paperFile.exists()) {
                    String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                    JSONObject paperJson = new JSONObject(paperJsonStr);

                    // 获取分数
                    int score = paperJson.has("defen") ? paperJson.getInt("defen") : -1;

                    // 获取开考时间
                    String startTime = paperJson.optString("kaikaoshijian", "未知时间");

                    // 打印信息
                    System.out.println(paperId + "\t" + score + "\t\t" + startTime);
                } else {
                    System.out.println(paperId + "\t文件不存在");
                }
            }

        } catch (Exception e) {
            System.err.println("处理试卷信息时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // 统一删除方法
    private void deletePreset(RadioButton radioButton) {
        try {
            JSONObject presetToRemove = (JSONObject) radioButton.getTag();
            String presetName = presetToRemove.getString("presetName");

            // 从列表中移除
            Iterator<JSONObject> iterator = customPresets.iterator();
            while (iterator.hasNext()) {
                JSONObject preset = iterator.next();
                if (preset.getString("presetName").equals(presetName)) {
                    iterator.remove();
                    break;
                }
            }

            // 从RadioGroup中移除视图
            presetRadioGroup.removeView(radioButton);

            // 立即保存并刷新
            saveCustomPresets();
            Toast.makeText(this, "配置已删除", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("MidActivity", "删除配置失败", e);
            Toast.makeText(this, "删除配置失败", Toast.LENGTH_SHORT).show();
        }
    }
    private void showDeletePresetDialog(RadioButton radioButton, String presetName, JSONObject presetToRemove) {
        new AlertDialog.Builder(MidActivity.this)
                .setTitle("删除配置")
                .setMessage("确定要删除配置 '" + presetName + "' 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    // 从customPresets中移除对应的配置
                    Iterator<JSONObject> iterator = customPresets.iterator();
                    while (iterator.hasNext()) {
                        JSONObject preset = iterator.next();
                        try {
                            if (preset.toString().equals(presetToRemove.toString())) {
                                iterator.remove();
                                break;
                            }
                        } catch (Exception e) {
                            Log.e("MidActivity", "比较配置出错", e);
                        }
                    }

                    // 从RadioGroup中移除视图
                    presetRadioGroup.removeView(radioButton);

                    // 立即保存更改
                    saveCustomPresets();
                    Toast.makeText(MidActivity.this, "配置已删除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void showAddPresetDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_preset, null);
        EditText presetNameEditText = dialogView.findViewById(R.id.preset_name);
        EditText danxuanCountEditText = dialogView.findViewById(R.id.danxuan_count);
        EditText danxuanScoreEditText = dialogView.findViewById(R.id.danxuan_score);
        EditText duoxuanCountEditText = dialogView.findViewById(R.id.duoxuan_count);
        EditText duoxuanScoreEditText = dialogView.findViewById(R.id.duoxuan_score);
        EditText panduanCountEditText = dialogView.findViewById(R.id.panduan_count);
        EditText panduanScoreEditText = dialogView.findViewById(R.id.panduan_score);
        EditText examTimeEditText = dialogView.findViewById(R.id.exam_time);

        new AlertDialog.Builder(this)
                .setTitle("添加新配置")
                .setView(dialogView)
                .setPositiveButton("保存", (dialog, which) -> {
                    try {
                        String presetName = presetNameEditText.getText().toString();
                        if (TextUtils.isEmpty(presetName)) {
                            Toast.makeText(this, "配置名称不能为空", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 检查名称是否已存在
                        for (int i = 0; i < presetRadioGroup.getChildCount(); i++) {
                            View child = presetRadioGroup.getChildAt(i);
                            if (child instanceof RadioButton) {
                                RadioButton rb = (RadioButton) child;
                                if (rb.getText().toString().equals(presetName)) {
                                    Toast.makeText(this, "配置名称已存在", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }
                        JSONObject newPreset = new JSONObject();
                        newPreset.put("presetName", presetName);
                        // 单选题配置
                        JSONArray danxuanConfig = new JSONArray();
                        danxuanConfig.put(Integer.parseInt(danxuanCountEditText.getText().toString()));
                        danxuanConfig.put(Integer.parseInt(danxuanScoreEditText.getText().toString()));
                        newPreset.put("danxuan", danxuanConfig);

                        // 多选题配置
                        JSONArray duoxuanConfig = new JSONArray();
                        duoxuanConfig.put(Integer.parseInt(duoxuanCountEditText.getText().toString()));
                        duoxuanConfig.put(Integer.parseInt(duoxuanScoreEditText.getText().toString()));
                        newPreset.put("duoxuan", duoxuanConfig);

                        // 判断题配置
                        JSONArray panduanConfig = new JSONArray();
                        panduanConfig.put(Integer.parseInt(panduanCountEditText.getText().toString()));
                        panduanConfig.put(Integer.parseInt(panduanScoreEditText.getText().toString()));
                        newPreset.put("panduan", panduanConfig);

                        // 考试时间
                        newPreset.put("kaoshishijian", Integer.parseInt(examTimeEditText.getText().toString()));

                        // 添加到自定义预设列表
                        customPresets.add(newPreset);
                        // 保存到SharedPreferences
                        saveCustomPresets();
                        // 添加到RadioGroup
                        addPresetRadioButton(presetName, newPreset);

                        Toast.makeText(this, "配置添加成功", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "添加配置失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("MidActivity", "添加配置失败", e);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void loadExamPresets() {
        try {
            File bankFile = new File(getFilesDir(), "question_banks/" + questionBankId + ".json");
            if (!bankFile.exists()) {
                Toast.makeText(this, "题库文件不存在", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String bankJsonStr = new String(Files.readAllBytes(bankFile.toPath()));
            JSONObject bankJson = new JSONObject(bankJsonStr);

            // 获取预设配置
            examPresets = bankJson.getJSONObject("yushelianxi");


            // 添加默认预设
            addPresetRadioButton("默认预设", examPresets);

            // 如果有其他预设，也可以在这里添加
//             addPresetRadioButton("其他预设", otherPresets);

        } catch (Exception e) {
            Log.e("MidActivity", "加载试卷预设失败", e);
            Toast.makeText(this, "加载试卷预设失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    // 修改保存方法确保数据一致性
    private void saveCustomPresets() {
        try {
            JSONArray presetsArray = new JSONArray();
            for (JSONObject preset : customPresets) {
                presetsArray.put(new JSONObject(preset.toString())); // 深拷贝
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("custom_presets", presetsArray.toString());
            editor.apply(); // 使用apply确保异步保存
        } catch (Exception e) {
            Log.e("MidActivity", "保存自定义配置失败", e);
        }
    }

    private void loadCustomPresets() {
        try {
            String presetsJson = prefs.getString("custom_presets", null);
            if (presetsJson != null) {
                JSONArray presetsArray = new JSONArray(presetsJson);
                customPresets.clear();

                for (int i = 0; i < presetsArray.length(); i++) {
                    JSONObject presetObj = presetsArray.getJSONObject(i);
                    // 确保包含presetName
                    if (!presetObj.has("presetName")) {
                        presetObj.put("presetName", "自定义配置 " + (i + 1));
                    }
                    customPresets.add(presetObj);
                    addPresetRadioButton(presetObj.getString("presetName"), presetObj);
                }
            }
        } catch (Exception e) {
            Log.e("MidActivity", "加载自定义配置失败", e);
            customPresets.clear();
            saveCustomPresets(); // 重置为干净状态
        }
    }
    private void addPresetRadioButton(String presetName, JSONObject presetConfig) {
        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(presetName);

        // 设置布局参数
        RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        radioButton.setLayoutParams(params);

        // 添加标签以便后续获取配置
        radioButton.setTag(presetConfig);

        // 为自定义配置添加长按删除功能
        radioButton.setOnLongClickListener(v -> {
            String currentPresetName = ((RadioButton)v).getText().toString();
            if (!currentPresetName.equals("默认预设")) {
                new AlertDialog.Builder(MidActivity.this)
                        .setTitle("删除配置")
                        .setMessage("确定要删除此配置吗？")
                        .setPositiveButton("删除", (dialog, which) -> {
                            // 从customPresets中移除对应的配置
                            Iterator<JSONObject> iterator = customPresets.iterator();
                            while (iterator.hasNext()) {
                                JSONObject preset = iterator.next();
                                // 比较名称或其他唯一标识
                                if (preset.optString("presetName", "").equals(currentPresetName)) {
                                    iterator.remove();
                                    break;
                                }
                            }

                            // 从RadioGroup中移除视图
                            presetRadioGroup.removeView(v);
                            saveCustomPresets();
                            Toast.makeText(MidActivity.this, "配置已删除", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            }
            return false;
        });

        // 添加到RadioGroup
        presetRadioGroup.addView(radioButton);

        // 默认选择第一个
        if (presetRadioGroup.getChildCount() == 1) {
            radioButton.setChecked(true);
        }
    }

    //#4添加用于判断是否处于考试状态的方法
    private Boolean checkIfInExam() {
        if (lastExamPaperId == null) {
            return false;
        }

        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + lastExamPaperId + ".json");
            if (!paperFile.exists()) {
                return false;
            }

            String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(paperJsonStr);

            boolean isExamFinished = paperJson.getBoolean("shifoukaoshijieshu");
            if (isExamFinished) {
                return false;
            }

            String endTimeStr = paperJson.getString("zuichijiezhishijian");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault());
            Date endTime = sdf.parse(endTimeStr);
            Date currentTime = new Date();

            return currentTime.before(endTime);
        } catch (Exception e) {
            Log.e("MidActivity", "检查考试状态失败", e);
            return false;
        }
    }


    private void startExamWithPreset(String presetName) {
        //#5生成试卷前需要调用方法判断当前是否已经处于考试中，如果已经处于考试中，则询问是回到上一场考试还是重新生成试卷，
        if (isInExam && lastExamPaperId != null) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("考试进行中")
                    .setMessage("您有一场未完成的考试，是否继续？")
                    .setPositiveButton("继续考试", (dialog, which) -> {
                        Intent intent = new Intent(this, QuizHomeActivity.class);
                        intent.putExtra("question_bank_id", questionBankId);
                        intent.putExtra("paper_id", lastExamPaperId);
                        intent.putExtra("exam_mode", true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("重新生成", (dialog, which) -> {
                        createNewExam(presetName);
                    })
                    .setNeutralButton("取消", null)
                    .show();
            return;
        }

        createNewExam(presetName);
    }

    private void createNewExam(String presetName) {
        try {
            // 获取选中的预设配置
            JSONObject selectedPreset = null;
            for (int i = 0; i < presetRadioGroup.getChildCount(); i++) {
                View child = presetRadioGroup.getChildAt(i);
                if (child instanceof RadioButton) {
                    RadioButton radioButton = (RadioButton) child;
                    if (radioButton.getText().toString().equals(presetName)) {
                        selectedPreset = (JSONObject) radioButton.getTag();
                        break;
                    }
                }
            }

            if (selectedPreset == null) {
                Toast.makeText(this, "获取预设配置失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 生成试卷ID
            String paperId = generatePaperId(questionBankId);

            // 创建试卷JSON文件
            if (!createExamPaper(questionBankId, paperId, selectedPreset)) {
                Toast.makeText(this, "创建试卷失败", Toast.LENGTH_SHORT).show();
                return;
            }

            // 将试卷ID添加到题库信息文件
            // #6在这里写一个log，log包含试卷id
            Log.d("MidActivity", "Generated new exam paper with ID: " + paperId);
            addPaperToBankInfo(questionBankId, paperId);

            // 保存当前试卷ID到SharedPreferences
            prefs.edit().putString("last_exam_paper_id", paperId).apply();

            // 启动考试Activity
            Intent intent = new Intent(this, QuizHomeActivity.class);
            intent.putExtra("question_bank_id", questionBankId);
            intent.putExtra("paper_id", paperId);
            intent.putExtra("exam_mode", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            Log.e("MidActivity", "开始考试失败", e);
            Toast.makeText(this, "开始考试失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String generatePaperId(String bankId) {
        return bankId + "paper" + System.currentTimeMillis();
    }

    private boolean createExamPaper(String bankId, String paperId, JSONObject presetConfig) {
        try {
            // 1. 读取题库文件
            File bankFile = new File(getFilesDir(), "question_banks/" + bankId + ".json");
            if (!bankFile.exists()) return false;

            String bankJsonStr = new String(Files.readAllBytes(bankFile.toPath()));
            JSONObject bankJson = new JSONObject(bankJsonStr);

            //创建试卷JSON对象
            JSONObject paperJson = new JSONObject();
            paperJson.put("id", paperId);

            // 复制预设配置
            JSONObject examConfig = new JSONObject();
            examConfig.put("danxuan", presetConfig.getJSONArray("danxuan"));
            examConfig.put("duoxuan", presetConfig.getJSONArray("duoxuan"));
            examConfig.put("panduan", presetConfig.getJSONArray("panduan"));
            paperJson.put("yushelianxi", examConfig);

            // 设置考试时间相关字段
            paperJson.put("kaoshishijian", presetConfig.getInt("kaoshishijian"));
            paperJson.put("kaikaoshijian", getCurrentTimeString());
            paperJson.put("zuichijiezhishijian", getEndTimeString(presetConfig.getInt("kaoshishijian")));
            paperJson.put("shifoutiqianjiaojuan", false);
            paperJson.put("shifoukaoshijieshu", false);
            paperJson.put("defen", -1);

            // 随机选择题目
            JSONObject timu = new JSONObject();

            // 处理单选题
            JSONArray danxuanQuestions = selectRandomQuestions(
                    bankJson.getJSONObject("tiku").getJSONArray("danxuan"),
                    presetConfig.getJSONArray("danxuan").getInt(0)
            );
            timu.put("danxuan", danxuanQuestions);

            // 处理多选题
            JSONArray duoxuanQuestions = selectRandomQuestions(
                    bankJson.getJSONObject("tiku").getJSONArray("duoxuan"),
                    presetConfig.getJSONArray("duoxuan").getInt(0)
            );
            timu.put("duoxuan", duoxuanQuestions);

            // 处理判断题
            JSONArray panduanQuestions = selectRandomQuestions(
                    bankJson.getJSONObject("tiku").getJSONArray("panduan"),
                    presetConfig.getJSONArray("panduan").getInt(0)
            );
            timu.put("panduan", panduanQuestions);

            paperJson.put("timu", timu);

            // 6. 保存试卷文件
            File paperFile = new File(getFilesDir(), "question_banks/" + paperId + ".json");
            Files.write(paperFile.toPath(), paperJson.toString().getBytes());

            return true;
        } catch (Exception e) {
            Log.e("MidActivity", "创建试卷失败", e);
            return false;
        }
    }

    private JSONArray selectRandomQuestions(JSONArray allQuestions, int count) throws Exception {
        JSONArray selected = new JSONArray();
        if (count <= 0) return selected;

        // 随机选择题目
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < allQuestions.length(); i++) {
            indices.add(i);
        }
        java.util.Collections.shuffle(indices);

        for (int i = 0; i < Math.min(count, indices.size()); i++) {
            JSONObject question = allQuestions.getJSONObject(indices.get(i));
            JSONObject examQuestion = new JSONObject();
            examQuestion.put("type", question.getString("type"));
            examQuestion.put("id", question.getInt("id"));
            examQuestion.put("yourans", ""); // 添加yourans字段，初始为null
            examQuestion.put("iscorr", false); // 初始化为未作答
            examQuestion.put("duicuo", false); // 初始化为未作答

            selected.put(examQuestion);
        }

        return selected;
    }

    private void addPaperToBankInfo(String bankId, String paperId) {
        try {
            File infoFile = new File(getFilesDir(), "question_banks/" + bankId + "_info.json");
            JSONObject infoJson;

            if (infoFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
                infoJson = new JSONObject(jsonStr);
            } else {
                infoJson = new JSONObject();
                infoJson.put("bankId", bankId);
                infoJson.put("favorites", new JSONObject());
                infoJson.put("wrongQuestions", new JSONObject());
                infoJson.put("generatedPapers", new JSONArray());
            }

            // 添加试卷ID到generatedPapers数组
            JSONArray generatedPapers = infoJson.getJSONArray("generatedPapers");
            generatedPapers.put(paperId);
            infoJson.put("generatedPapers", generatedPapers);

            // 写回文件
            Files.write(infoFile.toPath(), infoJson.toString().getBytes());
        } catch (Exception e) {
            Log.e("MidActivity", "添加试卷到题库信息失败", e);
        }
    }

    private String getCurrentTimeString() {
        // #7实现获取当前时间字符串的逻辑，格式为"yyyy.MM.dd.HH.mm.ss"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private String getEndTimeString(int minutes) {
        // #8实现获取结束时间字符串的逻辑
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.getDefault());
        Date currentTime = new Date();
        long endTimeMillis = currentTime.getTime() + minutes * 60 * 1000;
        return sdf.format(new Date(endTimeMillis));
    }

    //#9写一个静态方法，参数是试卷id
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

    public static void tiqianfinishExamPaper(File filesDir, String paperId) {
        try {
            File paperFile = new File(filesDir, "question_banks/" + paperId + ".json");
            if (!paperFile.exists()) {
                return;
            }

            String paperJsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(paperJsonStr);
            paperJson.put("shifoutiqianjiaojuan", true);

            Files.write(paperFile.toPath(), paperJson.toString().getBytes());
        } catch (Exception e) {
            Log.e("MidActivity", "提前结束考试失败", e);
        }
    }

    /**
     * 计算试卷总分
     * @param filesDir 应用的文件目录
     * @param paperId 试卷ID
     * @return 试卷总分，如果计算失败返回-1
     */
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