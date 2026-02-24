package com.travelplanner.alternative.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대안 선택 로그 엔티티 (ML 학습 데이터).
 */
@Entity
@Table(name = "selection_logs")
@Getter
@NoArgsConstructor
public class SelectionLog {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "alt_card_id", nullable = false, length = 36)
    private String altCardId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "selected_rank", nullable = false)
    private int selectedRank;

    @Column(name = "elapsed_seconds", nullable = false)
    private int elapsedSeconds;

    @Column(name = "adopted", nullable = false)
    private boolean adopted;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static SelectionLog create(String id, String altCardId, String userId,
                                       int selectedRank, int elapsedSeconds, boolean adopted) {
        SelectionLog log = new SelectionLog();
        log.id = id;
        log.altCardId = altCardId;
        log.userId = userId;
        log.selectedRank = selectedRank;
        log.elapsedSeconds = elapsedSeconds;
        log.adopted = adopted;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}
