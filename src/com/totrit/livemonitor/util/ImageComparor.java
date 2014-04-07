package com.totrit.livemonitor.util;

import org.opencv.android.OpenCVLoader;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.YuvImage;
import android.util.Log;

public class ImageComparor {
  private final static String LOG_TAG = "ImageComparor";
  private static ImageComparor mInstance = null;

  public static ImageComparor getInstance() {
    if (mInstance == null) {
      return mInstance = new ImageComparor();
    } else {
      return mInstance;
    }
  }
  
  static {
      if (!OpenCVLoader.initDebug()) {
        Log.e(LOG_TAG, "OpenCV load not successfully");
    }
  }

  public Path compare(Bitmap base, Bitmap target, int sensitivity) {
    return null;
  }
  
  public Bitmap convertYUVImageToBitmap(YuvImage yuv) {
//    if (mInstance == null) {
//      if (Controller.logEnabled()) {
//        Log.e(LOG_TAG, "calling convertYUVImageToBitmap without initializing an ImageComparor object.");
//      }
//      return null;
//    }
//    return mInstance.nativeConvertYUVImageToBitmap(yuv);
    return null;
  }
  
  private native Bitmap nativeConvertYUVImageToBitmap(YuvImage yuv);
}
