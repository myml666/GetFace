package com.itfitness.getface;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.bumptech.glide.Glide;
import com.itfitness.getface.activity.GetFaceActicity;
import com.itfitness.getface.common.Constans;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;

import io.reactivex.functions.Consumer;
import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RxPermissions rxPermissions;
    private Button mBtActive,mBtGetFace;
    private TextView mTvSize;
    private ImageView mImgFace;
    private FaceEngine faceEngine = new FaceEngine();
    private int activeCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rxPermissions = new RxPermissions(this);
        initView();
        initListener();
    }

    private void initListener() {
        mBtActive.setOnClickListener(this);
        mBtGetFace.setOnClickListener(this);
    }

    private void initView() {
        mBtActive = findViewById(R.id.bt_activeengine);
        mBtGetFace = findViewById(R.id.bt_findface);
        mImgFace = findViewById(R.id.img);
        mTvSize = findViewById(R.id.tv_faceimgsize);
    }
    /**
     * 加载权限
     */
    private void initPermission() {
        rxPermissions.requestEachCombined(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            activeengine();
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            ToastUtils.showShort("您已拒绝权限申请");
                        } else {
                            ToastUtils.showShort("您已拒绝权限申请，请前往设置>应用管理>权限管理打开权限");
                        }
                    }
                });
    }
    /**
     * 激活引擎
     */
    private void activeengine() {
        //激活引擎
        new Thread(new Runnable(){
            @Override
            public void run() {
                activeCode = faceEngine.activeOnline(MainActivity.this, Constans.APP_ID, Constans.SDK_KEY);
                String msg = "";
                if(activeCode == ErrorInfo.MOK){
                    msg = "引擎激活成功";
                }else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED){
                    msg = "引擎已经激活无需再次激活";
                }else{
                    msg = "引擎激活失败";
                }
                ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                int res = faceEngine.getActiveFileInfo(MainActivity.this, activeFileInfo);
                if (res == ErrorInfo.MOK) {
                    Log.i("激活信息", activeFileInfo.toString());
                }
                final String finalMsg = msg;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ToastUtils.showShort(finalMsg);
                    }
                });
            }

        }).start();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GetFaceActicity.REQUESTCODE && resultCode == Activity.RESULT_OK) {
            String img = data.getStringExtra(GetFaceActicity.FACEIMGDATA);
            Luban.with(this)
                    .load(img)
                    .ignoreBy(100)
                    .filter(new CompressionPredicate() {
                        @Override
                        public boolean apply(String path) {
                            return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                        }
                    })
                    .setCompressListener(new OnCompressListener() {
                        @Override
                        public void onStart() {
                            Log.e("测试", "开始");
                        }

                        @Override
                        public void onSuccess(File file) {
                            //转换成功后的文件
                            String fileSize = FileUtils.getFileSize(file);
                            mTvSize.setText("文件大小："+fileSize);
                            LogUtils.eTag("压缩后的文件",file.getAbsolutePath());
                            Glide.with(MainActivity.this).load(file).into(mImgFace);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e("测试", "失败");
                        }
                    }).launch();
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.bt_activeengine:
                initPermission();
                break;
            case R.id.bt_findface:
                if(activeCode == ErrorInfo.MOK || activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED){
                    Intent startActivityIntent = new Intent(MainActivity.this, GetFaceActicity.class);
                    startActivityForResult(startActivityIntent, GetFaceActicity.REQUESTCODE);
                }else {
                    ToastUtils.showShort("请先激活引擎");
                }
                break;
        }
    }
}
