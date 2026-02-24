package com.travelplanner.auth.client;

import com.travelplanner.common.enums.OAuthProvider;
import com.travelplanner.common.exception.BusinessException;
import com.travelplanner.common.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * OAuth2 프로바이더 통신 클라이언트.
 *
 * <p>Google OAuth2 Authorization Code를 Access Token으로 교환하고,
 * UserInfo 엔드포인트에서 사용자 프로파일을 조회한다.</p>
 *
 * <p>Apple Sign In은 Phase 1 범위에서 제외되며, 호출 시 예외를 발생시킨다.</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthClient {

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    @Value("${oauth2.google.client-id:}")
    private String googleClientId;

    @Value("${oauth2.google.client-secret:}")
    private String googleClientSecret;

    @Value("${oauth2.google.redirect-uri:http://localhost:8081/login/oauth2/code/google}")
    private String googleRedirectUri;

    private final RestTemplate restTemplate;

    /**
     * OAuth 프로바이더에 따라 적절한 검증 메서드를 호출한다.
     *
     * @param provider   OAuth 프로바이더 식별자 (google, apple)
     * @param oauthCode  Authorization Code
     * @return 사용자 프로파일
     * @throws BusinessException Google 외 프로바이더 요청 시
     */
    public OAuthProfile verify(String provider, String oauthCode) {
        OAuthProvider oAuthProvider = parseProvider(provider);
        return switch (oAuthProvider) {
            case GOOGLE -> verifyGoogle(oauthCode);
            case APPLE -> throw new BusinessException(
                "UNSUPPORTED_PROVIDER",
                "Apple Sign In은 현재 지원하지 않습니다. (Phase 2 예정)",
                501
            );
        };
    }

    /**
     * Google OAuth2 Authorization Code를 검증하고 사용자 프로파일을 반환한다.
     *
     * <p>처리 순서:</p>
     * <ol>
     *   <li>Authorization Code → Access Token 교환 (token endpoint)</li>
     *   <li>Access Token으로 UserInfo 조회</li>
     *   <li>OAuthProfile 변환</li>
     * </ol>
     *
     * @param oauthCode Google Authorization Code
     * @return Google 사용자 프로파일
     * @throws ExternalApiException Google API 호출 실패 시
     */
    public OAuthProfile verifyGoogle(String oauthCode) {
        try {
            if (oauthCode.startsWith("eyJ")) {
                // idToken(JWT) → tokeninfo로 직접 검증
                return verifyGoogleIdToken(oauthCode);
            } else if (oauthCode.startsWith("ya29.")) {
                // accessToken → userinfo로 직접 조회 (웹 implicit flow)
                return fetchGoogleUserInfo(oauthCode);
            }
            // authorization code → 토큰 교환 후 userinfo 조회
            String accessToken = exchangeGoogleToken(oauthCode);
            return fetchGoogleUserInfo(accessToken);
        } catch (HttpClientErrorException e) {
            log.warn("Google OAuth 검증 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException("INVALID_OAUTH_CODE", "유효하지 않은 OAuth 코드입니다.", 401);
        } catch (RestClientException e) {
            log.error("Google OAuth API 호출 실패", e);
            throw new ExternalApiException("GOOGLE", "Google 인증 서비스와 통신에 실패했습니다.", e);
        }
    }

    /**
     * Google ID Token을 tokeninfo 엔드포인트로 직접 검증한다.
     * Flutter 웹의 google_sign_in은 serverAuthCode 대신 idToken을 반환하므로 이 경로를 사용한다.
     *
     * @param idToken Google ID Token (JWT)
     * @return 사용자 프로파일
     */
    @SuppressWarnings("unchecked")
    private OAuthProfile verifyGoogleIdToken(String idToken) {
        String url = GOOGLE_TOKENINFO_URL + "?id_token=" + idToken;
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

        Map<String, Object> info = response.getBody();
        if (info == null || !googleClientId.equals(info.get("aud"))) {
            throw new BusinessException("INVALID_ID_TOKEN", "유효하지 않은 Google ID Token입니다.", 401);
        }

        String sub = (String) info.get("sub");
        String email = (String) info.get("email");
        String name = (String) info.getOrDefault("name", email);
        String picture = (String) info.get("picture");

        log.info("Google ID Token 검증 성공: email={}", email);
        return new OAuthProfile(sub, email, name, picture, OAuthProvider.GOOGLE);
    }

    /**
     * Google Authorization Code를 Access Token으로 교환한다.
     *
     * @param oauthCode Authorization Code
     * @return Google Access Token
     */
    private String exchangeGoogleToken(String oauthCode) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", oauthCode);
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GOOGLE_TOKEN_URL, request, Map.class);

        Map<?, ?> body = response.getBody();
        if (body == null || !body.containsKey("access_token")) {
            throw new BusinessException("GOOGLE_TOKEN_ERROR", "Google Access Token 발급에 실패했습니다.", 502);
        }
        return (String) body.get("access_token");
    }

    /**
     * Google Access Token으로 사용자 정보를 조회한다.
     *
     * @param accessToken Google Access Token
     * @return 사용자 프로파일
     */
    @SuppressWarnings("unchecked")
    private OAuthProfile fetchGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
            GOOGLE_USERINFO_URL, HttpMethod.GET, request, Map.class
        );

        Map<String, Object> userInfo = response.getBody();
        if (userInfo == null) {
            throw new BusinessException("GOOGLE_USERINFO_ERROR", "Google 사용자 정보 조회에 실패했습니다.", 502);
        }

        String sub = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.getOrDefault("name", email);
        String picture = (String) userInfo.get("picture");

        return new OAuthProfile(sub, email, name, picture, OAuthProvider.GOOGLE);
    }

    /**
     * 문자열 프로바이더를 OAuthProvider Enum으로 변환한다.
     *
     * @param provider 프로바이더 문자열 (대소문자 무관)
     * @return OAuthProvider Enum
     * @throws BusinessException 지원하지 않는 프로바이더인 경우
     */
    private OAuthProvider parseProvider(String provider) {
        try {
            return OAuthProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("UNSUPPORTED_PROVIDER",
                "지원하지 않는 OAuth 프로바이더입니다: " + provider, 400);
        }
    }
}
