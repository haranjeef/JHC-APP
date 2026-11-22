package lk.jhc.app;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * JHC app: shows https://jhc.lk full screen. Pull down to refresh, back button goes back,
 * phone / mail / WhatsApp links open the right app, other websites open in the browser,
 * photo upload (Quick Post) works, and a friendly page shows when there is no internet.
 */
public class MainActivity extends AppCompatActivity {

    // Change this one line if the website address ever changes.
    private static final String HOME = "https://jhc.lk/?source=app";
    private static final String HOST = "jhc.lk";

    private WebView web;
    private SwipeRefreshLayout swipe;
    private ProgressBar bar;
    private ValueCallback<Uri[]> fileCallback;

    private final ActivityResultLauncher<Intent> chooser = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (fileCallback != null) {
                    Uri[] r = WebChromeClient.FileChooserParams.parseResult(result.getResultCode(), result.getData());
                    fileCallback.onReceiveValue(r);
                    fileCallback = null;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        web = findViewById(R.id.web);
        swipe = findViewById(R.id.swipe);
        bar = findViewById(R.id.bar);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setUserAgentString(s.getUserAgentString() + " JHCApp/1.0");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        swipe.setColorSchemeResources(R.color.jhc_navy);
        swipe.setOnRefreshListener(() -> web.reload());
        // only pull-to-refresh when the page is scrolled to the top
        swipe.setOnChildScrollUpCallback((parent, child) -> web.getScrollY() > 0);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                return handle(req.getUrl());
            }

            @Override
            public void onPageStarted(WebView v, String url, Bitmap icon) {
                bar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                bar.setVisibility(View.GONE);
                swipe.setRefreshing(false);
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    v.loadUrl("file:///android_asset/offline.html");
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int progress) {
                bar.setProgress(progress);
            }

            // photo / file upload from the website (Quick Post, Quick Event)
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = cb;
                try {
                    chooser.launch(params.createIntent());
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                r.setMimeType(mime);
                String ck = CookieManager.getInstance().getCookie(url);
                if (ck != null) {
                    r.addRequestHeader("cookie", ck);
                }
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, cd, mime));
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                Toast.makeText(this, "Downloading…", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (web.canGoBack()) {
                    web.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            Uri start = getIntent() != null ? getIntent().getData() : null;
            web.loadUrl(start != null && HOST.equals(start.getHost()) ? start.toString() : HOME);
        }
    }

    /** true = we dealt with it, false = let the WebView load it. */
    private boolean handle(Uri u) {
        String scheme = u.getScheme() == null ? "" : u.getScheme();
        String host = u.getHost() == null ? "" : u.getHost();
        if (("http".equals(scheme) || "https".equals(scheme)) && (host.equals(HOST) || host.endsWith("." + HOST))) {
            return false; // stay inside the app
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u)); // tel:, mailto:, whatsapp:, facebook, other sites
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app found to open this link", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // if the app was opened offline and the connection is back, reload the real site
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        String url = web.getUrl();
        if (cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected()
                && url != null && url.startsWith("file:///android_asset")) {
            web.loadUrl(HOME);
        }
    }
}
