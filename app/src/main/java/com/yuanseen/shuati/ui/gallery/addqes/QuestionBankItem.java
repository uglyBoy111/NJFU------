package com.yuanseen.shuati.ui.gallery.addqes;

import com.yuanseen.shuati.ui.gallery.GalleryItem;

public class QuestionBankItem extends GalleryItem {
    private int imageRes;
    private String title;
    private String subtitle;
    private String desc;
    private boolean isSelected;

    public QuestionBankItem(int imageRes, String title, String subtitle, String desc, boolean isSelected) {
        super(null, imageRes, title, subtitle, desc);  // 修改为调用父类带id的构造方法
        this.imageRes = imageRes;
        this.title = title;
        this.subtitle = subtitle;
        this.desc = desc;
        this.isSelected = isSelected;
    }

    // 新增带id的构造方法
    public QuestionBankItem(String id, int imageRes, String title, String subtitle, String desc, boolean isSelected) {
        super(id, imageRes, title, subtitle, desc);
        this.imageRes = imageRes;
        this.title = title;
        this.subtitle = subtitle;
        this.desc = desc;
        this.isSelected = isSelected;
    }

    // Getter和Setter方法
    public int getImageRes() { return imageRes; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDesc() { return desc; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}