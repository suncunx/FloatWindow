package com.scx.floatwindow;

public class WakeAppManager {
    private static WakeAppManager instance = null;
    private WakeAppListener listener;
    private WakeAppManager(){}

    public static WakeAppManager getInstance() {
        if (instance == null) {
            synchronized (WakeAppManager.class) {
                if (instance == null) {
                    instance = new WakeAppManager();
                }
            }
        }
        return instance;
    }

    public void registerWakeAppListener(WakeAppListener listener) {
        this.listener = listener;
    }

    public void unregisterWakeAppListener() {
        this.listener = null;
    }

    public WakeAppListener getWakeAppListener() {
        return listener;
    }

    public interface WakeAppListener {
        void onWakeApp();
    }
}
