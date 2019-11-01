package com.itfitness.getface.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.blankj.utilcode.util.LogUtils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FaceRectView extends View {
    private CopyOnWriteArrayList<Rect> faceRectList = new CopyOnWriteArrayList<>();
    private Paint mPaint;
    public FaceRectView(Context context) {
        this(context,null);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs,0);
    }

    public FaceRectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faceRectList.size() > 0) {
            for (Rect rect:faceRectList
                 ) {
                drawFaceRect(rect,canvas);
            }

        }
    }
    /**
     * 绘制人脸矩形
     */
    private void drawFaceRect(Rect rect, Canvas canvas) {
//        LogUtils.eTag("矩形",rect.left+"===="+rect.top+"===="+rect.right+"====="+rect.bottom);
        canvas.drawRect(rect, mPaint);
    }
    /**
     * 清空人脸矩形
     */
    public void clearRect() {
        faceRectList.clear();
        postInvalidate();
    }

    /**
     * 设置人脸绘制数据
     */
    public void setFaceRectDatas(CopyOnWriteArrayList<Rect> datas) {
        faceRectList = datas;
        postInvalidate();
    }
}
