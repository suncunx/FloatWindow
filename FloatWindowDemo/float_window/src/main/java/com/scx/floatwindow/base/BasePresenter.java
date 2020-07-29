package com.scx.floatwindow.base;


import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * Created by sun.cunxing on 2018/9/25.
 * 包含生命周期的presenter
 * 推荐实现onCreate() 和 onDestroy()
 */
public interface BasePresenter extends DefaultLifecycleObserver {
    @Override
    void onCreate(@NonNull LifecycleOwner owner);

    @Override
    void onDestroy(@NonNull LifecycleOwner owner);
}
