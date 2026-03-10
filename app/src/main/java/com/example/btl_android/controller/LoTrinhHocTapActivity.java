package com.example.btl_android.controller;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.btl_android.R;
import com.example.btl_android.adapters.MonHocAdapter;
import com.example.btl_android.model.dao.MonHocDAO;
import com.example.btl_android.model.entity.MonHoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LoTrinhHocTapActivity extends AppCompatActivity {

    private TextView tvCurrentTinChi, tvCurrentGPA, tvHocTiepResult, tvSuggestImprove;
    private EditText edtTargetGPA;
    private Button btnSyncData, btnAnalyze;
    private LinearLayout layoutResult;
    private ListView lvSuggestMonHoc;
    private Toolbar toolbar;

    private MonHocDAO monHocDAO;
    private List<MonHoc> allMonHoc;
    private List<MonHoc> bestAttemptsMonHoc; // Danh sách đã lọc điểm cao nhất
    private float currentGPA = 0;
    private int currentTinChi = 0;
    private final int TOTAL_CREDITS_REQUIRED = 140; // Mốc HaUI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lo_trinh_hoc_tap);

        initViews();
        setupToolbar();
        monHocDAO = new MonHocDAO(this);
        
        loadCurrentStats();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvCurrentTinChi = findViewById(R.id.tvCurrentTinChi);
        tvCurrentGPA = findViewById(R.id.tvCurrentGPA);
        tvHocTiepResult = findViewById(R.id.tvHocTiepResult);
        tvSuggestImprove = findViewById(R.id.tvSuggestImprove);
        edtTargetGPA = findViewById(R.id.edtTargetGPA);
        btnSyncData = findViewById(R.id.btnSyncData);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        layoutResult = findViewById(R.id.layoutResult);
        lvSuggestMonHoc = findViewById(R.id.lvSuggestMonHoc);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadCurrentStats() {
        allMonHoc = monHocDAO.getAllMonHoc();
        if (allMonHoc == null || allMonHoc.isEmpty()) {
            tvCurrentTinChi.setText("0");
            tvCurrentGPA.setText("0.0");
            return;
        }

        // LỌC: Chỉ lấy lần học có điểm cao nhất của từng môn
        bestAttemptsMonHoc = getBestAttempts(allMonHoc);

        float tongDiem = 0;
        currentTinChi = 0;
        for (MonHoc mh : bestAttemptsMonHoc) {
            tongDiem += mh.getDiemTongKet4() * mh.getSoTinChi();
            currentTinChi += mh.getSoTinChi();
        }

        if (currentTinChi > 0) {
            currentGPA = tongDiem / currentTinChi;
            tvCurrentGPA.setText(String.format(Locale.getDefault(), "%.2f", currentGPA));
            tvCurrentTinChi.setText(String.valueOf(currentTinChi));
        }
    }

    // Hàm lọc lấy danh sách môn học duy nhất với điểm cao nhất (Chuẩn quy tắc HaUI)
    private List<MonHoc> getBestAttempts(List<MonHoc> list) {
        Map<String, MonHoc> bestMap = new HashMap<>();
        for (MonHoc mh : list) {
            if ("Đang học".equals(mh.getTrangThai())) continue;
            
            String key = mh.getTenMon().trim().toLowerCase();
            // Nếu môn chưa có trong map hoặc điểm lần này cao hơn lần trước thì cập nhật
            if (!bestMap.containsKey(key) || mh.getDiemTongKet4() > bestMap.get(key).getDiemTongKet4()) {
                bestMap.put(key, mh);
            }
        }
        return new ArrayList<>(bestMap.values());
    }

    private void setupClickListeners() {
        btnSyncData.setOnClickListener(v -> {
            Intent intent = new Intent(this, LayDuLieuWebViewActivity.class);
            startActivity(intent);
        });

        btnAnalyze.setOnClickListener(v -> {
            hideKeyboard();
            analyzeLoTrinh();
        });
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void analyzeLoTrinh() {
        String sTarget = edtTargetGPA.getText().toString().trim();
        if (sTarget.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập GPA mục tiêu", Toast.LENGTH_SHORT).show();
            return;
        }

        float targetGPA = Float.parseFloat(sTarget);
        if (targetGPA > 4.0) {
            Toast.makeText(this, "GPA mục tiêu tối đa là 4.0", Toast.LENGTH_SHORT).show();
            return;
        }

        layoutResult.setVisibility(View.VISIBLE);

        int remainingCredits = TOTAL_CREDITS_REQUIRED - currentTinChi;
        if (remainingCredits > 0) {
            float totalPointsNeeded = (targetGPA * TOTAL_CREDITS_REQUIRED) - (currentGPA * currentTinChi);
            float avgNeeded = totalPointsNeeded / remainingCredits;

            if (avgNeeded > 4.0) {
                float maxGPAPossible = (currentGPA * currentTinChi + 4.0f * remainingCredits) / TOTAL_CREDITS_REQUIRED;
                String warning = String.format(Locale.getDefault(),
                    "Với %d tín chỉ còn lại, dù đạt full A (4.0) bạn cũng chỉ đạt tối đa %.2f GPA ra trường.\n\n" +
                    "-> Bạn CẦN học cải thiện ngay các môn bên dưới.",
                    remainingCredits, maxGPAPossible);
                tvHocTiepResult.setText(warning);
                tvHocTiepResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                String breakdown = getBreakdownText(avgNeeded, remainingCredits);
                tvHocTiepResult.setText(breakdown);
                tvHocTiepResult.setTextColor(getResources().getColor(android.R.color.black));
            }
        } else {
            tvHocTiepResult.setText("Bạn đã hoàn thành đủ " + TOTAL_CREDITS_REQUIRED + " tín chỉ.");
        }

        calculateImprovementNow(targetGPA);
    }

    private void calculateImprovementNow(float targetGPA) {
        // Lấy các môn điểm thấp (< 2.5) từ danh sách kết quả tốt nhất
        List<MonHoc> lowScoreSubjects = new ArrayList<>();
        for (MonHoc mh : bestAttemptsMonHoc) {
            if (mh.getDiemTongKet4() < 2.5) {
                lowScoreSubjects.add(mh);
            }
        }
        
        // Sắp xếp ưu tiên:
        // 1. Môn đã qua nhưng điểm thấp (D, D+, C) để cải thiện nhanh
        // 2. Môn trượt (F) để học lại
        // 3. Tín chỉ cao hơn xếp trước
        Collections.sort(lowScoreSubjects, (o1, o2) -> {
            boolean isO1Passed = o1.getDiemTongKet4() > 0;
            boolean isO2Passed = o2.getDiemTongKet4() > 0;
            
            if (isO1Passed != isO2Passed) {
                return isO1Passed ? -1 : 1; // Ưu tiên môn đã qua lên trước
            }
            if (o1.getSoTinChi() != o2.getSoTinChi()) {
                return o2.getSoTinChi() - o1.getSoTinChi(); // Ưu tiên tín chỉ nhiều
            }
            return Float.compare(o1.getDiemTongKet4(), o2.getDiemTongKet4());
        });

        float tempTotalPoints = currentGPA * currentTinChi;
        int improveCount = 0;
        int retakeCount = 0;
        List<MonHoc> suggestList = new ArrayList<>();

        for (MonHoc mh : lowScoreSubjects) {
            if (tempTotalPoints / currentTinChi >= targetGPA) break;
            
            tempTotalPoints += (4.0f - mh.getDiemTongKet4()) * mh.getSoTinChi();
            if (mh.getDiemTongKet4() > 0) improveCount++;
            else retakeCount++;
            
            suggestList.add(mh);
        }

        float simulatedGPA = tempTotalPoints / currentTinChi;
        
        StringBuilder sb = new StringBuilder();
        if (simulatedGPA >= targetGPA) {
            sb.append(String.format(Locale.getDefault(), 
                "Tại thời điểm hiện tại (%d tín), bạn cần xử lý ít nhất %d môn để đạt GPA %.2f:\n", 
                currentTinChi, suggestList.size(), targetGPA));
        } else {
            sb.append(String.format(Locale.getDefault(), 
                "Kể cả xử lý hết danh sách dưới đây, GPA cũng chỉ đạt %.2f. Bạn cần học thêm môn mới để đạt mục tiêu %.2f:\n", 
                simulatedGPA, targetGPA));
        }
        
        if (improveCount > 0) sb.append(String.format("• Học cải thiện: %d môn (Điểm D, D+, C)\n", improveCount));
        if (retakeCount > 0) sb.append(String.format("• Học lại: %d môn (Điểm F)\n", retakeCount));
        
        tvSuggestImprove.setText(sb.toString());

        MonHocAdapter adapter = new MonHocAdapter(this, suggestList);
        lvSuggestMonHoc.setAdapter(adapter);
        lvSuggestMonHoc.setNestedScrollingEnabled(true);
    }

    private String getBreakdownText(float avgNeeded, int N) {
        float[] grades = {4.0f, 3.5f, 3.0f, 2.5f, 2.0f, 1.5f, 1.0f};
        String[] labels = {"A", "B+", "B", "C+", "C", "D+", "D"};

        if (avgNeeded <= 1.0) return "Bạn chỉ cần đạt trung bình điểm D cho các môn còn lại.";

        int i = 0;
        while (i < grades.length - 1 && grades[i] > avgNeeded) {
            i++;
        }
        
        float G1 = grades[i-1];
        float G2 = grades[i];
        String L1 = labels[i-1];
        String L2 = labels[i];

        int x = Math.round(N * (avgNeeded - G2) / (G1 - G2));
        int y = N - x;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(), "Lộ trình ra trường (còn %d tín): Bạn cần đạt tối thiểu:\n", N));
        if (x > 0) sb.append(String.format(Locale.getDefault(), " • %d tín chỉ điểm %s\n", x, L1));
        if (y > 0) sb.append(String.format(Locale.getDefault(), " • %d tín chỉ điểm %s\n", y, L2));
        
        return sb.toString();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCurrentStats();
    }
}
