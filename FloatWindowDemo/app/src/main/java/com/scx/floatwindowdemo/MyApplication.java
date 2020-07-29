package com.scx.floatwindowdemo;

import android.app.Application;
import android.content.Intent;

import com.scx.floatwindow.WakeAppManager;

public class MyApplication extends Application implements WakeAppManager.WakeAppListener {

    @Override
    public void onCreate() {
        super.onCreate();
        WakeAppManager.getInstance().registerWakeAppListener(this);
    }

    @Override
    public void onWakeApp() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setAction(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(intent);
    }
}
