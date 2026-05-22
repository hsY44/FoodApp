package kr.ac.baekseok.ab;

// 아이디 중복 확인 결과 상태 - AuthViewModel → Join Activity LiveData 전달용
public enum IdCheckState {
    AVAILABLE,  // 사용 가능
    TAKEN,      // 이미 사용 중
    EMPTY,      // 입력값 없음
    TOO_SHORT   // 3자 미만
}
