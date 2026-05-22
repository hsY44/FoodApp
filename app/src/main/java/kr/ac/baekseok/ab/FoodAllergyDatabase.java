package kr.ac.baekseok.ab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 모델이 분류하는 17개 과자의 알러지 유발 성분 데이터베이스
// 한국 식품 알러지 표기 기준(22종) 참고하여 작성
// labels.txt의 클래스명과 정확히 일치하는 키 사용
public class FoodAllergyDatabase {

    // 과자명 → 알러지 유발 성분 목록
    private static final Map<String, List<String>> ALLERGEN_MAP = new HashMap<>();

    static {
        // 해태 포키 블루베리: 밀(웨이퍼), 우유(유크림), 달걀(난황), 대두(레시틴)
        ALLERGEN_MAP.put("해태포키블루베리",
                Arrays.asList("밀", "우유", "달걀", "대두", "팜유"));

        // 꼬칼콘 고소한맛: 밀(소맥분), 옥수수, 대두(팜유, 대두유), 우유(버터향)
        ALLERGEN_MAP.put("꼬칼콘고소한맛",
                Arrays.asList("밀", "옥수수", "대두", "우유", "팜유"));

        // 농심 매운새우깡: 밀(소맥분), 새우, 대두(대두유), 우유(유청), 달걀
        ALLERGEN_MAP.put("농심매운새우깡",
                Arrays.asList("밀", "새우", "대두", "우유", "달걀"));

        // 콘초: 밀(소맥분), 옥수수, 대두(팜유), 우유(버터향)
        ALLERGEN_MAP.put("콘초",
                Arrays.asList("밀", "옥수수", "대두", "우유", "팜유"));

        // 프링글스: 감자(감자전분), 밀(소맥전분), 우유(버터밀크), 대두(식물성유지)
        ALLERGEN_MAP.put("프링글스",
                Arrays.asList("감자", "밀", "우유", "대두", "팜유"));

        // 포테토칩 (일반): 감자, 대두(팜유/해바라기유), 우유(소금버터향)
        ALLERGEN_MAP.put("포테토칩",
                Arrays.asList("감자", "대두", "우유", "팜유"));

        // 포카칩 (오리온): 감자, 밀(소맥전분-일부 제품), 우유(치즈파우더), 대두(팜유)
        ALLERGEN_MAP.put("포카칩",
                Arrays.asList("감자", "밀", "우유", "대두", "팜유"));

        // 빠다코코낫: 밀(소맥분), 우유(버터/유크림), 달걀(난황분), 대두(대두유), 코코넛
        ALLERGEN_MAP.put("빠다코코낫",
                Arrays.asList("밀", "우유", "달걀", "대두", "코코넛"));

        // 몽쉘: 밀(소맥분), 우유(유크림/연유), 달걀(계란), 대두(대두유/레시틴), 팜유
        ALLERGEN_MAP.put("몽쉘",
                Arrays.asList("밀", "우유", "달걀", "대두", "팜유"));

        // 야채크래커: 밀(소맥분), 대두(팜유/대두유), 우유(유크림), 달걀
        ALLERGEN_MAP.put("야채크래커",
                Arrays.asList("밀", "대두", "우유", "달걀"));

        // 쁘띠첼 구미젤리: 젤라틴(돼지), 설탕, 포도당, 산미료(구연산)
        ALLERGEN_MAP.put("쁘띠첼구미젤리",
                Arrays.asList("젤라틴", "돼지고기"));

        // 해태 에이스: 밀(소맥분), 대두(팜유/대두유), 우유(유크림), 달걀(계란)
        ALLERGEN_MAP.put("해태에이스",
                Arrays.asList("밀", "대두", "우유", "달걀"));

        // 허니버터칩: 감자, 우유(버터/유크림), 대두(팜유), 밀(소맥전분), 달걀(난황분)
        ALLERGEN_MAP.put("허니버터칩",
                Arrays.asList("감자", "우유", "대두", "밀", "달걀", "팜유"));

        // 농심 알새우칩: 밀(소맥분), 새우, 대두(대두유), 우유(유청분말), 달걀
        ALLERGEN_MAP.put("농심알새우칩",
                Arrays.asList("밀", "새우", "대두", "우유", "달걀"));

        // 예감 치즈그라탕: 밀(소맥분), 우유(치즈/유크림), 달걀, 대두(팜유), 치즈
        ALLERGEN_MAP.put("예감치즈그라탕",
                Arrays.asList("밀", "우유", "치즈", "달걀", "대두", "팜유"));

        // 쫄병: 밀(소맥분), 대두(팜유/대두유), 우유(유크림), 달걀(난황)
        ALLERGEN_MAP.put("쫄병",
                Arrays.asList("밀", "대두", "우유", "달걀"));

        // 크라운 쵸코하임: 밀(소맥분), 우유(유크림/전지분유), 달걀, 대두(레시틴/팜유), 팜유
        ALLERGEN_MAP.put("크라운쵸코하임",
                Arrays.asList("밀", "우유", "달걀", "대두", "팜유"));
    }

    // 특정 과자의 알러지 성분 목록 반환 - 없으면 빈 리스트
    public static List<String> getAllergens(String foodName) {
        List<String> result = ALLERGEN_MAP.get(foodName);
        return result != null ? result : Collections.emptyList();
    }

    // 과자 성분과 사용자 알러지 중 일치하는 성분 반환
    // userAllergy: 쉼표 구분 문자열 (예: "감자, 우유, 새우")
    public static List<String> matchingAllergens(String foodName, String userAllergy) {
        if (userAllergy == null || userAllergy.isEmpty() || userAllergy.equals("없음")) {
            return Collections.emptyList();
        }

        List<String> foodAllergens = getAllergens(foodName);
        if (foodAllergens.isEmpty()) return Collections.emptyList();

        // 사용자 알러지를 쉼표로 분리하고 공백 제거
        String[] userArr = userAllergy.split(",");
        List<String> userList = new ArrayList<>();
        for (String s : userArr) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) userList.add(trimmed);
        }

        // 교집합 반환
        List<String> matched = new ArrayList<>();
        for (String allergen : foodAllergens) {
            if (userList.contains(allergen)) matched.add(allergen);
        }
        return matched;
    }
}
