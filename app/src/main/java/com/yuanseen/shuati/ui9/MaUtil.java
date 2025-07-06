package com.yuanseen.shuati.ui9;

import android.util.Log;

import com.yuanseen.shuati.ui5.Question;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaUtil {

    private String currentBankId;
    private String currentPaperId;

    private File url;

    private File getFilesDir(){return url;}

    public MaUtil(String currentBankId, String currentPaperId, File url) {
        this.currentBankId = currentBankId;
        this.currentPaperId = currentPaperId;
        this.url = url;
    }

    // 添加新方法获取已保存的答案
    public String getSavedAnswerFromPaper(String questionType, int questionNumber) {
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (!paperFile.exists()) return null;

            String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
            JSONObject paperJson = new JSONObject(jsonStr);
            JSONObject timu = paperJson.getJSONObject("timu");
            String typeKey = getTypeKey(questionType);
            JSONArray questions = timu.getJSONArray(typeKey);

            for (int i = 0; i < questions.length(); i++) {
                JSONObject question = questions.getJSONObject(i);
                if (question.getInt("id") == questionNumber) {
                    // 对于多选题，确保答案格式正确（如"AB"而不是"A,B"）
                    String answer = question.optString("yourans", null);
                    if (answer != null && questionType.equals("MULTI_CHOICE")) {
                        return answer.replace(",", "").replace(" ", "");
                    }
                    return answer;
                }
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "获取已保存答案失败", e);
        }
        return null;
    }

    public boolean checkAnswerCorrectness(String questionType, int questionNumber, String userAnswer) {
        try {
            // 从题库中获取正确答案
            Question question = loadQuestionFromFile(currentBankId, questionType, questionNumber);
            if (question == null) return false;

            String correctAnswer = question.getAnswer();

            if (questionType.equals("MULTI_CHOICE")) {
                // 多选题需要特殊处理
                String sortedUserAnswer = sortAnswer(userAnswer);

                String sortedCorrectAnswer = sortAnswer(correctAnswer);

                return sortedUserAnswer.equals(sortedCorrectAnswer);
            }
            return userAnswer.equals(correctAnswer);
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "检查答案正确性失败", e);
            return false;
        }
    }

    public Map<String, List<Integer>> loadFavoritesFromInfoFile() {
        Map<String, List<Integer>> favoritesMap = new HashMap<>();
        try {
            File paperFile = new File(getFilesDir(), "question_banks/" + currentPaperId + ".json");
            if (paperFile.exists()) {
                String jsonStr = new String(Files.readAllBytes(paperFile.toPath()));
                JSONObject paperJson = new JSONObject(jsonStr);
                JSONObject timu = paperJson.getJSONObject("timu");

                // 从试卷中获取单选题列表
                JSONArray danxuanQuestions = timu.getJSONArray("danxuan");
                List<Integer> danxuanList = new ArrayList<>();
                for (int i = 0; i < danxuanQuestions.length(); i++) {
                    danxuanList.add(danxuanQuestions.getJSONObject(i).getInt("id"));
                }
                favoritesMap.put("danxuan", danxuanList);

                // 从试卷中获取多选题列表
                JSONArray duoxuanQuestions = timu.getJSONArray("duoxuan");
                List<Integer> duoxuanList = new ArrayList<>();
                for (int i = 0; i < duoxuanQuestions.length(); i++) {
                    duoxuanList.add(duoxuanQuestions.getJSONObject(i).getInt("id"));
                }
                favoritesMap.put("duoxuan", duoxuanList);

                // 从试卷中获取判断题列表
                JSONArray panduanQuestions = timu.getJSONArray("panduan");
                List<Integer> panduanList = new ArrayList<>();
                for (int i = 0; i < panduanQuestions.length(); i++) {
                    panduanList.add(panduanQuestions.getJSONObject(i).getInt("id"));
                }
                favoritesMap.put("panduan", panduanList);
            }
        } catch (Exception e) {
            Log.e("QuizHomeActivity", "加载试卷题目失败", e);
        }
        return favoritesMap;
    }

    public Question loadQuestionFromFile(String bankId, String type, int number) {
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

    private String sortAnswer(String answer) {
        if (answer == null) return "";
        char[] chars = answer.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }
}
