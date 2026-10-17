package com.example.btl_android.model.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.btl_android.model.database.DBHelper;
import com.example.btl_android.model.entity.MonHoc;

import java.util.ArrayList;
import java.util.List;

public class MonHocDAO {
    private DBHelper dbHelper;

    public MonHocDAO(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    public List<MonHoc> getAllMonHoc() {
        List<MonHoc> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM MonHoc", null);
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToMonHoc(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public List<MonHoc> getMonHocByKy(int kyHocId) {
        List<MonHoc> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM MonHoc WHERE ky_hoc_id = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(kyHocId)});
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToMonHoc(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private MonHoc cursorToMonHoc(Cursor cursor) {
        Float diemTx3 = null;
        int tx3Index = cursor.getColumnIndex("diem_tx3");
        if (!cursor.isNull(tx3Index)) {
            diemTx3 = cursor.getFloat(tx3Index);
        }
        return new MonHoc(
                cursor.getInt(0),
                cursor.getInt(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getFloat(4),
                cursor.getFloat(5),
                diemTx3,
                cursor.getFloat(7),
                cursor.getFloat(8),
                cursor.getFloat(9),
                cursor.getString(10),
                cursor.getString(11)
        );
    }

    public long insertMonHoc(MonHoc mh) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = getMonHocContentValues(mh);
        return db.insert("MonHoc", null, values);
    }

    public boolean updateMonHoc(MonHoc mh) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = getMonHocContentValues(mh);
        return db.update("MonHoc", values, "id = ?", new String[]{String.valueOf(mh.getId())}) > 0;
    }

    public boolean deleteMonHocByKyHocId(int kyHocId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("MonHoc", "ky_hoc_id = ?", new String[]{String.valueOf(kyHocId)}) > 0;
    }

    private ContentValues getMonHocContentValues(MonHoc mh) {
        ContentValues values = new ContentValues();
        values.put("ky_hoc_id", mh.getKyHocId());
        values.put("ten_mon", mh.getTenMon());
        values.put("so_tin_chi", mh.getSoTinChi());
        values.put("diem_tx1", mh.getDiemTx1());
        values.put("diem_tx2", mh.getDiemTx2());
        if (mh.getDiemTx3() != null) values.put("diem_tx3", mh.getDiemTx3());
        else values.putNull("diem_tx3");
        values.put("diem_thi", mh.getDiemThi());
        values.put("diem_tong_ket_10", mh.getDiemTongKet10());
        values.put("diem_tong_ket_4", mh.getDiemTongKet4());
        values.put("diem_chu", mh.getDiemChu());
        values.put("trang_thai", mh.getTrangThai());
        return values;
    }

    /**
     * Đồng bộ CPA và Tín chỉ tích lũy theo quy tắc:
     * - Nếu có nhiều lần học (trùng tên môn), chỉ lấy điểm hệ 4 CAO NHẤT.
     * - Môn điểm F (0.0) chưa học lại sẽ tính là 0.
     */
    public void syncGlobalStats() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String query =
                "SELECT SUM(m.diem_tong_ket_4 * m.so_tin_chi), " +
                        "SUM(m.so_tin_chi) " +
                        "FROM MonHoc m " +
                        "WHERE m.diem_chu IS NOT NULL " +
                        "AND TRIM(m.diem_chu) != '' " +
                        "AND NOT EXISTS (" +
                        "SELECT 1 FROM MonHoc m2 " +
                        "WHERE LOWER(TRIM(m2.ten_mon)) = LOWER(TRIM(m.ten_mon)) " +
                        "AND m2.diem_chu IS NOT NULL " +
                        "AND TRIM(m2.diem_chu) != '' " +
                        "AND (" +
                        "m2.diem_tong_ket_4 > m.diem_tong_ket_4 " +
                        "OR (m2.diem_tong_ket_4 = m.diem_tong_ket_4 " +
                        "AND m2.id < m.id)" +
                        ")" +
                        ")";

        float tongDiemTichLuy = 0;
        int tongTinChi = 0;

        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor.moveToFirst() && !cursor.isNull(1)) {
                tongDiemTichLuy = cursor.getFloat(0);
                tongTinChi = cursor.getInt(1);
            }
        }

        float cpa = tongTinChi > 0
                ? tongDiemTichLuy / tongTinChi
                : 0;

        ContentValues values = new ContentValues();
        values.put("cpa_hien_tai", cpa);
        values.put("tong_tin_chi_tich_luy", tongTinChi);

        db.update("SinhVien", values, "id = 1", null);
    }
}