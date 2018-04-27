/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zxing.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 邮箱: 1076559197@qq.com | tauchen1990@gmail.com
 * <p/>
 * 作者: 陈涛
 * <p/>
 * 日期: 2014年8月20日
 * <p/>
 * 描述: 该类主要负责设置相机的参数信息，获取最佳的预览界面
 */
public final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    private static final int MIN_PREVIEW_PIXELS = 480 * 320;
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private static final int TEN_DESIRED_ZOOM = 27;
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");


    private final Context context;

    // 屏幕分辨率
    // 相机分辨率
    private Point cameraResolution;

    private Camera.Size mCameraResolution;
    private Camera.Size mPictureResolution;

    public CameraConfigurationManager(Context context) {
        this.context = context;
    }

    public void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(displayMetrics);

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            Log.v(TAG, "support preview size:" + size.width + "," + size.height);
        }
        mCameraResolution = findCloselySize(displayMetrics.widthPixels, displayMetrics.heightPixels,
            parameters.getSupportedPreviewSizes());
        mPictureResolution = findCloselySize(displayMetrics.widthPixels, displayMetrics.heightPixels,
            parameters.getSupportedPictureSizes());
    }


    public void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        Log.i(TAG, "Initial camera parameters: " + parameters.flatten());

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored");
        }

        parameters.setPreviewSize(mCameraResolution.width, mCameraResolution.height);
        parameters.setPictureSize(mPictureResolution.width, mPictureResolution.height);

        //使用新策略，暂时屏蔽老策略
//        setZoom(parameters);
        camera.setParameters(parameters);

//        Camera.Parameters afterParameters = camera.getParameters();
//        Camera.Size afterSize = afterParameters.getPreviewSize();

        /** 设置相机预览为竖屏 */
        camera.setDisplayOrientation(90);
    }

    public Camera.Size getCameraResolution() {
        return mCameraResolution;
    }

    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param surfaceWidth  需要被进行对比的原宽
     * @param surfaceHeight 需要被进行对比的原高
     * @param preSizeList   需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    protected Camera.Size findCloselySize(int surfaceWidth, int surfaceHeight, List<Camera.Size> preSizeList) {
        Collections.sort(preSizeList, new SizeComparator(surfaceWidth, surfaceHeight));
        return preSizeList.get(0);
    }

    private void setZoom(Camera.Parameters parameters) {

        String zoomSupportedString = parameters.get("zoom-supported");
        if (zoomSupportedString != null && !Boolean.parseBoolean(zoomSupportedString)) {
            return;
        }

        int tenDesiredZoom = TEN_DESIRED_ZOOM;

        String maxZoomString = parameters.get("max-zoom");
        if (maxZoomString != null) {
            try {
                int tenMaxZoom = (int) (10.0 * Double.parseDouble(maxZoomString));
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Bad max-zoom: " + maxZoomString);
            }
        }

        String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
        if (takingPictureZoomMaxString != null) {
            try {
                int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                Log.e(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
            }
        }

        String motZoomValuesString = parameters.get("mot-zoom-values");
        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
        }

        String motZoomStepString = parameters.get("mot-zoom-step");
        if (motZoomStepString != null) {
            try {
                double motZoomStep = Double.parseDouble(motZoomStepString.trim());
                int tenZoomStep = (int) (10.0 * motZoomStep);
                if (tenZoomStep > 1) {
                    tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                }
            } catch (NumberFormatException nfe) {
                // continue
            }
        }

        // Set zoom. This helps encourage the user to pull back.
        // Some devices like the Behold have a zoom parameter
        // if (maxZoomString != null || motZoomValuesString != null) {
        // parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
        // }
        if (parameters.isZoomSupported()) {
            Log.e(TAG, "max-zoom:" + parameters.getMaxZoom());
            Log.i("0000", "tenDesiredZoom:" + tenDesiredZoom);
            parameters.setZoom(parameters.getMaxZoom() / tenDesiredZoom);
        } else {
            Log.e(TAG, "Unsupported zoom.");
        }

        // Most devices, like the Hero, appear to expose this zoom parameter.
        // It takes on values like "27" which appears to mean 2.7x zoom
        // if (takingPictureZoomMaxString != null) {
        // parameters.set("taking-picture-zoom", tenDesiredZoom);
        // }
    }

    private static int findBestMotZoomValue(CharSequence stringValues, int tenDesiredZoom) {
        int tenBestValue = 0;
        for (String stringValue : COMMA_PATTERN.split(stringValues)) {
            stringValue = stringValue.trim();
            double value;
            try {
                value = Double.parseDouble(stringValue);
            } catch (NumberFormatException nfe) {
                return tenDesiredZoom;
            }
            int tenValue = (int) (10.0 * value);
            if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
                tenBestValue = tenValue;
            }
        }
        return tenBestValue;
    }


    /**
     * 预览尺寸与给定的宽高尺寸比较器。首先比较宽高的比例，在宽高比相同的情况下，根据宽和高的最小差进行比较。
     */
    private static class SizeComparator implements Comparator<Camera.Size> {

        private final int width;
        private final int height;

        SizeComparator(int width, int height) {
            if (width < height) {
                this.width = height;
                this.height = width;
            } else {
                this.width = width;
                this.height = height;
            }
        }

        @Override
        public int compare(Camera.Size size1, Camera.Size size2) {
            int width1 = size1.width;
            int height1 = size1.height;
            int width2 = size2.width;
            int height2 = size2.height;

            int minGap1 = Math.abs(width - width1) + Math.abs(height - height1);
            int minGap2 = Math.abs(width - width2) + Math.abs(height - height2);
            return minGap1 - minGap2;
        }
    }


}
