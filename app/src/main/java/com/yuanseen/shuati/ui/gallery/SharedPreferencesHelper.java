package com.yuanseen.shuati.ui.gallery;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPreferencesHelper {
    private static final String PREFS_NAME = "GalleryPrefs";
    private static final String KEY_ITEMS = "gallery_items";

    private static final String LOCAL_STORAGE_DIR = "question_banks";
    private static final String RECENT_BANKS_KEY = "recent_question_banks";

    private SharedPreferences prefs;
    private Gson gson = new Gson();

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // 保存列表
    public void saveItems(List<GalleryItem> items) {
        String json = gson.toJson(items);
        prefs.edit().putString(KEY_ITEMS, json).apply();
    }

    // 加载列表
    public List<GalleryItem> loadItems() {
        String json = prefs.getString(KEY_ITEMS, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<List<GalleryItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // 保存最近访问的题库列表
    public void saveRecentItems(List<GalleryItem> items) {
        String json = new Gson().toJson(items);
        prefs.edit().putString(RECENT_BANKS_KEY, json).apply();
    }

    // 加载最近访问的题库列表
    public List<GalleryItem> loadRecentItems() {
        String json = prefs.getString(RECENT_BANKS_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<GalleryItem>>() {}.getType();
        return new Gson().fromJson(json, type);
    }
}