package com.vhallyun.rtc;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


import com.vhall.framework.VhallSDK;
import com.vhall.framework.utils.SignatureUtil;

import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by Hank on 2018/3/9.
 */
public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final int REQUEST_READ_PHONE_STATE = 0;
    EditText mEditAppid;
    EditText mEditUserid;
    TextView tvPackageName, tvSignatures,app_version;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);
        mEditAppid = this.findViewById(R.id.et_appid);
        mEditUserid = this.findViewById(R.id.et_userid);
        mEditAppid.setText("");
        mEditUserid.setText(Build.MODEL);//String.valueOf(System.currentTimeMillis())
        tvPackageName = findViewById(R.id.tv_package_name);
        tvPackageName.setText(getPackageName());
        tvSignatures = findViewById(R.id.tv_signatures);
        app_version = findViewById(R.id.app_version);
        tvSignatures.setText(SignatureUtil.getSignatureSHA1(this));
        getPermission();

        app_version.setText(String.format("Version: v%s",BuildConfig.VERSION_NAME));
    }

    //初始化SDK需要读取手机信息做信息统计，如果取不到权限，信息为空，不影响SDK使用
    private void getPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        if (checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            return;
        }
        requestPermissions(new String[]{READ_PHONE_STATE,WRITE_EXTERNAL_STORAGE}, REQUEST_READ_PHONE_STATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_PHONE_STATE) {
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "get READ_PHONE_STATE permission success");
            }
        }
    }

    public void enter(View view) {

        String appid = mEditAppid.getText().toString();
        String userid = mEditUserid.getText().toString();

        if (!TextUtils.isEmpty(appid)) {
            VhallSDK.getInstance().init(getApplicationContext(), appid, userid);//初始化成功会打印日志：初始化成功！，请确保注册的appid与当前应用包名签名一致
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }
}
