package com.totrit.livemonitor.core;

import android.graphics.Bitmap;
import android.graphics.Path;

public class ImageComparor {
	private static ImageComparor mInstance = null;
	
	public static ImageComparor getInstance() {
		if (mInstance == null) {
			return mInstance = new ImageComparor();
		} else {
			return mInstance;
		}
	}
	
	public Path compare(Bitmap base, Bitmap target, int sensitivity) {
		return null;
	}
}
