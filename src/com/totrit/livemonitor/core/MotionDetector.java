package com.totrit.livemonitor.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.util.LinkedList;

import com.totrit.livemonitor.util.CameraManager;
import com.totrit.livemonitor.util.Controller;
import com.totrit.livemonitor.util.ImageComparor;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.ImageFormat;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class MotionDetector {
  private static final String LOG_TAG = "MotionDetector";

  private Handler mHandler = new PrivateHandler();
  private final static int INVALID_IMAGE_HANDLE = -1;
  private int mBaseImageHandle = INVALID_IMAGE_HANDLE;
  private int mSensitivity = 0;
  private Messenger mMessengerToService = null;
  private int mCameraId = -1;
  private final int TIME_MILLIS_DETECT_SPAN = 1 * 1000;
  private long mLastDetectTime = timeMillis();
  private boolean mStoped = false;
  // Buffers stuff that are used for Camera.PreviewCallback.
  private LinkedList<byte[]> mPreviewBuffers = null;
  private final static int sPreviewBufferNum = 2;
  private final static int sPreviewBufferSize = 2 * 1024 * 1024;

  public MotionDetector(int cameraId, int sensitivity, Messenger callbackMessenger) {
    mCameraId = cameraId;
    mSensitivity = sensitivity;
    mMessengerToService = callbackMessenger;
    registerCallbacks();
  }

  private void registerCallbacks() {
    for (int i = 0; i < sPreviewBufferNum; i ++) {
      try {
        byte[] buffer = new byte[sPreviewBufferSize];
        if (mPreviewBuffers == null) {
          mPreviewBuffers = new LinkedList<byte[]>();
        }
        mPreviewBuffers.add(buffer);
      } catch (OutOfMemoryError e) {
        break;
      }
    }
    CameraManager.getInstance().registerPreviewCallback(mCameraId, mPreviewCallback, mPreviewBuffers);
  }

  @SuppressLint("HandlerLeak")
  private class PrivateHandler extends Handler {
    private final static int MSG_CAPTURE_DONE = 1;

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_CAPTURE_DONE:
          int capturedImageNativeHandle = msg.arg1;
          if (mBaseImageHandle == INVALID_IMAGE_HANDLE && capturedImageNativeHandle != INVALID_IMAGE_HANDLE) {
            mBaseImageHandle = capturedImageNativeHandle;
          } else if (mBaseImageHandle != INVALID_IMAGE_HANDLE && capturedImageNativeHandle != INVALID_IMAGE_HANDLE){
            Rect result = ImageComparor.getInstance().compare(mBaseImageHandle, capturedImageNativeHandle, mSensitivity);
            //TODO
            ImageComparor.getInstance().releaseLocalImage(capturedImageNativeHandle);
            if (result != null) {
              if (Controller.logEnabled()) {
                Log.d(LOG_TAG, "motion detected by Motion Detector, send message to service.");
              }
              Message msgToService =
                  Message.obtain(null, ProcessService.MSG_MOTION_DETECTED, result);
              try {
                mMessengerToService.send(msgToService);
              } catch (RemoteException e) {
                if (Controller.logEnabled()) {
                  Log.d(LOG_TAG,
                      "error occured when sending message to service from motion-detector.");
                }
                e.printStackTrace();
              }
            }
          }
          break;
      }
    }
  }

  private Camera.PreviewCallback mPreviewCallback = new PreviewCallback() {

    @Override
    public void onPreviewFrame(byte[] frameData, Camera cameraObj) {
      if (mStoped) {
        cameraObj.setPreviewCallbackWithBuffer(null);
        return;
      }
      long currentTime = timeMillis();
      if (currentTime - mLastDetectTime < TIME_MILLIS_DETECT_SPAN) {
        return;
      } else {
        mLastDetectTime = currentTime;
      }
      Size previewSize = CameraManager.getInstance().getPreviewSize(mCameraId);
      int newHandle = ImageComparor.getInstance().convertYUVImageToLocalImage(frameData, frameData.length, previewSize.width, previewSize.height);
      // Make sure that the messages and decoded images will not pile.
      mHandler.removeMessages(PrivateHandler.MSG_CAPTURE_DONE);
      mHandler.sendMessage(Message.obtain(null, PrivateHandler.MSG_CAPTURE_DONE, newHandle, 0));
    }
    
  };

  public void destroy() {
    mStoped = true;
    mHandler = null;
    //TODO: If other handle maintained, release them also.
    if (mBaseImageHandle != INVALID_IMAGE_HANDLE) {
      ImageComparor.getInstance().releaseLocalImage(mBaseImageHandle);
    }
    mBaseImageHandle = INVALID_IMAGE_HANDLE;
    mMessengerToService = null;
  }

  @SuppressWarnings("unused")
  private boolean saveBitmapToDisk(Bitmap bitmap, String path) {
    try {
      bitmap.compress(CompressFormat.PNG, 90, new FileOutputStream(new File(path)));
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  /**
   * This function get the time since boot, and it's NOT GMT time.
   * @return relative time in milli-seconds.
   */
  private static long timeMillis() {
    return (long) (System.nanoTime() * 0.000001);
  }

}
