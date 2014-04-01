package com.totrit.livemonitor.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;

import com.totrit.livemonitor.MainActivity;
import com.totrit.livemonitor.util.CameraManager;
import com.totrit.livemonitor.util.Controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Handler;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
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
	private long mLastDetectTime = System.currentTimeMillis();
	private boolean mStoped = false;
	private byte[] mDetectionBuffer = new byte[2 * 1024 * 1024];
	
	public MotionDetector(int cameraId, int sensitivity, Messenger callbackMessenger) {
		mCameraId = cameraId;
		mSensitivity = sensitivity;
		mMessengerToService = callbackMessenger;
		registerCallbacks();
	}
	
	private void registerCallbacks() {
		CameraManager.getInstance().registerPreviewCallback(mCameraId, mPreviewCallback, null);
	}
	
	private class PrivateHandler extends Handler {
		private final static int MSG_CAPTURE_DONE = 1;
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_CAPTURE_DONE:
				Bitmap target = (Bitmap)msg.obj;
				if (mBaseImage == null && target != null) {
					mBaseImage = new SoftReference<Bitmap>(target);
				} else {
					Bitmap base = mBaseImage.get();
					if (base != null && target != null) {
						Path result = ImageComparor.getInstance().compare(base, target, mSensitivity);
						if (result != null) {
							if (Controller.logEnabled()) {
								Log.d(LOG_TAG, "motion detected by Motion Detector, send message to service.");
							}
							Message msgToService = Message.obtain(null, ProcessService.MSG_MOTION_DETECTED, result);
							try {
								mMessengerToService.send(msgToService);
							} catch (RemoteException e) {
								if (Controller.logEnabled()) {
									Log.d(LOG_TAG, "error occured when sending message to service from motion-detector.");
								}
								e.printStackTrace();
							}
						}
					} else if (base == null && target != null){
						mBaseImage = new SoftReference<Bitmap>(target);
					} else {
						if (Controller.logEnabled()) {
							Log.d(LOG_TAG, "cature abnormal occured, base=" + base + ", target=" + target);
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
			if (System.currentTimeMillis() - mLastDetectTime < TIME_MILLIS_DETECT_SPAN) {
				return;
			}
			final Size previewSize = cameraObj.getParameters().getPreviewSize();
			final YuvImage image = new YuvImage(frameData, ImageFormat.NV21, previewSize.width, previewSize.height, null);
			try {
				image.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 90, new FileOutputStream(new File("/sdcard/test.png")));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	};
	
	public void destroy() {
		mStoped = true;
		mHandler = null;
		mBaseImage = null;
		mMessengerToService = null;
	}
	
	private boolean saveBitmapToDisk(Bitmap bitmap, String path) {
		try {
			bitmap.compress(CompressFormat.PNG, 90, new FileOutputStream(new File(path)));
			return true;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
}
