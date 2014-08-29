package org.apereo.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.rest.Rest;
import org.apache.commons.lang.StringUtils;
import org.apereo.App;
import org.apereo.R;
import org.apereo.constants.AppConstants;
import org.apereo.deserializers.LayoutDeserializer;
import org.apereo.models.Layout;
import org.apereo.services.RestApi;
import org.apereo.services.UmobileRestCallback;
import org.apereo.utils.LayoutManager;
import org.apereo.utils.Logger;

/**
 * Created by schneis on 8/28/14.
 */
@EActivity(R.layout.portlet_webview)
public class LoginActivity extends Activity {

    private static final String TAG = LoginActivity.class.getName();

    @ViewById(R.id.webview)
    WebView webView;

    @Extra
    String url;

    @Bean
    RestApi restApi;

    @Bean
    LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @AfterViews
    void initiailize() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Logger.d(TAG, "starting " + url);
                super.onPageStarted(view, url, favicon);

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Logger.d(TAG, "finishing" + url);
                String cookie = "empty";
                boolean loggedin = false;
                CookieManager cookieManager = CookieManager.getInstance();
                if(StringUtils.equalsIgnoreCase(url, getResources().getString(R.string.login_url))) {
                    Logger.d(TAG, url);
                    cookie = cookieManager.getCookie(url);

                    if (cookie != null) {
                        String[] temp = cookie.split("[;]");
                        for (String key : temp) {
                            if (key.contains(AppConstants.LOGINKEY)) {
                                loggedin = true;
                                break;
                            }
                        }
                    }

                    if (loggedin) {
                        getLoggedInFeed();
                    }
                }
                Logger.d(TAG, "cookies from cas = " + cookie);
                Logger.d(TAG, "cookies from mysail" + cookieManager.getCookie("https://mysail.oakland.edu/uPortal"));
                super.onPageFinished(view, url);
            }
        });
        webView.loadUrl(url);
    }

    private void getLoggedInFeed() {
        String cookie = CookieManager.getInstance().getCookie("https://mysail.oakland.edu/uPortal");

        if (cookie != null) {
            String[] temp = cookie.split("[;]");
            for (String key : temp) {
                if (key.contains(AppConstants.JSESSIONID)) {
                    String[] temp1 = key.split("[=]");
                    restApi.setCookie(temp1[1]);
                    break;
                }
            }
        }
        restApi.getMainFeed(new UmobileRestCallback<String>() {
            @Override
            public void onError(Exception e, String responseBody) {
                Logger.e(TAG, responseBody, e);

            }

            @Override
            public void onSuccess(String response) {
                Gson g = new GsonBuilder()
                        .registerTypeAdapter(Layout.class, new LayoutDeserializer())
                        .create();

                Layout layout = g.fromJson(response, Layout.class);
                Logger.d(TAG, response);
                layoutManager.setLayout(layout);
                HomePage_
                        .intent(LoginActivity.this)
                        .start();
                finish();

            }
        });
    }

}
