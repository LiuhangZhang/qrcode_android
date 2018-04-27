package com.cardinfolink.qrscanner.base;

import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.cardinfolink.qrscanner.R;
import utils.NetUtils;
import utils.RegexUtils;


/**
 * webActivity 的基本管理类
 * <p>
 * Created by wanny-n1 on 2017/2/14.
 */
public class BaseWebActivity extends BaseActivity {
    private ProgressBar mLoadingBar;
    private WebView mWebView;
    private ViewStub mNetworkExceptionStub;
    private View mNetworkExceptionView;
    protected Toolbar mToolbar;
    private LinearLayout mContentLL;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setHomeButtonEnabled(true); // 设置导航按钮有效
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // 显示导航按钮
        getSupportActionBar().setDisplayShowTitleEnabled(true); // 显示标题
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mLoadingBar = (ProgressBar) findViewById(R.id.web_loading_bar);
        mContentLL = (LinearLayout) findViewById(R.id.activity_web);

        mWebView = new WebView(this);
        mWebView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);// 设置允许访问文件数据
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setAllowContentAccess(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        mNetworkExceptionStub = (ViewStub) findViewById(R.id.network_exception_stub);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                onWebViewReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return BaseWebActivity.this.shouldOverrideUrlLoading(view, url) || super.shouldOverrideUrlLoading(view, url);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (mLoadingBar != null) {
                    mLoadingBar.setVisibility(newProgress < mLoadingBar.getMax() ? View.VISIBLE : View.GONE);
                    mLoadingBar.setProgress(newProgress);
                }
            }
        });
        mContentLL.addView(mWebView);


        String url = getIntent().getStringExtra("url");
        loadUrl(url);
    }

    public void onResume() {
        super.onResume();
        if (mWebView != null) {
            mWebView.onResume();
        }
    }

    @CallSuper
    @Override
    public void onPause() {
        super.onPause();
        if (mWebView != null) {
            mWebView.onPause();
        }
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        // 避免WebView引起内存泄漏
        if (mWebView != null) {
            /* WebView中包含一个ZoomButtonsController，当使用web.getSettings().setBuiltInZoomControls(true);
            启用该设置后，用户一旦触摸屏幕，就会出现缩放控制图标,这个图标过上几秒会自动消失.
            但在3.0系统以上上，如果图标自动消失前退出当前Activity的话，就会发生ZoomButton找不到依附的Window而造成程序崩溃，
            解决办法很简单就是在Activity的ondestory方法中调用web.setVisibility(View.GONE);方法，手动将其隐藏，就不会崩溃了。
            在3.0一下系统上不会出现该崩溃问题 */
            mWebView.setVisibility(View.GONE);
            mWebView.stopLoading();
            mWebView.clearHistory();
            ViewGroup webParent = (ViewGroup) mWebView.getParent();
            if (webParent != null) {
                webParent.removeView(mWebView);
            }
            mWebView.removeAllViews();
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }
        super.onBackPressed();
    }


    public void loadUrl(String url) {
        if (!NetUtils.isConnected(this)) {
            onWebViewReceivedError(mWebView, WebViewClient.ERROR_CONNECT, null, url);
            return;
        }

        if (!RegexUtils.isUrl(url)) {
            showErrorHint(url);
            return;
        }

        if (mWebView != null) {
            mWebView.clearHistory(); // 清空 back/forward
            mWebView.loadUrl(url);
        }

    }

    public void onWebViewReceivedError(WebView view, int errorCode, CharSequence description, String failingUrl) {
        Log.i("0000", "errorCode:   " + errorCode);
        switch (errorCode) {
            case WebViewClient.ERROR_CONNECT:
            case WebViewClient.ERROR_TIMEOUT:
            case WebViewClient.ERROR_HOST_LOOKUP:
            case WebViewClient.ERROR_BAD_URL:
                showErrorHint(failingUrl);
                break;
        }
    }

    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    public void setTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void showErrorHint(String failingUrl) {
        if (mWebView != null) {
            mWebView.setVisibility(View.GONE);
        }
        if (mNetworkExceptionView == null) {
            mNetworkExceptionView = mNetworkExceptionStub.inflate();
        }
        mNetworkExceptionView.setVisibility(View.VISIBLE);
        TextView textView = (TextView) mNetworkExceptionView.findViewById(R.id.network_exception_hint);
        textView.setText(failingUrl);
    }

}
