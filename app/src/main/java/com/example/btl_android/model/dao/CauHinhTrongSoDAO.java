package com.example.btl_android.model.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.btl_android.model.database.DBHelper;
import com.example.btl_android.model.entity.CauHinhTrongSo;

public class CauHinhTrongSoDAO {
    private DBHelper dbHelper;

    public CauHinhTrongSoDAO(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    /**
     * Lấy cấu hình trọng số đầu tiên trong bảng.
     * Ứng dụng chỉ có 1 cấu hình chung cho toàn bộ sinh viên.
     * @return Đối tượng CauHinhTrongSo hoặc null nếu chưa có dữ liệu.
     */
    public CauHinhTrongSo getCauHinh() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        CauHinhTrongSo cauHinh = null;
        // Lấy bản ghi đầu tiên (LIMIT 1)
        Cursor cursor = db.rawQuery("SELECT * FROM CauHinhTrongSo LIMIT 1", null);

        if (cursor != null && cursor.moveToFirst()) {
            cauHinh = new CauHinhTrongSo(
                    cursor.getInt(0), // id
                    cursor.getString(1), // ten_he_dao_tao
                    cursor.getFloat(2), // trong_so_tx1
                    cursor.getFloat(3), // trong_so_tx2
                    cursor.getFloat(4), // trong_so_tx3
                    cursor.getFloat(5)  // trong_so_thi
            );
            cursor.close();
        }
        return cauHinh;
    }

    /**
     * Cập nhật cấu hình trọng số.
     * @param cauHinh Đối tượng chứa thông tin mới.
     * @return true nếu cập nhật thành công, false nếu thất bại.
     */
    public boolean updateCauHinh(CauHinhTrongSo cauHinh) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("ten_he_dao_tao", cauHinh.getTenHeDaoTao());
        values.put("trong_so_tx1", cauHinh.getTrongSoTx1());
        values.put("trong_so_tx2", cauHinh.getTrongSoTx2());
        values.put("trong_so_tx3", cauHinh.getTrongSoTx3());
        values.put("trong_so_thi", cauHinh.getTrongSoThi());

        // Cập nhật dựa trên ID (thường là ID = 1 vì chỉ có 1 dòng cấu hình)
        int rows = db.update("CauHinhTrongSo", values, "id = ?", new String[]{String.valueOf(cauHinh.getId())});
        return rows > 0;
    }

    /**
     * Thêm mới cấu hình (chỉ dùng khi khởi tạo lần đầu nếu chưa có Seed Data).
     */
    public long insertCauHinh(CauHinhTrongSo cauHinh) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put("ten_he_dao_tao", cauHinh.getTenHeDaoTao());
        values.put("trong_so_tx1", cauHinh.getTrongSoTx1());
        values.put("trong_so_tx2", cauHinh.getTrongSoTx2());
        values.put("trong_so_tx3", cauHinh.getTrongSoTx3());
        values.put("trong_so_thi", cauHinh.getTrongSoThi());

        return db.insert("CauHinhTrongSo", null, values);
    }
}