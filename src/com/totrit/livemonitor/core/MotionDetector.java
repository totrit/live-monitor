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
  private SoftReference<Bitmap> mBaseImage = null;
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
  // Buffer that used to store the NV21 image data which will be sent to native and create a native bitmap.
  private byte[] mBufferForCreateBitmapNatively = new byte[sPreviewBufferSize];

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
          YuvImage yuv = (YuvImage) msg.obj;
          Bitmap capturedBitmap = ImageComparor.getInstance().convertYUVImageToBitmap(yuv);
          if (mBaseImage == null && capturedBitmap != null) {
            mBaseImage = new SoftReference<Bitmap>(capturedBitmap);
          } else if (mBaseImage != null && capturedBitmap != null){
            Bitmap base = mBaseImage.get();
            if (base != null && capturedBitmap != null) {
              Path result = ImageComparor.getInstance().compare(base, capturedBitmap, mSensitivity);
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
            } else if (base == null && capturedBitmap != null) {
              mBaseImage = new SoftReference<Bitmap>(capturedBitmap);
            } else {
              if (Controller.logEnabled()) {
                Log.d(LOG_TAG, "cature abnormal occured, base=" + base + ", target=" + capturedBitmap);
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
      if (frameData.length > mBufferForCreateBitmapNatively.length) {
        mBufferForCreateBitmapNatively = null;
        // If the coming data is larger than the buffer, enlarge it by 5/4.
        mBufferForCreateBitmapNatively = new byte[frameData.length * 5 / 4];
      }
      System.arraycopy(mBufferForCreateBitmapNatively, 0, frameData, 0, frameData.length);
      // Make sure that the messages and decoded images will not pile.
      mHandler.removeMessages(PrivateHandler.MSG_CAPTURE_DONE);
      mHandler.sendMessage(Message.obtain(null, PrivateHandler.MSG_CAPTURE_DONE, frameData.length, 0));
    }

  };

  public void destroy() {
    mStoped = true;
    mHandler = null;
    mBaseImage = null;
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
