package kr.ac.baekseok.ab;

import android.content.Context;
import android.content.SharedPreferences;

// 앱을 껐다 켜도 로그인 상태를 유지하기 위해 내부 저장소에 사용자 ID를 저장하는 클래스
// 게스트 로그인은 저장하지 않음
public class SessionManager {

    private static final String PREF = "session";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    // commit() 사용 이유: apply()는 저장 전에 앱이 강제 종료되면 데이터가 사라질 수 있음
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
