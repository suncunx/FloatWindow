package com.scx.floatwindow;

import android.animation.ValueAnimator;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.scx.floatwindow.databinding.ServiceVideoChatBinding;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;


public class VideoChatService extends Service implements View.OnTouchListener, VideoChatContract.View, PopupWindow.OnDismissListener, View.OnLayoutChangeListener {
    private static final String TAG = "VideoChatService";
    private static final String EXTRA_CALL_ID = "callId";
    private static final String TOAST_WINDOW_MIN = "视频已最小化";

    //状态栏高度.
    int statusBarHeight = -1;
    private int downX;
    private int downY;
    private int initX;
    private int initY;
    private int screenWidth;
    // 去掉状态栏高度的屏幕高度
    private int screenHeight;
    private int marginTop;
    private int marginRight;

    private final static int DP_MARGIN_TOP = 10;
    private final static int DP_MARGIN_RIGHT = 10;

    private int minX = 0;
    private int maxX;
    private int minY = 0;
    private int maxY;
    private int maxXVelocity;
    private int callId = -1;
    private long lastTime;
    private boolean isPreviewSmall = true;

    private FrameLayout.LayoutParams params; // surfaceViewLocal的params
    private WindowManager.LayoutParams paramsWindow;
    private WindowManager windowManager;

    private MyImageView surfaceViewSmall;
    private TextView textTime;

    // 永远是大屏的surfaceView
    private ImageView surfaceViewBig;
    private FrameLayout contentView;

    private VideoChatContract.Presenter presenter;

    private ServiceVideoChatBinding dataBinding;
    private VideoChat videoChat;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        if (intent != null) {
            callId = intent.getIntExtra(EXTRA_CALL_ID, -1);
            Log.d(TAG, "call Id " + callId);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: ");
        return super.onUnbind(intent);
    }

    // 只会走一次
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        setPresenter(new VideoChatPresenter(this));
        initView();
        videoChat = new VideoChat(true);
        presenter.onCreate(null);
    }

    public void initView() {

        initWindow();

        initScreenDisplay();

        initContentView();

        windowManager.addView(contentView, paramsWindow);
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
    }


    private void initScreenDisplay() {

        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        screenWidth = ScreenUtil.getSceenWidth(this);
        screenHeight = ScreenUtil.getSceenHeight(this);

        marginTop = statusBarHeight + ScreenUtil.dp2px(getApplicationContext(), DP_MARGIN_TOP);
        marginRight = ScreenUtil.dp2px(getApplicationContext(), DP_MARGIN_RIGHT);

        //设置悬浮窗口长宽数据.
        paramsWindow.width = screenWidth;
        paramsWindow.height = screenHeight;

        Log.i(TAG, "initScreenDisplay: statusBarHeight = " + statusBarHeight + ", screenWidth = " + screenWidth + ", screenHeight = " + screenHeight);
    }

    private void initWindow() {
        Log.d(TAG, "initWindow: ");
        //赋值WindowManager&LayoutParam.  如果这里的context是activity，那么window只能在该activity运行在前台的时候，才能响应touchEvent
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        paramsWindow = new WindowManager.LayoutParams();
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.TYPE_SYSTEM_ERROR可以覆盖状态栏
        paramsWindow.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        //设置效果为背景透明.
//        paramsWindow.format = PixelFormat.TRANSLUCENT; 半透明
//        paramsWindow.format = PixelFormat.RGBA_8888; 透明
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        paramsWindow.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        //params.x  y都以此重心作为坐标原点.
        paramsWindow.gravity = Gravity.LEFT | Gravity.TOP;

        //设置窗口初始停靠位置.
        paramsWindow.x = 0;
        paramsWindow.y = 0;
    }

    private void initContentView() {
        Log.d(TAG, "initContentView: ");
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        dataBinding = DataBindingUtil.inflate(inflater, R.layout.service_video_chat, null, false);
        //获取浮动窗口视图所在布局.
        contentView = (FrameLayout) dataBinding.getRoot();
        textTime = dataBinding.tvTime;

        surfaceViewSmall = dataBinding.ivSmall;

        params = (FrameLayout.LayoutParams) surfaceViewSmall.getLayoutParams();
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.width = screenWidth / 4;
        params.height = screenHeight / 4;
        maxX = screenWidth - params.width;
        maxY = screenHeight - params.height + statusBarHeight;
        surfaceViewSmall.setLayoutParams(params);
        surfaceViewSmall.addOnLayoutChangeListener(this);
        surfaceViewSmall.requestMyLayout();

        surfaceViewBig = dataBinding.ivBig;

        //主动计算出当前View的宽高信息.
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        surfaceViewBig.setOnTouchListener(this);
        surfaceViewSmall.setOnTouchListener(this);
        contentView.setOnTouchListener((v, event) -> {
            if (paramsWindow.width == screenWidth) { // 如果全屏，则不监听
                return false;
            }
            VelocityTracker velocityTracker = VelocityTracker.obtain();
            velocityTracker.addMovement(event);
            velocityTracker.computeCurrentVelocity(100);
            int xVelocity = (int) velocityTracker.getXVelocity();
            int yVelocity = (int) velocityTracker.getYVelocity(); // 如果速度够快并且时间够短，则移到一边。如果时间较长，根据在屏幕的位置移动
            maxXVelocity = Math.abs(xVelocity) > Math.abs(maxXVelocity) ? xVelocity : maxXVelocity;
            Log.d(TAG, "window onTouch: xVelocity = " + xVelocity + ", yVelocity = " + yVelocity);
            // 如果是小窗口，则监听
            int deltaX = (int) event.getRawX() - downX; // deltaX为正，在原始位置的右边
            int deltaY = (int) event.getRawY() - downY; // deltaY为正，在原始位置的下边
            Log.d(TAG, "contentView onTouch: surfaceView deltaX = " + deltaX + ", deltaY = " + deltaY);
            Log.d(TAG, "onTouch: contentView.getTranslationX() = " + contentView.getTranslationX());
//                Log.d(TAG, "onTouch: event.getAction() = " + event.getAction());
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "contentView onTouch: ACTION_DOWN");
                    downWindow(event);
                    break;
                case MotionEvent.ACTION_MOVE:
//                        Log.d(TAG, "contentView onTouch: ACTION_MOVE");
                    moveWindow(deltaX, deltaY);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "contentView onTouch: ACTION_UP");
                    upWindow(deltaX, deltaY);
                    break;
            }
            return true;
        });
    }

    //平移的时候并不会改变surfaceViewSmall的left top等值，只会改变x, y, translationX, translationY的值
    private void upWindow(int deltaX, int deltaY) {
        long deltaTime = System.currentTimeMillis() - lastTime;
        Log.d(TAG, "upWindow onTouch: deltaTime = " + deltaTime);
        if (deltaX == 0 && deltaY == 0 && deltaTime < 500) {
            presenter.wholeToFullScreen();
            return;
        }
        final int startX = paramsWindow.x;
        final int deltaX1 = Math.abs(maxXVelocity) > screenWidth / 3 && deltaTime < 500 ? startX + maxXVelocity < 0 ? -startX : maxX - startX : startX + (paramsWindow.width / 2) < screenWidth / 2 ? -startX : maxX - startX;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, 1).setDuration(200);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                Log.d(TAG, "onAnimationUpdate: fraction = " + fraction);
                paramsWindow.x = (int) (startX + (deltaX1 * fraction));
                windowManager.updateViewLayout(contentView, paramsWindow);
            }
        });
        valueAnimator.start();

        maxXVelocity = 0;
    }

    private void windowToBig() {
        // 全屏显示
        paramsWindow.x = 0;
        paramsWindow.y = 0;
        paramsWindow.width = screenWidth;
        paramsWindow.height = screenHeight;
//        paramsWindow.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        contentView.setBackground(getApplicationContext().getResources().getDrawable(R.drawable.bg_video_chat));

        windowManager.updateViewLayout(contentView, paramsWindow);
    }

    private void localSurfaceViewToBig() {
        params.width = screenWidth / 4;
        params.height = screenHeight / 4;
        surfaceViewSmall.setLayoutParams(params);
        surfaceViewSmall.setVisibility(View.VISIBLE);
    }

    private void downWindow(MotionEvent event) {
        lastTime = System.currentTimeMillis();
        initX = paramsWindow.x;
        initY = paramsWindow.y;
        downX = (int) event.getRawX();
        downY = (int) event.getRawY();
        Log.d(TAG, "downWindow ACTION_DOWN: downX = " + downX + " downY = " + downY);
        Log.d(TAG, "downWindow ACTION_DOWN: initX = " + initX + " initY = " + initY);
    }

    private void moveWindow(int deltaX, int deltaY) {
        if (deltaX < minX - initX) {
            initX = minX - deltaX; // View到最左边，手指往左移的时候，让初始位置往右移|initX - deltaX|(deltaX超出屏幕左边的部分)
        } else if (deltaX > maxX - initX) {
            initX = maxX - deltaX; // View到最右边，手指往右移的时候，让初始位置往左移（deltaX超出x最大值右边的部分）
        }

        if (deltaY < minY - initY) {
            initY = minY - deltaY;
        } else if (deltaY > maxY - statusBarHeight - initY) {
            initY = maxY - statusBarHeight - deltaY;
        }

        int x = initX + deltaX;
        int y = initY + deltaY;

        paramsWindow.x = Math.max(minX, Math.min(maxX, x));
        paramsWindow.y = Math.max(minY, Math.min(maxY, y));
        windowManager.updateViewLayout(contentView, paramsWindow);
    }

    public boolean isTimeVisible() {
        return textTime.getVisibility() == View.VISIBLE;
    }

    @Override
    public boolean isFullScreen() {
        return paramsWindow.width == screenWidth;
    }

    @Override
    public void registerHomeWatcherReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        registerReceiver(receiver, filter);
    }

    @Override
    public void unRegisterHomeWatcherReceiver(BroadcastReceiver receiver) {
        unregisterReceiver(receiver);
    }

    @Override
    public void updateTime(String min, String sec) {
        textTime.setText(String.format("%s:%s", min, sec));
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void stopVideoChat() {
        Toast.makeText(this, "视频通话已结束", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "callStateChange: 6  stopSelf");
        stopSelf();
    }

    @Override
    public ImageView getBigSurfaceView() {
        return surfaceViewBig;
    }

    @Override
    public ImageView getSmallSurfaceView() {
        return surfaceViewSmall;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        try {
            if (contentView != null) {
                windowManager.removeView(contentView);
            }
            presenter.onDestroy(null);

        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        callId = intent.getIntExtra(EXTRA_CALL_ID, -1);
        return new VideoChatBinder();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        boolean isSmallWindow = paramsWindow.width < screenWidth;
        if (isSmallWindow) {
            return false;
        }
        if (v.getId() == R.id.iv_small) {
//            Log.d(TAG, "onTouch: sv_local");
            int deltaX = (int) event.getRawX() - downX; // deltaX为正，在原始位置的右边
            int deltaY = (int) event.getRawY() - downY; // deltaY为正，在原始位置的下边
//            Log.d(TAG, "onTouch: surfaceView deltaX = " + deltaX + ", deltaY = " + deltaY);
            Log.d(TAG, "onTouch: surfaceViewSmall.getTranslationX() = " + surfaceViewSmall.getTranslationX());
            if (params.width == screenWidth) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downSurfaceView(surfaceViewSmall, event);

                    break;
                case MotionEvent.ACTION_MOVE:
                    moveSurfaceView(surfaceViewSmall, deltaX, deltaY);
                    break;

                case MotionEvent.ACTION_UP:
                    upSurfaceView(deltaX, deltaY);
                    break;

            }

        } else if (v.getId() == R.id.iv_big) {
            Log.d(TAG, "onTouch: sv_remote");
            boolean isClickSmallSurfaceView = event.getRawX() > surfaceViewSmall.getLeft() && event.getRawX() < surfaceViewSmall.getRight() && event.getRawY() > surfaceViewSmall.getTop() && event.getRawY() < surfaceViewSmall.getBottom();
            if (isClickSmallSurfaceView) {
                return false;
            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "onTouch: ACTION_UP");
                    presenter.clickScreen();
                    break;
            }
            return true;
        }

//        Log.d(TAG, "onTouch: x = " + event.getX() + ", y = " + event.getY()); // 相对于surfaceView左上角的坐标x;0--v.width  y:-标题栏高度--v.height
//        Log.d(TAG, "onTouch: rawX = " + event.getRawX() + ", rawY = " + event.getRawY()); // 相对于屏幕的坐标X 0-480   Y 0-800 ，包含通知栏高度
        return true;
    }

    private void upSurfaceView(int deltaX, int deltaY) {
        //如果没移动，并且间隔时间小于500ms，则切换窗口播放
        long deltaTime = System.currentTimeMillis() - lastTime;
        Log.d(TAG, "onTouch: deltaTime = " + deltaTime);
        if (deltaX == 0 && deltaY == 0 && deltaTime < 500) {
            Log.d(TAG, "onTouch: changeSurface");
            if (isPreviewSmall) {
                Log.d(TAG, "onTouch: 远程视频切换为小窗口");
                presenter.previewToBig();
            } else {
                Log.d(TAG, "onTouch: 本地视频切换为小窗口");
                presenter.previewToSmall();
            }
            isPreviewSmall = !isPreviewSmall;
        }

    }

    private void downSurfaceView(ImageView surfaceView, MotionEvent event) {
        presenter.goneWidget();
        lastTime = System.currentTimeMillis();
        initX = surfaceView.getLeft();
        initY = surfaceView.getTop();
        downX = (int) event.getRawX();
        downY = (int) event.getRawY();
        Log.d(TAG, "ACTION_DOWN: downX = " + downX + " downY = " + downY);
        Log.d(TAG, "ACTION_DOWN: initX = " + initX + " initY = " + initY);
    }

    private void moveSurfaceView(MyImageView surfaceView, int deltaX, int deltaY) {
        if (deltaX < minX - initX) {
            initX = minX - deltaX; // View到最左边，手指往左移的时候，让初始位置往右移|initX - deltaX|(deltaX超出屏幕左边的部分)
        } else if (deltaX > maxX - initX) {
            initX = maxX - deltaX; // View到最右边，手指往右移的时候，让初始位置往左移（deltaX超出x最大值右边的部分）
        }

        if (deltaY < minY - initY) {
            initY = minY - deltaY;
        } else if (deltaY > maxY - statusBarHeight - initY) {
            initY = maxY - statusBarHeight - deltaY;
        }

        int left = initX + deltaX;
        left = Math.max(minX, Math.min(maxX, left));
        int top = initY + deltaY;
        top = Math.max(minY, Math.min(maxY, top));
        int right = left + surfaceView.getWidth();
        int bottom = top + surfaceView.getHeight();
        Log.d(TAG, "moveSurfaceView: left = " + left + ", top = " + top + ", right = " + right + ", bottom = " + bottom);
        surfaceView.myLayout(left, top, right, bottom);
    }

    @Override
    public void onDismiss() {
        dismissPopupWindow();
    }

    @Override
    public void showButtons(boolean visible) {
        videoChat.setButtonVisible(visible);
    }


    @Override
    public void wakeActivity() {

        // ActivityManager中  5.0(21)  废弃 getRunningTasks        方法
        //                    5.1.1(22)废弃 getRunningAppProcesses 方法
        Log.d(TAG, "wakeActivity: ");
        if (WakeAppManager.getInstance().getWakeAppListener() != null) {
            WakeAppManager.getInstance().getWakeAppListener().onWakeApp();
        }
    }

    private void dismissPopupWindow() {
        paramsWindow.alpha = 1.0f;
        windowManager.updateViewLayout(contentView, paramsWindow);
    }

    // localSurfaceView
    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        Log.d(TAG, "onLayoutChange:  left = " + left + ", top = " + top + ", right = " + right + ", bottom = " + bottom
                + ", oldLeft = " + oldLeft + ", oldTop = " + oldTop + ", oldRight = " + oldRight + ", oldBottom = " + oldBottom);
        if (top == 0 && right == screenWidth && (oldTop <= 2 || oldRight <= screenWidth - 2)) {
            Log.d(TAG, "onLayoutChange: dont return to original point");
//            surfaceViewSmall.layout(oldLeft, oldTop, oldRight, oldBottom);
        }
    }

    @Override
    public void setData(VideoChatListener listener) {
        dataBinding.setVideoChat(videoChat);
        dataBinding.setClickListener(listener);
    }

    @Override
    public void viewToFullScreen() {
        windowToBig();
        localSurfaceViewToBig();
    }

    @Override
    public void viewToSmall() {
        checkPlayOrder();
        localSurfaceViewToSmall();
        windowToSmall();
    }

    @Override
    public int getCallId() {
        return callId;
    }

    // 让surfaceViewSmall播放本地视频
    private void checkPlayOrder() {
        if (!isPreviewSmall) {
            presenter.previewToSmall();
            isPreviewSmall = !isPreviewSmall;
        }
    }

    private void localSurfaceViewToSmall() {
        surfaceViewSmall.setVisibility(View.INVISIBLE);
        params.width = screenWidth / 4 / 4;
        params.height = screenHeight / 4 / 4;
        surfaceViewSmall.layout(screenWidth - params.width, 0, screenWidth, params.height);
        surfaceViewSmall.setLayoutParams(params);
    }

    private void windowToSmall() {
        paramsWindow.width = screenWidth / 4;
        paramsWindow.height = screenHeight / 4;
        paramsWindow.x = screenWidth - paramsWindow.width;
        paramsWindow.y = 0;
//        paramsWindow.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        contentView.setBackground(getApplicationContext().getResources().getDrawable(R.drawable.bg_video_chat_float));

        windowManager.updateViewLayout(contentView, paramsWindow);
        Toast.makeText(getApplicationContext(), TOAST_WINDOW_MIN, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setPresenter(VideoChatContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public class VideoChatBinder extends Binder {
        public VideoChatService getService() {
            return VideoChatService.this;
        }
    }
}
