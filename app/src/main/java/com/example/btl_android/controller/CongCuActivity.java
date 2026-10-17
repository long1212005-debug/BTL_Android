package com.example.btl_android.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.example.btl_android.R;

public class CongCuActivity extends AppCompatActivity {

    private CardView btnTinhGPA, btnCaiThienDiem, btnMoPhongGPA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cong_cu);

        // Khởi tạo Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Ánh xạ Views
        btnTinhGPA = findViewById(R.id.btnTinhGPA);
        btnCaiThienDiem = findViewById(R.id.btnCaiThienDiem);
        btnMoPhongGPA = findViewById(R.id.btnMoPhongGPA);

        // Thiết lập sự kiện click
        setupClickListeners();
    }

    private void setupClickListeners() {
        btnTinhGPA.setOnClickListener(v -> {
            // Mở màn hình tính điểm cần thi
            Intent intent = new Intent(CongCuActivity.this, TinhDiemCanThiActivity.class);
            startActivity(intent);
        });

        btnCaiThienDiem.setOnClickListener(v -> {
            // Mở màn hình lộ trình học tập thông minh
            Intent intent = new Intent(CongCuActivity.this, LoTrinhHocTapActivity.class);
            startActivity(intent);
        });
        btnMoPhongGPA.setOnClickListener(v -> {
            Intent intent = new Intent(
                    CongCuActivity.this,
                    MoPhongGPAActivity.class
            );
            startActivity(intent);
        });
    }
}