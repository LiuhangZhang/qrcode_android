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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.cardinfolink.qrscanner.R;
import com.cardinfolink.qrscanner.base.BaseActivity;
import com.cardinfolink.qrscanner.util.BitmapUtil;
import com.google.zxing.Result;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import listener.IZXingActivity;
import opencv.ImagePreProcess;
import utils.ToastUtils;
import view.CameraZoomTouchListener;
import view.ScanBoxView;
import zxing.camera.CameraManager;
import zxing.decode.CameraDecodeThread;
import zxing.decode.DecodeCore;
import zxing.utils.BeepManager;
import zxing.utils.CaptureActivityHandler;
import zxing.utils.InactivityTimer;


/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends BaseActivity implements SurfaceHolder.Callback, IZXingActivity {
    private static final String TAG = CaptureActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSION_CAMERA = 0x2;
    private static final int REQUEST_IMAGE = 1;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    private SurfaceView scanPreview = null;
    private ScanBoxView scanBoxView = null;
    private ImageView flashBtnIv = null;
    private boolean isHasSurface = false;
    private Rect mCropRect = null;

    private RelativeLayout testLayout = null;
    private ImageView testImageIv = null;

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);

        findViewById(R.id.whole_layout).setOnTouchListener(new CameraZoomTouchListener());
        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanBoxView = (ScanBoxView) findViewById(R.id.capture_crop_view_v);
        flashBtnIv = findViewById(R.id.flash_btn);
        flashBtnIv.setTag(false);

        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        flashBtnIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraManager == null) {
                    return;
                }
                if (flashBtnIv.getTag() != null && (Boolean) flashBtnIv.getTag()) {
                    flashBtnIv.setTag(false);
                    flashBtnIv.setImageResource(R.mipmap.ic_flash_off);
                    cameraManager.disableFlash();
                } else {
                    flashBtnIv.setTag(true);
                    flashBtnIv.setImageResource(R.mipmap.ic_flash_on);
                    cameraManager.enableFlash();
                }
            }
        });

        if(DecodeCore.isDebugMode) {
            testLayout = findViewById(R.id.test_layout);
            testImageIv = findViewById(R.id.test_image);
            findViewById(R.id.test_opencv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (DecodeCore.lastPreProcessData != null) {
                        int width = DecodeCore.lastPreProcessWidth;
                        int height = DecodeCore.lastPreProcessHeight;
                        byte[] rgbaData = new byte[width * height * 4];
                        ImagePreProcess.i420ToRGBA(DecodeCore.lastPreProcessData, width, height, rgbaData);

                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaData));

                        testImageIv.setImageBitmap(bitmap);
                        testLayout.setVisibility(View.VISIBLE);
                    }
                }
            });
            testLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    testLayout.setVisibility(View.GONE);
                }
            });

            findViewById(R.id.test_save).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProgressDialog("正在保存...");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                //保存scanbox中的扫描区域图片到相册
                                if (DecodeCore.lastPreviewData != null) {
                                    int width = DecodeCore.lastPreProcessWidth;
                                    int height = DecodeCore.lastPreProcessHeight;
                                    byte[] rgbaData = new byte[width * height * 4];
                                    ImagePreProcess.i420ToRGBA(DecodeCore.lastPreviewData, width, height, rgbaData);

                                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgbaData));

                                    File dir = getExternalCacheDir();
                                    if (!dir.exists() && !dir.mkdir()) {
                                        return;
                                    }

                                    String path = dir.getAbsolutePath() + "/test" + System.currentTimeMillis() + ".png";
                                    final String resultPath = BitmapUtil.writeToPngFile(bitmap, path);

                                    if (path != null) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent intent = new Intent(Intent.ACTION_SEND); // 启动分享发送的属性
                                                File file = new File(resultPath);
                                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));// 分享的内容
                                                intent.setType("image/*");// 分享发送的数据类型
                                                Intent chooser = Intent.createChooser(intent, "Share screen shot");
                                                if (intent.resolveActivity(getPackageManager()) != null) {
                                                    startActivity(chooser);
                                                }

                                                cancelProgressDialog();
                                            }
                                        });
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(CaptureActivity.this, "保存失败", Toast.LENGTH_SHORT).show();
                                                cancelProgressDialog();
                                            }
                                        });
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            });

            findViewById(R.id.test_gallery).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, REQUEST_IMAGE);
                }
            });
        }else{
            findViewById(R.id.test_layout).setVisibility(View.GONE);
            findViewById(R.id.test_opencv).setVisibility(View.GONE);
            findViewById(R.id.test_save).setVisibility(View.GONE);
            findViewById(R.id.test_gallery).setVisibility(View.GONE);
        }
    }

    private ProgressDialog progressDialog;

    public void showProgressDialog(String text) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
        progressDialog.setMessage(text);
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    public void cancelProgressDialog() {
        if (progressDialog != null)
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());
        Log.e("camera", "instance~~~~~~~~");

        handler = null;

        if (isHasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(scanPreview.getHolder());
            Log.e("camera", "2~~~~~~~~");
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            scanPreview.getHolder().addCallback(this);
        }

        inactivityTimer.onResume();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
            Log.e("camera", "3~~~~~~~~");
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     */
    @Override
    public void handleDecode(Result rawResult) {
        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        handleQCode(rawResult.getText());
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
            return;
        }

        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, CameraDecodeThread.ALL_MODE);
            }
            initCrop();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("Camera error");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                finish();
            }
        });
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop() {
        int cameraWidth = cameraManager.getCameraResolution().height;
        int cameraHeight = cameraManager.getCameraResolution().width;
        mCropRect = scanBoxView.getScanBoxAreaRect(cameraWidth, cameraHeight);
    }

    private void handleQCode(String qCode) {
        if (TextUtils.isEmpty(qCode)) {
            ToastUtils.showToast(this, R.string.qcode_empty);
            return;
        }
        startWebActivity(qCode);
    }

    private void startWebActivity(String url) {
        Intent intent = new Intent(this, WebActivity.class);
        intent.putExtra(WebActivity.URL, url);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera(scanPreview.getHolder());
                Log.e("camera", "1~~~~~~~~");
            } else {
                // Permission Denied
                ToastUtils.showToast(this, "Permission Denied");
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取图片路径
        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String[] filePathColumns = {MediaStore.Images.Media.DATA};
            Cursor c = getContentResolver().query(selectedImage, filePathColumns, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePathColumns[0]);
            String imagePath = c.getString(columnIndex);
            c.close();

            Log.v(TAG, "image path:" + imagePath);
            if (imagePath != null && new File(imagePath).exists()) {
                Intent intent = new Intent();
                intent.setClass(this, ImageDetectActivity.class);
                intent.putExtra("image", imagePath);
                startActivity(intent);
            }
        }
    }
}