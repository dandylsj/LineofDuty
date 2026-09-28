package com.example.lineofduty.product;

import com.example.lineofduty.common.model.enums.DeliveryType;
import com.example.lineofduty.common.model.enums.ProductStatus;
import com.example.lineofduty.domain.product.Product;
import com.example.lineofduty.domain.product.repository.ProductRepository;
import com.example.lineofduty.domain.product.service.ProductFacade;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.AbstractPreferences;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("concurrency")
class ProductServiceConcurrencyTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        testProduct = new Product(
                "테스트 상품",
                "동시성 테스트용 상품",
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
    @Order(1)
    @DisplayName("동시에 100명이 1개씩 구매 - 재고 감소 동시성 테스트")
    void concurrentDecreaseStock_100Requests() throws InterruptedException {
        // Given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        System.out.println("=== 100명 동시 구매 테스트 시작 ===");
        System.out.println("초기 재고: 100");

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.decreaseStock(testProduct.getId(), 1L);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failCount.get());
        System.out.println("최종 재고: " + result.getStock());

        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(result.getStock()).isEqualTo(0L);
    }

    @Test
    @Order(2)
    @DisplayName("50명이 2개씩 구매 - 재고 부족 테스트")
    void concurrentDecreaseStock_OutOfStock() throws InterruptedException {
        // Given
        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("=== 재고 부족 테스트 시작 ===");
        System.out.println("초기 재고: 100");

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.decreaseStock(testProduct.getId(), 2L);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 재고 부족 예외 예상됨
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("성공: " + successCount.get());
        System.out.println("최종 재고: " + result.getStock());

        assertThat(successCount.get()).isEqualTo(50);
        assertThat(result.getStock()).isEqualTo(0L);
    }

    @Test
    @Order(3)
    @DisplayName("재고 증가 동시성 테스트")
    void concurrentIncreaseStock() throws InterruptedException {
        // Given
        productFacade.decreaseStock(testProduct.getId(), 50L);

        int threadCount = 30;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("=== 재고 증가 테스트 시작 ===");
        System.out.println("감소 후 재고: 50");

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.increaseStock(testProduct.getId(), 1L);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("성공: " + successCount.get());
        System.out.println("최종 재고: " + result.getStock());

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(result.getStock()).isEqualTo(80L);
    }

    @Test
    @Order(4)
    @DisplayName("구매와 취소 동시 발생 - 혼합 테스트")
    void concurrentMixedOperations() throws InterruptedException {
        // Given
        int decreaseCount = 30;
        int increaseCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(decreaseCount + increaseCount);
        AtomicInteger decreaseSuccess = new AtomicInteger(0);
        AtomicInteger increaseSuccess = new AtomicInteger(0);

        System.out.println("=== 혼합 동시성 테스트 시작 ===");
        System.out.println("초기 재고: 100");

        // When
        for (int i = 0; i < decreaseCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.decreaseStock(testProduct.getId(), 1L);
                    decreaseSuccess.incrementAndGet();
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        for (int i = 0; i < increaseCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.increaseStock(testProduct.getId(), 1L);
                    increaseSuccess.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();
        long expected = 100 - decreaseSuccess.get() + increaseSuccess.get();

        System.out.println("감소 성공: " + decreaseSuccess.get());
        System.out.println("증가 성공: " + increaseSuccess.get());
        System.out.println("최종 재고: " + result.getStock());

        assertThat(result.getStock()).isEqualTo(expected);
    }

    @Test
    @Order(5)
    @DisplayName("재고 0일 때 SOLD_OUT 상태 확인")
    void stockZero_BecomesSOLDOUT() throws InterruptedException {
        // Given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        System.out.println("=== SOLD_OUT 상태 테스트 시작 ===");

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.decreaseStock(testProduct.getId(), 1L);
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("최종 재고: " + result.getStock());
        System.out.println("상품 상태: " + result.getStatus());

        assertThat(result.getStock()).isEqualTo(0L);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }

    @Test
    @Order(6)
    @DisplayName("SOLD_OUT에서 재고 증가 시 ON_SALE 변경")
    void soldOut_IncreaseStock_BecomesONSALE() {
        // Given
        productFacade.decreaseStock(testProduct.getId(), 100L);
        Product soldOut = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("=== 상태 변경 테스트 시작 ===");
        System.out.println("초기 상태: " + soldOut.getStatus());

        assertThat(soldOut.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);

        // When
        productFacade.increaseStock(testProduct.getId(), 10L);

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("변경 후 상태: " + result.getStatus());
        System.out.println("재고: " + result.getStock());

        assertThat(result.getStock()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    @Order(7)
    @DisplayName("대량 동시 요청 - 500명 구매")
    void massiveConcurrentRequests() throws InterruptedException {
        // Given
        productFacade.increaseStock(testProduct.getId(), 400L);

        int threadCount = 500;
        ExecutorService executorService = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("=== 대량 동시 요청 테스트 시작 ===");
        System.out.println("초기 재고: 500");

        long start = System.currentTimeMillis();

        // When
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productFacade.decreaseStock(testProduct.getId(), 1L);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 예외 무시
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long end = System.currentTimeMillis();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("성공: " + successCount.get());
        System.out.println("최종 재고: " + result.getStock());
        System.out.println("소요 시간: " + (end - start) + "ms");

        assertThat(result.getStock()).isEqualTo(0L);
    }

    @Test
    @Order(8)
    @DisplayName("트랜잭션 실패 시 락 정상 해제")
    void lockRelease_OnTransactionFailure() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("=== 락 해제 테스트 시작 ===");
        System.out.println("초기 재고: 100");

        // When
        Thread t1 = new Thread(() -> {
            try {
                productFacade.decreaseStock(testProduct.getId(), 150L);
            } catch (Exception e) {
                System.out.println("스레드1 실패: 재고 부족");
            } finally {
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(100);
                productFacade.decreaseStock(testProduct.getId(), 10L);
                successCount.incrementAndGet();
                System.out.println("스레드2 성공: 10개 감소");
            } catch (Exception e) {
                System.out.println("스레드2 실패");
            } finally {
                latch.countDown();
            }
        });

        t1.start();
        t2.start();
        latch.await();

        // Then
        Product result = productRepository.findById(testProduct.getId()).orElseThrow();

        System.out.println("최종 재고: " + result.getStock());

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(result.getStock()).isEqualTo(90L);
    }
}