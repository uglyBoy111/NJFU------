package com.yuanseen.shuati.ui.gallery;

public class GalleryItem {
    private String id;  // 新增id字段
    private int imageResId;
    private String title;
    private String subtitle;
    private String description;

    // 必须有无参构造方法（Gson 需要）
    public GalleryItem() {}

    public GalleryItem(int imageResId, String title, String subtitle, String description) {
        this.imageResId = imageResId;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
    }

    // 新增带id的构造方法
    public GalleryItem(String id, int imageResId, String title, String subtitle, String description) {
        this.id = id;
        this.imageResId = imageResId;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
    }

    // Getter 和 Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getImageResId() { return imageResId; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}