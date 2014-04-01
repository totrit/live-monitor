package com.totrit.livemonitor.util;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

public class CameraManager {
	private final static String LOG_TAG = "CameraManager";
	private static CameraManager mInstance = null;
	private static final int mCameraQuality = 90;
	
	private Hashtable<Integer, Camera> mCameras = new Hashtable<Integer, Camera>();
	
	public static CameraManager getInstance() {
		if (mInstance == null) {
			return mInstance = new CameraManager();
		} else {
			return mInstance;
		}
	}
	
	public void startPreview(SurfaceHolder holder, int cameraId, int rotation, int width, int height) {
		startCamera(cameraId);
		Camera camera = mCameras.get(cameraId);
		try {
			assert(holder != null);
			camera.setPreviewDisplay(holder);
			// Set Width and Height.
			Camera.Size previewSize = getOptimalPreviewSize(cameraId, width, height);
			if (Controller.logEnabled()) {
				Log.d(LOG_TAG, "optimal preview and picture size: " + previewSize.width + ", " + previewSize.height);
			}
			assert(previewSize != null);
			Camera.Parameters parameters = camera.getParameters();
			parameters.setPreviewSize(previewSize.width, previewSize.height);
			parameters.setPreviewFormat(ImageFormat.NV21);
			// Set parameters for snapshot.
			parameters.setPictureFormat(ImageFormat.JPEG);
			parameters.setPictureSize(previewSize.width, previewSize.height);
			parameters.setJpegQuality(mCameraQuality);
			// Set Orientation.
			int cameraRotation = getOptimalRotation(rotation, cameraId);
			parameters.setRotation(cameraRotation);
			camera.setDisplayOrientation(cameraRotation);
			// Set into the parameters.
			camera.setParameters(parameters);
			// Start it.
			camera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}
	
	public boolean registerPreviewCallback(int cameraId, Camera.PreviewCallback cb, byte[] buff) {
		startCamera(cameraId);
		Camera camera = mCameras.get(cameraId);
		//camera.stopPreview();
		if (buff == null) {
			camera.setPreviewCallback(cb);
		} else {
			camera.addCallbackBuffer(buff);
			camera.setPreviewCallbackWithBuffer(cb);
		}
		//camera.startPreview();
		return true;
	}
	
	public boolean takePicture(int cameraId, Camera.PictureCallback callback) {
		Camera camera = mCameras.get(cameraId);
		if (camera != null) {
			// Just in case that the camera was released.
			try {
				camera.takePicture(null, null, callback);
			} catch (Throwable t) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}
	
	public boolean resumePreviewAfterSnapshot(int cameraId) {
		Camera camera = mCameras.get(cameraId);
		if (camera != null) {
			camera.startPreview();
			return true;
		} else {
			return false;
		}
	}
	
	public void stopPreview(int cameraId) {
		Camera camera = mCameras.get(cameraId);
		if (camera != null) {
			camera.stopPreview();
		}
	}
	
	public void releaseCarema(int cameraId) {
		Camera camera = mCameras.get(cameraId);
		if (camera != null) {
			camera.release();
			camera = null;
		}
	}
	
	private boolean startCamera(int cameraId) {
		Camera camera = mCameras.get(cameraId);
		if (camera != null) {
			return true;
		}
		camera = Camera.open();
		if (camera == null) {
			return false;
		} else {
			mCameras.put(cameraId, camera);
			return true;
		}
	}
	
	private int getOptimalRotation(int rotation, int cameraId) {
		Camera camera = mCameras.get(cameraId);
		if (camera == null) {
			if (Controller.logEnabled()) {
				Log.d(LOG_TAG, "camera not initialized, can not set camera Display Orientation.");
			}
			return 0;
		}
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		if (Controller.logEnabled()) {
			Log.d(LOG_TAG, "optimal rotation, camera's rotation is " + info.orientation + ", view's degree is " + degrees + ", result is " + result);
		}
		return result;
	}

	/**
	 * Because the Preview Size cannot be an arbitrary tuple of value, it has to be selected from the supported options.
	 * @param w
	 * @param h
	 * @return
	 */
	private Camera.Size getOptimalPreviewSize(int cameraId, int w, int h) {
		Camera camera = mCameras.get(cameraId);
		if (camera == null) {
			if (Controller.logEnabled()) {
				Log.d(LOG_TAG, "camera not initialized, can not set Preview size.");
			}
			return null;
		}
		
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
		if (Controller.logEnabled()) {
			String log = "";
			for (int i = 0, listSize = sizes.size(); i < listSize; i ++) {
				log += "(" + sizes.get(i).width + ", " + sizes.get(i).height + "), ";
			}
			Log.d(LOG_TAG, log);
		}
		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

}
