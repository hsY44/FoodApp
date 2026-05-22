package kr.ac.baekseok.ab;

// 분류 히스토리 한 건의 데이터를 담는 모델 클래스
public class HistoryRecord {

    public final int id;
    public final String source;      // "카메라" 또는 "갤러리"
    public final String result;      // 분류 클래스명
    public final float probability;  // 확률 (0.0 ~ 1.0)
    public final String timestamp;   // 분류 일시 (yyyy-MM-dd HH:mm:ss)

    public HistoryRecord(int id, String source, String result, float probability, String timestamp) {
        this.id = id;
        this.source = source;
        this.result = result;
        this.probability = probability;
        this.timestamp = timestamp;
    }
}
