package utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;


/**
 * 系统工具类
 * <p>
 * Created by wanny-n1 on 2017/1/3.
 */
public class SystemUtils {
    public static boolean isActivityValid(Activity activity) {
        boolean isActivityValid = false;
        if (activity != null) {
            if (Build.VERSION.SDK_INT >= 17) {
                isActivityValid = !activity.isDestroyed();
            } else {
                isActivityValid = !activity.isFinishing();
            }
        }
        return isActivityValid;
    }

    public static void checkContextValid(Context ctx) throws Exception {
        if (ctx == null) {
            throw new Exception("传入的context对象为空");
        } else if (ctx instanceof Activity && !isActivityValid((Activity) ctx)) {
            throw new Exception("传入的activity实力对象无效");
        }
    }


    public static Display getDisplay(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        return wm.getDefaultDisplay();
    }

    public static DisplayMetrics getDisplayMetrics(Context ctx) {
        return ctx.getResources().getDisplayMetrics();
    }


    /**
     * 判断TextView的内容宽度是否超出其可用宽度
     *
     * @param tv
     * @return
     */
    public static boolean isOverFlowed(TextView tv, int maxWidth) {
        int availableWidth = maxWidth - tv.getPaddingLeft() - tv.getPaddingRight();
        Paint textViewPaint = tv.getPaint();
        float textWidth = textViewPaint.measureText(tv.getText().toString());
        if (textWidth > availableWidth) {
            return true;
        } else {
            return false;
        }
    }

    public static int dp2px(@NonNull Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static int sp2px(@NonNull Context context, int sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     * @param pxValue （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }
}
