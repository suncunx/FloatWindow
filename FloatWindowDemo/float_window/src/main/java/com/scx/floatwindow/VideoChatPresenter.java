package com.scx.floatwindow;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleOwner;


/**
 * Created by sun.cunxing on 2018/9/18.
 */
public class VideoChatPresenter implements VideoChatContract.Presenter {
    private static final String TAG = "VideoChatPresenter";
    private static final String CHANNEL_ID = "123";
    private static final String NOTIFICATION_VIDEO_CHAT = "视频通话中，轻击以继续...";
    private static final String ACTION_BROADCAST = "com.xupin.kit.intent.broadcast.BROADCAST_VIDEO_CHAT";
    private static final int NOTIFICATION_ID = 1;
    private static final int TOTAL_TIME = 10000;
    private static final int MAX_SECOND = 59;
    private static final int MAX_MINUTE = 99;
    private static final int INTERVAL = 1000;

    private int second;
    private int minute;

    private HomeWatcherReceiver homeWatcherReceiver;
    private final VideoChatContract.View videoChatView;
    private TimerUtil timerUtil;
    private NotificationManager notificationManager;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (second == MAX_SECOND) {
                if (minute < MAX_MINUTE) {
                    minute++;
                    second = 0;
                } else {
                    return;
                }
            } else {
                second++;
            }
            String sec = second < 10 ? "0" + second : "" + second;
            String min = minute < 10 ? "0" + minute : "" + minute;

            videoChatView.updateTime(min, sec);
            handler.postDelayed(this, INTERVAL);
        }
    };

    public VideoChatPresenter(final VideoChatContract.View videoChatView) {
        this.videoChatView = videoChatView;
//        videoChatView.setPresenter(this);
    }

    private void initCall() {
        startVideoChat();
    }

    // 开始视频对讲
    private void startVideoChat() {
        // 开始计时
        startTime();
        startTimeDown();
    }

    private void startTime() {
//        textTime.setVisibility(View.VISIBLE);
        handler.postDelayed(runnable, INTERVAL);
    }

    private void cancelTimeDown() {
        if (timerUtil != null) {
            timerUtil.cancel();
        }
    }

    private void startTimeDown() {
        if (timerUtil != null) {
            timerUtil.cancel();
            timerUtil.start();
        }

    }

    private void initTimer() {
        timerUtil = new TimerUtil(TOTAL_TIME, INTERVAL);
        timerUtil.setOnTimerFinishListener(new TimerUtil.TimerFinishListener() {

            @Override
            public void onTimerFinish() {
                videoChatView.showButtons(false);
            }
        });
    }

    private void bindData() {

        videoChatView.setData(new VideoChatListenerImpl());
    }

    private void registerReceiver() {
        homeWatcherReceiver = new HomeWatcherReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(ACTION_BROADCAST);
        videoChatView.registerHomeWatcherReceiver(homeWatcherReceiver, filter);
    }

    // 发送通知
    private void sendNotification() {
        notificationManager = (NotificationManager) videoChatView.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, NOTIFICATION_VIDEO_CHAT, NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
//            channel.enableVibration(true);
            channel.setShowBadge(false);
//            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

            notificationManager.createNotificationChannel(channel);
//            notificationManager.notify();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(videoChatView.getContext(), CHANNEL_ID);
        builder.setOngoing(true);
        builder.setTicker(NOTIFICATION_VIDEO_CHAT);
        builder.setContentText(NOTIFICATION_VIDEO_CHAT);
        builder.setContentTitle("通话的id：" + videoChatView.getCallId());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLights(0xff0000ff, 300, 300); // 灯光颜色，亮的时间，暗的时间
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC); // 在锁屏模式下也显示
        // 加入以下两行代码以显示Ticker
        builder.setDefaults(~0);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH); //

        Intent resultIntent = new Intent(ACTION_BROADCAST);
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
//        stackBuilder.addParentStack(MainActivity.class);
//        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = PendingIntent.getBroadcast(videoChatView.getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void removeNotification() {
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
        }
    }

    @Override
    public void wholeToFullScreen() {
        videoChatView.wakeActivity();
        videoChatView.viewToFullScreen();
        removeNotification();
    }

    //相机预览放在大屏幕里
    @Override
    public void previewToBig() {
        videoChatView.getSmallSurfaceView().setBackgroundColor(videoChatView.getContext().getResources().getColor(R.color.black));
        videoChatView.getBigSurfaceView().setBackgroundColor(videoChatView.getContext().getResources().getColor(R.color.darkgray));
    }

    //相机预览放在小屏幕里
    @Override
    public void previewToSmall() {
        videoChatView.getSmallSurfaceView().setBackgroundColor(videoChatView.getContext().getResources().getColor(R.color.darkgray));
        videoChatView.getBigSurfaceView().setBackgroundColor(videoChatView.getContext().getResources().getColor(R.color.black));
    }

    @Override
    public void goneWidget() {
        videoChatView.showButtons(false);
    }

    @Override
    public void wholeToSmallWindow() {
        if (videoChatView.isFullScreen()) {
            videoChatView.viewToSmall();
            sendNotification();
            goneWidget();
        }
    }

    @Override
    public void clickScreen() {
        // 当时间不可见的时候，显示时间及按钮
        if (!videoChatView.isTimeVisible()) {
            videoChatView.showButtons(true);
            // 开始倒计时
            startTimeDown();
        } else {
            cancelTimeDown();
            videoChatView.showButtons(false);
        }
    }

    @Override
    public void onCreate(LifecycleOwner owner) {
        Log.d(TAG, "onCreate: ");
        // move to presenter
        initTimer();
        initCall();
        registerReceiver();
        // model business logic
        bindData();

    }

    @Override
    public void onDestroy(LifecycleOwner owner) {
        Log.d(TAG, "onDestroy: ");
        if (homeWatcherReceiver != null) {
            videoChatView.unRegisterHomeWatcherReceiver(homeWatcherReceiver);
        }
        handler.removeCallbacks(runnable);
        removeNotification();
    }


    private class HomeWatcherReceiver extends BroadcastReceiver {

        private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {

            String intentAction = intent.getAction();
            Log.i(TAG, "intentAction =" + intentAction);
            if (TextUtils.equals(intentAction, Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                Log.i(TAG, "reason =" + reason);
                if (TextUtils.equals(SYSTEM_DIALOG_REASON_HOME_KEY, reason)) {
                    Log.d(TAG, "onReceive: todo ");
                    wholeToSmallWindow();
                }
            } else if (TextUtils.equals(intentAction, ACTION_BROADCAST)) {
                removeNotification();
                wholeToFullScreen();
                Log.d(TAG, "onReceive: 我又回来了");
            }
        }

    }

    private class VideoChatListenerImpl implements VideoChatListener {

        @Override
        public void onClickCollapse() {
            cancelTimeDown();
            wholeToSmallWindow();
        }
    }
}
