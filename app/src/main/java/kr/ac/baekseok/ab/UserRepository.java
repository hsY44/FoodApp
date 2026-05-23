package kr.ac.baekseok.ab;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// 사용자 인증/등록/알러지 관련 DB 작업 Repository
// DB 버전 3: 비밀번호를 SHA-256 해시값으로 저장 (보안 강화)
// 버전 업그레이드 시 기존 평문 비밀번호와 호환 불가 → 재가입 필요
public class UserRepository {

    private static final String DB_NAME = "LoginDB";
    private static final String TABLE_NAME = "JoinInfo";
    private static final String GUEST_PREF = "guest_data";
    private static final String KEY_GUEST_ALLERGY = "allergy";

    private final DBHelper dbHelper;
    private final SharedPreferences guestPrefs;

    public UserRepository(Context context) {
        dbHelper = new DBHelper(context);
        guestPrefs = context.getApplicationContext()
                .getSharedPreferences(GUEST_PREF, Context.MODE_PRIVATE);
    }

    public boolean isIdRegistered(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE uId = ?", new String[]{id});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        db.close();
        return exists;
    }

    // 로그인 - 입력 비밀번호를 SHA-256 해싱하여 DB 저장값과 비교
    public boolean login(String id, String pw) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_NAME + " WHERE uId = ? AND uPassword = ?",
                new String[]{id, PasswordUtil.hash(pw)});
        boolean success = cursor.moveToFirst();
        cursor.close();
        db.close();
        return success;
    }

    // 회원가입 - 비밀번호를 SHA-256으로 해싱하여 저장
    public void register(String id, String pw, String allergy) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String allergyVal = (allergy == null || allergy.trim().isEmpty()) ? "없음" : allergy.trim();
        db.execSQL("INSERT INTO " + TABLE_NAME + " VALUES(?, ?, ?)",
                new Object[]{id, PasswordUtil.hash(pw), allergyVal});
        db.close();
    }

    public String getAllergy(String id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT allergy FROM " + TABLE_NAME + " WHERE uId = ?", new String[]{id});
        String allergy = "없음";
        if (cursor.moveToFirst()) {
            String val = cursor.getString(0);
            allergy = (val != null && !val.isEmpty()) ? val : "없음";
        }
        cursor.close();
        db.close();
        return allergy;
    }

    public void updateAllergy(String id, String allergy) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String allergyVal = (allergy == null || allergy.trim().isEmpty()) ? "없음" : allergy.trim();
        db.execSQL("UPDATE " + TABLE_NAME + " SET allergy = ? WHERE uId = ?",
                new Object[]{allergyVal, id});
        db.close();
    }

    // 게스트 알러지 - SharedPreferences에 저장 (DB 계정 없음)
    public String getGuestAllergy() {
        return guestPrefs.getString(KEY_GUEST_ALLERGY, "없음");
    }

    public void updateGuestAllergy(String allergy) {
        String val = (allergy == null || allergy.trim().isEmpty()) ? "없음" : allergy.trim();
        guestPrefs.edit().putString(KEY_GUEST_ALLERGY, val).apply();
    }

    public void clearGuestAllergy() {
        guestPrefs.edit().remove(KEY_GUEST_ALLERGY).apply();
    }

    // 비밀번호 변경 - 새 비밀번호를 SHA-256으로 해싱하여 저장
    public void updatePassword(String id, String newPw) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NAME + " SET uPassword = ? WHERE uId = ?",
                new Object[]{PasswordUtil.hash(newPw), id});
        db.close();
    }

    private static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, DB_NAME, null, 3); // 버전 3: SHA-256 해싱 도입
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_NAME +
                    "(uId TEXT, uPassword TEXT, allergy TEXT DEFAULT '없음');");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 3) {
                // 버전 1,2 → 3: 비밀번호 해싱 방식 변경으로 기존 데이터 호환 불가
                // 기존 평문 비밀번호를 재해싱하는 것이 불가능하므로 테이블 재생성
                // 사용자는 재가입이 필요합니다
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
                onCreate(db);
            }
        }
    }
}
