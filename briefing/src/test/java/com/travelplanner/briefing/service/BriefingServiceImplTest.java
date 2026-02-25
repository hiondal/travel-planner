package com.travelplanner.briefing.service;

import com.travelplanner.briefing.client.EventPublisher;
import com.travelplanner.briefing.client.FcmClient;
import com.travelplanner.briefing.client.MonitorServiceClient;
import com.travelplanner.briefing.client.PayServiceClient;
import com.travelplanner.briefing.client.PlaceServiceClient;
import com.travelplanner.briefing.domain.*;
import com.travelplanner.briefing.dto.internal.GenerateBriefingResult;
import com.travelplanner.briefing.dto.internal.MonitorData;
import com.travelplanner.briefing.dto.internal.SubscriptionInfo;
import com.travelplanner.briefing.dto.request.GenerateBriefingRequest;
import com.travelplanner.briefing.generator.BriefingTextGenerator;
import com.travelplanner.briefing.repository.BriefingLogRepository;
import com.travelplanner.briefing.repository.BriefingRepository;
import com.travelplanner.common.enums.BriefingType;
import com.travelplanner.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * BriefingServiceImpl 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class BriefingServiceImplTest {

    @Mock
    private BriefingRepository briefingRepository;

    @Mock
    private BriefingLogRepository briefingLogRepository;

    @Mock
    private BriefingTextGenerator briefingTextGenerator;

    @Mock
    private MonitorServiceClient monitorServiceClient;

    @Mock
    private PayServiceClient payServiceClient;

    @Mock
    private PlaceServiceClient placeServiceClient;

    @Mock
    private FcmClient fcmClient;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BriefingServiceImpl briefingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(briefingService, "freeTierDailyLimit", 1);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("generateBriefing 테스트")
    class GenerateBriefingTest {

        @Test
        @DisplayName("정상적으로 브리핑을 생성한다")
        void generateBriefing_success() {
            // given
            GenerateBriefingRequest request = createRequest("user_001", "place_001");

            given(valueOperations.get(anyString())).willReturn(null);
            given(briefingRepository.findByIdempotencyKey(anyString())).willReturn(Optional.empty());

            SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
            subscriptionInfo.setTier("FREE");
            given(payServiceClient.getSubscriptionInfo(anyString())).willReturn(subscriptionInfo);

            MonitorData monitorData = createMonitorData("SAFE");
            given(monitorServiceClient.getLatestStatus(anyString())).willReturn(monitorData);
            given(briefingTextGenerator.generate(any())).willReturn(new BriefingText("현재까지 모든 항목 정상입니다. 예정대로 출발하세요."));
            given(placeServiceClient.getPlaceName(anyString())).willReturn("테스트 장소");
            given(briefingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(briefingLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            GenerateBriefingResult result = briefingService.generateBriefing(request);

            // then
            assertThat(result.isCreated()).isTrue();
            assertThat(result.getType()).isEqualTo(BriefingType.SAFE);
            verify(briefingRepository).save(any(Briefing.class));
            verify(fcmClient).sendPush(anyString(), anyString(), anyString(), anyMap());
            verify(eventPublisher).publishBriefingCreated(any());
        }

        @Test
        @DisplayName("멱등성 키가 일치하면 기존 브리핑을 반환한다")
        void generateBriefing_existingIdempotencyKey() {
            // given
            GenerateBriefingRequest request = createRequest("user_001", "place_001");

            Briefing existingBriefing = createBriefing("brif_001", "user_001", BriefingType.SAFE);
            given(valueOperations.get(anyString())).willReturn("brif_001");
            given(briefingRepository.findById("brif_001")).willReturn(Optional.of(existingBriefing));

            // when
            GenerateBriefingResult result = briefingService.generateBriefing(request);

            // then
            assertThat(result.getStatus()).isEqualTo("EXISTING");
            assertThat(result.getBriefingId()).isEqualTo("brif_001");
            verify(briefingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Free 티어 일일 한도 초과 시 SKIPPED를 반환한다")
        void generateBriefing_freeTierLimitExceeded() {
            // given
            GenerateBriefingRequest request = createRequest("user_001", "place_001");

            given(valueOperations.get(anyString())).willReturn(null);
            given(briefingRepository.findByIdempotencyKey(anyString())).willReturn(Optional.empty());

            SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
            subscriptionInfo.setTier("FREE");
            given(payServiceClient.getSubscriptionInfo(anyString())).willReturn(subscriptionInfo);

            // Redis 카운트 한도 초과
            given(valueOperations.get(contains("brif:count:"))).willReturn("1");
            given(briefingRepository.countByUserIdAndCreatedAtDate(anyString(), any())).willReturn(1);
            given(briefingLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            GenerateBriefingResult result = briefingService.generateBriefing(request);

            // then
            assertThat(result.isSkipped()).isTrue();
            verify(briefingRepository, never()).save(any(Briefing.class));
        }

        @Test
        @DisplayName("CAUTION 상태이면 WARNING 타입 브리핑이 생성된다")
        void generateBriefing_warningType() {
            // given
            GenerateBriefingRequest request = createRequest("user_001", "place_001");

            given(valueOperations.get(anyString())).willReturn(null);
            given(briefingRepository.findByIdempotencyKey(anyString())).willReturn(Optional.empty());

            SubscriptionInfo subscriptionInfo = new SubscriptionInfo();
            subscriptionInfo.setTier("PRO");
            given(payServiceClient.getSubscriptionInfo(anyString())).willReturn(subscriptionInfo);

            MonitorData monitorData = createMonitorData("CAUTION");
            monitorData.setCongestion("혼잡");
            given(monitorServiceClient.getLatestStatus(anyString())).willReturn(monitorData);
            given(briefingTextGenerator.generate(any())).willReturn(new BriefingText("혼잡도이(가) 감지되었습니다. 대안을 확인해보세요."));
            given(placeServiceClient.getPlaceName(anyString())).willReturn("테스트 장소");
            given(briefingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(briefingLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            // when
            GenerateBriefingResult result = briefingService.generateBriefing(request);

            // then
            assertThat(result.isCreated()).isTrue();
            assertThat(result.getType()).isEqualTo(BriefingType.WARNING);
        }
    }

    @Nested
    @DisplayName("getBriefing 테스트")
    class GetBriefingTest {

        @Test
        @DisplayName("존재하는 브리핑을 정상 조회한다")
        void getBriefing_success() {
            // given — 현재 구현은 findById로 조회 후 userId 소유권을 수동 검증한다.
            Briefing briefing = createBriefing("brif_001", "user_001", BriefingType.SAFE);
            given(briefingRepository.findById("brif_001"))
                    .willReturn(Optional.of(briefing));

            // when
            Briefing result = briefingService.getBriefing("brif_001", "user_001");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("brif_001");
        }

        @Test
        @DisplayName("존재하지 않는 브리핑 조회 시 ResourceNotFoundException이 발생한다")
        void getBriefing_notFound() {
            // given
            given(briefingRepository.findById("brif_999"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> briefingService.getBriefing("brif_999", "user_001"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getBriefingList 테스트")
    class GetBriefingListTest {

        @Test
        @DisplayName("날짜별 브리핑 목록을 조회한다")
        void getBriefingList_success() {
            // given
            LocalDate date = LocalDate.now();
            List<Briefing> briefings = List.of(
                    createBriefing("brif_001", "user_001", BriefingType.SAFE),
                    createBriefing("brif_002", "user_001", BriefingType.WARNING)
            );
            given(briefingRepository.findByUserIdAndDate("user_001", date)).willReturn(briefings);

            // when
            List<Briefing> result = briefingService.getBriefingList("user_001", date);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("날짜가 null이면 오늘 날짜로 조회한다")
        void getBriefingList_nullDateUsesToday() {
            // given
            given(briefingRepository.findByUserIdAndDate(eq("user_001"), any(LocalDate.class)))
                    .willReturn(List.of());

            // when
            List<Briefing> result = briefingService.getBriefingList("user_001", null);

            // then
            verify(briefingRepository).findByUserIdAndDate(eq("user_001"), any(LocalDate.class));
            assertThat(result).isEmpty();
        }
    }

    private GenerateBriefingRequest createRequest(String userId, String placeId) {
        GenerateBriefingRequest request = new GenerateBriefingRequest();
        ReflectionTestUtils.setField(request, "scheduleItemId", "si_001");
        ReflectionTestUtils.setField(request, "placeId", placeId);
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "departureTime", LocalDateTime.now().plusHours(1));
        ReflectionTestUtils.setField(request, "triggeredAt", LocalDateTime.now());
        return request;
    }

    private MonitorData createMonitorData(String overallStatus) {
        MonitorData data = new MonitorData();
        data.setPlaceId("place_001");
        data.setPlaceName("테스트 장소");
        data.setBusinessStatus("영업 중");
        data.setCongestion("보통");
        data.setWeather("맑음");
        data.setPrecipitationProb(0);
        data.setWalkingMinutes(15);
        data.setDistanceM(420);
        data.setOverallStatus(overallStatus);
        return data;
    }

    private Briefing createBriefing(String id, String userId, BriefingType type) {
        BriefingContent content = new BriefingContent("영업 중", "보통", "맑음", 15, null, 420);
        StatusLevel statusLevel = type == BriefingType.SAFE ? StatusLevel.SAFE : StatusLevel.CAUTION;
        return Briefing.create(id, userId, "si_001", "place_001", "테스트 장소",
                type, LocalDateTime.now().plusHours(1), UUID.randomUUID().toString(),
                "테스트 브리핑", statusLevel, content, List.of());
    }
}
