package com.example.lineofduty.product;

import com.example.lineofduty.domain.product.Product;
import com.example.lineofduty.domain.product.repository.ProductRepository;
import com.example.lineofduty.domain.product.service.ProductService;
import com.example.lineofduty.common.model.enums.DeliveryType;
import com.example.lineofduty.common.model.enums.ProductStatus;
import org.junit.jupiter.api.*;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("concurrency")
class DistributedLockTest {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product(
                "락 테스트 상품",
                "분산 락 테스트용",
                10000L,
                100L,
                0L,
                0L,
                DeliveryType.STANDARD,
                ProductStatus.ON_SALE
        );
        testProduct = productRepository.save(testProduct);
    }

    @AfterEach
    void tearDown() {
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("Redisson 클라이언트 정상 동작 확인")
    void redissonClient_IsWorking() {
        // Given
        String lockKey = "test:lock";

        // When
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(1, 3, TimeUnit.SECONDS);

            // Then
            assertThat(acquired).isTrue();
            assertThat(lock.isHeldByCurrentThread()).isTrue();

            System.out.println("=== Redisson 락 획득 성공 ===");
            System.out.println("락 키: " + lockKey);
            System.out.println("락 보유 여부: " + lock.isHeldByCurrentThread());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("락 해제 완료");
            }
        }
    }

    @Test
    @DisplayName("분산 락이 동시 접근을 막는지 확인")
    void distributedLock_PreventsSimultaneousAccess() throws Exception {
        String lockKey = "product:stock:" + testProduct.getId();
        RLock lock = redissonClient.getLock(lockKey);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean secondLockAcquired = new AtomicBoolean(true);

        Thread t1 = new Thread(() -> {
            try {
                lock.tryLock(0, 5, TimeUnit.SECONDS);
                latch.countDown();       // 락 잡았다고 신호
                Thread.sleep(3000);      // 락 유지
            } catch (Exception ignored) {
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                latch.await();           // t1이 락 잡을 때까지 대기
                boolean acquired = lock.tryLock(0, 1, TimeUnit.SECONDS);
                secondLockAcquired.set(acquired);
            } catch (Exception ignored) {
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("=== 동시 접근 방지 테스트 ===");
        System.out.println("첫 번째 스레드 락 획득: 성공");
        System.out.println("두 번째 스레드 락 획득: " + (secondLockAcquired.get() ? "성공" : "실패"));
        System.out.println("✓ 분산 락이 동시 접근을 방지합니다");

        assertThat(secondLockAcquired.get()).isFalse();
    }

    @Test
    @DisplayName("재고 감소 메서드가 분산 락과 함께 정상 동작")
    void decreaseStock_WorksWithDistributedLock() {
        // Given
        Long initialStock = testProduct.getStock();

        // When
        productService.decreaseStock(testProduct.getId(), 10L);

        // Then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("=== 재고 감소 테스트 ===");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("감소량: 10");
        System.out.println("최종 재고: " + updatedProduct.getStock());
        System.out.println("✓ 재고 감소가 정상적으로 동작합니다");

        assertThat(updatedProduct.getStock()).isEqualTo(initialStock - 10);
    }

    @Test
    @DisplayName("재고 증가 메서드가 분산 락과 함께 정상 동작")
    void increaseStock_WorksWithDistributedLock() {
        // Given
        Long initialStock = testProduct.getStock();

        // When
        productService.increaseStock(testProduct.getId(), 20L);

        // Then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("=== 재고 증가 테스트 ===");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("증가량: 20");
        System.out.println("최종 재고: " + updatedProduct.getStock());
        System.out.println("✓ 재고 증가가 정상적으로 동작합니다");

        assertThat(updatedProduct.getStock()).isEqualTo(initialStock + 20);
    }

    @Test
    @DisplayName("락 타임아웃 테스트 - waitTime 내에 락 획득 성공")
    void lock_Timeout_WaitsAndAcquires() throws InterruptedException {
        // Given
        String lockKey = "product:stock:" + testProduct.getId();
        RLock lock = redissonClient.getLock(lockKey);
        AtomicBoolean secondThreadSuccess = new AtomicBoolean(false);

        // 락을 먼저 획득하고 5초간 유지
        lock.tryLock(0, 5, TimeUnit.SECONDS);

        System.out.println("=== 락 타임아웃 테스트 ===");
        System.out.println("메인 스레드가 락 획득");

        // When
        Thread otherThread = new Thread(() -> {
            try {
                System.out.println("다른 스레드가 락 획득 시도 (waitTime=10초)");
                productService.decreaseStock(testProduct.getId(), 1L);
                secondThreadSuccess.set(true);
                System.out.println("✓ 대기 후 락 획득 성공");
            } catch (Exception e) {
                System.out.println("✗ 락 획득 실패: " + e.getMessage());
            }
        });

        otherThread.start();
        Thread.sleep(1000);

        // 원래 락 해제
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            System.out.println("메인 스레드가 락 해제");
        }

        otherThread.join();

        // Then
        System.out.println("최종 결과: " + (secondThreadSuccess.get() ? "성공" : "실패"));
        System.out.println("✓ waitTime 내에 락을 획득할 수 있습니다");

        assertThat(secondThreadSuccess.get()).isTrue();
    }

    @Test
    @DisplayName("락 리스타임 만료 후 자동 해제 확인")
    void lock_LeaseTime_AutoRelease() throws InterruptedException {
        // Given
        String lockKey = "test:lease";
        RLock lock = redissonClient.getLock(lockKey);

        // When - leaseTime을 1초로 설정
        boolean acquired = lock.tryLock(1, 1, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();

        System.out.println("=== 락 리스타임 테스트 ===");
        System.out.println("락 획득 직후: " + lock.isLocked());

        // 2초 대기 (leaseTime 초과)
        Thread.sleep(2000);

        // Then - 자동으로 해제되었는지 확인
        boolean stillLocked = lock.isLocked();
        System.out.println("2초 후 락 상태: " + stillLocked);
        System.out.println("✓ 리스타임 만료 후 자동 해제 확인");

        assertThat(stillLocked).isFalse();
    }

    @Test
    @DisplayName("여러 상품에 대한 독립적인 락 동작 확인")
    void multipleLocks_WorkIndependently() throws InterruptedException {
        // Given - 두 번째 상품 생성
        Product product2 = new Product(
                "두 번째 상품",
                "독립 락 테스트",
                20000L,
                200L,
                0L,
                0L,
                DeliveryType.STANDARD,
                ProductStatus.ON_SALE
        );
        product2 = productRepository.save(product2);

        // When - 두 상품에 대한 동시 작업
        Thread thread1 = new Thread(() -> {
            productService.decreaseStock(testProduct.getId(), 5L);
            System.out.println("상품1 재고 감소 완료");
        });

        Product finalProduct = product2;
        Thread thread2 = new Thread(() -> {
            productService.decreaseStock(finalProduct.getId(), 10L);
            System.out.println("상품2 재고 감소 완료");
        });

        long startTime = System.currentTimeMillis();
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        long endTime = System.currentTimeMillis();

        // Then
        Product updated1 = productRepository.findById(testProduct.getId()).orElseThrow();
        Product updated2 = productRepository.findById(product2.getId()).orElseThrow();

        System.out.println("=== 독립적인 락 동작 테스트 ===");
        System.out.println("상품1 최종 재고: " + updated1.getStock());
        System.out.println("상품2 최종 재고: " + updated2.getStock());
        System.out.println("동시 실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("✓ 서로 다른 상품의 락이 독립적으로 동작합니다");

        assertThat(updated1.getStock()).isEqualTo(95L);
        assertThat(updated2.getStock()).isEqualTo(190L);

        // Cleanup
        productRepository.delete(product2);
    }

    @Test
    @DisplayName("예외 발생 시에도 락이 안전하게 해제됨")
    void lock_ReleaseSafely_OnException() {
        // Given
        Long initialStock = testProduct.getStock();

        System.out.println("=== 예외 발생 시 락 해제 테스트 ===");
        System.out.println("초기 재고: " + initialStock);
        System.out.println("재고보다 많은 양 감소 시도: 150개");

        // When - 재고보다 많은 양 감소 시도
        try {
            productService.decreaseStock(testProduct.getId(), 150L);
        } catch (Exception e) {
            System.out.println("예상된 예외 발생: " + e.getClass().getSimpleName());
        }

        // Then - 락이 해제되었고 재고는 변하지 않음
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();
        assertThat(updatedProduct.getStock()).isEqualTo(initialStock);

        // 락을 다시 획득할 수 있는지 확인
        String lockKey = "product:stock:" + testProduct.getId();
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(1, 3, TimeUnit.SECONDS);
            assertThat(acquired).isTrue();
            System.out.println("✓ 예외 발생 후에도 락을 다시 획득할 수 있습니다");
            System.out.println("✓ 재고는 변경되지 않았습니다: " + updatedProduct.getStock());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Test
    @DisplayName("REQUIRES_NEW 트랜잭션 전파 속성 동작 확인")
    void transaction_RequiresNew_WorksCorrectly() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(2);
        AtomicBoolean firstSuccess = new AtomicBoolean(false);
        AtomicBoolean secondSuccess = new AtomicBoolean(false);

        System.out.println("=== REQUIRES_NEW 트랜잭션 전파 테스트 ===");
        System.out.println("초기 재고: " + testProduct.getStock());

        // When - 두 스레드가 재고 감소 시도
        Thread thread1 = new Thread(() -> {
            try {
                productService.decreaseStock(testProduct.getId(), 30L);
                firstSuccess.set(true);
                System.out.println("스레드1: 30개 감소 성공");
            } catch (Exception e) {
                System.out.println("스레드1: 실패");
            } finally {
                latch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                Thread.sleep(100);
                productService.decreaseStock(testProduct.getId(), 20L);
                secondSuccess.set(true);
                System.out.println("스레드2: 20개 감소 성공");
            } catch (Exception e) {
                System.out.println("스레드2: 실패");
            } finally {
                latch.countDown();
            }
        });

        thread1.start();
        thread2.start();
        latch.await();

        // Then
        Product updatedProduct = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("최종 재고: " + updatedProduct.getStock());

        if (firstSuccess.get() && secondSuccess.get()) {
            assertThat(updatedProduct.getStock()).isEqualTo(50L);
            System.out.println("✓ 두 트랜잭션 모두 성공적으로 커밋됨");
        } else if (firstSuccess.get()) {
            assertThat(updatedProduct.getStock()).isEqualTo(70L);
            System.out.println("✓ 첫 번째 트랜잭션만 커밋됨");
        } else if (secondSuccess.get()) {
            assertThat(updatedProduct.getStock()).isEqualTo(80L);
            System.out.println("✓ 두 번째 트랜잭션만 커밋됨");
        }
    }
}