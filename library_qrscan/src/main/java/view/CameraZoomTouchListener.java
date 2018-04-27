package view;

import android.view.MotionEvent;
import android.view.View;

import zxing.camera.CameraZoomStrategy;

/**
 * @author lh
 * @since 2018/4/18.
 */

public class CameraZoomTouchListener implements View.OnTouchListener {
    private float startDis;
    private int startZoom;

    /**
     * 计算两个手指间的距离
     */
    private float distance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                startDis = distance(event);
                startZoom = CameraZoomStrategy.getInstance().getCurrentZoom();
                break;
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() < 2) {
                    break;
                }
                float endDis = distance(event);
                int scale = (int) ((endDis - startDis) / 20f);
                CameraZoomStrategy.getInstance().setTouchZoom(startZoom + scale);
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        return true;
    }
}
