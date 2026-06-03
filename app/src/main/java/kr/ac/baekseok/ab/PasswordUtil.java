package kr.ac.baekseok.ab;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 비밀번호 암호화 유틸리티
// 한 번 암호화하면 원문 복원 불가 - DB에는 암호화된 값만 저장
// 로그인 시 입력값을 같은 방식으로 암호화해 저장된 값과 비교
public class PasswordUtil {

    private PasswordUtil() {}

    public static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // 암호화 결과(바이트 배열)를 문자열로 변환
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 Java 기본 제공 알고리즘이라 실제로는 발생하지 않는 예외
            throw new RuntimeException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
