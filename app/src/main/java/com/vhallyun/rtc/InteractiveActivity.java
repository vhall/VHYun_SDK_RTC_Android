package com.vhallyun.rtc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;


public class InteractiveActivity extends FragmentActivity {
    private static final String TAG = "InteractiveActivity";
    String mRoomId;
    String mAccessToken;
    String mBroadCastId;
    int resolutionRation = 2;
    InteractiveFragment mInteractiveFrag = null;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoomId = getIntent().getStringExtra("channelid");
        mAccessToken = getIntent().getStringExtra("token");
        mBroadCastId = getIntent().getStringExtra("broadCastId");
        resolutionRation = getIntent().getIntExtra("resolutionRation", 2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.interactive_layout);

    }

    @Override
    protected void onResume() {
        super.onResume();
        /**
         * 页面可见时加载fragment
         * 1.可提升页面响应速度
         * 2.可避免fragment内异步请求引起的异常
         */
        if(mInteractiveFrag ==null)
        {
            mInteractiveFrag = InteractiveFragment.getInstance(mRoomId, mAccessToken, mBroadCastId, resolutionRation);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.videoFrame, mInteractiveFrag);
            transaction.commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
