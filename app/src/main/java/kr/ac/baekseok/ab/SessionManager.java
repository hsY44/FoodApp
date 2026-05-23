package kr.ac.baekseok.ab;

import android.content.Context;
import android.content.SharedPreferences;

// 앱 재시작 후에도 로그인 상태를 유지하기 위한 SharedPreferences 래퍼
// 게스트 로그인은 저장하지 않음
public class SessionManager {

    private static final String PREF = "session";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // commit(): 동기 쓰기 - apply()는 비동기라 프로세스 강제 종료 시 디스크 미기록 가능
    public void save(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).commit();
    }

    public String getSavedUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void clear() {
        prefs.edit().remove(KEY_USER_ID).commit();
    }
}
