package kr.ac.baekseok.ab;

import java.util.List;

// 이미지 분류 결과를 담는 데이터 클래스
// ViewModel → Activity로 LiveData를 통해 전달
public class ClassificationResult {

    private static final float LOW_CONFIDENCE_THRESHOLD = 0.60f;

    public final String foodName;
    public final float probability;
    public final List<String> foodAllergens;   // 해당 식품의 전체 알러지 성분
    public final List<String> matchedAllergens; // 사용자 알러지와 일치하는 성분

    public ClassificationResult(String foodName, float probability,
                                List<String> foodAllergens, List<String> matchedAllergens) {
        this.foodName = foodName;
        this.probability = probability;
        this.foodAllergens = foodAllergens;
        this.matchedAllergens = matchedAllergens;
    }

    // 신뢰도 60% 미만이면 인식 불가 처리
    public boolean isLowConfidence() {
        return probability < LOW_CONFIDENCE_THRESHOLD;
    }

    // 사용자 알러지와 일치하는 성분이 있을 때 true
    public boolean hasAllergyWarning() {
        return matchedAllergens != null && !matchedAllergens.isEmpty();
    }
}
