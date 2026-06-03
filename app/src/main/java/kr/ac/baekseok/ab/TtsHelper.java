package kr.ac.baekseok.ab;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

// 음성 안내(TTS) 헬퍼
// speakAndThen(): 말이 끝난 뒤 다음 동작 실행 (예: 회원가입 완료 안내 → 화면 전환)
// speak(): 단순 음성 안내
public class TtsHelper implements TextToSpeech.OnInitListener {

    private static final String TAG = "[TTS]";
    private static final String UID_PLAIN = "UID_PLAIN";
    private static final String UID_CALLBACK = "UID_CALLBACK";

    private TextToSpeech tts;
    private Runnable completionCallback;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public TtsHelper(Context context) {
        tts = new TextToSpeech(context, this);

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {}

            @Override
            public void onDone(String utteranceId) {
                Log.i(TAG, "발화 완료: " + utteranceId);
                // 말이 끝나면 화면 처리 스레드에서 다음 동작 실행
                if (UID_CALLBACK.equals(utteranceId) && completionCallback != null) {
                    Runnable cb = completionCallback;
                    completionCallback = null; // 한 번만 실행되도록 즉시 초기화
                    mainHandler.post(cb);
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "발화 오류: " + utteranceId);
                // 음성 오류가 나도 콜백은 실행 - TTS 오류로 화면 전환이 막히지 않도록
                if (UID_CALLBACK.equals(utteranceId) && completionCallback != null) {
                    Runnable cb = completionCallback;
                    completionCallback = null;
                    mainHandler.post(cb);
                }
            }
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.KOREAN);
            tts.setSpeechRate(1.0f);
            tts.setPitch(1.0f);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "한국어 TTS 미지원 기기");
            }
        } else {
            Log.e(TAG, "TTS 초기화 실패");
        }
    }

    // 단순 음성 안내 - 끝난 뒤 별도 동작 없음
    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UID_PLAIN);
        }
    }

    // 말이 끝난 뒤 다음 동작 실행 - 화면 전환처럼 TTS가 끝나야 할 때 사용
    // 성공/실패 모두 콜백을 실행해 TTS 오류로 화면 전환이 막히지 않도록 함
    public void speakAndThen(String text, Runnable onDone) {
        if (tts != null) {
            this.completionCallback = onDone;
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UID_CALLBACK);
        } else if (onDone != null) {
            // TTS 자체가 null이면 콜백만 즉시 실행
            mainHandler.post(onDone);
        }
    }

    // 손가락이 버튼에 닿는 순간 버튼 이름을 음성으로 안내
    // false를 반환해야 클릭 이벤트도 정상 동작함
    public void bindTouchTts(View view, String text) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                speak(text);
            }
            return false;
        });
    }

    // 화면 종료 시 반드시 호출
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
