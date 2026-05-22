package kr.ac.baekseok.ab;

import static org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Pair;
import android.util.Size;

import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

// TFLite Support Library의 Model 클래스를 사용하는 분류기 구현체
// ImageProcessor를 통해 크롭/리사이즈/회전/정규화 전처리를 파이프라인으로 처리
// 현재 CameraActivity, GalleryActivity에서 실제로 사용 중인 기본 구현체
public class ClassiferWithModel implements IClassifier {

    private static final String MODEL_NAME = "Inq1.tflite";
    private static final String LABEL_FILE = "labels.txt";

    Context context;
    Model model;
    int modelInputWidth, modelInputHeight, modelInputChannel;
    TensorImage inputImage;
    TensorBuffer outputBuffer;
    private List<String> labels;

    // init() 완료 여부 - SafeClassifierDecorator에서 사전 검증에 사용
    private boolean isInitialized = false;

    public ClassiferWithModel(Context context) {
        this.context = context;
    }

    @Override
    public void init() throws IOException {
        model = Model.createModel(context, MODEL_NAME);
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
        Tensor inputTensor = model.getInputTensor(0);
        int[] shape = inputTensor.shape();
        modelInputChannel = shape[0];
        modelInputWidth = shape[1];
        modelInputHeight = shape[2];
        inputImage = new TensorImage(inputTensor.dataType());

        Tensor outputTensor = model.getOutputTensor(0);
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType());
    }

    // 분류기가 초기화된 경우에만 입력 크기를 반환 - UI에서 이미지 사전 크기 조정 시 활용 가능
    public Size getModelInputSize() {
        if (!isInitialized)
            return new Size(0, 0);
        return new Size(modelInputWidth, modelInputHeight);
    }

    private Bitmap convertBitmapToARGB8888(Bitmap bitmap) {
        // 모델 입력이 ARGB_8888 포맷을 요구하므로 다른 포맷의 비트맵은 변환
        return bitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    // 이미지를 모델 입력 형식에 맞게 전처리 후 TensorImage로 반환
    // 처리 순서: ARGB 변환 → 정사각형 크롭 → 모델 입력 크기로 리사이즈 → 회전 → 정규화
    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
        if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap));
        } else {
            inputImage.load(bitmap);
        }

        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90; // 90도 단위 회전 횟수

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(cropSize, cropSize)) // 짧은 쪽 기준 정사각형 크롭
                .add(new ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR))
                .add(new Rot90Op(numRotation))
                .add(new NormalizeOp(0.0f, 224.0f)) // 픽셀값을 0.0~1.0 범위로 정규화
                .build();

        return imageProcessor.process(inputImage);
    }

    // 센서 방향을 지정하여 분류 - 카메라 촬영 이미지의 회전 보정이 필요한 경우 사용
    public Pair<String, Float> classify(Bitmap image, int sensorOrientation) {
        inputImage = loadImage(image, sensorOrientation);

        Object[] inputs = new Object[]{inputImage.getBuffer()};
        Map<Integer, Object> outputs = new java.util.HashMap<>();
        outputs.put(0, outputBuffer.getBuffer().rewind());
        model.run(inputs, outputs);

        // 출력 버퍼를 라벨과 매핑하여 (클래스명 → 확률) 맵 생성
        Map<String, Float> output = new TensorLabel(labels, outputBuffer).getMapWithFloatValue();
        return argmax(output);
    }

    // 센서 방향 0으로 분류 - 갤러리 이미지처럼 이미 정방향인 경우 사용
    @Override
    public Pair<String, Float> classify(Bitmap image) {
        return classify(image, 0);
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
        if (model != null)
            model.close();
    }
}
