
package com.example.btl_android.controller;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.btl_android.R;
import com.example.btl_android.model.dao.MonHocDAO;
import com.example.btl_android.model.entity.MonHoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoPhongGPAActivity extends AppCompatActivity {

    private LinearLayout layoutDanhSachMon;
    private TextView tvGPAHienTai;
    private TextView tvGPADuKien;
    private TextView tvChenhLechGPA;
    private final List<MonHoc> danhSachMon = new ArrayList<>();
    private final List<EditText> danhSachONhap = new ArrayList<>();
    private float gpaHienTai;
    private int tongTinChi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mo_phong_gpa);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        layoutDanhSachMon = findViewById(R.id.layoutDanhSachMon);
        tvGPAHienTai = findViewById(R.id.tvGPAHienTai);
        tvGPADuKien = findViewById(R.id.tvGPADuKien);
        tvChenhLechGPA = findViewById(R.id.tvChenhLechGPA);

        Button btnTinh = findViewById(R.id.btnTinhGPAduKien);
        btnTinh.setOnClickListener(v -> tinhGPADuKien());

        taiDanhSachMon();
    }

    private void taiDanhSachMon() {
        MonHocDAO monHocDAO = new MonHocDAO(this);
        List<MonHoc> tatCaMon = monHocDAO.getAllMonHoc();
        Map<String, MonHoc> monTotNhat = new HashMap<>();

        for (MonHoc mon : tatCaMon) {
            String tenMon = mon.getTenMon();

            if (tenMon == null || tenMon.trim().isEmpty()) {
                continue;
            }

            String diemChu = mon.getDiemChu();

            if (diemChu == null || diemChu.trim().isEmpty()) {
                continue;
            }

            String trangThai = mon.getTrangThai();

            if (trangThai != null
                    && trangThai.equalsIgnoreCase("Đang học")) {
                continue;
            }

            String key = tenMon.trim()
                    .replaceAll("\\s+", " ")
                    .toLowerCase(Locale.ROOT);

            MonHoc monCu = monTotNhat.get(key);

            if (monCu == null
                    || mon.getDiemTongKet4() > monCu.getDiemTongKet4()) {
                monTotNhat.put(key, mon);
            }
        }

        danhSachMon.clear();
        danhSachMon.addAll(monTotNhat.values());

        danhSachMon.sort((a, b) ->
                a.getTenMon().compareToIgnoreCase(b.getTenMon()));

        tongTinChi = 0;
        float tongDiem = 0;

        for (MonHoc mon : danhSachMon) {
            if (mon.getSoTinChi() <= 0) {
                continue;
            }

            tongTinChi += mon.getSoTinChi();
            tongDiem += mon.getDiemTongKet4() * mon.getSoTinChi();
        }

        gpaHienTai = tongTinChi > 0 ? tongDiem / tongTinChi : 0;

        tvGPAHienTai.setText(String.format(
                Locale.getDefault(),
                "GPA hiện tại: %.2f/4.00 (%d tín chỉ)",
                gpaHienTai,
                tongTinChi
        ));

        if (danhSachMon.isEmpty()) {
            TextView tvTrong = new TextView(this);
            tvTrong.setText("Chưa có môn học đủ dữ liệu để mô phỏng.");
            tvTrong.setTextSize(15);
            layoutDanhSachMon.addView(tvTrong);
            return;
        }

        for (MonHoc mon : danhSachMon) {
            themDongMon(mon);
        }
    }

    private void themDongMon(MonHoc mon) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 12, 16, 12);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        TextView tvTenMon = new TextView(this);
        tvTenMon.setText(mon.getTenMon());
        tvTenMon.setTextSize(16);
        tvTenMon.setTypeface(null, Typeface.BOLD);
        tvTenMon.setTextColor(Color.rgb(40, 40, 40));
        card.addView(tvTenMon);

        TextView tvThongTin = new TextView(this);
        tvThongTin.setText(String.format(
                Locale.getDefault(),
                "Điểm hiện tại: %.2f/4.00  |  %d tín chỉ",
                mon.getDiemTongKet4(),
                mon.getSoTinChi()
        ));
        tvThongTin.setTextSize(14);
        tvThongTin.setTextColor(Color.DKGRAY);
        card.addView(tvThongTin);

        EditText edtDiem = new EditText(this);
        edtDiem.setSingleLine(true);
        edtDiem.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED
        );
        edtDiem.setHint("Điểm giả định (0 - 4), để trống nếu giữ nguyên");
        edtDiem.setTextSize(14);
        card.addView(edtDiem);

        danhSachONhap.add(edtDiem);
        layoutDanhSachMon.addView(card);
    }

    private void tinhGPADuKien() {
        if (tongTinChi <= 0) {
            Toast.makeText(
                    this,
                    "Không có đủ dữ liệu tín chỉ để tính.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        float tongDiemDuKien = 0;

        for (int i = 0; i < danhSachMon.size(); i++) {
            MonHoc mon = danhSachMon.get(i);

            if (mon.getSoTinChi() <= 0) {
                continue;
            }

            float diem = mon.getDiemTongKet4();
            String input = danhSachONhap.get(i).getText()
                    .toString().trim().replace(',', '.');

            if (!input.isEmpty()) {
                try {
                    diem = Float.parseFloat(input);
                } catch (NumberFormatException e) {
                    danhSachONhap.get(i).setError("Điểm không hợp lệ");
                    return;
                }

                if (diem < 0 || diem > 4) {
                    danhSachONhap.get(i).setError(
                            "Điểm phải nằm trong khoảng 0 đến 4"
                    );
                    return;
                }
            }

            tongDiemDuKien += diem * mon.getSoTinChi();
        }

        float gpaDuKien = tongDiemDuKien / tongTinChi;
        float chenhLech = gpaDuKien - gpaHienTai;

        tvGPADuKien.setText(String.format(
                Locale.getDefault(),
                "GPA dự kiến: %.2f/4.00",
                gpaDuKien
        ));

        tvChenhLechGPA.setText(String.format(
                Locale.getDefault(),
                "Mức thay đổi: %+.2f điểm",
                chenhLech
        ));

        tvChenhLechGPA.setTextColor(
                chenhLech > 0 ? Color.rgb(46, 125, 50)
                        : chenhLech < 0 ? Color.RED
                          : Color.DKGRAY
        );
    }
}
