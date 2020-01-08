package com.vhallyun.rtc.otoInteractive.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.vhall.framework.VhallSDK;
import com.vhall.ilss.VHInteractive;
import com.vhall.ilss.VHOTOInteractive;
import com.vhall.vhallrtc.client.Stream;
import com.vhallyun.rtc.Member;
import com.vhallyun.rtc.R;
import com.vhallyun.rtc.otoInteractive.fragment.MemberListFragment;
import com.vhallyun.rtc.otoInteractive.fragment.OTOFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * Created by zwp on 2019-12-11
 */
public class OTOActivity extends FragmentActivity {
    private static final String TAG = "OTOActivity";
    VHOTOInteractive otoInteractive;
    String mRoomId, mAccessToken;
    Stream localStream, remoteStream;
    OTOFragment otoFragment;
    int resolutionRation = 2;
    MemberListFragment memberFragment;
    private Context mContext;
    private Handler mHandler;
    private List<Member> roomMembers;
    boolean isFinished = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRoomId = getIntent().getStringExtra("channelid");
        mAccessToken = getIntent().getStringExtra("token");
        resolutionRation = getIntent().getIntExtra("resolutionRation", 2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_oto);
        mContext = this;
        mHandler = new Handler();

        otoInteractive = new VHOTOInteractive(this);
        otoInteractive.init(mRoomId, mAccessToken, initCallback);
        otoInteractive.setOTOListener(listener);
    }


    @Override
    protected void onResume() {
        super.onResume();
        isFinished = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isFinished = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        otoInteractive.leaveRoom();
        otoInteractive.release();
    }

    private VHInteractive.InitCallback initCallback = new VHInteractive.InitCallback() {
        @Override
        public void onSuccess() {
            otoInteractive.enterRoom("extra info");
            getMembers();
        }

        @Override
        public void onFailure(int errorCode, String errorMsg) {
            Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
        }
    };


    public void getMembers() {
        if (otoInteractive != null) {
            otoInteractive.getMembers(getMemberCallback);
        }
    }

    /**
     * 呼叫
     *
     * @param position
     */
    public void otoCall(int position) {
        Log.e(TAG, "otoCall: ");
        if (otoInteractive != null) {
            otoInteractive.OTOCall(roomMembers.get(position).userid);
            if (localStream == null) {
                localStream = otoInteractive.createLocalStream("extraInfo");
            }
            otoFragment = new OTOFragment();
            otoFragment.setType(OTOFragment.TYPE_CALLING);
            otoFragment.setLocalStream(localStream);
            getSupportFragmentManager().beginTransaction().replace(R.id.fl_oto_container, otoFragment).commit();
        }
    }

    /**
     * 拒接或挂断
     */
    public void otoNegativeCall() {
        Log.e(TAG, "otoNegativeCall: ");
        //返回列表页
        getMembers();
        //消息发送
        otoInteractive.OTONegativeAnswer();
    }


    /**
     * 接听
     */
    public void otoPositiveCall() {
        Log.e(TAG, "otoPositiveCall: ");
        int result = otoInteractive.OTOPositiveAnswer();
        Log.e(TAG, "otoPositiveCall:result =  " + result);
    }

    private Callback getMemberCallback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {

        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String res = response.body().string();
            try {
                JSONObject obj = new JSONObject(res);
                int code = obj.optInt("code");
                if (code == 200) {
                    JSONObject data = obj.optJSONObject("data");
                    JSONArray list = data.optJSONArray("lists");
                    if (list != null && list.length() > 0) {
                        roomMembers = new ArrayList<>();
                        for (int i = 0; i < list.length(); i++) {
                            JSONObject usr = list.getJSONObject(i);
                            Member member = new Member();
                            member.userid = usr.getString("third_party_user_id");
                            member.status = usr.getInt("status");
                            if (!member.userid.equals(VhallSDK.getInstance().getmUserId())) {
                                roomMembers.add(member);
                            }
                        }
                        if (!isFinished) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (memberFragment == null) {
                                        memberFragment = new MemberListFragment();
                                    }
                                    memberFragment.setMembers(roomMembers);
                                    getSupportFragmentManager().beginTransaction().replace(R.id.fl_oto_container, memberFragment).commit();
                                }
                            });
                        }
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private VHOTOInteractive.OTOInteractiveListener listener = new VHOTOInteractive.OTOInteractiveListener() {
        @Override
        public void onRoomDidConnect() {//加入房间成功
            Log.e(TAG, "onRoomDidConnect: ");
        }

        @Override
        public void onRoomConnectError() {//加入房间失败
            Log.e(TAG, "onRoomConnectError: ");
        }

        @Override
        public void onRoomReconnect() {//房间重连
            Log.e(TAG, "onRoomReconnect: ");
        }

        /**
         * 服务连接状态不影响音视频互动，仅影响房间内消息接收
         */
        @Override
        public void onServerConnecting() {//房间内服务连接中
            Log.e(TAG, "onServerConnecting: ");
        }

        @Override
        public void onServerConnected() {//服务已连接
            Log.e(TAG, "onServerConnected: ");
        }

        @Override
        public void onServerDisConnected() {//服务断开
            Log.e(TAG, "onServerDisConnected: ");
        }

        @Override
        public void onOTOCall(String callerId) {
            Log.e(TAG, "onOTOCall: ");
            //被呼叫，呼叫者id  callerID
            if (localStream == null) {
                localStream = otoInteractive.createLocalStream("extraInfo");
            }
            otoFragment = new OTOFragment();
            otoFragment.setType(OTOFragment.TYPE_CALLED);
            otoFragment.setLocalStream(localStream);
            getSupportFragmentManager().beginTransaction().replace(R.id.fl_oto_container, otoFragment).commit();
        }

        @Override
        public void onOTOPositiveAnswer() {
            //呼叫被接听
            Log.e(TAG, "onOTOPositiveAnswer: ");
        }

        @Override
        public void onOTONegativeAnswer() {
            Log.e(TAG, "onOTONegativeAnswer: ");
            //呼叫被拒绝
            if (memberFragment == null) {
                memberFragment = new MemberListFragment();
            }
            memberFragment.setMembers(roomMembers);
            getSupportFragmentManager().beginTransaction().replace(R.id.fl_oto_container, memberFragment).commit();
        }

        @Override
        public void onOTOCallEnd() {
            Log.e(TAG, "onOTOCallEnd: ");
            //通话被挂断,返回列表页
            otoFragment.setRemoteStream(null);
            getMembers();
        }

        @Override
        public void onDidAddStream(Stream stream) {
            Log.e(TAG, "onDidAddStream: ");
            //被叫方接听并上麦
            //通话画面同步
            remoteStream = stream;
            otoFragment.setRemoteStream(remoteStream);
        }
    };


}
