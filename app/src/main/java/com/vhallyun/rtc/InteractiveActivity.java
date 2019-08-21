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
    InteractiveFragment mInteractiveFrag;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoomId = getIntent().getStringExtra("channelid");
        mAccessToken = getIntent().getStringExtra("token");
        mBroadCastId = getIntent().getStringExtra("broadCastId");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.interactive_layout);
        mInteractiveFrag = InteractiveFragment.getInstance(mRoomId, mAccessToken,mBroadCastId);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.videoFrame, mInteractiveFrag);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
