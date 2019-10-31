package com.itfitness.getface.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.itfitness.getface.R;
import com.itfitness.getface.widget.FaceCameraView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;

public class GetFaceActicity extends AppCompatActivity {
    private FaceCameraView faceCameraView;
    private String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private FaceEngine faceEngine;
    public static final String FACEIMGDATA = "faceimgdata";
    public static final int REQUESTCODE = 111;
    private int afCode = -1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_findface);
        initView();
        initEngine();
        initListener();
    }

    private void initView() {
        faceCameraView = findViewById(R.id.facecamera);
    }
    private void initListener() {
        faceCameraView.setPreviewCallback(new FaceCameraView.PreviewCallback() {
            @Override
            public void onPreview(byte[] data, Camera camera) {
                ArrayList faceInfoList = new ArrayList();
                int  code = faceEngine.detectFaces(
                        data,//图像数据
                        faceCameraView.getPreviewSize().width,//图像的宽度，为4的倍数
                        faceCameraView.getPreviewSize().height,//图像的高度，NV21(CP_PAF_NV21)格式为2的倍数；BGR24(CP_PAF_BGR24)、GRAY(CP_PAF_GRAY)、DEPTH_U16(CP_PAF_DEPTH_U16)格式无限制
                        FaceEngine.CP_PAF_NV21,// 图像的颜色空间格式，支持NV21(CP_PAF_NV21)、BGR24(CP_PAF_BGR24)、GRAY(CP_PAF_GRAY)、DEPTH_U16(CP_PAF_DEPTH_U16)
                        faceInfoList//人脸列表，传入后赋值
                );
                if (code == ErrorInfo.MOK && faceInfoList.size() > 0) {
                    try {
                        Camera.Size size = camera.getParameters().getPreviewSize();
                        YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                        if (image != null) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                            Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                            //纠正图像的旋转角度问题
                            Matrix m = new Matrix();
                            m.setRotate(-90f, bmp.getWidth() / 2, bmp.getHeight() / 2);
                            Bitmap bm = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                            String savePath = mFilePath + System.currentTimeMillis() + "face.png";
                            boolean save = ImageUtils.save(
                                    bm,
                                    savePath,
                                    Bitmap.CompressFormat.PNG
                            );
                            if (save) {
                                Intent intent = getIntent();
                                intent.putExtra(FACEIMGDATA, savePath);
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            } else {
                                ToastUtils.showShort("存储失败");
                            }
                        }
                    }catch (Exception e){

                    }
                    if (code != ErrorInfo.MOK) {
                        return;
                    }
                }else {
                    ToastUtils.showShort("识别失败");
                    return;
                }
            }
        });
    }
    /**
     * 初始化人脸识别引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        //初始化人脸识别引擎
        afCode = faceEngine.init(
                this,
                FaceEngine.ASF_DETECT_MODE_VIDEO,//检测模式，支持VIDEO模式(ASF_DETECT_MODE_VIDEO)和IMAGE模式(ASF_DETECT_MODE_IMAGE)
                FaceEngine.ASF_OP_270_ONLY,//人脸检测角度，支持0度(ASF_OP_0_ONLY)，90度(ASF_OP_90_ONLY)，180度(ASF_OP_180_ONLY)，270度(ASF_OP_270_ONLY)，全角度检测(ASF_OP_0_HIGHER_EXT)，建议使用单一指定角度检测，性能比全角度检测更佳，IMAGE模式（ASF_DETECT_MODE_IMAGE）为了提高检测识别率不支持全角度（ASF_OP_0_HIGHER_EXT）检测
                16,//识别的最小人脸比例（图片长边与人脸框长边的比值），在VIDEO模式(ASF_DETECT_MODE_VIDEO)下有效值范围[2，32]，推荐值16；在IMAGE模式(ASF_DETECT_MODE_IMAGE)下有效值范围[2，32]，推荐值30
                20,// 引擎最多能检测出的人脸数，有效值范围[1,50]
                FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS//需要启用的功能组合，可多选
        );
    }
    /**
     * 释放引擎
     */
    private void unInitEngine() {
        if (afCode == 0) {
            afCode = faceEngine.unInit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unInitEngine();
    }
}
