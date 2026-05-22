package kr.ac.baekseok.ab;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.List;

// 저수준 TFLite API를 직접 사용하는 분류기 구현체
// ByteBuffer를 직접 다루고 이미지를 그레이스케일로 변환하여 추론 수행
// ClassiferWithModel(Support Library 기반)보다 세밀한 제어가 필요할 때 사용
public class Classifer implements IClassifier {

    private static final String MODEL_NAME = "Inq1.tflite";
    private static final String LABEL_FILE = "labels.txt";

    Context context;
    Interpreter interpreter = null;
    int modelInputWidth, modelInputHeight, modelInputChannel;
    int modelOutputClasses;

    // 분류 인덱스를 라벨명으로 변환하기 위한 라벨 목록
    private List<String> labels;

    // init() 완료 여부 - SafeClassifierDecorator에서 사전 검증에 사용
    private boolean isInitialized = false;

    public Classifer(Context context) {
        this.context = context;
    }

    @Override
    public void init() throws IOException {
        interpreter = getInterpreter();
        initModelShape();
        labels = FileUtil.loadLabels(context, LABEL_FILE);
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    // AssetManager를 통해 모델 파일을 메모리 매핑 방식으로 로드
    // FileUtil.loadMappedFile() 과 동일한 역할을 직접 구현한 버전
    private ByteBuffer loadModelFile(String modelName) throws IOException {
        AssetManager am = context.getAssets();
        AssetFileDescriptor afd = am.openFd(modelName);
        FileInputStream fis = new FileInputStream(afd.getFileDescriptor());
        FileChannel fc = fis.getChannel();
        long startOffset = afd.getStartOffset();
        long declaredLength = afd.getDeclaredLength();
        return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private Interpreter getInterpreter() throws IOException {
        ByteBuffer model = loadModelFile(MODEL_NAME);
        model.order(ByteOrder.nativeOrder());
        return new Interpreter(model);
    }

    // 모델의 입출력 텐서 형태(shape)를 읽어 입력 크기와 출력 클래스 수를 초기화
    private void initModelShape() {
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        modelInputChannel = inputShape[0];
        modelInputWidth = inputShape[1];
        modelInputHeight = inputShape[2];

        Tensor outputTensor = interpreter.getOutputTensor(0);
        int[] outputShape = outputTensor.shape();
        modelOutputClasses = outputShape[1];
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        return Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, false);
    }

    // 비트맵을 RGB 평균값으로 그레이스케일 변환하여 ByteBuffer로 직렬화
    // 정규화 범위: 0.0 ~ 1.0 (픽셀값 / 224.0f)
    private ByteBuffer convertBitmapToGrayByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bitmap.getByteCount());
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixel : pixels) {
            int r = pixel >> 16 & 0xFF;
            int g = pixel >> 8 & 0xFF;
            int b = pixel & 0xFF;
            float avgPixelValue = (r + g + b) / 3.0f;
            float normalizedPixelValue = avgPixelValue / 224.0f;
            byteBuffer.putFloat(normalizedPixelValue);
        }

        return byteBuffer;
    }

    @Override
    public Pair<String, Float> classify(Bitmap image) {
        ByteBuffer buffer = convertBitmapToGrayByteBuffer(resizeBitmap(image));
        float[][] result = new float[1][modelOutputClasses];
        interpreter.run(buffer, result);

        // 최고 확률 인덱스를 라벨명으로 변환하여 반환
        Pair<Integer, Float> idxResult = argmaxIndex(result[0]);
        String label = (labels != null && idxResult.first < labels.size())
                ? labels.get(idxResult.first)
                : String.valueOf(idxResult.first); // 라벨 파일 오류 시 인덱스 번호로 대체
        return new Pair<>(label, idxResult.second);
    }

    // 출력 배열에서 최고 확률의 (인덱스, 확률) 쌍 반환
    private Pair<Integer, Float> argmaxIndex(float[] array) {
        int argmax = 0;
        float max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                argmax = i;
                max = array[i];
            }
        }
        return new Pair<>(argmax, max);
    }

    @Override
    public void finish() {
        if (interpreter != null)
            interpreter.close();
    }
}
