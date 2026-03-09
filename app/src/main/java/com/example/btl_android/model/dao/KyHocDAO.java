package com.example.btl_android.model.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.btl_android.model.database.DBHelper;
import com.example.btl_android.model.entity.KyHoc;

import java.util.ArrayList;
import java.util.List;

public class KyHocDAO {
    private DBHelper dbHelper;

    public KyHocDAO(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    // Lấy tất cả kỳ bao gồm cả kỳ ẩn (Dùng cho tính toán CPA tổng)
    public List<KyHoc> getAllKyHoc(int sinhVienId) {
        List<KyHoc> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM KyHoc WHERE sinh_vien_id = ? ORDER BY id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sinhVienId)});
        if (cursor.moveToFirst()) {
            do {
                list.add(new KyHoc(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), 
                        cursor.getFloat(3), cursor.getInt(4), cursor.getInt(5) == 1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Lấy danh sách kỳ để hiển thị lên màn hình (Ẩn kỳ kỹ thuật gom dữ liệu)
    public List<KyHoc> getDisplayKyHoc(int sinhVienId) {
        List<KyHoc> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // SỬA LỖI: Chỉ loại bỏ đúng kỳ "Dữ liệu từ Website", cho phép hiện "Kỳ 1, Kỳ 2..."
        String query = "SELECT * FROM KyHoc WHERE sinh_vien_id = ? AND ten_ky != 'Dữ liệu từ Website' ORDER BY id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sinhVienId)});
        if (cursor.moveToFirst()) {
            do {
                list.add(new KyHoc(cursor.getInt(0), cursor.getInt(1), cursor.getString(2), 
                        cursor.getFloat(3), cursor.getInt(4), cursor.getInt(5) == 1));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // Xóa dữ liệu cũ trước khi cào mới
    public void deleteDisplayKyHoc(int sinhVienId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("KyHoc", "sinh_vien_id = ? AND ten_ky LIKE 'Kỳ %'", new String[]{String.valueOf(sinhVienId)});
    }

    public long insertKyHoc(KyHoc kyHoc) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("sinh_vien_id", kyHoc.getSinhVienId());
        values.put("ten_ky", kyHoc.getTenKy());
        values.put("gpa_ky", kyHoc.getGpaKy());
        values.put("tong_tin_chi_ky", kyHoc.getTongTinChiKy());
        values.put("trang_thai", kyHoc.isTrangThai() ? 1 : 0);
        return db.insert("KyHoc", null, values);
    }

    public boolean updateKyHoc(KyHoc kyHoc) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("ten_ky", kyHoc.getTenKy());
        values.put("gpa_ky", kyHoc.getGpaKy());
        values.put("tong_tin_chi_ky", kyHoc.getTongTinChiKy());
        values.put("trang_thai", kyHoc.isTrangThai() ? 1 : 0);
        return db.update("KyHoc", values, "id = ?", new String[]{String.valueOf(kyHoc.getId())}) > 0;
    }

    public boolean deleteKyHoc(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("MonHoc", "ky_hoc_id = ?", new String[]{String.valueOf(id)});
        return db.delete("KyHoc", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }
}