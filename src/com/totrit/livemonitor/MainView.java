package com.totrit.livemonitor;

import com.totrit.livemonitor.util.CameraManager;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainView extends SurfaceView {
  private final static String LOG_TAG = "MainView";
  private final int mAttachedCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

  public MainView(Context context) {
    super(context);
    this.getHolder().addCallback(mSurfaceCallback);
  }
  
  public MainView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.getHolder().addCallback(mSurfaceCallback);
  }
  
  public MainView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.getHolder().addCallback(mSurfaceCallback);
  }

  SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
      CameraManager.getInstance().stopPreview(mAttachedCameraId);
      CameraManager.getInstance().releaseCarema(mAttachedCameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
      // CameraManager.getInstance().startPreview(getContext(), holder, getWidth(), getHeight());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      // TODO
      int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
      CameraManager.getInstance().startPreview(holder, mAttachedCameraId, rotation, width, height);
    }

  };

  public void onConfigurationChanged(Configuration newConfig) {
    int rotation = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation();
    CameraManager.getInstance().startPreview(getHolder(), mAttachedCameraId, rotation, getWidth(),
        getHeight());
  }

}
