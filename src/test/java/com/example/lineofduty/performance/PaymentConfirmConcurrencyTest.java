package com.example.lineofduty.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.lineofduty.common.model.enums.DeliveryType;
import com.example.lineofduty.common.model.enums.ProductStatus;
import com.example.lineofduty.common.model.enums.Role;
import com.example.lineofduty.domain.order.Order;
import com.example.lineofduty.domain.order.repository.OrderRepository;
import com.example.lineofduty.domain.orderItem.OrderItem;
import com.example.lineofduty.domain.payment.Payment;
import com.example.lineofduty.domain.payment.PaymentStatus;
import com.example.lineofduty.domain.payment.dto.PaymentConfirmRequest;
import com.example.lineofduty.domain.payment.dto.TossConfirmRequest;
import com.example.lineofduty.domain.payment.repository.PaymentRepository;
import com.example.lineofduty.domain.payment.service.PaymentService;
import com.example.lineofduty.domain.payment.service.TossPaymentClient;
import com.example.lineofduty.domain.product.Product;
import com.example.lineofduty.domain.product.repository.ProductRepository;
import com.example.lineofduty.domain.user.User;
import com.example.lineofduty.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 결제 승인(confirm) 동시 요청: 재고 정합성 + 토스 API 대기 중 DB 커넥션 점유.
 *
 * <p>재고 30개 상품에 60명이 동시에 결제 승인을 요청한다. 토스 API는 실제로 호출할 수 없으니, 실제 네트워크처럼
 * {@value #TOSS_LATENCY_MS}ms 뒤에 승인 응답을 주는 가짜로 바꿔 끼운다. 결제 한 건이 DB 커넥션을 얼마나 오래
 * 붙잡는지와, 커넥션을 얻으려고 얼마나 기다리는지(커넥션 풀 고갈)를 Hikari 메트릭으로 재고, 별도 스레드가 계속 날리는
 * 가벼운 조회가 최대 얼마나 밀리는지도 잰다.
 *
 * <p>같은 테스트를 개선 전 커밋과 개선 후 커밋에서 각각 실행해 비교했다(개선 전에는 아래 검증이 실패한다).
 * 실제 MySQL + Redis가 필요해서 {@code PERF_TEST=true}일 때만 실행되고, 별도 스키마(lineofduty_perf)를 쓴다:
 * <pre>PERF_TEST=true ./gradlew test --tests "com.example.lineofduty.performance.*"</pre>
 */
@EnabledIfEnvironmentVariable(named = "PERF_TEST", matches = "true")
@SpringBootTest(properties = {
        "DB_URL=jdbc:mysql://${PERF_DB_HOST:localhost}:${PERF_DB_PORT:3306}/lineofduty_perf"
                + "?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true",
        "DB_USERNAME=${PERF_DB_USERNAME:root}", "DB_PASSWORD=${PERF_DB_PASSWORD:root}", "DDL_AUTO=update",
        "REDIS_HOST=${PERF_REDIS_HOST:localhost}", "REDIS_PORT=${PERF_REDIS_PORT:6379}",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false",
        "JWT_SECRET=cGVyZm9ybWFuY2UtdGVzdC1zZWNyZXQta2V5LXBlcmZvcm1hbmNlLXRlc3Qtc2VjcmV0",
        "ADMIN_TOKEN=dummy", "FRONTEND_URL=http://localhost", "GEMINI_API_KEY=dummy", "TOSS_SECRET_KEY=dummy",
        "MAIL_USERNAME=dummy", "MAIL_PASSWORD=dummy", "WEATHER_API_KEY=dummy",
        "KAKAO_CLIENT_ID=dummy", "KAKAO_REDIRECT_URI=http://localhost"
})
class PaymentConfirmConcurrencyTest {

    private static final int TOSS_LATENCY_MS = 300;
    private static final long STOCK = 30;
    private static final int BUYERS = 60;
    private static final int HIKARI_POOL_SIZE = 10;

    @MockBean
    private TossPaymentClient tossPaymentClient;

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MeterRegistry meterRegistry;

    private final AtomicInteger tossApproved = new AtomicInteger();
    private Long productId;
    private final List<String> paymentKeys = new ArrayList<>();

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        for (String table : List.of("payments", "order_items", "orders", "products", "users")) {
            jdbcTemplate.execute("TRUNCATE TABLE " + table);
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        Product product = productRepository.save(new Product("전투화", "동시성 테스트", 1000L, STOCK,
                0L, 0L, DeliveryType.STANDARD, ProductStatus.ON_SALE));
        productId = product.getId();

        for (int i = 0; i < BUYERS; i++) {
            String paymentKey = "pk-" + i;
            int buyer = i;
            // Payment가 Order를 PERSIST로 cascade하므로 주문과 결제를 한 트랜잭션에서 만든다
            transactionTemplate.executeWithoutResult(status -> {
                User user = userRepository.save(new User("buyer" + buyer + "@lod.test", "buyer" + buyer, "pw", Role.ROLE_USER));
                Order order = new Order(user, "전투화 1개", "ORD-" + UUID.randomUUID(), 0L, new ArrayList<>());
                order.addOrderItem(new OrderItem(productRepository.getReferenceById(productId), null, 1000L, 1L));
                orderRepository.save(order);
                Payment payment = new Payment(order);
                payment.updatePaymentKey(paymentKey);
                paymentRepository.save(payment);
            });
            paymentKeys.add(paymentKey);
        }

        // 가짜 토스: 네트워크 지연 후 승인 응답
        ObjectMapper mapper = new ObjectMapper();
        given(tossPaymentClient.confirm(any(TossConfirmRequest.class))).willAnswer(invocation -> {
            TossConfirmRequest request = invocation.getArgument(0);
            Thread.sleep(TOSS_LATENCY_MS);
            tossApproved.incrementAndGet();
            ObjectNode json = mapper.createObjectNode();
            json.put("status", "DONE");
            json.put("paymentKey", request.getPaymentKey());
            json.put("totalAmount", request.getAmount());
            json.put("requestedAt", OffsetDateTime.now().toString());
            json.put("approvedAt", OffsetDateTime.now().toString());
            return (JsonNode) json;
        });
    }

    private static PaymentConfirmRequest confirmRequest(String paymentKey) {
        PaymentConfirmRequest request = new PaymentConfirmRequest();
        ReflectionTestUtils.setField(request, "paymentKey", paymentKey);
        return request;
    }

    @Test
    void 재고보다_많은_동시_결제승인() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(BUYERS);
        CountDownLatch ready = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(BUYERS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        // 결제가 몰리는 동안 다른 API(가벼운 조회)가 커넥션을 얼마나 기다리는지 측정
        List<Double> probeMillis = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean running = new AtomicBoolean(true);
        Thread probe = new Thread(() -> {
            while (running.get()) {
                long start = System.nanoTime();
                productRepository.findById(productId);
                probeMillis.add((System.nanoTime() - start) / 1_000_000.0);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });

        for (String paymentKey : paymentKeys) {
            pool.submit(() -> {
                try {
                    ready.await();
                    paymentService.confirmPaymentService(confirmRequest(paymentKey));
                    success.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        Timer usage = meterRegistry.get("hikaricp.connections.usage").timer();
        Timer acquire = meterRegistry.get("hikaricp.connections.acquire").timer();
        long usageCountBefore = usage.count();
        double usageTotalBefore = usage.totalTime(TimeUnit.MILLISECONDS);
        long acquireCountBefore = acquire.count();
        double acquireTotalBefore = acquire.totalTime(TimeUnit.MILLISECONDS);

        probe.start();
        long start = System.currentTimeMillis();
        ready.countDown();
        done.await(2, TimeUnit.MINUTES);
        long elapsed = System.currentTimeMillis() - start;
        running.set(false);
        probe.join();
        pool.shutdown();

        long finalStock = productRepository.findById(productId).orElseThrow().getStock();
        long donePayments = paymentRepository.findAll().stream().filter(p -> p.getStatus() == PaymentStatus.DONE).count();
        double probeMax = probeMillis.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double usageAvg = (usage.totalTime(TimeUnit.MILLISECONDS) - usageTotalBefore) / (usage.count() - usageCountBefore);
        double acquireAvg = (acquire.totalTime(TimeUnit.MILLISECONDS) - acquireTotalBefore) / (acquire.count() - acquireCountBefore);

        System.out.printf("%n=== 결제 승인 동시 요청 (재고 %d개, 구매자 %d명, 토스 응답 %dms, 커넥션 풀 %d) ===%n",
                STOCK, BUYERS, TOSS_LATENCY_MS, HIKARI_POOL_SIZE);
        System.out.printf("승인 성공(DB 반영)     : %d건 / 실패: %d건%n", success.get(), failed.get());
        System.out.printf("결제 완료(DONE)         : %d건 → 초과 판매 %d건%n", donePayments, Math.max(0, donePayments - STOCK));
        System.out.printf("최종 재고              : %d개 → 차감 누락(lost update) %d건%n",
                finalStock, finalStock - (STOCK - donePayments));
        System.out.printf("토스 승인됐지만 DB 실패  : %d건 (돈은 빠져나갔는데 주문은 실패)%n", tossApproved.get() - donePayments);
        System.out.printf("전체 처리 시간          : %,dms%n", elapsed);
        System.out.printf("DB 커넥션 1회 점유 시간  : 평균 %.1fms, 최대 %.1fms (Hikari usage)%n", usageAvg, usage.max(TimeUnit.MILLISECONDS));
        System.out.printf("DB 커넥션 획득 대기 시간  : 평균 %.1fms, 최대 %.1fms (Hikari acquire)%n", acquireAvg, acquire.max(TimeUnit.MILLISECONDS));
        System.out.printf("동시간대 다른 조회 API    : 최대 %.1fms 지연%n", probeMax);

        assertThat(donePayments).isEqualTo(STOCK);
        assertThat(finalStock).isZero();
        assertThat(tossApproved.get()).isEqualTo((int) STOCK);
    }
}
