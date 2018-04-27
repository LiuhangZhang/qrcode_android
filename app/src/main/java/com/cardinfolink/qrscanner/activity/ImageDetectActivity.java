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
package com.cardinfolink.qrscanner.activity;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.cardinfolink.qrscanner.R;
import com.cardinfolink.qrscanner.base.BaseActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import opencv.ImagePreProcess;
import zxing.decode.DecodeCore;
import zxing.decode.DecodeFormatManager;

public final class ImageDetectActivity extends BaseActivity {
    private static final int MSG_SUCCESS = 0;
    private static final int MSG_FAILURE = 1;

    private ImageView imageIv;
    private int screenWidth, screenHeight;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS: {
                    showDialog("检测成功：" + msg.obj);
                    break;
                }
                case MSG_FAILURE: {
                    showDialog("检测失败");
                    break;
                }
            }
            return false;
        }
    });

    @Override
    public void onCreate(Bundle saveInstance) {
        super.onCreate(saveInstance);
        setContentView(R.layout.activity_image_detect);
        imageIv = findViewById(R.id.image_iv);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        final String imagePath = getIntent().getStringExtra("image");
        detect(imagePath);
    }

    private void showDialog(String text) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ImageDetectActivity.this);
        alertDialogBuilder.setMessage(text);
        alertDialogBuilder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ImageDetectActivity.this.finish();
            }
        });

        Dialog dialog = alertDialogBuilder.create();
        //设置点击Dialog外部任意区域关闭Dialog
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        //设置弹窗在底部
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
    }

    private void detect(final String imagePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imagePath, options);

                options.inSampleSize = Math.max(options.outWidth / screenWidth, options.outHeight / screenHeight);
                options.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);
                if (bitmap == null) {
                    onError();
                    return;
                }

                final Bitmap targetBitmap = bitmap;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        imageIv.setImageBitmap(targetBitmap);
                    }
                });

                if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                }

                byte[] rgbaData = bitmap2Bytes(bitmap);
                if (rgbaData == null) {
                    onError();
                    return;
                }

                int width = bitmap.getWidth() - (bitmap.getWidth() % 6);
                int height = bitmap.getHeight() - (bitmap.getHeight() % 6);

                final byte[] i420Data = new byte[width * height * 3 / 2];
                ImagePreProcess.RGBAToI420(rgbaData, width, height, i420Data);
                if (i420Data == null) {
                    onError();
                    return;
                }

                //just for test
//                final int targetWidth = width;
//                final int targetHeight = height;
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        byte[] rgbaData = new byte[targetWidth * targetHeight * 4];
//                        ImagePreProcess.i420ToRGBA(i420Data, targetWidth, targetHeight, rgbaData);
//
//                        Bitmap bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
//                        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaData));
//
//                        imageIv.setImageBitmap(bitmap);
//                    }
//                });

                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                Collection<BarcodeFormat> decodeFormats = new ArrayList<BarcodeFormat>();
                decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
                hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
                DecodeCore decodeCore = new DecodeCore(hints);

                Result result = decodeCore.decode(i420Data, width, height, null, false);
                if (result != null && !TextUtils.isEmpty(result.getText())) {
                    onSuccess(result.getText());
                } else {
                    onError();
                }
            }
        }).start();
    }

    private byte[] bitmap2Bytes(Bitmap bitmap) {
        int picw = bitmap.getWidth(), pich = bitmap.getHeight();
        int[] pix = new int[picw * pich];
        bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

        int tempH = pich - (pich % 6);
        int tempW = picw - (picw % 6);
        byte[] result = new byte[tempW * tempH * 4];

        for (int y = 0; y < tempH; y++) {
            for (int x = 0; x < tempW; x++) {
                int dstIndex = y * tempW + x;
                int srcIndex = y * picw + x;
                result[dstIndex * 4] = (byte) ((pix[srcIndex] >> 16) & 0xff);     //bitwise shifting
                result[dstIndex * 4 + 1] = (byte) ((pix[srcIndex] >> 8) & 0xff);
                result[dstIndex * 4 + 2] = (byte) (pix[srcIndex] & 0xff);
                result[dstIndex * 4 + 3] = (byte) 0xff;
            }
        }
        return result;
    }

    private void onError() {
        handler.sendEmptyMessage(MSG_FAILURE);
    }

    private void onSuccess(String result) {
        Message msg = new Message();
        msg.what = MSG_SUCCESS;
        msg.obj = result;
        handler.sendMessage(msg);
    }
}