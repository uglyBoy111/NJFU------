package com.yuanseen.shuati.ui.gallery.addqes.bankinfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankInfo {
    private String bankId;
    private Map<String, List<Integer>> favorites = new HashMap<>();
    private Map<String, List<Integer>> wrongQuestions = new HashMap<>();
    private List<String> generatedPapers = new ArrayList<>();  // Changed from Map to List

    public BankInfo(String bankId) {
        this.bankId = bankId;
        // Initialize with empty lists for each question type
        favorites.put("danxuan", new ArrayList<>());
        favorites.put("duoxuan", new ArrayList<>());
        favorites.put("panduan", new ArrayList<>());

        wrongQuestions.put("danxuan", new ArrayList<>());
        wrongQuestions.put("duoxuan", new ArrayList<>());
        wrongQuestions.put("panduan", new ArrayList<>());
    }

    // Getters
    public String getBankId() {
        return bankId;
    }

    public Map<String, List<Integer>> getFavorites() {
        return favorites;
    }

    public Map<String, List<Integer>> getWrongQuestions() {
        return wrongQuestions;
    }

    public List<String> getGeneratedPapers() {
        return generatedPapers;
    }

    // JSON serialization
    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put("bankId", bankId);

            // Favorites
            JSONObject favoritesJson = new JSONObject();
            for (Map.Entry<String, List<Integer>> entry : favorites.entrySet()) {
                favoritesJson.put(entry.getKey(), new JSONArray(entry.getValue()));
            }
            json.put("favorites", favoritesJson);

            // Wrong questions
            JSONObject wrongQuestionsJson = new JSONObject();
            for (Map.Entry<String, List<Integer>> entry : wrongQuestions.entrySet()) {
                wrongQuestionsJson.put(entry.getKey(), new JSONArray(entry.getValue()));
            }
            json.put("wrongQuestions", wrongQuestionsJson);

            // Generated papers (now as a simple array)
            json.put("generatedPapers", new JSONArray(generatedPapers));

            return json.toString(2);
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // JSON deserialization
    public static BankInfo fromJsonString(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            BankInfo info = new BankInfo(json.getString("bankId"));

            // Process favorites
            if (json.has("favorites")) {
                JSONObject favoritesJson = json.getJSONObject("favorites");
                for (String key : new String[]{"danxuan", "duoxuan", "panduan"}) {
                    if (favoritesJson.has(key)) {
                        JSONArray array = favoritesJson.getJSONArray(key);
                        for (int i = 0; i < array.length(); i++) {
                            info.favorites.get(key).add(array.getInt(i));
                        }
                    }
                }
            }

            // Process wrong questions
            if (json.has("wrongQuestions")) {
                JSONObject wrongQuestionsJson = json.getJSONObject("wrongQuestions");
                for (String key : new String[]{"danxuan", "duoxuan", "panduan"}) {
                    if (wrongQuestionsJson.has(key)) {
                        JSONArray array = wrongQuestionsJson.getJSONArray(key);
                        for (int i = 0; i < array.length(); i++) {
                            info.wrongQuestions.get(key).add(array.getInt(i));
                        }
                    }
                }
            }

            // Process generated papers (now as a simple array)
            if (json.has("generatedPapers")) {
                JSONArray generatedPapersArray = json.getJSONArray("generatedPapers");
                for (int i = 0; i < generatedPapersArray.length(); i++) {
                    info.generatedPapers.add(generatedPapersArray.getString(i));
                }
            }

            return info;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Add favorite question (with type)
    public void addFavorite(String type, int questionId) {
        if (!favorites.get(type).contains(questionId)) {
            favorites.get(type).add(questionId);
        }
    }

    // Remove favorite question (with type)
    public void removeFavorite(String type, int questionId) {
        favorites.get(type).remove(Integer.valueOf(questionId));
    }

    // Add wrong question (with type)
    public void addWrongQuestion(String type, int questionId) {
        if (!wrongQuestions.get(type).contains(questionId)) {
            wrongQuestions.get(type).add(questionId);
        }
    }

    // Remove wrong question (with type)
    public void removeWrongQuestion(String type, int questionId) {
        wrongQuestions.get(type).remove(Integer.valueOf(questionId));
    }

    // Add generated paper (sequential ID)
    public void addGeneratedPaper() {
        int nextId = generatedPapers.size() + 1;
        String paperId = String.format("%spaper%03d", bankId, nextId);
        generatedPapers.add(paperId);
    }

    // Remove generated paper
    public void removeGeneratedPaper(String paperId) {
        generatedPapers.remove(paperId);
    }
}