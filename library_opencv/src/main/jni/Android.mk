LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


OpenCV_INSTALL_MODULES:=on
OPENCV_CAMERA_MODULES:=off


OPENCV_LIB_TYPE:=STATIC


ifeq ("$(wildcard $(OPENCV_MK_PATH))","")
#include /Users/yxt/Downloads/opencv-3.0.0/sdk/native/jni/OpenCV.mk
include /Users/lh/Downloads/OpenCV-android-sdk/sdk/native/jni/OpenCV.mk
else
include $(OPENCV_MK_PATH)
endif


LOCAL_MODULE    := ProcessImg
LOCAL_SRC_FILES := image_pre_process.cpp
LOCAL_LDLIBS    += -lm -llog

include $(BUILD_SHARED_LIBRARY)