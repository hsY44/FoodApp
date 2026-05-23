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

// TTS 공통 헬퍼
// speakAndThen(): 발화 완료 후 콜백 실행 - 회원가입 완료 후 화면 전환처럼 TTS가 끝난 뒤 동작해야 할 때 사용
// speak(): 단순 발화 (완료 콜백 없음)
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
                // 콜백이 등록된 발화가 완료되면 메인 스레드에서 콜백 실행
                if (UID_CALLBACK.equals(utteranceId) && completionCallback != null) {
                    Runnable cb = completionCallback;
                    completionCallback = null; // 한 번만 실행되도록 즉시 초기화
                    mainHandler.post(cb);
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "발화 오류: " + utteranceId);
                // 오류 시에도 콜백 실행 - TTS 오류로 화면 전환이 막히지 않도록
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

    // 단순 발화 - 완료 콜백 없음
    public void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UID_PLAIN);
        }
    }

    // 발화 완료 후 콜백 실행 - 화면 전환처럼 TTS가 끝난 뒤 동작해야 할 때 사용
    // onDone 또는 onError 둘 다에서 콜백이 실행되므로 TTS 오류 시에도 동작이 막히지 않음
    public void speakAndThen(String text, Runnable onDone) {
        if (tts != null) {
            this.completionCallback = onDone;
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UID_CALLBACK);
        } else if (onDone != null) {
            // TTS 자체가 null이면 콜백만 즉시 실행
            mainHandler.post(onDone);
        }
    }

    // 터치(손가락 닿는 순간)에 버튼 이름을 TTS로 안내
    // ACTION_DOWN에서 발화 후 false를 반환해 클릭 이벤트가 정상 처리되도록 함
    public void bindTouchTts(View view, String text) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                speak(text);
            }
            return false;
        });
    }

    // Activity onDestroy()에서 반드시 호출
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
