package com.example.btl_android.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.btl_android.R;
import com.example.btl_android.adapters.KyHocAdapter;
import com.example.btl_android.model.dao.KyHocDAO;
import com.example.btl_android.model.dao.MonHocDAO;
import com.example.btl_android.model.dao.SinhVienDAO;
import com.example.btl_android.model.entity.KyHoc;
import com.example.btl_android.model.entity.SinhVien;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ListView lvKyHoc;
    private TextView tvEmpty, tvCpaHienTai, tvTongTinChiTichLuy, tvSoKyHoc;
    private KyHocAdapter adapter;
    private List<KyHoc> listDisplayKyHoc;
    private SinhVienDAO sinhVienDAO;
    private KyHocDAO kyHocDAO;
    private MonHocDAO monHocDAO;
    private ImageButton btnReSyncData; // Nút làm mới dữ liệu
    private LinearLayout btnTrangChu, btnThongKe, btnCongCu, btnCaiDat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initData();
        setupClickListeners();
    }

    private void initViews() {
        lvKyHoc = findViewById(R.id.lvKyHoc);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvCpaHienTai = findViewById(R.id.tvCpaHienTai);
        tvTongTinChiTichLuy = findViewById(R.id.tvTongTinChiTichLuy);
        tvSoKyHoc = findViewById(R.id.tvSoKyHoc);
        btnReSyncData = findViewById(R.id.btnReSyncData);
        
        btnTrangChu = findViewById(R.id.btnTrangChu);
        btnThongKe = findViewById(R.id.btnThongKe);
        btnCongCu = findViewById(R.id.btnCongCu);
        btnCaiDat = findViewById(R.id.btnCaiDat);
    }

    private void initData() {
        kyHocDAO = new KyHocDAO(this);
        sinhVienDAO = new SinhVienDAO(this);
        monHocDAO = new MonHocDAO(this);
        refreshDisplay();
    }

    private void refreshDisplay() {
        int svId = 1;
        List<KyHoc> allKyHoc = kyHocDAO.getAllKyHoc(svId);
        
        int tongTinChi = 0;
        float tongDiemTichLuy = 0;

        for (KyHoc kh : allKyHoc) {
            if (kh.getTongTinChiKy() > 0) {
                tongTinChi += kh.getTongTinChiKy();
                tongDiemTichLuy += (kh.getGpaKy() * kh.getTongTinChiKy());
            }
        }

        float cpa = (tongTinChi > 0) ? (tongDiemTichLuy / tongTinChi) : 0;

        tvCpaHienTai.setText(String.format(Locale.getDefault(), "%.2f", cpa));
        tvTongTinChiTichLuy.setText(String.valueOf(tongTinChi));

        listDisplayKyHoc = kyHocDAO.getDisplayKyHoc(svId);
        tvSoKyHoc.setText(listDisplayKyHoc.size() + " kỳ");
        
        adapter = new KyHocAdapter(this, listDisplayKyHoc);
        lvKyHoc.setAdapter(adapter);

        if (listDisplayKyHoc.isEmpty()) {
            lvKyHoc.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
        } else {
            lvKyHoc.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        lvKyHoc.setOnItemClickListener((parent, view, position, id) -> {
            KyHoc kyHoc = listDisplayKyHoc.get(position);
            Intent intent = new Intent(MainActivity.this, ChiTietKyHocActivity.class);
            intent.putExtra("kyHoc", kyHoc);
            startActivity(intent);
        });

        // Nút lấy lại dữ liệu từ web
        btnReSyncData.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LayDuLieuWebViewActivity.class);
            startActivity(intent);
        });

        btnTrangChu.setOnClickListener(v -> refreshDisplay());
        btnThongKe.setOnClickListener(v -> startActivity(new Intent(this, ThongKe.class)));
        btnCongCu.setOnClickListener(v -> startActivity(new Intent(this, CongCuActivity.class)));
        
        // CHUYỂN MÀN HÌNH LIÊN HỆ: Đã sửa từ Toast sang startActivity
        btnCaiDat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CaiDatActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDisplay();
    }
}