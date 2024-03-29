package com.vhallyun.rtc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.vhall.framework.VhallSDK;
import com.vhall.ilss.VHInteractive;
import com.vhall.vhallrtc.client.FinishCallback;
import com.vhall.vhallrtc.client.Room;
import com.vhall.vhallrtc.client.Stream;
import com.vhall.vhallrtc.client.VHRenderView;
import com.vhallyun.rtc.util.ListUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.SurfaceViewRenderer;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class InteractiveFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "InteractiveFragment";
    Context mContext;
    RecyclerView mStreamContainer;//远程流渲染view容器
    LinearLayoutManager mLayoutManager;
    StreamAdapter mAdapter;//容器Adapter

    VHRenderView localView;//本地流渲染view
    Stream localStream;//本地流
    Button mReqBtn, mJoinBtn, mQuitBtn, mMemberBtn;//操作隐藏，demo默认进入直接上麦
    AlertDialog mDialog;
    //功能按钮
    ImageView mSwitchCameraBtn, mInfoBtn, beautifyFaceBtn;
    CheckBox mBroadcastTB, mVideoTB, mAudioTB, mChangeVoiceTB, mMianlayoutTB;
    TextView mOnlineTV, tvScaleType,tvlayoutType;

    public String mRoomId;
    public String mAccessToken;
    VHInteractive interactive = null;
    boolean isEnable = false;//是否可用
    boolean isOnline = false;//是否上麦
    String mRoomAttr = "roomAttr";
    String mBroadcastid = "";
    int mDefinition = 0;
    MemberPopu mMemberPopu;
    ActionPopu mActionPopu;
    StreamInfoPopu streamPop;
    Stream tempLocal;
    int changePosition = -1;
    int updatePosition = -1;
    String[] scaleText = {"fit", "fill", "none"};
    int scaleType = 0;
    int beautyLeve = 2;
    int layerType = 2;
    int layoutType = VHInteractive.CANVAS_LAYOUT_PATTERN_TILED_5_1T4D;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    List<Stream> streams = (List<Stream>) msg.obj;
                    for (Stream stream : streams) {
                        try {
                            //订阅大小流设置 0 小流 1 大流 默认小流
                            stream.streamOption.put(Stream.kDualKey, 0);
                            //禁流设置 不设置，默认订阅音视频
                            //禁用视频，仅订阅音频
                            stream.muteStream.put(Stream.kStreamOptionVideo, true);
                            //禁用音频，仅订阅视频
//                            stream.muteStream.put(Stream.kStreamOptionAudio, true);
                            interactive.subscribeStream(stream); //订阅房间内的其他流
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
            }
            return false;
        }
    });

    SharedPreferences sp;

    CopyOnWriteArrayList<Stream> mStreams = new CopyOnWriteArrayList<>();

    public static InteractiveFragment getInstance(String roomid, String accessToken, String broadcastid,int resolutionRatio) {
        InteractiveFragment fragment = new InteractiveFragment();
        fragment.mRoomId = roomid;
        fragment.mAccessToken = accessToken;
        fragment.mBroadcastid = broadcastid;
        fragment.layerType = resolutionRatio;
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_interactive, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initView();
        //取配置
        sp = mContext.getSharedPreferences("config", Context.MODE_PRIVATE);
        interactive = new VHInteractive(mContext, new RoomListener());
        interactive.setOnMessageListener(new MyMessageListener());
//        VHTool.enableDebugLog(true);
        initLocalView();
        initLocalStream();
        mAdapter = new StreamAdapter();
        mLayoutManager = new LinearLayoutManager(mContext);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mStreamContainer.setLayoutManager(mLayoutManager);
        mStreamContainer.setAdapter(mAdapter);
        mStreamContainer.setItemViewCacheSize(16);//最多16路

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isEnable) {
            initInteractive();
        } else if (!isOnline) {
            interactive.setListener(new RoomListener());
            interactive.enterRoom(mRoomAttr);
            //demo 中一路流仅渲染到一个视图中，因此在添加渲染视图时移除所有已存在RenderView
            if(localStream != null){
                localStream.removeAllRenderView();
                localStream.addRenderView(localView);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //离开房间，清空stream列表
        leaveRoom();
        interactive.setListener(null);
        isOnline = false;
    }

    @Override
    public void onDestroy() {
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
        interactive.release();
        super.onDestroy();
    }


    private void initInteractive() {
        interactive.init(mRoomId, mAccessToken,mBroadcastid, new VHInteractive.InitCallback() {
            @Override
            public void onSuccess() {
                isEnable = true;
                interactive.enterRoom(mRoomAttr);//初始化成功，直接进入房间
                refreshMembers();
            }

            @Override
            public void onFailure(int errorCode, String errorMsg) {
                isEnable = false;
                Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    class RoomListener implements Room.RoomDelegate {

        @Override
        public void onDidConnect(Room room, JSONObject jsonObject) {//进入房间
            Log.i(TAG, "onDidConnect");
            subscribeStreams(room.getRemoteStreams());
            join();//进入房间成功，自动上麦

        }

        /**
         * 受设备硬件及网速原因影响，部分机型一次性加载16路流存在大概率奔溃风险；
         * 进行延时加载处理，demo单次最多加载5路。单次最多8路，8路以上存在风险不建议使用
         *
         * @param streams
         */
        private void subscribeStreams(List<Stream> streams) {
            List<List<Stream>> list = ListUtil.sublistAsNum(streams, 5);
            for (int i = 0; i < list.size(); i++) {
                Message message = new Message();
                message.what = 0;
                message.obj = list.get(i);
                mHandler.sendMessageDelayed(message, i * 1500);
            }
        }

        @Override
        public void onDidError(Room room, Room.VHRoomErrorStatus vhRoomErrorStatus, String s) {//进入房间失败
            Log.i(TAG, "onDidError");
            removeAllStream();
        }

        @Override
        public void onDidPublishStream(Room room, Stream stream) {//上麦
            Log.i(TAG, "onDidPublishStream");
            isOnline = true;
        }

        @Override
        public void onDidUnPublishStream(Room room, Stream stream) {//下麦
            Log.i(TAG, "onDidUnPublishStream");
            isOnline = false;
        }

        @Override
        public void onDidSubscribeStream(Room room, Stream stream) {//订阅其他流
            Log.i(TAG, "onDidSubscribeStream" + stream.streamId);
            addStream(stream);
            refreshMembers();
        }

        @Override
        public void onDidUnSubscribeStream(Room room, Stream stream) {//取消订阅
            Log.i(TAG, "onDidUnSubscribeStream");
            removeStream(stream);
            refreshMembers();
        }

        @Override
        public void onDidChangeStatus(Room room, Room.VHRoomStatus vhRoomStatus) {//状态改变
            Log.i(TAG, "onDidChangeStatus");
            switch (vhRoomStatus) {
                case VHRoomStatusDisconnected:// 断开连接
                    //TODO 销毁页面
                    removeAllStream();
                    Log.e(TAG, "VHRoomStatusDisconnected");
                    break;
                case VHRoomStatusError:
                    Log.e(TAG, "VHRoomStatusError");
                    openErrorDialog();
                    break;
                case VHRoomStatusReady:
                    Log.e(TAG, "VHRoomStatusReady");
                    break;
                case VHRoomStatusConnected: // 连接成功
                    removeAllStream();
//                    join();// 当房间重连,如果之前已经上麦,则重连后自动上麦
                    Log.e(TAG, "VHRoomStatusConnected");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDidAddStream(Room room, Stream stream) {//有流加入
            Log.i(TAG, "onDidAddStream");
            try {
                stream.streamOption.put(Stream.kDualKey, 0);
                stream.muteStream.put(Stream.kStreamOptionVideo, true);
                room.subscribe(stream);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onDidRemoveStream(Room room, Stream stream) {//有流退出
            Log.i(TAG, "onDidRemoveStream : " + stream.streamId);
            removeStream(stream);
        }

        @Override
        public void onDidUpdateOfStream(Stream stream, JSONObject jsonObject) {//流状态更新
            Log.i(TAG, "onDidUpdateOfStream");
            JSONObject obj = jsonObject.optJSONObject("muteStream");
            boolean muteAudio = obj.optBoolean("audio");// true 禁音、false 未禁音
            boolean muteVideo = obj.optBoolean("video");// true 禁视频、 false 未禁视频
            //订阅端如需更新标识可自行处理业务逻辑

            changeStream(stream);

        }

        @Override
        public void onReconnect(int i, int i1) {
            Log.e(TAG, "onReconnect" + i + " i1 " + i1);
        }

        @Override
        public void onStreamMixed(JSONObject jsonObject) {

        }
    }


    private void initLocalView() {
        localView.init(null, null);
        localView.setScalingMode(SurfaceViewRenderer.VHRenderViewScalingMode.kVHRenderViewScalingModeAspectFit);
    }

    //初始化本地流
    private void initLocalStream() {
        int pixType = 0;
        JSONObject option = new JSONObject();
        try {
            switch (layerType) {
                case 0:
                    pixType = Stream.VhallFrameResolutionValue.VhallFrameResolution192x144.getValue();
                    option.put(Stream.kFrameResolutionTypeKey, pixType);
                    break;
                case 1:
                    pixType = Stream.VhallFrameResolutionValue.VhallFrameResolution320x240.getValue();
                    option.put(Stream.kFrameResolutionTypeKey, pixType);
                    break;
                case 2:
                    //该分辨率下支持双流
                    pixType = Stream.VhallFrameResolutionValue.VhallFrameResolution480x360.getValue();
                    option.put(Stream.kFrameResolutionTypeKey, pixType);
                    //重置双流码率，当前分辨率默认码率仅支持单流
                    option.put(Stream.kMinBitrateKbpsKey, 200);
                    option.put(Stream.kCurrentBitrateKey, 400);
                    option.put(Stream.kMaxBitrateKey, 600);
                    break;
                case 3:
                    //该分辨率下支持双流
                    pixType = Stream.VhallFrameResolutionValue.VhallFrameResolution640x480.getValue();
                    option.put(Stream.kFrameResolutionTypeKey, pixType);
                    //重置双流码率，当前分辨率默认码率仅支持单流
                    option.put(Stream.kMinBitrateKbpsKey, 500);
                    option.put(Stream.kCurrentBitrateKey, 900);
                    option.put(Stream.kMaxBitrateKey, 1200);
                    layerType = 2;
                    break;
            }
            option.put(Stream.kStreamOptionStreamType, Stream.VhallStreamType.VhallStreamTypeAudioAndVideo.getValue());
            option.put(Stream.kNumSpatialLayersKey, layerType);//单双流设置 2 双流 其他默认单流
        } catch (JSONException e) {
            e.printStackTrace();
        }
        localStream = interactive.createLocalStream(option, "paassdk");
//        localStream = interactive.createLocalStream(pixType, "paassdk", layerType);
        localStream.removeAllRenderView();
        localStream.addRenderView(localView);
        localStream.setEnableBeautify(true);
        tempLocal = localStream;
    }


    public void join() {//上麦
        if (!interactive.isPushAvailable()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "无上麦权限", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        interactive.publish();
    }

    private void leaveRoom() {
        mStreams.clear();
        mAdapter.notifyDataSetChanged();
        interactive.leaveRoom();
    }


    public void refreshMembers() {
        interactive.getMembers(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String res = response.body().string();
                Log.i(TAG, "members:" + res);
                try {
                    JSONObject result = new JSONObject(res);
                    String msg = result.optString("msg");
                    int code = result.optInt("code");
                    if (code == 200) {
                        JSONObject data = result.getJSONObject("data");
                        JSONArray list = data.getJSONArray("lists");
                        if (list != null && list.length() > 0) {
                            final List<Member> members = new LinkedList<>();
                            for (int i = 0; i < list.length(); i++) {
                                JSONObject obj = list.getJSONObject(i);
                                Member member = new Member();
                                member.userid = obj.getString("third_party_user_id");
                                member.status = obj.getInt("status");
                                members.add(member);
                            }
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    showMember(false, members);
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private void openErrorDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle("已离开互动房间");
                builder.setPositiveButton("OK", (dialog, which) -> {
                    getActivity().finish();//结束App
                });
                builder.show();
            }
        });
    }

    class MyMessageListener implements VHInteractive.OnMessageListener {

        @Override
        public void onMessage(JSONObject data) {
            try {
                String event = data.getString("inav_event");
                int status = data.optInt("status");
                String userid = data.optString("third_party_user_id");
                switch (event) {
                    case VHInteractive.apply_inav_publish://申请上麦消息
                        showDialog(VHInteractive.apply_inav_publish, userid);
                        break;
                    case VHInteractive.audit_inav_publish://申请审核结果消息
                        if (status == 1) {//批准上麦
                            interactive.publish();
                        } else {
                            Toast.makeText(mContext, "您的上麦请求未通过!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case VHInteractive.askfor_inav_publish://邀请上麦消息
                        showDialog(VHInteractive.askfor_inav_publish, userid);
                        break;
                    case VHInteractive.kick_inav_stream:
                        Toast.makeText(mContext, "您已被请下麦！", Toast.LENGTH_SHORT).show();
                        break;
                    case VHInteractive.kick_inav:
                        Toast.makeText(mContext, "您已被踢出房间！", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                        break;
                    case VHInteractive.user_publish_callback:
                        String action = "";
                        switch (status) {
                            case 1:
                                action = "上麦啦！";
                                break;
                            case 2:
                                action = "下麦啦！";
                                break;
                            case 3:
                                action = "拒绝上麦！";
                                break;
                        }
                        Toast.makeText(mContext, userid + ":" + action, Toast.LENGTH_SHORT).show();
                        break;
                    case VHInteractive.inav_close:
                        Toast.makeText(mContext, "直播间已关闭", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                        break;

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRefreshMemberState() {
            refreshMembers();
        }

        @Override
        public void onRefreshMembers(JSONObject obj) {
            int onlineNum = obj.optInt("user_online_num");
            mOnlineTV.setText("online:" + onlineNum);
        }
    }

    private void addStream(final Stream stream) {
        if (stream == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mStreams.contains(stream))
                    return;
                mStreams.add(stream);
                mAdapter.notifyItemInserted(mStreams.size() - 1);
            }
        });
    }

    //修改流状态
    public void changeStream(Stream stream) {
        if (stream == null)
            return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mStreams.size(); i++) {
                    if (stream.streamId.equals(mStreams.get(i).streamId)) {
                        updatePosition = i;
                        mStreams.set(i, stream);
                        mAdapter.notifyItemChanged(i);
                        break;
                    }
                }
            }
        });

    }

    private void removeStream(final Stream stream) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (stream == null)
                    return;
                for (int i = 0; i < mStreams.size(); i++) {
                    if (mStreams.get(i).streamId == stream.streamId) {
                        mStreams.remove(stream);
                        mAdapter.notifyItemRemoved(i);
                        break;
                    }
                }

            }
        });

    }

    private void removeAllStream() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mStreams.clear();
                mAdapter.notifyDataSetChanged();
            }
        });

    }


    private void initView() {
        mStreamContainer = getView().findViewById(R.id.ll_streams);
        localView = getView().findViewById(R.id.localView);
        mJoinBtn = getView().findViewById(R.id.btn_join);
        mQuitBtn = getView().findViewById(R.id.btn_quit);
        mReqBtn = getView().findViewById(R.id.btn_request);
        mBroadcastTB = getView().findViewById(R.id.tb_broadcast);
        mVideoTB = getView().findViewById(R.id.tb_video);
        mAudioTB = getView().findViewById(R.id.tb_audio);
        mChangeVoiceTB = getView().findViewById(R.id.tb_changeVoice);
        beautifyFaceBtn = getView().findViewById(R.id.iv_beautify_face);
        mSwitchCameraBtn = getView().findViewById(R.id.iv_camera);
        mMianlayoutTB = getView().findViewById(R.id.tb_mainlayout);
        mMemberBtn = getView().findViewById(R.id.btn_members);
        mInfoBtn = getView().findViewById(R.id.iv_info);
        mOnlineTV = getView().findViewById(R.id.tv_online);
        tvScaleType = getView().findViewById(R.id.tv_scale_type);
        tvlayoutType= getView().findViewById(R.id.tv_layout);
        mJoinBtn.setOnClickListener(this);
        mQuitBtn.setOnClickListener(this);
        mReqBtn.setOnClickListener(this);
        mMemberBtn.setOnClickListener(this);
        mSwitchCameraBtn.setOnClickListener(this);
        mInfoBtn.setOnClickListener(this);
        mChangeVoiceTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                localStream.changeVoiceType(isChecked ? 1 : 0);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, isChecked?"已开启变声":"已关闭变声", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        //设置主屏
        mMianlayoutTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tempLocal.setMixLayoutMainScreen(null, new FinishCallback() {
                    @Override
                    public void onFinish(int i, @Nullable String s) {
                        if(i==200){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mContext, "成功设置主画面", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
            }
        });
        mBroadcastTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (TextUtils.isEmpty(mBroadcastid)) {
                    mBroadcastTB.setChecked(false);
                    Toast.makeText(mContext, "旁路ID为空，无法推旁路", Toast.LENGTH_SHORT).show();
                    return;
                }
                int type = isChecked ? 1 : 2;
                interactive.broadcastRoom(type, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, isChecked?"开启旁路失败":"关闭旁路失败", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, isChecked?"已开启旁路":"已关闭旁路", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
        mVideoTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)//关闭视频开
                    localStream.muteVideo(null);
                else//关闭视频关
                    localStream.unmuteVideo(null);

            }
        });
        mAudioTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    localStream.muteAudio(null);
                else
                    localStream.unmuteAudio(null);

            }
        });

        beautifyFaceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int level = ++beautyLeve % 5;
                if (level == 0) {
                    localStream.setEnableBeautify(false);
                    Toast.makeText(getContext(), "关闭美颜", Toast.LENGTH_SHORT).show();
                } else {
                    localStream.setEnableBeautify(true);
                    localStream.setBeautifyLevel(level);
                    Toast.makeText(getContext(), "美颜等级" + level, Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvScaleType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int type = ++scaleType % 3;
                tvScaleType.setText(scaleText[type]);
                switch (type) {
                    case 0:
                        localView.setScalingMode(SurfaceViewRenderer.VHRenderViewScalingMode.kVHRenderViewScalingModeAspectFit);
                        break;
                    case 1:
                        localView.setScalingMode(SurfaceViewRenderer.VHRenderViewScalingMode.kVHRenderViewScalingModeAspectFill);
                        break;
                    case 2:
                        localView.setScalingMode(SurfaceViewRenderer.VHRenderViewScalingMode.kVHRenderViewScalingModeNone);
                        break;
                }
            }
        });

        tvlayoutType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int type = ++layoutType % 26;
                int layoutType = VHInteractive.CANVAS_LAYOUT_PATTERN_TILED_5_1T4D;
                tvlayoutType.setText(String.valueOf(type));
                interactive.broadcastLayout(type, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext,e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext,"已设置旁路布局"+type, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }

    /**
     * @param event
     * @param userid
     */
    private void showDialog(String event, final String userid) {
        if (getActivity().isFinishing())
            return;
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        switch (event) {
            case VHInteractive.apply_inav_publish:
                mDialog = builder.setTitle("申请上麦")
                        .setMessage(userid + " 申请上麦，是否批准！")
                        .setNegativeButton("不批", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                interactive.checkPublishRequest(userid, 2, null);
                            }
                        })
                        .setPositiveButton("批准", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                interactive.checkPublishRequest(userid, 1, null);
                            }
                        })
                        .create();
                mDialog.show();
                break;
            case VHInteractive.askfor_inav_publish:
                mDialog = builder.setTitle("邀请上麦")
                        .setMessage(userid + " 邀请您上麦，是否同意！")
                        .setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                interactive.refusePublish();
                            }
                        })
                        .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                interactive.publish();
                            }
                        })
                        .create();
                mDialog.show();
                break;
        }


    }

    private void showMember(boolean show, List<Member> data) {
        if (mMemberPopu == null) {
            mMemberPopu = new MemberPopu(mContext);
            mMemberPopu.setItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    showAction((Member) parent.getItemAtPosition(position));
                }
            });
        }
        refreshMembers();
        if (data != null)
            mMemberPopu.refreshData(data);
        if (show)
            mMemberPopu.showAtLocation(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }

    private void showAction(Member member) {
        if(member.userid.equals(VhallSDK.getInstance().mUserId))//本人不做处理
            return;

        if (mActionPopu == null) {
            mActionPopu = new ActionPopu(mContext);
        }
        mActionPopu.setInteractive(interactive);
        mActionPopu.setMember(member);
        mActionPopu.showAtLocation(getActivity().getWindow().getDecorView().findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }


    private String getPixName(int type) {
        String name = "";
        switch (type) {
            case 0:
                name = "SD";
                break;
            case 1:
                name = "HD";
                break;
            case 2:
                name = "UHD";
                break;

        }
        return name;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_join:
                join();
                break;
            case R.id.btn_quit:
                interactive.unpublish();
                break;
            case R.id.btn_request:
                joinRequest();
                break;
            case R.id.iv_camera:
                localStream.switchCamera();
                break;
            case R.id.btn_members:
                showMember(true, null);
                break;
            case R.id.iv_info:
                if (infoListener != null) {
                    infoListener.onInfoClick(tempLocal);
                }
                break;

        }
    }

    public void joinRequest() {//申请上麦
        if (!isEnable)
            return;
        interactive.requestPublish(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String result = response.body().string();
                try {
                    final JSONObject obj = new JSONObject(result);
                    int code = obj.optInt("code");
                    if (code != 200) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, obj.optString("msg"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private ItemClickListener itemClickListener = new ItemClickListener() {
        @Override
        public void onItemClick(int position) {
            Stream stream = mStreams.get(position);
            mStreams.remove(position);
            stream.removeAllRenderView();
            stream.addRenderView(localView);
            if (!tempLocal.isLocal) {
                tempLocal.switchDualStream(0, null);
            }
            mStreams.add(position, tempLocal);
            mAdapter.notifyItemChanged(position);
            changePosition = position;
            tempLocal = stream;
            tempLocal.removeAllRenderView();
            tempLocal.addRenderView(localView);
            if (!tempLocal.isLocal) {
                tempLocal.switchDualStream(1, null);
            }
        }
    };

    private InfoClickListener infoListener = new InfoClickListener() {
        @Override
        public void onInfoClick(Stream stream) {
            if (streamPop == null) {
                streamPop = new StreamInfoPopu(getContext());
            }
            streamPop.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    stream.startStats(null);

                }
            });
            stream.startStats(new Stream.StatsCallback() {
                @Override
                public void onResponse(String s, long l, Map<String, String> map) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            streamPop.refreshData(stream, s, l, map);
                        }
                    });
                }
            });
            streamPop.showAtLocation(getView(), Gravity.CENTER, 0, 0);
        }
    };

    interface ItemClickListener {
        void onItemClick(int position);
    }

    interface InfoClickListener {
        void onInfoClick(Stream stream);
    }

    class StreamAdapter extends RecyclerView.Adapter<MyHolder> {

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MyHolder holder = new MyHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remote_stream, parent, false));
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyHolder holder, int position) {
            Log.e(TAG, "onBindViewHolder:" + position);
            holder.stream = mStreams.get(position);
            //demo 中一路流仅渲染到一个视图中，因此在添加渲染视图时移除所有已存在RenderView
            holder.stream.removeAllRenderView();
            holder.stream.addRenderView(holder.renderView);
            holder.tvDescribe.setText(holder.stream.userId);
            JSONObject obj = holder.stream.remoteMuteStream;
            boolean muteVideo;
            boolean muteAudio;
            if (obj == null) {
                obj = holder.stream.muteStream;
            }
            muteVideo = obj.optBoolean("video");
            muteAudio = obj.optBoolean("audio");
            holder.cbAudio.setChecked(muteAudio);
            holder.cbVideo.setChecked(muteVideo);
            holder.cbVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.cbVideo.isChecked())//关闭视频开
                        holder.stream.muteVideo(null);
                    else//关闭视频关
                        holder.stream.unmuteVideo(null);
                }
            });
            holder.cbAudio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.cbAudio.isChecked())//关闭视频开
                        holder.stream.muteAudio(null);
                    else//关闭视频关
                        holder.stream.unmuteAudio(null);
                }
            });

            holder.ivInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (infoListener != null) {
                        infoListener.onInfoClick(holder.stream);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mStreams.size();
        }

        @Override
        public void onViewAttachedToWindow(@NonNull MyHolder holder) {
            super.onViewAttachedToWindow(holder);
            Log.e(TAG, "onViewAttachedToWindow");
            if (holder.getAdapterPosition() != changePosition) {
                holder.stream.unmuteVideo(null);
            }
        }


        @Override
        public void onViewDetachedFromWindow(@NonNull MyHolder holder) {
            super.onViewDetachedFromWindow(holder);
            Log.e(TAG, "onViewDetachedFromWindow");
            changePosition = -1;
            if (updatePosition != holder.getAdapterPosition()) {
                if (holder.stream != tempLocal) {
                    if (!holder.stream.isLocal) {
                        holder.stream.muteVideo(null);
                    }
                }
            }
        }
    }


    class MyHolder extends RecyclerView.ViewHolder {

        VHRenderView renderView;
        CheckBox cbVideo;
        CheckBox cbAudio;
        TextView tvDescribe;
        ImageView ivInfo;
        Stream stream;


        public MyHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (itemClickListener != null) {
                        itemClickListener.onItemClick(getAdapterPosition());
                    }
                }
            });
            renderView = itemView.findViewById(R.id.renderview);
            renderView.init(null, null);
            tvDescribe = itemView.findViewById(R.id.tv_speed);
            ivInfo = itemView.findViewById(R.id.iv_info);
            cbVideo = itemView.findViewById(R.id.cb_video);
            cbAudio = itemView.findViewById(R.id.cb_audio);
        }
    }
}
