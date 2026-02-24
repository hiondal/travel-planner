package com.travelplanner.auth.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 동의 저장 요청 DTO.
 *
 * <p>API 설계서 AUTH-05: POST /api/v1/users/consent</p>
 *
 * @author 강도윤/데브-백
 * @since 1.0.0
 */
@Getter
@NoArgsConstructor
public class ConsentRequest {

    /** 위치정보 수집 동의 여부 */
    @NotNull(message = "location 동의 여부는 필수입니다.")
    private Boolean location;

    /** Push 알림 동의 여부 */
    @NotNull(message = "push 동의 여부는 필수입니다.")
    private Boolean push;

    /** 동의 일시 (클라이언트에서 측정한 시각) */
    @NotNull(message = "timestamp는 필수입니다.")
    private LocalDateTime timestamp;
}
