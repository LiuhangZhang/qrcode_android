package opencv;

/**
 * Created by yxt on 2018/4/20.
 */

public class ImagePreProcess {
    static {
        System.loadLibrary("ProcessImg");
    }

    public static native void getYUVCropRect(byte[] src, int width, int height, byte[] dest, int rectLeft, int rectTop, int rectWidth, int rectHeight);

    public static native void preProcess(byte[] src, int width, int height, byte[] dest);

    public static native void i420ToRGBA(byte[] src, int width, int height, byte[] dest);

    public static native void nv21ToRGBA(byte[] src, int width, int height, byte[] dest);

    public static native void RGBAToI420(byte[] src, int width, int height, byte[] dest);
}
