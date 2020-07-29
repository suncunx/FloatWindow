package com.scx.floatwindow;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

/**
 * Created by sun.cunxing on 2018/8/3.
 */
public class MyImageView extends ImageView {

    private static final String TAG = "MySurfaceView";
    private boolean needLayout;

    public MyImageView(Context context) {
        super(context);
    }

    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void myLayout(int l, int t, int r, int b) {
        this.needLayout = true;
        layout(l, t, r, b);
    }

    @Override
    public void layout(int l, int t, int r, int b) {
        Log.d(TAG, "layout: needLayout = " + needLayout);
        Log.d(TAG, "layout: left = " + l + ", top = " + t + ", right = " + r + ", bottom = " + b);
        if (needLayout) {
            super.layout(l, t, r, b);
        }
        needLayout = false;
    }

//    @Override
//    public void requestLayout() {
//        super.requestLayout();
//    }

    public void requestMyLayout() {
        this.needLayout = true;
        requestLayout();
    }
}
