LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
OpenCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=STATIC
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
#try to load OpenCV.mk from default install location
include /home/totrit/software/installess/OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif
LOCAL_MODULE    := motion_detector
LOCAL_SRC_FILES := src/image_comparor.cpp
LOCAL_LDLIBS := -L/home/totrit/software/installess/OpenCV-2.4.8-android-sdk/sdk/native/libs/armeabi -llog -ldl -lz -lopencv_java
LOCAL_STATIC_LIBRARIES += libopencv_contrib libopencv_legacy libopencv_ml libopencv_stitching libopencv_objdetect libopencv_videostab libopencv_calib3d libopencv_photo libopencv_video libopencv_features2d libopencv_highgui libopencv_androidcamera libopencv_flann libopencv_imgproc libopencv_ts libopencv_core
LOCAL_SHARED_LIBRARIES := opencv_java
include $(BUILD_SHARED_LIBRARY)
include $(LOCAL_PATH)/prebuilt/Android.mk
