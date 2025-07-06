package com.yuanseen.shuati.ui.slideshow;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SlideshowViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public SlideshowViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("未来这里可能是全局设置、题库制作教学、后端部署教学：后端将去中心化部署即开放");
    }

    public LiveData<String> getText() {
        return mText;
    }
}