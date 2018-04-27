//
// Created by yxt on 2018/4/20.
//

#include <jni.h>
#include "android/log.h"
#include <opencv2/opencv.hpp>
#include <opencv2/highgui.hpp>
#include "opencv_ImagePreProcess.h"

#define TAG "processImg-jni" // 这个是自定义的LOG的标识
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE,TAG ,__VA_ARGS__)

using namespace cv;
using namespace std;

void getCropRect(unsigned char *nv21, int width, int height,
                 unsigned char *dest, int rectLeft, int rectTop, int rectWidth, int rectHeight) {
    Mat imgMat(height * 3 / 2, width, CV_8UC1, nv21);
    Mat rgbMat(height, width, CV_8UC3);
    Mat resultMat(rectHeight * 3 / 2, rectWidth, CV_8UC1, dest);

    cvtColor(imgMat, rgbMat, CV_YUV2RGB_NV21);
    Rect cropRect(rectLeft, rectTop, rectWidth, rectHeight);
    cvtColor(rgbMat(cropRect), resultMat, CV_RGB2YUV_I420);
}

void preProcess(unsigned char *i420, int width, int height, unsigned char *dest) {
    Mat imgMat(height * 3 / 2, width, CV_8UC1, i420);
    Mat binMat(height, width, CV_8UC1);
    Mat innerMat(height, width, CV_8UC1);
    Mat rgbMat(height, width, CV_8UC3);
    Mat resultMat(height * 3 / 2, width, CV_8UC1, dest);

    cvtColor(imgMat, innerMat, CV_YUV2GRAY_NV21);
    medianBlur(innerMat, innerMat, 7);

    int rectWidth = width / 5;
    int rectHeight = height / 5;
    int centerX = width / 2, centerY = height / 2;

    Rect cropLTRect(centerX - rectWidth, centerY - rectHeight, rectWidth, rectHeight);
    Rect cropRTRect(centerX, centerY - rectHeight, rectWidth, rectHeight);
    Rect cropLBRect(centerX - rectWidth, centerY, rectWidth, rectHeight);
    Rect cropRBRect(centerX, centerY, rectWidth, rectHeight);

    double threshLT = threshold(innerMat(cropLTRect), binMat(cropLTRect), 0, 255, CV_THRESH_OTSU);
    double threshRT = threshold(innerMat(cropRTRect), binMat(cropRTRect), 0, 255, CV_THRESH_OTSU);
    double threshLB = threshold(innerMat(cropLBRect), binMat(cropLBRect), 0, 255, CV_THRESH_OTSU);
    double threshRB = threshold(innerMat(cropRBRect), binMat(cropRBRect), 0, 255, CV_THRESH_OTSU);

    LOGV("width:%d, height:%d, centerX:%d, centerY:%d", width, height, centerX, centerY);
    LOGV("threshold:%f,%f,%f,%f", threshLT, threshRT, threshLB, threshRB);

    Rect LTRect(0, 0, centerX, centerY);
    Rect RTRect(centerX - 1, 0, centerX, centerY);
    Rect LBRect(0, centerY - 1, centerX, centerY);
    Rect RBRect(centerX - 1, centerY - 1, centerX, centerY);

    threshold(innerMat(LTRect), innerMat(LTRect), threshLT, 255, CV_THRESH_BINARY);
    threshold(innerMat(RTRect), innerMat(RTRect), threshRT, 255, CV_THRESH_BINARY);
    threshold(innerMat(LBRect), innerMat(LBRect), threshLB, 255, CV_THRESH_BINARY);
    threshold(innerMat(RBRect), innerMat(RBRect), threshRB, 255, CV_THRESH_BINARY);

    cvtColor(innerMat, rgbMat, CV_GRAY2RGB);
    cvtColor(rgbMat, resultMat, CV_RGB2YUV_I420);
}

void I420ToRGBA(unsigned char *yuv, int width, int height, unsigned char *rgba) {
    Mat yuvMat(height * 3 / 2, width, CV_8UC1, yuv);
    Mat rgbaMat(height, width, CV_8UC4, rgba);
    cvtColor(yuvMat, rgbaMat, CV_YUV2RGBA_I420);
}

void nv21ToRGBA(unsigned char *yuv, int width, int height, unsigned char *rgba) {
    Mat yuvMat(height * 3 / 2, width, CV_8UC1, yuv);
    Mat rgbaMat(height, width, CV_8UC4, rgba);
    cvtColor(yuvMat, rgbaMat, CV_YUV2RGBA_NV21);
}

void RGBAToI420(unsigned char *rgba, int width, int height, unsigned char *yuv) {
    Mat rgbaMat(height, width, CV_8UC4, rgba);
    Mat yuvMat(height * 3 / 2, width, CV_8UC1, yuv);
    cvtColor(rgbaMat, yuvMat, CV_RGBA2YUV_I420);
}

void nv21Rotate90(unsigned char *yuv, int width, int height, unsigned char *rgba) {
    Mat yuvMat(height * 3 / 2, width, CV_8UC1, yuv);
    Mat result(height, width, CV_8UC1, rgba);
}

JNIEXPORT void JNICALL Java_opencv_ImagePreProcess_getYUVCropRect
        (JNIEnv *env, jclass thiz, jbyteArray jsrcArray, jint width, jint height,
         jbyteArray jdstArray, jint rectLeft, jint rectTop, jint rectWidth, jint rectHeight) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    getCropRect(srcArray, width, height, dstArray, rectLeft, rectTop, rectWidth, rectHeight);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL Java_opencv_ImagePreProcess_preProcess
        (JNIEnv *env, jclass thiz, jbyteArray jsrcArray, jint width, jint height,
         jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    preProcess(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL Java_opencv_ImagePreProcess_i420ToRGBA
        (JNIEnv *env, jclass thiz, jbyteArray jsrcArray, jint width, jint height,
         jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    I420ToRGBA(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL Java_opencv_ImagePreProcess_nv21ToRGBA
        (JNIEnv *env, jclass thiz, jbyteArray jsrcArray, jint width, jint height,
         jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    nv21ToRGBA(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}

JNIEXPORT void JNICALL Java_opencv_ImagePreProcess_RGBAToI420
        (JNIEnv *env, jclass thiz, jbyteArray jsrcArray, jint width, jint height,
         jbyteArray jdstArray) {
    unsigned char *srcArray = (unsigned char *) env->GetPrimitiveArrayCritical(jsrcArray, 0);
    unsigned char *dstArray = (unsigned char *) env->GetPrimitiveArrayCritical(jdstArray, 0);

    RGBAToI420(srcArray, width, height, dstArray);

    env->ReleasePrimitiveArrayCritical(jsrcArray, srcArray, 0);
    env->ReleasePrimitiveArrayCritical(jdstArray, dstArray, 0);
}
