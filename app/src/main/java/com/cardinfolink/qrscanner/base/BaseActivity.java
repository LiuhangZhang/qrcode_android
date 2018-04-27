package com.cardinfolink.qrscanner.base;

import android.support.v7.app.AppCompatActivity;

/**
 * Activity  的基本类
 * <p>
 * Created by wanny-n1 on 2017/5/31.
 */

public class BaseActivity extends AppCompatActivity {

    @Override
    public void setRequestedOrientation(int requestedOrientation) {
        //防止自动旋转
        //super.setRequestedOrientation(requestedOrientation);
    }
}
