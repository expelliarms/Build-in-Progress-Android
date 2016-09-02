package com.bip_public_android;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;

public class HTML5WebView extends WebView {

    private Context                             mContext;
    private MyWebChromeClient                   mWebChromeClient;
    private View                                mCustomView;
    private FrameLayout                         mCustomViewContainer;
    private WebChromeClient.CustomViewCallback  mCustomViewCallback;

    private FrameLayout                         mContentView;
    //private RelativeLayout                      mBrowserFrameLayout;
    private FrameLayout                         mBrowserFrameLayout;
    private FrameLayout                         mLayout;
    private ImageButton                         okayButton;
    private ImageButton                         deleteButton;
    private ProgressBar                         mediaProgressBar;
    private FrameLayout                         mHolder;

    static final String LOGTAG = "HTML5WebView";

    static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);


    /**Since this class is only called for creating a link to display vimeo videos
     *
     * NOTE: Depending on the vimeo video, some will play and some wont. The videos that wont' play throw this error:
     * I/chromium﹕ [INFO:CONSOLE(7)] "Uncaught TypeError: Cannot read property 'hls' of null", source: https://f.vimeocdn.com/p/2.7.1/js/player.js (7)
     * The video I found that doesn't work was uploaded 6 years ago, so maybe some properties were
     * changed/removed and the new vimeo player code is not completely back compatible
     * https://f.vimeocdn.com/p/2.7.1/js/player.js <- current player code*/
    private void init(Context context) {
        Log.d(LOGTAG, "VCJ : init for HTML5 webview ");
        mContext = context;     
        Activity a = (Activity) mContext;

        mLayout = new FrameLayout(context);
        mHolder.addView(this, COVER_SCREEN_PARAMS); // add to the layout
        Log.d(LOGTAG, "cover params : "+COVER_SCREEN_PARAMS);
        this.bringToFront();

        // Configure the webview
        WebSettings s = getSettings();
        Log.d(LOGTAG, "got settings");
        s.setLoadWithOverviewMode(true);
        s.setJavaScriptEnabled(true);

        /*s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        this.clearView();
        this.measure(100, 100);
        s.setPluginState(WebSettings.PluginState.ON);
        s.setPluginState(WebSettings.PluginState.ON_DEMAND);
*/
        mWebChromeClient = new MyWebChromeClient();
        setWebChromeClient(mWebChromeClient);
        Log.d(LOGTAG, "set chrome client");

        setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                //mediaProgressBar.setVisibility(View.GONE);
                Log.d(LOGTAG, "Page finished");
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.d(LOGTAG, "Oh no! " + description);
            }
        });
        Log.d(LOGTAG, "set view client");
    }

    public HTML5WebView(Context context, FrameLayout mediaHolder) {
        super(context);
        this.mHolder = mediaHolder;
        init(context);
    }

    public HTML5WebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HTML5WebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /*public FrameLayout getLayout() {
        return mLayout;
    }*/

    public boolean inCustomView() {
        return (mCustomView != null);
    }

    public void hideCustomView() {
        mWebChromeClient.onHideCustomView();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((mCustomView == null) && canGoBack()){
                goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyWebChromeClient extends WebChromeClient {
        private Bitmap      mDefaultVideoPoster;
        private View        mVideoProgressView;

        @Override
        public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback)
        {
            Log.d(LOGTAG, "----------HERE IN SHOW CUSTOM VIEW----------");
            HTML5WebView.this.setVisibility(View.GONE);

            // if a view already exists then immediately terminate the new one
            if (mCustomView != null) {
                callback.onCustomViewHidden();
                return;
            }

            //mCustomViewContainer.addView(view);
            mHolder.addView(view);
            mCustomView = view;
            mCustomViewCallback = callback;
            mHolder.setVisibility(View.VISIBLE);
           // mCustomViewContainer.setVisibility(View.VISIBLE);
        }

        @Override
        public void onHideCustomView() {
            Log.d(LOGTAG, "--------hide custom view----------");
            if (mCustomView == null)
                return;        

            // Hide the custom view.
            mCustomView.setVisibility(View.GONE);

            // Remove the custom view from its container.
            //mCustomViewContainer.removeView(mCustomView);
            mHolder.removeView(mCustomView);
            mCustomView = null;
            //mCustomViewContainer.setVisibility(View.GONE);
            mHolder.setVisibility(View.GONE);
            mCustomViewCallback.onCustomViewHidden();

            HTML5WebView.this.setVisibility(View.VISIBLE);
            HTML5WebView.this.goBack();
            
            //Log.i(LOGTAG, "set it to webVew");
        }

        @Override
        public View getVideoLoadingProgressView() {
            Log.i(LOGTAG, "here in on getVideoLoadingProgressView");
            LayoutInflater inflater = LayoutInflater.from(mContext);
            mVideoProgressView = inflater.inflate(com.bip_public_android.R.layout.video_loading_progress, null);
            return mVideoProgressView; 
        }

         @Override
         public void onReceivedTitle(WebView view, String title) {
             //Log.d(LOGTAG, "Title : "+title);
             //((Activity) mContext).setTitle(title);
         }

         @Override
         public void onProgressChanged(WebView view, int newProgress) {
             Log.d(LOGTAG, "on progress changed : "+newProgress+" "+ this);
             ((Activity) mContext).getWindow().setFeatureInt(Window.FEATURE_PROGRESS, newProgress*100);
         }

         @Override
         public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
             Log.d(LOGTAG, "geolocation permissions");
             callback.invoke(origin, true, false);
         }
    }

}