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
import com.arcsoft.face.FaceInfo;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.itfitness.getface.R;
import com.itfitness.getface.widget.FaceCameraView;
import com.itfitness.getface.widget.FaceRectView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class GetFaceActicity extends AppCompatActivity {
    private FaceCameraView faceCameraView;
    private FaceRectView faceRectView;
    private String mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;
    private FaceEngine faceEngine;
    public static final String FACEIMGDATA = "faceimgdata";
    public static final int REQUESTCODE = 111;
    private int afCode = -1;
    private boolean isSavaing = false;
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
        faceRectView = findViewById(R.id.facerectview);
    }
    private void initListener() {
        faceCameraView.setPreviewCallback(new FaceCameraView.PreviewCallback() {
            @Override
            public void onPreview(byte[] data, Camera camera) {
                if(faceRectView!=null){
                    faceRectView.clearRect();
                }
                ArrayList<FaceInfo> faceInfoList = new ArrayList();
                int  code = faceEngine.detectFaces(
                        data,//图像数据
                        faceCameraView.getPreviewSize().width,
                        faceCameraView.getPreviewSize().height,
                        FaceEngine.CP_PAF_NV21,
                        faceInfoList
                );
                if (code == ErrorInfo.MOK && faceInfoList.size() > 0) {
                    if(faceInfoList.size()>1){
                        ToastUtils.showShort("请不要多个人出现在屏幕中");
                        return;
                    }
                    CopyOnWriteArrayList<Rect> faceRects = new CopyOnWriteArrayList<>();
                    Rect adjustRect = adjustRect(faceInfoList.get(0).getRect());
                    faceRects.add(adjustRect);
                    faceRectView.setFaceRectDatas(faceRects);
                    saveImage(adjustRect,data,camera);
                }else {
                    ToastUtils.showShort("请将面部放入屏幕内");
                }
            }
        });
    }

    private void saveImage(Rect adjustRect, byte[] data, Camera camera) {
        if(isSavaing){
            return;
        }
        isSavaing = true;
        try {
            Camera.Size size = camera.getParameters().getPreviewSize();
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            if (image != null) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
                Bitmap bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
                Matrix m = new Matrix();
                m.setRotate(-90f, bmp.getWidth() / 2, bmp.getHeight() / 2);
                Bitmap bm = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
                Bitmap cutBitmap = cutBitmap(adjustRect,bm);
                Bitmap scaleBitmap = scaleBitmap(cutBitmap);
                String savePath = mFilePath + System.currentTimeMillis() + "face.png";
                boolean save = ImageUtils.save(
                        scaleBitmap,
                        savePath,
                        Bitmap.CompressFormat.PNG
                );
                if (save) {
                    Intent intent = getIntent();
                    intent.putExtra(FACEIMGDATA, savePath);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } else {
                    isSavaing = false;
                    ToastUtils.showShort("存储失败");
                }
            }
        }catch (Exception e){
            isSavaing = false;
            LogUtils.eTag("错误",e.getMessage());
        }
    }

    private Bitmap scaleBitmap(Bitmap cutBitmap) {
        return Bitmap.createScaledBitmap(cutBitmap,(int)(cutBitmap.getWidth()/1.1),(int)(cutBitmap.getHeight()/1.1),true);
    }

    private Bitmap cutBitmap(Rect adjustRect, Bitmap bm) {
        float scalWidthVal = (float)(bm.getWidth())/(float) (faceRectView.getWidth());
        float scalHeightVal = (float)(bm.getHeight())/(float) (faceRectView.getHeight());
        int x = (int) (adjustRect.right*scalWidthVal);
        int y = (int) (adjustRect.bottom*scalHeightVal);
        int width = (int) (Math.abs(adjustRect.width())*scalWidthVal);
        int height = (int) (Math.abs(adjustRect.height())*scalHeightVal);
        return Bitmap.createBitmap(bm,x,y,width,height);
    }

    private Rect adjustRect(Rect rect){
        Rect justRect = new Rect();
        float scalWidthVal = (float) (faceRectView.getWidth())/(float)(faceCameraView.getPreviewSize().height);
        float scalHeightVal = (float) (faceRectView.getHeight())/(float)(faceCameraView.getPreviewSize().width);
        rect.left = (int) (scalHeightVal*rect.left);
        rect.right = (int) (scalHeightVal*rect.right);
        rect.top = (int) (scalWidthVal*rect.top);
        rect.bottom = (int) (scalWidthVal*rect.bottom);
        justRect.left  = faceRectView.getWidth() - rect.top;
        justRect.right =faceRectView.getWidth() -  rect.bottom;
        justRect.top = faceRectView.getHeight() - rect.left+Math.abs(rect.width()/10);
        justRect.bottom = faceRectView.getHeight() - rect.right-Math.abs(rect.width()/5);
        return justRect;
    }
    /**
     * 初始化人脸识别引擎
     */
    private void initEngine() {
        faceEngine = new FaceEngine();
        //初始化人脸识别引擎
        afCode = faceEngine.init(
                this,
                FaceEngine.ASF_DETECT_MODE_VIDEO,
                FaceEngine.ASF_OP_270_ONLY,
                16,
                20,
                FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_FACE3DANGLE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS
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
