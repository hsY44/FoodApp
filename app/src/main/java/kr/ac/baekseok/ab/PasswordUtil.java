package kr.ac.baekseok.ab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 비밀번호 단방향 해싱 유틸리티
// SHA-256: 복호화 불가 - DB에 원문 대신 해시값만 저장하여 보안 강화
// 로그인 시 입력값을 동일하게 해싱하여 저장된 해시와 비교
public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // 바이트 배열을 16진수 문자열로 변환
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 Java SE 표준 - 이 예외는 실제로 발생하지 않음
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
