package com.example.lineofduty.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lineofduty.common.model.enums.Role;
import com.example.lineofduty.domain.qna.Qna;
import com.example.lineofduty.domain.qna.repository.QnaRepository;
import com.example.lineofduty.domain.qna.service.QnaService;
import com.example.lineofduty.domain.user.User;
import com.example.lineofduty.domain.user.repository.UserRepository;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * QnA 상세 조회 시 조회수 증가: 낙관적 락(개선 전 실제 API) vs 비관적 락 vs 원자적 UPDATE(개선 후).
 *
 * <p>같은 글을 {@value #REQUESTS}번 동시에 조회해서 요청 성공률, 최종 조회수 정확도, 처리 시간을 비교한다.
 * 실제 MySQL + Redis가 필요해서 {@code PERF_TEST=true}일 때만 실행된다(별도 스키마 lineofduty_perf).
 */
@EnabledIfEnvironmentVariable(named = "PERF_TEST", matches = "true")
@SpringBootTest(properties = {
        "DB_URL=jdbc:mysql://${PERF_DB_HOST:localhost}:${PERF_DB_PORT:3306}/lineofduty_perf"
                + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true",
        "DB_USERNAME=${PERF_DB_USERNAME:root}", "DB_PASSWORD=${PERF_DB_PASSWORD:root}", "DDL_AUTO=update",
        "REDIS_HOST=${PERF_REDIS_HOST:localhost}", "REDIS_PORT=${PERF_REDIS_PORT:6379}",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF",
        "JWT_SECRET=cGVyZm9ybWFuY2UtdGVzdC1zZWNyZXQta2V5LXBlcmZvcm1hbmNlLXRlc3Qtc2VjcmV0",
        "ADMIN_TOKEN=dummy", "FRONTEND_URL=http://localhost", "GEMINI_API_KEY=dummy", "TOSS_SECRET_KEY=dummy",
        "MAIL_USERNAME=dummy", "MAIL_PASSWORD=dummy", "WEATHER_API_KEY=dummy",
        "KAKAO_CLIENT_ID=dummy", "KAKAO_REDIRECT_URI=http://localhost"
})
class QnaViewCountPerformanceTest {

    private static final int REQUESTS = 300;
    private static final int THREADS = 32;

    @Autowired
    private QnaService qnaService;
    @Autowired
    private QnaRepository qnaRepository;
    @Autowired
    private UserRepository userRepository;

    private record Result(int success, int failed, long finalViewCount, long elapsedMillis) {
        long lost() {
            return success - finalViewCount;
        }
    }

    @Test
    void 조회수_증가_방식별_비교() throws InterruptedException {
        User user = userRepository.save(new User("qna-" + UUID.randomUUID() + "@lod.test", "qna", "pw", Role.ROLE_USER));

        Result optimistic = run(user, qnaService::qnaInquiryWithOptimisticLock);
        Result pessimistic = run(user, qnaService::qnaInquiryWithPessimisticLock);
        Result atomic = run(user, qnaService::qnaInquiryWithAtomicUpdate);

        System.out.printf("%n=== QnA 조회수 증가 (같은 글 동시 조회 %d건, 스레드 %d) ===%n", REQUESTS, THREADS);
        System.out.printf("%-26s | %6s %6s | %8s %8s | %8s %9s%n", "방식", "성공", "실패", "최종조회수", "유실", "시간(ms)", "처리량(/s)");
        print("낙관적 락 (개선 전 실제 API)", optimistic);
        print("비관적 락", pessimistic);
        print("원자적 UPDATE (개선 후)", atomic);

        assertThat(atomic.success()).isEqualTo(REQUESTS);
        assertThat(atomic.finalViewCount()).isEqualTo(REQUESTS);
    }

    private void print(String label, Result r) {
        System.out.printf("%-26s | %6d %6d | %8d %8d | %8d %9.0f%n", label, r.success(), r.failed(),
                r.finalViewCount(), r.lost(), r.elapsedMillis(), r.success() * 1000.0 / r.elapsedMillis());
    }

    private Result run(User user, LongConsumer inquiry) throws InterruptedException {
        Long qnaId = qnaRepository.save(new Qna("동시 조회 테스트", "내용", user)).getId();
        try {
            inquiry.accept(qnaId); // warmup
        } catch (Exception e) {
            System.out.println("[warmup 실패] " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        long before = qnaRepository.findById(qnaId).orElseThrow().getViewCount();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(REQUESTS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        for (int i = 0; i < REQUESTS; i++) {
            pool.submit(() -> {
                try {
                    ready.await();
                    inquiry.accept(qnaId);
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        long start = System.currentTimeMillis();
        ready.countDown();
        done.await(2, TimeUnit.MINUTES);
        long elapsed = System.currentTimeMillis() - start;
        pool.shutdown();

        long finalCount = qnaRepository.findById(qnaId).orElseThrow().getViewCount() - before;
        return new Result(success.get(), failed.get(), finalCount, elapsed);
    }
}
