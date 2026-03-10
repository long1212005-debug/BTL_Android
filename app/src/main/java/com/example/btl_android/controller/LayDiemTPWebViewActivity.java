package com.example.btl_android.controller;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.btl_android.R;
import com.example.btl_android.model.dao.MonHocDAO;
import com.example.btl_android.model.entity.MonHoc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class LayDiemTPWebViewActivity extends AppCompatActivity {

    private WebView wvLaydiem;
    private Button btnLaydiem;
    private ProgressBar progressBar;
    private MonHocDAO monHocDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sử dụng layout riêng biệt để tránh nhầm lẫn
        setContentView(R.layout.activity_lay_diem_tp);

        monHocDAO = new MonHocDAO(this);

        initViews();
        setupWebView();

        btnLaydiem.setOnClickListener(v -> runScrapingScript());
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        wvLaydiem = findViewById(R.id.wvLaydiem);
        btnLaydiem = findViewById(R.id.btnLaydiem);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupWebView() {
        WebSettings webSettings = wvLaydiem.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        wvLaydiem.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        wvLaydiem.addJavascriptInterface(new WebDataInterface(), "AndroidBridge");
        wvLaydiem.loadUrl("https://sv.haui.edu.vn/student/result/studyresults");
    }

    private void runScrapingScript() {
        // Script bóc tách Tên môn (cột 1) và các điểm TX (cột 4, 5, 6) từ đúng bảng Kết quả học tập
        String script = "javascript:(function() { " +
                "   var rows = document.querySelectorAll('tr.kTableRow, tr.kTableAltRow'); " +
                "   var data = []; " +
                "   rows.forEach(function(row) { " +
                "       var cols = row.querySelectorAll('td'); " +
                "       if (cols.length >= 7) { " +
                "           var item = { " +
                "               tenMon: cols[1].innerText.trim(), " +
                "               tx1: cols[4].innerText.replace(/\\u00a0/g, '').trim(), " +
                "               tx2: cols[5].innerText.replace(/\\u00a0/g, '').trim(), " +
                "               tx3: cols[6].innerText.replace(/\\u00a0/g, '').trim() " +
                "           }; " +
                "           if (item.tenMon !== '') data.push(item); " +
                "       } " +
                "   }); " +
                "   AndroidBridge.sendData(JSON.stringify(data)); " +
                "})();";
        wvLaydiem.loadUrl(script);
    }

    public class WebDataInterface {
        @JavascriptInterface
        public void sendData(String jsonData) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                List<MonHoc> allLocalMonHoc = monHocDAO.getAllMonHoc();
                int updateCount = 0;

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    String tenWeb = obj.getString("tenMon");

                    for (MonHoc mh : allLocalMonHoc) {
                        // Chỉ cập nhật điểm TX cho những môn chưa có điểm kết thúc
                        if (normalize(mh.getTenMon())
                                .equals(normalize(tenWeb)))  {
                            mh.setDiemTx1(parseSafeFloat(obj.getString("tx1")));
                            mh.setDiemTx2(parseSafeFloat(obj.getString("tx2")));
                            String tx3Val = obj.getString("tx3");
                            mh.setDiemTx3(tx3Val.isEmpty() ? null : parseSafeFloat(tx3Val));
                            
                            if (monHocDAO.updateMonHoc(mh)) {
                                updateCount++;
                            }
                            break;
                        }
                    }
                }

                int finalCount = updateCount;
                runOnUiThread(() -> {
                    Toast.makeText(LayDiemTPWebViewActivity.this, 
                        "Đã cập nhật điểm thành phần cho " + finalCount + " môn học!", Toast.LENGTH_LONG).show();
                    finish();
                });

            } catch (Exception e) {
                Log.e("SCRAPING_ERROR", "Lỗi: " + e.getMessage());
            }
        }
    }

    private float parseSafeFloat(String val) {
        if (val == null || val.trim().isEmpty() || val.equals("-")) return 0.0f;
        try {
            return Float.parseFloat(val.replace(",", "."));
        } catch (Exception e) {
            return 0.0f;
        }
    }
    private String normalize(String s) {
        if (s == null) return "";
        return s.replace("\u00a0", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
}
