package zxing.camera;


import android.content.Context;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import com.google.zxing.ResultPoint;


/**
 * @author lh
 * @since 2018/4/10.
 */

public class CameraZoomStrategy {
    private static final String TAG = CameraZoomStrategy.class.getSimpleName();
    private static final int MSG_ZOOM_UPDATE = 1;
    private static volatile CameraZoomStrategy instance;
    private boolean hasInit = false;
    private boolean isSupportZoom = false;
    private int maxZoom = 0;
    private int currentZoom = 0;
    private int lastZoom = 0;

    private Camera camera = null;

    //当检测不到定位点时, 尝试放大缩小，看看能否找到定位点
    private boolean hasTouchZoom = false;
    private boolean isTryZoomIn = true;
    private int maxTryZoomIn = 0;
    private int minTryZoomOut = 0;
    private int zoomStep = 8;

    private long lastFindPointTs = 0;
    private long lastNoPointUpdateZoomTs = 0;
    private long lastFindPointUpdateZoomTs = 0;

    private ResultPoint lastFindPoint = null;
    private int screenWidth = 0;

    private InnerHandler handler;

    public static CameraZoomStrategy getInstance() {
        if (instance == null) {
            synchronized (CameraZoomStrategy.class) {
                if (instance == null) {
                    instance = new CameraZoomStrategy();
                }
            }
        }
        return instance;
    }

    private class InnerHandler extends Handler {
        private InnerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ZOOM_UPDATE:
                    removeMessages(MSG_ZOOM_UPDATE);
                    if (camera == null || lastZoom == currentZoom || hasTouchZoom) {
                        return;
                    }
                    try {
                        Camera.Parameters parameters = camera.getParameters();
                        if (parameters == null) {
                            return;
                        }
                        if (lastZoom < currentZoom) {
                            lastZoom++;
                        } else {
                            lastZoom--;
                        }

                        parameters.setZoom(lastZoom);
                        camera.setParameters(parameters);

                        if (lastZoom != currentZoom) {
                            removeMessages(MSG_ZOOM_UPDATE);
                            sendEmptyMessageDelayed(MSG_ZOOM_UPDATE, 30);
                        }
                    } catch (Exception e) {
                        removeMessages(MSG_ZOOM_UPDATE);
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private void clearState() {
        hasInit = false;
        isSupportZoom = false;
        maxZoom = 0;
        currentZoom = 0;
        lastZoom = 0;

        isTryZoomIn = true;
        maxTryZoomIn = 0;
        minTryZoomOut = 0;

        lastFindPoint = null;
    }

    //初始化，设置默认zoom参数
    public void init(Camera camera, Context context) {
        clearState();
        if (camera == null || context == null) {
            return;
        }
        handler = new InnerHandler(context.getMainLooper());
        this.camera = camera;

        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null) {
            return;
        }

        isSupportZoom = parameters.isZoomSupported();
        maxZoom = parameters.getMaxZoom();
        minTryZoomOut = maxZoom / 10;
        maxTryZoomIn = maxZoom * 5 / 10;

        currentZoom = minTryZoomOut;

        Log.v(TAG, "isSupportZoom:" + isSupportZoom +
            ", maxZoom:" + maxZoom + ", currentZoom:" + currentZoom
            + ", maxTryZoomIn:" + maxTryZoomIn + ", minTryZoomOut:" + minTryZoomOut);
        //降低maxZoom对应值，摄像头一旦放到最大，会出现抖动
        maxZoom = maxZoom * 9 / 10;

        if (isSupportZoom) {
            parameters.setZoom(currentZoom);
            lastZoom = currentZoom;
            camera.setParameters(parameters);
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;

        hasInit = true;
    }

    private void updateZoom() {
        if (!hasInit || !isSupportZoom || hasTouchZoom | camera == null || lastZoom == currentZoom) {
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        if (parameters == null) {
            return;
        }

        currentZoom = Math.min(maxZoom, currentZoom);
        currentZoom = Math.max(0, currentZoom);

        handler.removeMessages(MSG_ZOOM_UPDATE);
        handler.sendEmptyMessage(MSG_ZOOM_UPDATE);
    }

    //找到可能定位点
    public void findPossiblePoint(ResultPoint currentFindPoint) {
//        int pointDistance = 0;
//        if (lastFindPoint != null && (System.currentTimeMillis() - lastFindPointTs < 20)) {
//            //短时间内发现的两个点，计算两点距离
//            pointDistance = (int) Math.sqrt(Math.pow(lastFindPoint.getX() - currentFindPoint.getX(), 2) +
//                Math.pow(lastFindPoint.getY() - currentFindPoint.getY(), 2));
//
//            if (screenWidth > 0) {
//                if (pointDistance < screenWidth * 1 / 4) {
//                    currentZoom += 12;
//                } else if (pointDistance < screenWidth * 1 / 3) {
//                    currentZoom += 8;
//                } else if (pointDistance < screenWidth * 2 / 3) {
//                    currentZoom += 4;
//                } else if (pointDistance < screenWidth * 4 / 5) {
//                    currentZoom += 2;
//                } else {
//                    currentZoom -= 4;
//                }
//
//                //控制zoom的时间，不能zoom太频繁
//                if (System.currentTimeMillis() - lastFindPointUpdateZoomTs < 2000) {
//                    lastFindPoint = currentFindPoint;
//                    lastFindPointTs = System.currentTimeMillis();
//                    return;
//                }
//                lastFindPointUpdateZoomTs = System.currentTimeMillis();
//                updateZoom();
//            }
//        }
//        lastFindPoint = currentFindPoint;
//        lastFindPointTs = System.currentTimeMillis();
    }

    //一个定位点都没有找到,尝试放大缩小摄像头，看能否找到
    public void findNoPoint() {
//        if (System.currentTimeMillis() - lastNoPointUpdateZoomTs < 2000 ||
//            System.currentTimeMillis() - lastFindPointTs < 5000) {
//            return;
//        }
//        Log.v(TAG, "find no point");
//
//        if (isTryZoomIn) {
//            if (currentZoom + zoomStep >= maxTryZoomIn) {
//                currentZoom = maxTryZoomIn;
//                isTryZoomIn = false;
//            } else {
//                currentZoom += zoomStep;
//            }
//        } else {
//            if (currentZoom - zoomStep <= minTryZoomOut) {
//                currentZoom = minTryZoomOut;
//                isTryZoomIn = true;
//            } else {
//                currentZoom -= zoomStep;
//            }
//        }
//
//        lastNoPointUpdateZoomTs = System.currentTimeMillis();
//        updateZoom();
    }

    public int getCurrentZoom() {
        return currentZoom;
    }

    public void setTouchZoom(int value) {
        hasTouchZoom = true;
        currentZoom = value;
        currentZoom = Math.min(maxZoom, currentZoom);
        currentZoom = Math.max(0, currentZoom);
        lastZoom = currentZoom;
        try {
            Camera.Parameters parameters = camera.getParameters();
            if (parameters == null) {
                return;
            }
            parameters.setZoom(lastZoom);
            camera.setParameters(parameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
