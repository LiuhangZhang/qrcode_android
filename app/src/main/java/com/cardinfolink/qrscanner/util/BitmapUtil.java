package com.cardinfolink.qrscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtil {
    public static final int MIN_SIZE_INLINE_PICTURE = 200;//内嵌图片最小规格
    private static final int DEFAULT_DECODE_MEMORY_RETRY_COUNT = 20;

    private static final String TAG = "BitmapUtil";
    private static final int MAX_SIZE = 400;// 最大尺寸400

    private static final Paint sPaint = new Paint();
    private static final Rect sBounds = new Rect();
    private static final Rect sOldBounds = new Rect();
    private static Canvas sCanvas = new Canvas();

    public static byte[] bitmap2Bytes(final Bitmap bm, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        return baos.toByteArray();
    }

    /*
     * input is rgba
     * return bgra
     */
    public static int getAvgColor(byte[] rgba) {
        int len = rgba.length / 4;
        long sumR = 0, sumG = 0, sumB = 0, sumA = 0;
        for (int i = 0; i < len; ++i) {
            sumR += (int) (rgba[i * 4 + 0] & 0xff);
            sumG += (int) (rgba[i * 4 + 1] & 0xff);
            sumB += (int) (rgba[i * 4 + 2] & 0xff);
            sumA += (int) (rgba[i * 4 + 3] & 0xff);
        }

        int r = (int) (sumR / len);
        int g = (int) (sumG / len);
        int b = (int) (sumB / len);
        int a = (int) (sumA / len);

        int avg = ((b << 24) & 0xff000000) | ((g << 16) & 0x00ff0000) | ((r << 8) & 0x0000ff00) | (a & 0x000000ff);

        return avg;
    }

    public static int getAvgColor(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] rgbPixel = new int[width * height];
        bitmap.getPixels(rgbPixel, 0, width, 0, 0, width, height);

        long sumA = 0, sumR = 0, sumG = 0, sumB = 0;
        for (int i = 0; i < rgbPixel.length; ++i) {
            sumA += (rgbPixel[i] & 0xff000000) >>> 24;
            sumR += (rgbPixel[i] & 0xff0000) >>> 16;
            sumG += (rgbPixel[i] & 0xff00) >>> 8;
            sumB += (rgbPixel[i] & 0xff);
        }

        int a = (int) (sumA / rgbPixel.length);
        int r = (int) (sumR / rgbPixel.length);
        int g = (int) (sumG / rgbPixel.length);
        int b = (int) (sumB / rgbPixel.length);

        Log.v(TAG, "r:" + r + " g:" + g + " b:" + b + " a:" + a);

        int avg = ((b << 24) & 0xff000000) | ((g << 16) & 0xff0000) | ((r << 8) & 0x00ff00) | ((a << 0) & 0x0000ff);
        return avg;
    }

    public static Bitmap resizeBitmap(Bitmap bm, int maxWidth) {
        Bitmap returnBm;
        int w = bm.getWidth();
        int h = bm.getHeight();
        float scaleWidth;
        float scaleHeight;
        if (w > h) {
            scaleWidth = ((float) maxWidth) / w;
            scaleHeight = scaleWidth;
        } else {
            scaleHeight = ((float) maxWidth) / h;
            scaleWidth = scaleHeight;
        }

        Matrix matrix = new Matrix();

        matrix.postScale(scaleWidth, scaleHeight);

        returnBm = Bitmap.createBitmap(bm, 0, 0, w, h, matrix, true);
        return returnBm;
    }


    public static byte[] createSizeThumbnail(Bitmap bitmap, int size) {
        if (bitmap == null || bitmap.isRecycled() || size == 0) {
            return null;
        }

        byte[] bytes = BitmapUtil.bitmap2Bytes(bitmap, 100);

        int loopTime = 0;
        while (bytes.length > size) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            switch (loopTime) {
                case 0:
                    bytes = BitmapUtil.bitmap2Bytes(bitmap, 80);
                    options.inPreferredConfig = Config.RGB_565;
                    break;
                case 1:
                    bytes = BitmapUtil.bitmap2Bytes(bitmap, 40);
                    options.inPreferredConfig = Config.RGB_565;
                    break;
                case 2:
                    bytes = BitmapUtil.bitmap2Bytes(bitmap, 20);
                    options.inPreferredConfig = Config.RGB_565;
                    break;
                case 3:
                    bytes = BitmapUtil.bitmap2Bytes(bitmap, 10);
                    options.inPreferredConfig = Config.RGB_565;
                    break;
                case 4:
                    bytes = BitmapUtil.bitmap2Bytes(bitmap, 5);
                    options.inPreferredConfig = Config.RGB_565;
                    break;
                default:
                    return bytes;
            }
            loopTime++;
        }

        return bytes;
    }

    /**
     * Returns a Bitmap representing the thumbnail of the specified Bitmap. The
     * size of the thumbnail is defined by the dimension
     * android.R.dimen.launcher_application_icon_size.
     * <p/>
     * This method is not thread-safe and should be invoked on the UI thread
     * only.
     *
     * @param bitmap The bitmap to get a thumbnail of.
     * @return A thumbnail for the specified bitmap or the bitmap itself if the
     * thumbnail could not be created.
     */
    public synchronized static Bitmap createBitmapThumbnail(Bitmap bitmap,
                                                            final int iconWidth, final int iconHeight) {
        if (bitmap == null || bitmap.isRecycled()) {
            return bitmap;
        }

        int width = iconWidth;
        int height = iconHeight;

        int srcWidth = iconWidth;
        int srcHeight = iconHeight;

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        if (width > 0 && height > 0) {
            if (width < bitmapWidth || height < bitmapHeight) {
                final float ratio = (float) bitmapWidth / bitmapHeight;

                if (bitmapWidth > bitmapHeight) {
                    height = (int) (width / ratio);
                } else if (bitmapHeight > bitmapWidth) {
                    width = (int) (height * ratio);
                }

                Config c = (width == srcWidth && height == srcHeight) ?
                        bitmap.getConfig() : Config.ARGB_8888;
                if (null == c) {
                    c = Config.ARGB_8888;
                }

                try {
                    final Bitmap thumb = Bitmap.createBitmap(srcWidth, srcHeight, c);
                    final Canvas canvas = sCanvas;
                    final Paint paint = sPaint;
                    canvas.setBitmap(thumb);
                    paint.setDither(false);
                    paint.setFilterBitmap(true);
                    sBounds.set((srcWidth - width) / 2, (srcHeight - height) / 2, width, height);
                    sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
                    canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
                    return thumb;
                } catch (OutOfMemoryError e) {
                    return null;
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            } else if (bitmapWidth < width || bitmapHeight < height) {
                try {
                    final Config c = Config.ARGB_8888;
                    final Bitmap thumb = Bitmap.createBitmap(srcWidth, srcHeight, c);
                    final Canvas canvas = sCanvas;
                    final Paint paint = sPaint;
                    canvas.setBitmap(thumb);
                    paint.setDither(false);
                    paint.setFilterBitmap(true);
                    sBounds.set(0, 0, width, height);
                    sOldBounds.set(0, 0, bitmapWidth, bitmapHeight);
                    canvas.drawBitmap(bitmap, sOldBounds, sBounds, paint);
                    return thumb;
                } catch (OutOfMemoryError e) {
                    return null;
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        return bitmap;
    }


    /**
     * 根据手机的分辨率�?dip 的单�?转成�?px(像素)
     */
    public static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    /**
     * 将bitmap写入JPEG文件
     *
     * @param bitmap
     * @param path
     * @return
     */
    public static String writeToFile(Bitmap bitmap, String path) {
        return writeToFile(bitmap, path, 100, false);
    }

    public static String writeToFile(Bitmap bitmap, String path, String filename) {
        return writeToFile(bitmap, path + filename);
    }

    public static String writeToFile(Bitmap bitmap, String path, int quality,
                                     boolean recycleSource) {
        return writeToFile(bitmap, path, quality, Bitmap.CompressFormat.JPEG, recycleSource);
    }

    public static String writeToWebPFile(Bitmap bitmap, String filename) {
        return writeToFile(bitmap, filename, 75, Bitmap.CompressFormat.WEBP, false);
    }

    public static String writeToPngFile(Bitmap bitmap, String filename) {
        return writeToFile(bitmap, filename, 100, Bitmap.CompressFormat.PNG, false);
    }

    private static void clear(Bitmap bitmap){
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
    
    public static String writeToFile(Bitmap bitmap, String path, int quality, Bitmap.CompressFormat format,
                                     boolean recycleSource) {

        if (bitmap == null || bitmap.isRecycled()) {
            Log.e(TAG, "bitmap == null || bitmap.isRecycled() , writeToFile error : " + path);
            return null;
        }

        try {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(path);

            if (f.exists()) {
                f.delete();
            }

            if (f.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(f);
                bitmap.compress(format, quality, fos);
                fos.flush();
                fos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (recycleSource) {
            clear(bitmap);
        }

        return path;
    }

    /**
     * 释放bitmap
     *
     * @param bitmap
     */
    public static void clean(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }
}
