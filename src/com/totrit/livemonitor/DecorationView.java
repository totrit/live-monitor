package com.totrit.livemonitor;

import com.totrit.livemonitor.util.CameraManager;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.Surface;
import android.view.View;

public class DecorationView extends View {
  private final static String LOG_TAG = "DecorationView";
  //TODO
  private final int mAttachedCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
  private RectDrawer mDrawer = new RectDrawer();

  public DecorationView(Context context) {
    super(context);
  }
  
  
  @Override
  protected void onDraw(Canvas canvas){
//    super.onDraw(canvas);
    mDrawer.draw(canvas);
  }

  public void setAdditionalDrawingRect(Rect rect) {
    mDrawer.setRect(rect);
  }
  
  private class RectDrawer {
    private Rect mAdditionalRectToDraw = null;
    private Paint mPaint = null;
    private Matrix mMatrix = null;
    
    public RectDrawer() {
      mPaint = new Paint();
      mPaint.setAntiAlias(true);
      mPaint.setColor(Color.RED);
      mPaint.setStyle(Style.STROKE);
      mMatrix = new Matrix();
    }
    
    public void setRect(Rect rect) {
      mAdditionalRectToDraw = rect;
      mMatrix.reset();
      int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
      Size previewSize = CameraManager.getInstance().getPreviewSize(mAttachedCameraId);
      float hScale = 1.0f, vScale = 1.0f;
      switch (rotation) {
        case Surface.ROTATION_0:
          hScale = (float)DecorationView.this.getWidth() / (float)previewSize.height;
          vScale = (float)DecorationView.this.getHeight() / (float)previewSize.width;
          mMatrix.postScale(hScale, vScale);
          mMatrix.postRotate(90);
          mMatrix.postTranslate(DecorationView.this.getWidth(), 0);
          break;
        case Surface.ROTATION_90:
          hScale = (float)DecorationView.this.getWidth() / (float)previewSize.width;
          vScale = (float)DecorationView.this.getHeight() / (float)previewSize.height;
          mMatrix.postScale(hScale, vScale);
          break;
        case Surface.ROTATION_180:
          hScale = (float)DecorationView.this.getWidth() / (float)previewSize.height;
          vScale = (float)DecorationView.this.getHeight() / (float)previewSize.width;
          mMatrix.postScale(hScale, vScale);
          mMatrix.postRotate(-90);
          mMatrix.postTranslate(0, DecorationView.this.getHeight());
          break;
        case Surface.ROTATION_270:
          hScale = (float)DecorationView.this.getWidth() / (float)previewSize.width;
          vScale = (float)DecorationView.this.getHeight() / (float)previewSize.height;
          mMatrix.postScale(hScale, vScale);
          mMatrix.postRotate(180);
          mMatrix.postTranslate(DecorationView.this.getWidth(), DecorationView.this.getHeight());
          break;
      }
//      Log.d(LOG_TAG, "calculate scale = (" + hScale + ", " + vScale + "), rotation = " + degrees);
      Log.d(LOG_TAG, "matrix after set: " + mMatrix);
    }
    
    public void draw(Canvas canvas) {
      if (mAdditionalRectToDraw == null) {
        return;
      }
      canvas.save();
      canvas.concat(mMatrix);
      canvas.drawRect(mAdditionalRectToDraw, mPaint);
      Log.d(LOG_TAG, "RectDrawer is drawing rect: " + mAdditionalRectToDraw + ", with matrix " + mMatrix);
      canvas.restore();
    }
  }
}
