package zxing.decode;

import android.graphics.Rect;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.Map;

import opencv.ImagePreProcess;
import zbar.ZbarController;
import zxing.camera.CameraZoomStrategy;

/**
 * @author lh
 * @since 2018/4/23.
 */

public class DecodeCore {
    private static final String TAG = DecodeCore.class.getSimpleName();

    private final MultiFormatReader multiFormatReader;

    public static boolean isDebugMode = true;
    public static byte[] lastPreviewData;
    public static byte[] lastPreProcessData;
    public static int lastPreProcessWidth;
    public static int lastPreProcessHeight;

    public DecodeCore(Map<DecodeHintType, Object> hints) {
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(hints);
    }

    private byte[] getMatrix(byte[] src, int oldWidth, int oldHeight, Rect rect) {
        byte[] matrix = new byte[rect.width() * rect.height() * 3 / 2];
        ImagePreProcess.getYUVCropRect(src, oldWidth, oldHeight, matrix, rect.left, rect.top, rect.width(), rect.height());
        return matrix;
    }

    private static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth)
                    + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    public Result decode(byte[] data, int width, int height, Rect cropRect, boolean needRotate90) {
        // 这里需要将获取的data翻转一下，因为相机默认拿的的横屏的数据
        byte[] rotatedData = needRotate90 ? rotateYUV420Degree90(data, width, height) : data;

        if (needRotate90) {
            // 宽高也要调整
            int tmp = width;
            width = height;
            height = tmp;
        }

        //截取scan的选择框中数据
        byte[] processSrc = rotatedData;
        int processWidth = width;
        int processHeight = height;
        if (cropRect != null) {
            processWidth = cropRect.width();
            processHeight = cropRect.height();

            if (processWidth % 6 != 0) {
                processWidth -= processWidth % 6;

                cropRect.right = cropRect.left + processWidth;
            }
            if (processHeight % 6 != 0) {
                processHeight -= processHeight % 6;
                cropRect.bottom = cropRect.top + processHeight;
            }

            processSrc = getMatrix(rotatedData, width, height, cropRect);
        }

        Result rawResult = null;

        //1.使用zxing
        PlanarYUVLuminanceSource source = buildLuminanceSource(processSrc, processWidth, processHeight);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));//new GlobalHistogramBinarizer(source)
        try {
            rawResult = multiFormatReader.decodeWithState(bitmap);
            Log.v(TAG, "zxing success");
        } catch (Exception re) {
            if (re instanceof NotFoundException) {
                CameraZoomStrategy.getInstance().findNoPoint();
            }
        } finally {
            multiFormatReader.reset();
        }

        //2.使用zbar
        if (rawResult == null) {
            String zbarResult = ZbarController.getInstance().scan(processSrc, processWidth, processHeight);
            if (zbarResult != null) {
                rawResult = new Result(zbarResult, null, null, null);
                Log.v(TAG, "zbar success");
            }
        }

        if (rawResult == null) {
            byte[] processData = new byte[processWidth * processHeight * 3 / 2];
            ImagePreProcess.preProcess(processSrc, processWidth, processHeight, processData);

            if (isDebugMode) {
                lastPreviewData = processSrc;
                lastPreProcessData = processData;
                lastPreProcessWidth = processWidth;
                lastPreProcessHeight = processHeight;
            }

            //3. opencv+zxing
            source = buildLuminanceSource(processData, processWidth, processHeight);
            bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                rawResult = multiFormatReader.decodeWithState(bitmap);
                Log.v(TAG, "zxing success with opencv");
            } catch (Exception re) {
                if (re instanceof NotFoundException) {
                    CameraZoomStrategy.getInstance().findNoPoint();
                }
                // continue
            } finally {
                multiFormatReader.reset();
            }

            //4. opencv+zbar
            if (rawResult == null) {
                String zbarResult = ZbarController.getInstance().scan(processData, processWidth, processHeight);
                if (zbarResult != null) {
                    rawResult = new Result(zbarResult, null, null, null);
                    Log.v(TAG, "zbar success with opencv");
                }
            }
        }

        return rawResult;
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on
     * the format of the preview buffers, as described by Camera.Parameters.
     *
     * @param data   A preview frame.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = new Rect(0, 0, width, height);

        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect
            .height(), false);
    }
}
