package hobby.com.food2fork;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

public class WebViewActivity extends AppCompatActivity {
    private WebView m_webview = null;
    private String  m_InstURL = null;
    private String  m_OrgURL  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);

        // Get the URL values
        m_InstURL = getIntent().getStringExtra(StaticValues.INSTRUCTIONS_URL);
        m_OrgURL = getIntent().getStringExtra(StaticValues.ORIGINAL_URL);

        m_webview = (WebView) findViewById(R.id.webviewBox);
        m_webview.getSettings().setJavaScriptEnabled(true);

        if(m_InstURL != null) {
            m_webview.loadUrl(m_InstURL);
        }

        if(m_OrgURL != null) {
            m_webview.loadUrl(m_OrgURL);
        }
    }
}
