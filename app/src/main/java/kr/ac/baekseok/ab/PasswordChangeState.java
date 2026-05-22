package kr.ac.baekseok.ab;

// 비밀번호 변경 결과 상태 - MyPageViewModel → Activity LiveData 전달용
public enum PasswordChangeState {
    SUCCESS,
    EMPTY_CURRENT,   // 현재 비밀번호 미입력
    WRONG_CURRENT,   // 현재 비밀번호 불일치
    EMPTY_NEW,       // 새 비밀번호 미입력
    NEW_TOO_SHORT,   // 새 비밀번호 4자 미만
    MISMATCH         // 새 비밀번호 / 재입력 불일치
}
