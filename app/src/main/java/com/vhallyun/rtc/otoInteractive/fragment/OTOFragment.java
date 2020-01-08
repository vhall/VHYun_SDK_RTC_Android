package com.vhallyun.rtc.otoInteractive.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.vhall.vhallrtc.client.Stream;
import com.vhall.vhallrtc.client.VHRenderView;
import com.vhallyun.rtc.R;
import com.vhallyun.rtc.otoInteractive.activity.OTOActivity;
import com.vhallyun.rtc.otoInteractive.widget.DragRelativeLayout;

import org.webrtc.SurfaceViewRenderer;


/**
 * Created by zwp on 2019-12-11
 */
public class OTOFragment extends Fragment implements View.OnClickListener {

    View rootView;
    VHRenderView fullScreen;
    VHRenderView smallScreen;
    DragRelativeLayout rootDrl;
    ImageView ivSwitchCamera, ivSwitchMic, ivCallEnd, ivCallStart;
    Stream localStream, remoteStream;
    public static int TYPE_CALLING = 1;
    public static int TYPE_CALLED = 2;
    private int type = TYPE_CALLING;
    private boolean isInitView = false;
    private boolean localSmall = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_oto, container, false);
        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!isInitView) {
            initView();
        }
        if (type == TYPE_CALLING) {
            ivCallStart.setVisibility(View.GONE);
        } else {
            ivCallStart.setVisibility(View.VISIBLE);
        }

    }

    private void initView() {
        isInitView = true;
        rootDrl = rootView.findViewById(R.id.root_drl);
        fullScreen = rootView.findViewById(R.id.rv_full_screen);
        fullScreen.init(null, null);
        fullScreen.setScalingMode(SurfaceViewRenderer.VHRenderViewScalingMode.kVHRenderViewScalingModeAspectFit);
        smallScreen = rootView.findViewById(R.id.rv_small_screen);
        smallScreen.init(null, null);
        smallScreen.setScalingMode(SurfaceViewRenderer.VHRenderViewScalingMode.kVHRenderViewScalingModeAspectFit);
        smallScreen.setZOrderMediaOverlay(true);
        rootDrl.setTargetView(smallScreen);
        ivSwitchCamera = rootView.findViewById(R.id.iv_change_camera);
        ivSwitchMic = rootView.findViewById(R.id.iv_change_mic);
        ivCallEnd = rootView.findViewById(R.id.iv_call_end);
        ivCallStart = rootView.findViewById(R.id.iv_call_start);

        if (localStream != null) {
            localStream.removeAllRenderView();
            localStream.addRenderView(fullScreen);
            localSmall = false;
        }

        ivSwitchCamera.setOnClickListener(this);
        ivSwitchMic.setOnClickListener(this);
        ivCallEnd.setOnClickListener(this);
        ivCallStart.setOnClickListener(this);
        smallScreen.setOnClickListener(this);
    }

    public void setLocalStream(Stream stream) {
        localStream = stream;
    }

    public void setRemoteStream(Stream stream) {
        remoteStream = stream;
        if (fullScreen != null && remoteStream != null) {
            smallScreen.setVisibility(View.VISIBLE);
            localStream.removeAllRenderView();
            localStream.addRenderView(smallScreen);
            localSmall = true;
            remoteStream.removeAllRenderView();
            remoteStream.addRenderView(fullScreen);
        }

    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_change_camera:
                localStream.switchCamera();
                break;
            case R.id.iv_change_mic:
                if (v.isSelected()) {
                    localStream.unmuteAudio(null);
                    v.setSelected(false);
                } else {
                    v.setSelected(true);
                    localStream.muteAudio(null);
                }
                break;
            case R.id.iv_call_end:
                ((OTOActivity) getActivity()).otoNegativeCall();
                break;

            case R.id.iv_call_start:
                //接听
                ((OTOActivity) getActivity()).otoPositiveCall();
                smallScreen.setVisibility(View.VISIBLE);
                ivCallStart.setVisibility(View.GONE);
                break;
            case R.id.rv_small_screen:
                if (localSmall) {
                    localStream.removeAllRenderView();
                    localStream.addRenderView(fullScreen);
                    localSmall = false;
                    remoteStream.removeAllRenderView();
                    remoteStream.addRenderView(smallScreen);
                } else {
                    localStream.removeAllRenderView();
                    localStream.addRenderView(smallScreen);
                    localSmall = true;
                    remoteStream.removeAllRenderView();
                    remoteStream.addRenderView(fullScreen);
                }
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        /*smallScreen.setVisibility(View.GONE);
        if (localStream != null) {
            localStream.removeAllRenderView();
        }
        if (remoteStream != null) {
            remoteStream.removeAllRenderView();
        }*/
    }
}
