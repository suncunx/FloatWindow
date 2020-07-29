package com.scx.floatwindow;


import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;

/**
 * Created by sun.cunxing on 2018/9/21.
 * ui部分
 */
public class VideoChat extends BaseObservable {
    private boolean buttonVisible;

    public VideoChat(boolean buttonVisible) {
        this.buttonVisible = buttonVisible;
    }

    @Bindable
    public boolean isButtonVisible() {
        return buttonVisible;
    }

    public void setButtonVisible(boolean buttonVisible) {
        this.buttonVisible = buttonVisible;
        notifyPropertyChanged(BR.buttonVisible);
    }
}
