package com.vhallyun.rtc.otoInteractive.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

public class DragRelativeLayout extends RelativeLayout {

    private static final String TAG = "DragRelativeLayout";

    public DragRelativeLayout(Context context) {
        super(context);
        initDragHelper();
    }

    public DragRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDragHelper();
    }

    public DragRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDragHelper();
    }

    private ViewDragHelper mViewDragHelper;
    private View mTargetView;//需要拖动的view

    public void setTargetView(View targetView) {
        mTargetView = targetView;
    }

    private void initDragHelper() {
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, mDragCallback);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent event) {//拦截事件
        return mViewDragHelper.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {//消费事件
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mViewDragHelper.continueSettling(true)) {//不停计算位置后，自动移动
            ViewCompat.postInvalidateOnAnimation(this);//重新绘制
        }
    }

    /**
     * ViewDragHelper回调接口
     */
    private ViewDragHelper.Callback mDragCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (mTargetView != null && child == mTargetView)
                return true;
            else
                return false;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {// 水平拖动
            final int leftPadding = getPaddingLeft();
            final int rightPadding = getWidth() - child.getWidth() - leftPadding;
            final int newLeft = Math.min(Math.max(left, leftPadding), rightPadding);
            return newLeft;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {//竖直拖动
            final int topPadding = getPaddingTop();
            final int bottomPadding = getHeight() - child.getHeight() - topPadding;
            final int newTop = Math.min(Math.max(top, topPadding), bottomPadding);
            return newTop;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
        }

        @Override
        public void onViewCaptured(@NonNull View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        @Override
        public void onViewPositionChanged(@NonNull View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            Log.e(TAG, "left:" + left + " top:" + top + " dx:" + dx + " dy:" + dy);
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {//拖动结束后
            super.onViewReleased(releasedChild, xvel, yvel);
//            float x = releasedChild.getX();
//            float y = releasedChild.getY();
//            if (x < (getMeasuredWidth() / 2f - releasedChild.getMeasuredWidth() / 2f)) { // 0-x/2
//                if (x < releasedChild.getMeasuredWidth() / 3f) {
//                    x = 0;
//                } else if (y < (releasedChild.getMeasuredHeight() * 3)) { // 0-y/3
//                    y = 0;
//                } else if (y > (getMeasuredHeight() - releasedChild.getMeasuredHeight() * 3)) { // 0-(y-y/3)
//                    y = getMeasuredHeight() - releasedChild.getMeasuredHeight();
//                } else {
//                    x = 0;
//                }
//            } else { // x/2-x
//                if (x > getMeasuredWidth() - releasedChild.getMeasuredWidth() / 3f - releasedChild.getMeasuredWidth()) {
//                    x = getMeasuredWidth() - releasedChild.getMeasuredWidth();
//                } else if (y < (releasedChild.getMeasuredHeight() * 3)) { // 0-y/3
//                    y = 0;
//                } else if (y > (getMeasuredHeight() - releasedChild.getMeasuredHeight() * 3)) { // 0-(y-y/3)
//                    y = getMeasuredHeight() - releasedChild.getMeasuredHeight();
//                } else {
//                    x = getMeasuredWidth() - releasedChild.getMeasuredWidth();
//                }
//            }
//            mViewDragHelper.smoothSlideViewTo(releasedChild, (int) x, (int) y);//平滑移动
//            ViewCompat.postInvalidateOnAnimation(DragRelativeLayout.this);
        }
    };
}
