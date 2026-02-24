package com.travelplanner.alternative.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 대안 카드 노출 스냅샷 엔티티 (ML 학습 데이터).
 */
@Entity
@Table(name = "alternative_card_snapshots")
@Getter
@NoArgsConstructor
public class AlternativeCardSnapshot {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "place_id", nullable = false, length = 200)
    private String placeId;

    @Column(name = "candidate_place_id", nullable = false, length = 200)
    private String candidatePlaceId;

    @Column(name = "score_weights", nullable = false, columnDefinition = "text")
    private String scoreWeights;

    @Column(name = "scores", nullable = false, columnDefinition = "text")
    private String scores;

    @Column(name = "exposed_at", nullable = false)
    private LocalDateTime exposedAt;

    public static AlternativeCardSnapshot create(String id, String userId, String placeId,
                                                  String candidatePlaceId, String scoreWeights, String scores) {
        AlternativeCardSnapshot snapshot = new AlternativeCardSnapshot();
        snapshot.id = id;
        snapshot.userId = userId;
        snapshot.placeId = placeId;
        snapshot.candidatePlaceId = candidatePlaceId;
        snapshot.scoreWeights = scoreWeights;
        snapshot.scores = scores;
        snapshot.exposedAt = LocalDateTime.now();
        return snapshot;
    }
}
