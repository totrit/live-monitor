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
import android.graphics.Path;
import android.os.Handler;
import android.hardware.Camera;
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
	private final int mDetectTimeSpanMillis = 1 * 1000;
	private boolean mStoped = false;
	
	public MotionDetector(int cameraId, int sensitivity, Messenger callbackMessenger) {
		mCameraId = cameraId;
		mSensitivity = sensitivity;
		mMessengerToService = callbackMessenger;
		scheduleSnapshot();
	}
	
	private class PrivateHandler extends Handler {
		private final static int MSG_SNAPSHOT = 0;
		private final static int MSG_CAPTURE_DONE = 1;
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case MSG_SNAPSHOT:
				snapshot();
				break;
				
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
				scheduleSnapshot();
				break;
			}
		}
	}
	
	private Camera.PictureCallback mSnapshotCallback = new Camera.PictureCallback() {
		
		private int mPictureNum = 0;
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO convert data to bitmap.
			Bitmap bitmap = null;
	        try {
	            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length );
	            // TODO for test.
	            if (mPictureNum == 0) {
	            	saveBitmapToDisk(bitmap, Environment.getExternalStorageDirectory().getPath() + "/test.png");
	            	mPictureNum ++;
	            }
	            if (Controller.logEnabled()) {
	            	Log.d(LOG_TAG, "snapshot succeeded, with data size: " + data.length);
	            }
	            CameraManager.getInstance().resumePreviewAfterSnapshot(mCameraId);
	        } catch (OutOfMemoryError e) {
	        	if (Controller.logEnabled()) {
	        		Log.d(LOG_TAG, "Out of memorry happened.");
	        	}
	            scheduleSnapshot();
	            CameraManager.getInstance().resumePreviewAfterSnapshot(mCameraId);
	            return;
	        }
	        data = null;
			mHandler.sendMessage(Message.obtain(null, PrivateHandler.MSG_CAPTURE_DONE, bitmap));
		}
	};
	
	private void scheduleSnapshot() {
		if (mStoped) {
			return;
		}
		mHandler.sendEmptyMessageDelayed(PrivateHandler.MSG_SNAPSHOT, mDetectTimeSpanMillis);
	}
	
	public void destroy() {
		mStoped = true;
		mHandler = null;
		mBaseImage = null;
		mMessengerToService = null;
	}
	
	private boolean snapshot() {
		return CameraManager.getInstance().takePicture(mCameraId, mSnapshotCallback);
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
