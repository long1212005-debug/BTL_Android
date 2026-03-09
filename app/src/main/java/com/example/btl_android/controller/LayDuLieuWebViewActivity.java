package com.example.btl_android.controller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btl_android.R;
import com.example.btl_android.model.dao.KyHocDAO;
import com.example.btl_android.model.dao.MonHocDAO;
import com.example.btl_android.model.entity.KyHoc;
import com.example.btl_android.model.entity.MonHoc;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LayDuLieuWebViewActivity extends AppCompatActivity {

    private WebView wv;
    private ProgressBar progressBar;
    private Button btnConfirm;
    private MonHocDAO monHocDAO;
    private KyHocDAO kyHocDAO;

    private final String URL_SEMESTER = "https://sv.haui.edu.vn/student/result/viewscorebysemester";
    private final String URL_EXAM = "https://sv.haui.edu.vn/student/result/examresult";
    private final String URL_STUDY = "https://sv.haui.edu.vn/student/result/studyresults";

    private int currentStep = 0;
    private List<KyHoc> tempSemesters = new ArrayList<>();
    private List<MonHoc> tempMonHocs = new ArrayList<>();
    private int totalIgnored = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lay_dl_tu_webview);

        monHocDAO = new MonHocDAO(this);
        kyHocDAO = new KyHocDAO(this);

        wv = findViewById(R.id.wvLaydiem);
        progressBar = findViewById(R.id.progressBar);
        btnConfirm = findViewById(R.id.btnLaydiem);

        setupWebView();

        btnConfirm.setOnClickListener(v -> {
            btnConfirm.setVisibility(View.GONE);
            startAutoCrawling();
        });
    }

    private void setupWebView() {
        WebSettings ws = wv.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        wv.addJavascriptInterface(new WebBridge(), "AndroidBridge");
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                handleAutoStep(url);
            }
        });
        wv.loadUrl("https://sv.haui.edu.vn");
    }

    private void startAutoCrawling() {
        currentStep = 1;
        tempSemesters.clear();
        tempMonHocs.clear();
        totalIgnored = 0;
        wv.loadUrl(URL_SEMESTER);
    }

    private void handleAutoStep(String url) {
        if (currentStep == 1 && url.contains("viewscorebysemester")) {
            wv.postDelayed(this::scrapeSemesterData, 500);
        } else if (currentStep == 2 && url.contains("examresult")) {
            wv.postDelayed(this::scrapeExamData, 500);
        } else if (currentStep == 3 && url.contains("studyresults")) {
            wv.postDelayed(this::scrapeStudyData, 500);
        }
    }

    private void scrapeSemesterData() {
        String script = "javascript:(function() { " +
                "   var rows = document.querySelectorAll('tr'); " +
                "   var data = []; " +
                "   rows.forEach(function(row) { " +
                "       var cols = row.querySelectorAll('td'); " +
                "       if (cols.length == 5 && !isNaN(parseInt(cols[0].innerText))) { " +
                "           data.push({ " +
                "               tenKy: 'Kỳ ' + cols[0].innerText.trim(), " +
                "               tinChi: cols[1].innerText.trim(), " +
                "               gpa: cols[2].innerText.trim() " +
                "           }); " +
                "       } " +
                "   }); " +
                "   AndroidBridge.saveSemesters(JSON.stringify(data)); " +
                "})();";
        wv.loadUrl(script);
    }

    private void scrapeExamData() {
        String script = "javascript:(function() { " +
                "   var rows = document.querySelectorAll('tr.kTableRow, tr.kTableAltRow'); " +
                "   var data = []; " +
                "   rows.forEach(function(row) { " +
                "       var cols = row.querySelectorAll('td'); " +
                "       if (cols.length >= 14) { " +
                "           data.push({ " +
                "               maHP: cols[1].innerText.trim(), " +
                "               tenMon: cols[3].innerText.trim(), " +
                "               tinChi: cols[5].innerText.trim(), " +
                "               diem4: cols[12].innerText.trim(), " +
                "               diemChu: cols[13].innerText.trim() " +
                "           }); " +
                "       } " +
                "   }); " +
                "   AndroidBridge.saveExams(JSON.stringify(data)); " +
                "})();";
        wv.loadUrl(script);
    }

    private void scrapeStudyData() {
        String script = "javascript:(function() { " +
                "   var rows = document.querySelectorAll('tr.kTableRow, tr.kTableAltRow'); " +
                "   var data = []; " +
                "   rows.forEach(function(row) { " +
                "       var cols = row.querySelectorAll('td'); " +
                "       if (cols.length >= 7) { " +
                "           data.push({ " +
                "               tenMon: cols[1].innerText.trim(), " +
                "               tx1: cols[4].innerText.replace(/\\u00a0/g, '').trim(), " +
                "               tx2: cols[5].innerText.replace(/\\u00a0/g, '').trim(), " +
                "               tx3: cols[6].innerText.replace(/\\u00a0/g, '').trim() " +
                "           }); " +
                "       } " +
                "   }); " +
                "   AndroidBridge.saveStudy(JSON.stringify(data)); " +
                "})();";
        wv.loadUrl(script);
    }

    public class WebBridge {
        @JavascriptInterface
        public void saveSemesters(String json) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    KyHoc kh = new KyHoc();
                    kh.setTenKy(o.getString("tenKy"));
                    kh.setTongTinChiKy((int)Float.parseFloat(o.getString("tinChi")));
                    kh.setGpaKy(parseF(o.getString("gpa")));
                    tempSemesters.add(kh);
                }
                currentStep = 2;
                runOnUiThread(() -> wv.loadUrl(URL_EXAM));
            } catch (Exception e) { Log.e("ERR", e.getMessage()); }
        }

        @JavascriptInterface
        public void saveExams(String json) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    if (isIgnored(o.getString("maHP"), o.getString("tenMon"))) {
                        totalIgnored++;
                        continue;
                    }
                    MonHoc mh = new MonHoc();
                    mh.setTenMon(o.getString("tenMon"));
                    mh.setSoTinChi((int)Float.parseFloat(o.getString("tinChi")));
                    mh.setDiemTongKet4(parseF(o.getString("diem4")));
                    mh.setDiemChu(o.getString("diemChu"));
                    mh.setTrangThai(mh.getDiemChu().isEmpty() ? "Đang học" : "Đã qua");
                    tempMonHocs.add(mh);
                }
                currentStep = 3;
                runOnUiThread(() -> wv.loadUrl(URL_STUDY));
            } catch (Exception e) { Log.e("ERR", e.getMessage()); }
        }

        @JavascriptInterface
        public void saveStudy(String json) {
            try {
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    for (MonHoc mh : tempMonHocs) {
                        if (mh.getTenMon().equalsIgnoreCase(o.getString("tenMon"))) {
                            mh.setDiemTx1(parseF(o.getString("tx1")));
                            mh.setDiemTx2(parseF(o.getString("tx2")));
                            String tx3Val = o.getString("tx3");
                            mh.setDiemTx3(tx3Val.isEmpty() ? null : parseF(tx3Val));
                            break;
                        }
                    }
                }
                runOnUiThread(() -> showFinalSummaryDialog());
            } catch (Exception e) { Log.e("ERR", e.getMessage()); }
        }
    }

    private void showFinalSummaryDialog() {
        int totalTC = 0;
        int completedMon = 0;
        int inProgressMon = 0;
        StringBuilder sb = new StringBuilder();
        
        sb.append("📊 TỔNG KẾT DỮ LIỆU CÀO ĐƯỢC:\n\n");
        sb.append("📅 Số học kỳ: ").append(tempSemesters.size()).append("\n");
        
        for (MonHoc mh : tempMonHocs) {
            totalTC += mh.getSoTinChi();
            if ("Đã qua".equals(mh.getTrangThai())) completedMon++;
            else inProgressMon++;
        }

        sb.append("📚 Tổng số môn tính tích lũy: ").append(tempMonHocs.size()).append("\n");
        sb.append("✅ Môn đã hoàn thành: ").append(completedMon).append("\n");
        sb.append("⏳ Môn đang học/chưa thi: ").append(inProgressMon).append("\n");
        sb.append("🎖️ Tổng tín chỉ tích lũy: ").append(totalTC).append("\n");
        sb.append("🚫 Đã loại bỏ: ").append(totalIgnored).append(" môn (Thể chất/Quân sự/TA cơ bản)\n\n");
        
        sb.append("📝 DANH SÁCH CHI TIẾT:\n");
        for (MonHoc mh : tempMonHocs) {
            sb.append("- ").append(mh.getTenMon())
              .append(" (").append(mh.getSoTinChi()).append(" TC) -> ")
              .append(mh.getDiemChu().isEmpty() ? "Chưa có điểm" : mh.getDiemChu())
              .append("\n");
        }

        ScrollView scroll = new ScrollView(this);
        TextView tv = new TextView(this);
        tv.setText(sb.toString());
        tv.setPadding(40, 40, 40, 40);
        tv.setTextColor(getResources().getColor(android.R.color.black));
        tv.setTextSize(14);
        scroll.addView(tv);

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận dữ liệu HaUI")
                .setView(scroll)
                .setCancelable(false)
                .setPositiveButton("LƯU VÀO MÁY", (dialog, which) -> commitDataToDatabase())
                .setNegativeButton("HỦY", (dialog, which) -> {
                    btnConfirm.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Đã hủy thao tác", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void commitDataToDatabase() {
        kyHocDAO.deleteDisplayKyHoc(1);
        for (KyHoc kh : tempSemesters) {
            kh.setSinhVienId(1);
            kh.setTrangThai(true);
            kyHocDAO.insertKyHoc(kh);
        }

        int targetId = getOrCreateWebKyHoc();
        monHocDAO.deleteMonHocByKyHocId(targetId);
        for (MonHoc mh : tempMonHocs) {
            mh.setKyHocId(targetId);
            monHocDAO.insertMonHoc(mh);
        }
        
        monHocDAO.syncGlobalStats();
        Toast.makeText(this, "Đồng bộ hoàn tất!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private boolean isIgnored(String code, String name) {
        String c = code.toUpperCase(); String n = name.toLowerCase();
        return c.startsWith("DC") || c.startsWith("PE") || (n.contains("tiếng anh") && n.contains("cơ bản")) || n.contains("kỹ năng sử dụng công nghệ thông tin");
    }

    private float parseF(String v) {
        try { return Float.parseFloat(v.replace(",", ".")); } catch (Exception e) { return 0.0f; }
    }

    private int getOrCreateWebKyHoc() {
        List<KyHoc> list = kyHocDAO.getAllKyHoc(1);
        for (KyHoc kh : list) if (kh.getTenKy().equals("Dữ liệu từ Website")) return kh.getId();
        KyHoc newKy = new KyHoc(); newKy.setSinhVienId(1); newKy.setTenKy("Dữ liệu từ Website"); newKy.setTrangThai(true);
        return (int) kyHocDAO.insertKyHoc(newKy);
    }
}