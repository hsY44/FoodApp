package kr.ac.baekseok.ab;

// 로그인 결과 상태 - AuthViewModel → Activity LiveData 전달용
public enum LoginState {
    SUCCESS,
    EMPTY_ID,
    EMPTY_PASSWORD,
    ID_NOT_FOUND,
    WRONG_PASSWORD
}
