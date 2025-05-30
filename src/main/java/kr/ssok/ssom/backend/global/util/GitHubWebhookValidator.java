package kr.ssok.ssom.backend.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * GitHub Webhook 서명 검증 유틸리티
 */
@Slf4j
@Component
public class GitHubWebhookValidator {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";
    
    /**
     * GitHub Webhook 서명 검증
     * 
     * @param payload Webhook 페이로드 (요청 본문)
     * @param signature GitHub가 보낸 서명 (X-Hub-Signature-256 헤더 값)
     * @param secret GitHub Webhook Secret
     * @return 서명이 유효한지 여부
     */
    public boolean validateSignature(String payload, String signature, String secret) {
        
        // 1. 입력값 검증
        if (payload == null || signature == null || secret == null) {
            log.warn("GitHub Webhook 서명 검증 실패 - null 입력값 발견");
            return false;
        }
        
        // 2. 서명 형식 검증 (sha256= 접두사 확인)
        if (!signature.startsWith(SIGNATURE_PREFIX)) {
            log.warn("GitHub Webhook 서명 형식 오류 - sha256= 접두사 없음: {}", signature);
            return false;
        }
        
        // 3. 서명에서 실제 해시값 추출
        String receivedHash = signature.substring(SIGNATURE_PREFIX.length());
        
        try {
            // 4. Secret과 Payload로 HMAC-SHA256 해시 생성
            String computedHash = computeHmacSha256(payload, secret);
            
            // 5. 생성된 해시와 받은 해시 비교 (타이밍 공격 방지를 위한 상수 시간 비교)
            boolean isValid = secureEquals(computedHash, receivedHash);
            
            if (isValid) {
                log.debug("GitHub Webhook 서명 검증 성공");
            } else {
                log.warn("GitHub Webhook 서명 검증 실패 - 해시 불일치. 받은 해시: {}, 계산된 해시: {}", 
                        receivedHash, computedHash);
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("GitHub Webhook 서명 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * HMAC-SHA256 해시 계산
     * 
     * @param data 해시할 데이터
     * @param secret Secret 키
     * @return 16진수 문자열로 변환된 해시
     * @throws NoSuchAlgorithmException 알고리즘을 찾을 수 없는 경우
     * @throws InvalidKeyException 키가 유효하지 않은 경우
     */
    private String computeHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(secretKeySpec);
        
        byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        // 바이트 배열을 16진수 문자열로 변환
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * 타이밍 공격을 방지하는 상수 시간 문자열 비교
     * 
     * @param a 비교할 문자열 A
     * @param b 비교할 문자열 B
     * @return 두 문자열이 같은지 여부
     */
    private boolean secureEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        
        return result == 0;
    }
    
    /**
     * Webhook Secret이 설정되어 있는지 확인
     * 
     * @param secret Webhook Secret
     * @return Secret이 유효한지 여부
     */
    public boolean isSecretConfigured(String secret) {
        return secret != null && !secret.trim().isEmpty();
    }
    
    /**
     * 서명 검증을 수행할지 여부 판단
     * Secret이 설정되어 있고 서명이 제공된 경우에만 검증 수행
     * 
     * @param secret Webhook Secret
     * @param signature 요청 서명
     * @return 검증을 수행할지 여부
     */
    public boolean shouldValidateSignature(String secret, String signature) {
        boolean secretConfigured = isSecretConfigured(secret);
        boolean signatureProvided = signature != null && !signature.trim().isEmpty();
        
        if (secretConfigured && !signatureProvided) {
            log.warn("Webhook Secret이 설정되어 있지만 서명이 제공되지 않음");
            return false;
        }
        
        if (!secretConfigured && signatureProvided) {
            log.warn("서명이 제공되었지만 Webhook Secret이 설정되지 않음");
        }
        
        return secretConfigured && signatureProvided;
    }
}
