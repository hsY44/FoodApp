package kr.ac.baekseok.ab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.IOException;
import java.util.Locale;

// 갤러리 선택 화면 - UI만 담당, 분류 로직은 ClassifierViewModel에 위임
// CameraActivity와 동일한 ClassifierViewModel 구조 사용
public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "[IC]GalleryActivity";
    private static final int GALLERY_IMAGE_REQUEST_CODE = 1;
    private static final String KEY_USER_ID = "USER_ID";

    private ClassifierViewModel viewModel;
    private TtsHelper tts;

    private ImageView imageView;
    private TextView textView;
    private Button selectBtn;
    private ProgressBar progressBar;

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        tts = new TtsHelper(this);
        userId = getIntent().getStringExtra(KEY_USER_ID);

        viewModel = new ViewModelProvider(this).get(ClassifierViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                tts.speakAndThen("뒤로 가기", this::finish));

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        selectBtn = findViewById(R.id.selectBtn);
        progressBar = findViewById(R.id.progressBar);

        tts.bindTouchTts(selectBtn, "사진 선택");
        selectBtn.setOnClickListener(v -> getImageFromGallery());

        viewModel.isReady.observe(this, ready ->
                selectBtn.setEnabled(Boolean.TRUE.equals(ready)));

        viewModel.isClassifying.observe(this, classifying -> {
            if (Boolean.TRUE.equals(classifying)) {
                progressBar.setVisibility(View.VISIBLE);
                selectBtn.setVisibility(View.GONE);
                textView.setText("분석 중...");
            } else {
                progressBar.setVisibility(View.GONE);
                selectBtn.setVisibility(View.VISIBLE);
            }
        });

        viewModel.result.observe(this, result -> {
            if (result == null) return;

            if (result.isLowConfidence()) {
                textView.setText("인식 불가 (신뢰도 부족)\n다른 이미지를 선택해주세요.");
                tts.speak("이미지를 인식하지 못했습니다. 다른 이미지를 선택해주세요.");
                return;
            }

            String ingredients = result.foodAllergens.isEmpty()
                    ? "성분 정보 없음"
                    : String.join(", ", result.foodAllergens);

            textView.setText(String.format(Locale.KOREAN,
                    "%s (%.1f%%)\n주요 성분: %s", result.foodName, result.probability * 100, ingredients));

            tts.speak(String.format(Locale.KOREAN,
                    "분류 결과: %s, 확률 %.0f퍼센트", result.foodName, result.probability * 100));

            if (result.hasAllergyWarning()) showAllergyWarning(result);
        });
    }

    private void getImageFromGallery() {
        startActivityForResult(
                new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"),
                GALLERY_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != GALLERY_IMAGE_REQUEST_CODE) return;
        if (data == null) return;

        Bitmap bitmap = decodeBitmap(data.getData());
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            // ViewModel에 분류 요청 - 백그라운드 스레드에서 처리
            viewModel.classify(bitmap, userId, "갤러리");
        }
    }

    private Bitmap decodeBitmap(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                return ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
        } catch (IOException e) {
            Log.e(TAG, "이미지 읽기 실패", e);
            return null;
        }
    }

    private void showAllergyWarning(ClassificationResult result) {
        String matchedStr = String.join(", ", result.matchedAllergens);
        new AlertDialog.Builder(this)
                .setTitle("⚠️ 알러지 경고")
                .setMessage("이 식품에 내 알러지 성분이 포함되어 있습니다!\n\n주의 성분: " + matchedStr)
                .setPositiveButton("확인", null)
                .show();
        tts.speak("경고! 알러지 성분이 포함되어 있습니다. 주의 성분: " + matchedStr);
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }
}
