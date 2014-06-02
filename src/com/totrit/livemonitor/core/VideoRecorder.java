package com.totrit.livemonitor.core;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.totrit.livemonitor.R;
import com.totrit.livemonitor.util.CameraManager;
import com.totrit.livemonitor.util.Controller;
import com.totrit.livemonitor.util.Utilities;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class VideoRecorder {
  private static final String LOG_TAG = "VideoRecorder";
  private int mCameraId = 0;
  private PrivateHandler mHandler = new PrivateHandler();
  
  public VideoRecorder(int cameraId) {
    mCameraId = cameraId;
  }
  
  public void startRecord() {
    CameraManager.getInstance().startRecord(mCameraId, getOutputMediaFile(true), false);
    mHandler.startTimerToStopRecord();
  }
  
  public void stopRecord() {
    CameraManager.getInstance().stopRecord(mCameraId);
  }
  
  public void notifyMotionDetected() {
    resumeRecord();
    mHandler.startTimerToStopRecord();
  }
  
  private void pauseRecord() {
    CameraManager.getInstance().stopRecord(mCameraId);
  }
  
  private void resumeRecord() {
    CameraManager.getInstance().startRecord(mCameraId, getOutputMediaFile(true), true);
  }
  
  /**
   * Get an appropriate video file path. Borrowed from http://developer.android.com/guide/topics/media/camera.html#saving-media
   * @param isVideo
   * @return
   */
  @SuppressLint("SimpleDateFormat")
  private static String getOutputMediaFile(boolean isVideo) {
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.
    File mediaStorageDir =
        new File(getSaveRootDir());
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (!mediaStorageDir.exists()) {
      if (!mediaStorageDir.mkdirs()) {
        if (Controller.logEnabled()) {
          Log.d(LOG_TAG, "failed to create directory");
        }
        return null;
      }
    }

    // Create a media file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    File mediaFile;
    if (!isVideo) {
      mediaFile =
          new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
    } else {
      mediaFile =
          new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
    }

    return mediaFile.getPath();
  }
  
  public static String getSaveRootDir() {
    return Utilities.getExternalFSRoot() + "/" + ProcessService.getInstance().getResources().getString(R.string.videos_save_dir);
  }
  
  @SuppressLint("HandlerLeak")
  private class PrivateHandler extends Handler {
    private final static int MSG_DELAYED_PAUSE_IF_NO_PREVIEW_CHANGE = 0;
    
    private static final int PAUSE_AFTER_NO_PREVIEW_CHANGE = 10 * 1000; // 10s
    @Override
    public void handleMessage(Message msg) {
      switch(msg.what) {
        case MSG_DELAYED_PAUSE_IF_NO_PREVIEW_CHANGE:
          pauseRecord();
          break;
      }
    }
    
    public void startTimerToStopRecord() {
      this.removeMessages(MSG_DELAYED_PAUSE_IF_NO_PREVIEW_CHANGE);
      this.sendMessageDelayed(Message.obtain(null, MSG_DELAYED_PAUSE_IF_NO_PREVIEW_CHANGE), PAUSE_AFTER_NO_PREVIEW_CHANGE);
    }
  }
}
