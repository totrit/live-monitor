LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
OpenCV_INSTALL_MODULES:=on
OPENCV_LIB_TYPE:=SHARED
ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
#try to load OpenCV.mk from default install location
include /home/totrit/software/installess/OpenCV-2.4.8-android-sdk/sdk/native/jni/OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif
LOCAL_MODULE    := motion_detector
LOCAL_SRC_FILES := src/image_comparor.cpp
LOCAL_LDLIBS := -L/home/totrit/software/installess/OpenCV-2.4.8-android-sdk/sdk/native/libs/armeabi
include $(BUILD_SHARED_LIBRARY)
