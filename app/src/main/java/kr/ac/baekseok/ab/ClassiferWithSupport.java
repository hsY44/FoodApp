package kr.ac.baekseok.ab;

import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

// TFLite Support Library와 저수준 Interpreter를 함께 사용하는 분류기 구현체
// Classifer(순수 저수준)와 ClassiferWithModel(고수준 Model 클래스) 사이의 중간 단계
// FileUtil.loadMappedFile()로 모델 로드, ImageProcessor로 전처리 수행
public class ClassiferWithSupport implements IClassifier {

    private static final String MODEL_NAME = "Inq1.tflite";
    private static final String LABEL_FILE = "labels.txt";

    Context context;
    Interpreter interpreter;
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;
    TensorBuffer outputBuffer;
    private List<String> labels;

    // init() 완료 여부 - SafeClassifierDecorator에서 사전 검증에 사용
    private boolean isInitialized = false;

    public ClassiferWithSupport(Context context) {
        this.context = context;
    }

    @Override
    public void init() throws IOException {
        // FileUtil로 모델 파일을 메모리 매핑 방식으로 로드 후 네이티브 바이트 순서 설정
        ByteBuffer model = FileUtil.loadMappedFile(context, MODEL_NAME);
        model.order(ByteOrder.nativeOrder());
        interpreter = new Interpreter(model);
        initModelShape();
        labels = FileUtil.loadLabels(context, LABEL_FILE);
        isInitialized = true;
    }

    @Override
    public boolean isInitialized() {
        return isInitialized;
    }

    // 모델의 입출력 텐서 형태를 읽어 입력 크기와 버퍼를 초기화
    private void initModelShape() {
        Tensor inputTensor = interpreter.getInputTensor(0);
        int[] shape = inputTensor.shape();
        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];
        inputImage = new TensorImage(inputTensor.dataType());

        Tensor outputTensor = interpreter.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());
    }

    private Bitmap convertBitmapToARGB8888(Bitmap bitmap) {
        // 모델 입력이 ARGB_8888 포맷을 요구하므로 다른 포맷의 비트맵은 변환
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    // 이미지를 모델 입력 형식에 맞게 전처리 - 리사이즈 + 정규화만 적용(크롭/회전 없음)
    private TensorImage loadImage(final Bitmap bitmap) {
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap));
        } else {
            inputImage.load(bitmap);
        }

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR))
                .add(new NormalizeOp(0.0f, 224.0f)) // 픽셀값을 0.0~1.0 범위로 정규화
                .build();
        return imageProcessor.process(inputImage);
    }

    @Override
    public Pair<String, Float> classify(Bitmap image) {
        inputImage = loadImage(image);
        // 저수준 Interpreter로 직접 추론 실행 - 입력 버퍼 → 출력 버퍼
        interpreter.run(inputImage.getBuffer(), outputBuffer.getBuffer().rewind());

        // 출력 버퍼를 라벨과 매핑하여 (클래스명 → 확률) 맵 생성
        Map<String, Float> output = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();
        return argmax(output);
    }

    // 확률 맵에서 최고 확률의 (클래스명, 확률) 쌍 반환
    private Pair<String, Float> argmax(Map<String, Float> map) {
        String maxKey = "";
        float maxVal = -1;
        for (Map.Entry<String, Float> entry : map.entrySet()) {
            if (entry.getValue() > maxVal) {
                maxKey = entry.getKey();
                maxVal = entry.getValue();
            }
        }
        return new Pair<>(maxKey, maxVal);
    }

    @Override
    public void finish() {
        if (interpreter != null)
            interpreter.close();
    }
}
