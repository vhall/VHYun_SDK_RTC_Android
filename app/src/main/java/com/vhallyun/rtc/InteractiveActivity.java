package com.vhallyun.rtc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.WindowManager;

import com.vhallyun.rtc.screenrecord.ScreenRecordInteractiveFragment;


public class InteractiveActivity extends FragmentActivity {
    private static final String TAG = "InteractiveActivity";
    String mRoomId;
    String mAccessToken;
    String mBroadCastId;
    int resolutionRation = 2;
    Fragment mInteractiveFrag;
    //相机直播
    public final static String CAMERA_LIVE = "camera_live";
    //录屏直播
    public final static String SCREEN_RECORD_LIVE = "screen_record_live";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoomId = getIntent().getStringExtra("channelid");
        mAccessToken = getIntent().getStringExtra("token");
        mBroadCastId = getIntent().getStringExtra("broadCastId");
        resolutionRation = getIntent().getIntExtra("resolutionRation", 2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.interactive_layout);

        /**
         * 页面可见时加载fragment
         * 1.可提升页面响应速度
         * 2.可避免fragment内异步请求引起的异常
         */
        String type = getIntent().getStringExtra("type");
        if(TextUtils.equals(type,CAMERA_LIVE)){
            mInteractiveFrag = InteractiveFragment.getInstance(mRoomId, mAccessToken, mBroadCastId, resolutionRation);
        }else if(TextUtils.equals(type,SCREEN_RECORD_LIVE)){
            mInteractiveFrag = ScreenRecordInteractiveFragment.getInstance(mRoomId, mAccessToken,mBroadCastId);
        }else{
            mInteractiveFrag = InteractiveFragment.getInstance(mRoomId, mAccessToken, mBroadCastId, resolutionRation);
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.videoFrame, mInteractiveFrag);
        transaction.commit();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
