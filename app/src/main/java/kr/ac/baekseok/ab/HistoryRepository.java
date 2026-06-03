package kr.ac.baekseok.ab;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// 분류 히스토리 저장/조회를 담당하는 Repository
// LoginDB와 분리된 별도 DB(HistoryDB)를 사용하여 사용자 데이터와 충돌 방지
// DB 버전 2: userId 컬럼 추가 - 사용자별 히스토리 분리
public class HistoryRepository {

    private static final String DB_NAME = "HistoryDB";
    private static final String TABLE_NAME = "ClassifyHistory";
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREAN);

    private final DBHelper dbHelper;

    public HistoryRepository(Context context) {
        dbHelper = new DBHelper(context);
    }

    // 분류 결과 1건 저장 - ClassifierViewModel에서 분류 후 호출
    public void save(String userId, String source, String result, float probability) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String timestamp = DATE_FORMAT.format(new Date());
        db.execSQL(
                "INSERT INTO " + TABLE_NAME + " (userId, source, result, probability, timestamp) VALUES(?, ?, ?, ?, ?)",
                new Object[]{userId, source, result, probability, timestamp});
        db.close();
    }

    // 해당 사용자의 히스토리 조회 - 최신순(id DESC) 정렬
    public List<HistoryRecord> getAll(String userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, source, result, probability, timestamp FROM " + TABLE_NAME +
                " WHERE userId = ? ORDER BY id DESC",
                new String[]{userId});

        List<HistoryRecord> list = new ArrayList<>();
        while (cursor.moveToNext()) {
            list.add(new HistoryRecord(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getFloat(3),
                    cursor.getString(4)));
        }
        cursor.close();
        db.close();
        return list;
    }

    // 해당 사용자의 히스토리 전체 삭제
    public void deleteAll(String userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE userId = ?", new Object[]{userId});
        db.close();
    }

    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DB_NAME, null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME +
                    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "userId TEXT, source TEXT, result TEXT, probability REAL, timestamp TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
