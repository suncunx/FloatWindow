package com.scx.floatwindow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.ImageView;

import com.scx.floatwindow.base.BasePresenter;
import com.scx.floatwindow.base.BaseView;


/**
 * Created by sun.cunxing on 2018/9/26.
 * 记住，View永远是被动的
 */
public interface VideoChatContract {

    interface View extends BaseView<Presenter> {

        void setData(VideoChatListener listener);
        /**
         * view变为全屏
         */
        void viewToFullScreen();

        /**
         * view缩小到小窗口大小
         */
        void viewToSmall();

        int getCallId();

        boolean isTimeVisible();

        boolean isFullScreen();

        void registerHomeWatcherReceiver(BroadcastReceiver receiver, IntentFilter filter);

        void unRegisterHomeWatcherReceiver(BroadcastReceiver receiver);

        void updateTime(String min, String sec);

        Context getContext();

        void stopVideoChat();

        ImageView getBigSurfaceView();

        ImageView getSmallSurfaceView();

        void showButtons(boolean visible);

        void wakeActivity();
    }

    interface Presenter extends BasePresenter {
        /**
         * 整个对讲页面由小窗口变为全屏
         */
        void wholeToFullScreen();

        /**
         * 整个对讲页面由全屏变为小窗口
         */
        void wholeToSmallWindow();


        void clickScreen();

//    void onDestroy();

        /**
         * 本地预览变大
         */
        void previewToBig();

        void previewToSmall();

        void goneWidget();
    }
}
