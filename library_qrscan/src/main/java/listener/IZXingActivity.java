package listener;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import com.google.zxing.Result;
import zxing.camera.CameraManager;


/**
 * Created by ryan on 18/4/4.
 */

public interface IZXingActivity
{
    void handleDecode(Result result);

    CameraManager getCameraManager();

    Rect getCropRect();

    Activity getActivity();

    Handler getHandler();
}
