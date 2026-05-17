# 배포 사이트

현재 개인 Nas 서버를 통해 구동중이며 아래 링크를 통해 Swagger 와 웹페이지 링크를 확인할 수 있습니다.

- Swagger UI: https://lsjyahoo.synology.me:8443/swagger-ui/index.html#/
- 웹페이지 링크 : https://lineof-duty-front.vercel.app/

## 홈 화면
<img width="1246" height="1535" alt="image" src="https://github.com/user-attachments/assets/9015bbaa-8082-40b2-8d01-7b44a177c1c8" />


## 입영신청 페이지
<img width="1239" height="1492" alt="image" src="https://github.com/user-attachments/assets/1a50b135-f248-419a-a70c-f2140626acaa" />

## 상품 판매 페이지
<img width="1242" height="1079" alt="image" src="https://github.com/user-attachments/assets/01e6adab-255b-4665-8f2b-fe266182415e" />
<img width="902" height="1422" alt="image" src="https://github.com/user-attachments/assets/0a3f6468-2ccd-4b32-84de-02aca5a555fc" />


# 🪖 Line of Duty

입영 신청 + 군 생활 준비 + 커뮤니티를 하나로 묶은 올인원 플랫폼
![img_3.png](img_3.png)

## 목차
- 📌 프로젝트 소개
- ✨️ 핵심 기능
- 🏗️ 아키텍처
- 💡 기술적 의사결정
- ⚡ 트러블 슈팅
- 🎯 성능개선 & 도메인 정책 개선
- 🛠️ 기술 스택
- 📁 설계 문서


## 📌 프로젝트 소개

청년 실업 50만 시대.
취업이 어려운 시기 속에서 많은 청년들이 원하는 시기에 군 복무를 선택하고 있습니다.

병장 월급 150만 원 시대,
18개월 복무 시 약 2,000만 원 이상의 목돈 마련이 가능하며
월세·식비·교통비 부담도 없습니다.

Line of Duty는 이러한 흐름에 맞춘 군 간편 입영 서비스를 제공합니다.

## ✨ 핵심 기능
### 📅 실시간 입영 신청
- 선착순 입영 신청 처리
- 입영 신청 완료 시 신청자에게 메일 알림 기능 제공
- 입영 신청 내역 확인 가능

### 🛒 입대 준비물 커머스
- 입영 준비물을 한 곳에서 찾고 구매 가능
- 주문 기록 조회로 구매 내역 확인 가능

### 💳 간편 결제 시스템
- Toss를 적용해 빠르고 간편하게 결제 가능
- 결제값 오류 검사를 통한 안전 결제

### 🔐 소셜 로그인
- 국민 SNS 카카오톡을 이용한 간편 로그인

### 💬 커뮤니티 & AI 챗봇
- GPT 기반 넓은 범위의 인공지능 QNA 기능 제공

## 🏗️아키텍처
![img_16.png](img_16.png)

## 💡기술적 의사결정
<details>
<summary><strong>분산 락(Distributed Lock - Redisson) 선택 이유</strong></summary>

### 1. 배경

상품 재고관리를 하는 것에 있어서 동시성 제어가 없으면 재고 값이 음수로 가는 오류가 발생했고
이것을 해결하기 위해 분산 락을 채택하게 되었다

### 2. 요구사항

1. **멀티 서버 환경**(서버 2대 이상)에서도 재고 정합성 보장
2. **높은 동시성 상황**(수백 명 동시 접속)에서도 안정적 동작
3. 락 대기 시간 최소화 (사용자 경험 유지)
4. **확장 가능한 아키텍처** (서버 추가 시에도 동작)

### 3. 고려한 대안

### 낙관적 락

**작동 원리**:

1. 조회 시 `version = 1` 함께 읽음
2. 업데이트 시 `UPDATE ... SET stock = ?, version = 2 WHERE id = ? AND version = 1`
3. 다른 트랜잭션이 먼저 업데이트했다면 version이 2가 되어 있음
4. WHERE 조건 불일치 → **업데이트 실패** → 예외 발생

**장점**:

- 락을 잡지 않아 **대기 시간 없음**
- 충돌이 적은 상황에서 **성능 우수**
- 읽기 작업이 많은 환경에 유리

**단점**:

- 충돌 시 예외가 발생하여 재시도 로직이 필수적임
- 경쟁이 심한 상황에서는 충돌 빈도 수가 매우 높음
- 재시도 로직 구현의 복잡도가 증가함

**결정**: ❌ 부적합

재고 차감은 동일 상품에 대한 동시 접근 빈도가 매우 높으며
충돌 발생 시 재시도 비용이 트래픽에 비례해 증가한다.

특히 플래시 세일과 같은 상황에서는 충돌률이 급격히 증가하여
시스템 전체 응답 시간이 불안정해질 가능성이 높다.

### 비관적 락

**작동 원리**:

1. `SELECT ... FOR UPDATE` 쿼리 실행
2.  해당 행(row)에 **배타적 락** 획득
3.  다른 트랜잭션은 대기 (락 해제까지)
4.  현재 트랜잭션 커밋 후 락 해제
5. 대기중인 다음 트랜잭션이 락 획득

**장점**:

- 충돌 자체를 방지(순차 처리)
- 재시도 로직이 필요하지 않음
- 단일 서버 환경에서는 완벽함

**단점**:

- 대기 시간 발생 (순차 처리의 Trade-off)
- DB 커넥션 점유 시간 증가

비관적 락의 한계:

- 트랜잭션이 커밋되어야 락이 해제되는데 만약 락을 먼저 해제하고 커밋이 늦어지면 다른 트랜잭션이 커밋 전 데이터를 읽을 수 있음
- 비관적 락은 트랜잭션 범위 내에서만 유효하므로 복잡한 비지니스 로직(외부 API 호출)이 있다면 제어가 어려움

**결정**: ⚠️ 보류
→ 트랜잭션 관리의 어려움-락 해제와 DB커밋 순서 보장 필요

### 분산 락

**작동 원리**:

1. **서버 A**가 상품 123 재고 차감 요청
    - Redis에 `product:stock:123` 키로 락 생성 시도
    - 성공 → 락 획득, 재고 차감 진행
2. **서버 B**가 동시에 같은 상품 재고 차감 요청
    - Redis에 `product:stock:123` 키로 락 생성 시도
    - 이미 존재 → **대기** (최대 10초)
3. 서버 A 작업 완료 → 락 해제
4. 서버 B가 락 획득 → 재고 차감 진행

**장점**:

- 애플리케이션 레벨에서 동시성 제어를 수행함으로써

  DB 락 의존도를 낮출 수 있다.

- 트랜잭션 범위와 락 범위를 분리하여

  비즈니스 로직의 유연성을 확보할 수 있다.

- 서버 인스턴스 수 증가와 무관하게 동일한 Redis를 통해

  전역적인 동기화를 보장할 수 있다.


**단점**:

- Redis 의존성 추가
- Redis 장애 시 락 시스템 전체 중단 가능
- 네트워크 오버헤드 약간 증가

**최종 결정**: ✅ **분산 락 채택**

→ 단점보다 장점이 압도적

→ 프로덕션 환경 필수 요구사항 충족

### 분산 락을 선택한 이유

- Redis는 단일 스레드 이벤트 루프 기반으로 동작하므로 하나의 키에 대한 명령은 원자적으로 처리된다. 따라서 락 획득/해제 연산의 정합성을 보장할 수 있다.
- 모든 서버가 **같은 Redis를 바라봄** → 진짜 "분산 환경"에서의 동기화
</details>

<details>
<summary><strong>입영 신청 동시성 제어 설계</strong></summary>
## 1. 🎯 도메인 특징

![img_5.png](img_5.png)

- 선착순 구조
- 매주 화요일 48개 슬롯 생성
- 특정 시점 트래픽 집중 가능
- 초과 배정 절대 불가
- 행정적 리스크 존재

> 이 도메인은 “속도”보다 “정확성”이 더 중요한 영역입니다.
>

## 2. 🚨 동시성 문제 발생 가능성

동일 일정에 다수 사용자가 동시에 신청할 경우 다음 문제가 발생할 수 있습니다.

### 1. Lost Update

- 여러 요청이 동일한 remainingSlots 값을 읽음
- 동시에 차감
- 음수 상태 발생 가능

### 2. 초과 배정 (Overbooking)

- 실제 가용 인원보다 많은 신청 승인
- 행정적/법적 리스크 발생
- 서비스 신뢰도 붕괴

---

# 3. 기능 요구사항

## ✅ 기능적 요구사항

1. 사용자는 특정 입영 일정에 신청할 수 있어야 한다.
2. 한 사용자는 동시에 하나의 신청만 유지할 수 있다.
3. 슬롯이 0 이하일 경우 신청 불가
4. 신청 성공 시 remainingSlots는 정확히 1 감소
5. 신청 성공 시 이메일 발송

---

## ⚙️ 비기능 요구사항

1. 동시 요청 상황에서도 정합성 보장
2. 초과 배정 절대 불가
3. 트랜잭션 단위 원자성 보장
4. 일관된 상태 유지

신청 로직 흐름

```jsx
1. 사용자 조회
2. 일정 조회 + 락 획득
3. 중복 신청 검증
4. 슬롯 확인
5. 신청 내역 저장
6. 슬롯 차감
7. 이벤트 발행 (메일)
8. 트랜잭션 커밋
```

동시성 전략 비교

| 전략 | 설명 | 장점 | 단점 | 적합성 |
| --- | --- | --- | --- | --- |
| 낙관적 락 | version 기반 충돌 감지 | DB 점유 짧음 | 재시도 필요 | 읽기 위주 시스템 |
| 비관적 락 | select for update | 즉시 정합성 확보 | 락 대기 발생 | 선착순 구조 |
| 분산 락 | Redis 기반 | 확장성 | 복잡도 증가 | 대규모 시스템 |

---

# 4. 의사결정

## 🎯 최종 선택: 비관적 락

### 선택 근거

- 선착순 구조
- 초과 배정 절대 불가
- 재시도 UX 허용 불가
- 단일 DB 환경

> 충돌 후 재시도보다
>
>
> 선점 후 확정이 도메인에 더 적합하다고 판단
>

---

## 📌 JPA 비관적 락 적용

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select s from EnlistmentSchedule s where s.id = :id")
EnlistmentSchedulefindByIdWithLock(Long id);
```

- DB row-level lock
- 트랜잭션 종료 시 해제
- 정합성 즉시 보장
</details>

<details>
<summary><strong>주문서 API 인덱싱 적용 이유</strong></summary>

### 1. 배경

**1. 주문을 추가하는 로직 성능 개선 시도**

주문서를 작성하고 결제를 시도하기 전까지는 주문서가 장바구니의 역할을 겸하도록 만들어짐

때문에 주문을 추가하려면 수많은 주문서들 중 주문이 완료되지 않은 주문서를 찾아내는 작업이 선행되어야함
→ 주문을 추가할 때마다 조회가 발생함

→ 주문 기록이 쌓일 수록 주문 조회 시간이 길어지는 현상을 막자

**2. 적용 시도**

캐싱으로 조회할 주문서를 미리 저장하자

- 장점
- 캐시 데이터를 우선으로 조회하면 되기에 주문 조회 시간이 빨라짐
- 레포지토리 접근이 줄어듬
- 단점
- 주문서 시스템 특성상 데이터 변경이 잦아 캐싱을 적용하기에는 부적합함

인덱싱을 적용하여 유저별로 주문서를 정렬하여 주문서를 빠르게 찾아내자

- 장점
- 유저별로 정렬된 주문서 중에서 가장 id가 큰 주문서를 찾아내는 방식으로 주문 조회 시간을 단축시킬 수 있음
- 단점
- 주문서 조회와 수정 시 재정렬 과정으로 인한 조회, 생성 비용 증가

**3. 결정**

인덱싱 전략 적합 판단
- 주문서 시스템 특성상 캐싱전략이 어울리지 않다고 판단
- 인덱싱 전략은 주문서 생성 및 수정 비용이 증가된다는 단점이 있으나 미미한 차이이다.
  또한 유저 id와 주문서 id를 기준으로 인덱싱을 정렬한다는 점을 고려했을 때 주문서 생성 작업을 제외한 주문 추가 혹은 수정 작업은 product와 총금액에 대한 간섭만 있어 해당 컬럼에 대한 간섭이 없다 → 즉 주문서를 새로 생성하거나 삭제하는 작업 이외의 작업은 추가적인 비용을 소모하지 않기 때문에 단점이 미미하다고 판단
</details>

## ⚡ 트러블 슈팅

### 토스 페이먼츠 결제 흐름 버그
- **문제**: 결제 중 이탈 후 재시도 시 금액 누적, 미완료 결제가 완료로 표시
- **원인**: READY 상태 Payment를 DONE과 구분하지 않고 existsByOrder()로 일괄 차단
- **해결**: 상태별 분기 처리, READY 결제 재생성 허용, OrderGetResponse에 paymentStatus 추가

<details>
<summary><strong>입영신청 동시성 문제 </strong></summary>

# k6 부하 테스트

## 테스트 조건

- VU: 3
- Iterations: 3
- 동일 scheduleId
- 랜덤 userId

---

## 🔎 결과

### ❌ 락 적용 전

![img_6.png](img_6.png)

- 3명 모두 성공
- 슬롯 1개만 차감
- 데이터 불일치 발생

### ✅ 락 적용 후

![img_7.png](img_7.png)
![img_8.png](img_8.png)

- 3명 성공
- 슬롯 정확히 3 감소
- 정합성 보장 성공
</details>

<details>
<summary><strong> 관리자 가입 로직 분리를 통한 권한 탈취 방어 </strong></summary>

## ⭐️ 주제

관리자 가입 로직 분리를 통한 권한 탈취 방어

---

## 🔥 발생

회원가입 API 설계 초기, 클라이언트가 보내는 JSON 요청 바디에 {"admin": true} 라는 파라미터만 추가하면 누구나 관리자 권한(ROLE_ADMIN)을 얻을 수 있는 심각한 보암 취약점을 발견했습니다.

---

## 🔍 원인

가입 시 권한 결정 로직이 클라이언트가 전달하는 데이터에만 의존하고 있었기 때문입니다.

악의적인 사용자가 API 요청 도구를 사용해 임의로 파라미터를 조작하여 전송할 경우, 서버 측에서 이를 차단하거나 진자 관리자인지 증명할 2차 검증 수단이 부재했습니다.

---

## ✅ 해결

관리자 권한 부여 시 서버 내부의 통제를 거치도록 **Admin Secret Key 검증 단계**를 도입했습니다.

- 서버의 환경 변수(`.env`)에 외부에 노출되지 않는 `admin.token` 값을 설정했습니다.
- 클라이언트가 관리자 가입을 요청(`admin=true`)할 경우, 반드시 발급받은 `adminToken`을 함께 전달하도록 DTO를 수정했습니다.
- 서버는 클라이언트가 보낸 토큰과 서버 내부의 시크릿 키를 대조하여, 일치할 때만 `ROLE_ADMIN`을 부여하고 틀릴 경우 즉시 예외(`400 INVALID_ADMIN_TOKEN`)를 발생시켜 가입을 차단했습니다.

```java
// [해결 코드] 관리자 가입 전 전용 키 검증
Role role = Role.ROLE_USER;
if (request.isAdmin()) {
    // 서버 환경변수에 저장된 Admin Secret Key와 대조
    if (!request.getAdminToken().equals(this.serverAdminSecret)) {
        throw new CustomException(ErrorMessage.INVALID_ADMIN_TOKEN);
    }
    role = Role.ROLE_ADMIN; // 검증 통과 시에만 관리자 권한 부여
}
```

---

## 💡 결론

이 문제를 통해 API를 설계할 때 편리함보다 ‘보안’이 우선되어야 함을 깨달았습니다.

시스템의 핵심 권한을 부여하는 행위는 클라이언트의 단방향 데이터 전달에 의존해서는 안되며, 반드시 **서버가 독립적으로 통제하고 검증할 수 있는 수단(Secret Key, 별도 인가 프로세스 등)이 병행되어야 함**을 배웠습니다.
</details>

<details>
<summary><strong> S3 도입 이전 파이어베이스 사용시 문제점 </strong></summary>

S3를 도입하기전 파이어베이스를 사용하여 업로드 기능을 사용하였고,

로컬에서는 문제없이 파일업로드가 가능했으나 실제 EC2 에서는 업로드 기능에 문제가 생겼다.

스웨거(Swagger)에서는 아래와 같은에러가 발생하였고

```jsx
Error getting access token for service account: 400 Bad Request\nPOST https://oauth2.googleapis.com/token\n{\"error\":\"invalid_grant\",\"error_description\":\"Invalid JWT Signature.\"}, iss: firebase-adminsdk-fbsvc@lineofdutyfileupload.iam.gserviceaccount.com"
```

서버에서는 아래와 같은 에러가 발생하였다.

```jsx
2026-02-10 16:13:50.534 ERROR 38773 --- [ http-nio-8080-exec-1 ] CustomExceptionHandler : 알 수 없는 에러 발생 : 
com.google.cloud.storage.StorageException: Error getting access token for service account: 400 Bad Request
POST https://oauth2.googleapis.com/token
{"error":"invalid_grant","error_description":"Invalid JWT Signature."}, iss: firebase-adminsdk-fbsvc@lineofdutyfileupload.iam.gserviceaccount.com
        at com.google.cloud.storage.StorageException.translate(StorageException.java:179)
        at com.google.cloud.storage.spi.v1.HttpStorageRpc.translate(HttpStorageRpc.java:330)
        at com.google.cloud.storage.spi.v1.HttpStorageRpc.get(HttpStorageRpc.java:524)
        at com.google.cloud.storage.StorageImpl.lambda$internalBucketGet$71(StorageImpl.java:1683)
        at com.google.api.gax.retrying.DirectRetryingExecutor.submit(DirectRetryingExecutor.java:102)
        at com.google.cloud.RetryHelper.run(RetryHelper.java:76)
        at com.google.cloud.RetryHelper.runWithRetries(RetryHelper.java:50)
        at com.google.cloud.storage.Retrying.run(Retrying.java:65)
        at com.google.cloud.storage.StorageImpl.run(StorageImpl.java:1608)
        at com.google.cloud.storage.StorageImpl.internalBucketGet(StorageImpl.java:1681)
        at com.google.cloud.storage.StorageImpl.get(StorageImpl.java:333)
        at com.google.firebase.cloud.StorageClient.bucket(StorageClient.java:97)
        at com.example.lineofduty.domain.fileUpload.FileUploadService.fileUpload(FileUploadService.java:35)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:355)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.transaction.interceptor.TransactionAspectSupport.invokeWithinTransaction(TransactionAspectSupport.java:379)
        at org.springframework.transaction.interceptor.TransactionInterceptor.invoke(TransactionInterceptor.java:119)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:720)
        at com.example.lineofduty.domain.fileUpload.FileUploadService$$SpringCGLIB$$0.fileUpload(<generated>)
        at com.example.lineofduty.domain.user.controller.UserController.uploadProfileImage(UserController.java:52)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:355)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.aspectj.MethodInvocationProceedingJoinPoint.proceed(MethodInvocationProceedingJoinPoint.java:89)
        at com.example.lineofduty.domain.log.LogAspect.logging(LogAspect.java:51)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethodWithGivenArgs(AbstractAspectJAdvice.java:637)
        at org.springframework.aop.aspectj.AbstractAspectJAdvice.invokeAdviceMethod(AbstractAspectJAdvice.java:627)
        at org.springframework.aop.aspectj.AspectJAroundAdvice.invoke(AspectJAroundAdvice.java:71)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.interceptor.ExposeInvocationInterceptor.invoke(ExposeInvocationInterceptor.java:97)
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184)
        at org.springframework.aop.framework.CglibAopProxy$CglibMethodInvocation.proceed(CglibAopProxy.java:768)
        at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:720)
        at com.example.lineofduty.domain.user.controller.UserController$$SpringCGLIB$$0.uploadProfileImage(<generated>)
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)
        at java.base/java.lang.reflect.Method.invoke(Method.java:580)
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:255)
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:188)
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:926)
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:831)
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87)
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979)
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014)
        at org.springframework.web.servlet.FrameworkServlet.doPost(FrameworkServlet.java:914)
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:590)
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885)
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:195)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:110)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:108)
        at org.springframework.security.web.FilterChainProxy.lambda$doFilterInternal$3(FilterChainProxy.java:231)
        at org.springframework.security.web.ObservationFilterChainDecorator$FilterObservation$SimpleFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:479)
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:340)
        at org.springframework.security.web.ObservationFilterChainDecorator.lambda$wrapSecured$0(ObservationFilterChainDecorator.java:82)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:128)
        at org.springframework.security.web.access.intercept.AuthorizationFilter.doFilter(AuthorizationFilter.java:100)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:126)
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:120)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.session.SessionManagementFilter.doFilter(SessionManagementFilter.java:131)
        at org.springframework.security.web.session.SessionManagementFilter.doFilter(SessionManagementFilter.java:85)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter(AnonymousAuthenticationFilter.java:100)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter(SecurityContextHolderAwareRequestFilter.java:179)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter(RequestCacheAwareFilter.java:63)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at com.example.lineofduty.common.filter.JwtFilter.doFilterInternal(JwtFilter.java:63)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:107)
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:93)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.web.filter.CorsFilter.doFilterInternal(CorsFilter.java:91)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90)
        at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:82)
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:69)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:62)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.session.DisableEncodeUrlFilter.doFilterInternal(DisableEncodeUrlFilter.java:42)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240)
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$0(ObservationFilterChainDecorator.java:323)
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:224)
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137)
        at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:233)
        at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:191)
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113)
        at org.springframework.web.servlet.handler.HandlerMappingIntrospector.lambda$createCacheFilter$3(HandlerMappingIntrospector.java:195)
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113)
        at org.springframework.web.filter.CompositeFilter.doFilter(CompositeFilter.java:74)
        at org.springframework.security.config.annotation.web.configuration.WebMvcSecurityConfiguration$CompositeFilterChainProxy.doFilter(WebMvcSecurityConfiguration.java:230)
        at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:362)
        at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:278)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.ServerHttpObservationFilter.doFilterInternal(ServerHttpObservationFilter.java:113)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201)
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116)
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164)
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140)
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167)
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90)
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:483)
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:115)
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93)
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74)
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:344)
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:397)
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63)
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:905)
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1741)
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52)
        at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1190)
        at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659)
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:63)
        at java.base/java.lang.Thread.run(Thread.java:1583)
Caused by: com.google.auth.oauth2.GoogleAuthException: Error getting access token for service account: 400 Bad Request
POST https://oauth2.googleapis.com/token
{"error":"invalid_grant","error_description":"Invalid JWT Signature."}, iss: firebase-adminsdk-fbsvc@lineofdutyfileupload.iam.gserviceaccount.com
        at com.google.auth.oauth2.GoogleAuthException.createWithTokenEndpointResponseException(GoogleAuthException.java:129)
        at com.google.auth.oauth2.ServiceAccountCredentials.refreshAccessToken(ServiceAccountCredentials.java:544)
        at com.google.auth.oauth2.OAuth2Credentials$1.call(OAuth2Credentials.java:270)
        at com.google.auth.oauth2.OAuth2Credentials$1.call(OAuth2Credentials.java:267)
        at java.base/java.util.concurrent.FutureTask.run(FutureTask.java:317)
        at com.google.auth.oauth2.OAuth2Credentials$RefreshTask.run(OAuth2Credentials.java:635)
        at com.google.common.util.concurrent.DirectExecutor.execute(DirectExecutor.java:31)
        at com.google.auth.oauth2.OAuth2Credentials$AsyncRefreshResult.executeIfNew(OAuth2Credentials.java:582)
        at com.google.auth.oauth2.OAuth2Credentials.asyncFetch(OAuth2Credentials.java:233)
        at com.google.auth.oauth2.OAuth2Credentials.getRequestMetadata(OAuth2Credentials.java:183)
        at com.google.auth.oauth2.ServiceAccountCredentials.getRequestMetadataForGdu(ServiceAccountCredentials.java:1044)
        at com.google.auth.oauth2.ServiceAccountCredentials.getRequestMetadata(ServiceAccountCredentials.java:1022)
        at com.google.auth.http.HttpCredentialsAdapter.initialize(HttpCredentialsAdapter.java:96)
        at com.google.cloud.http.HttpTransportOptions$1.initialize(HttpTransportOptions.java:199)
        at com.google.cloud.http.CensusHttpModule$CensusHttpRequestInitializer.initialize(CensusHttpModule.java:109)
        at com.google.cloud.storage.spi.v1.HttpStorageRpc$InvocationIdInitializer.initialize(HttpStorageRpc.java:169)
        at com.google.api.client.http.HttpRequestFactory.buildRequest(HttpRequestFactory.java:91)
        at com.google.api.client.googleapis.services.AbstractGoogleClientRequest.buildHttpRequest(AbstractGoogleClientRequest.java:455)
        at com.google.api.client.googleapis.services.AbstractGoogleClientRequest.executeUnparsed(AbstractGoogleClientRequest.java:565)
        at com.google.api.client.googleapis.services.AbstractGoogleClientRequest.executeUnparsed(AbstractGoogleClientRequest.java:506)
        at com.google.api.client.googleapis.services.AbstractGoogleClientRequest.execute(AbstractGoogleClientRequest.java:616)
        at com.google.cloud.storage.spi.v1.HttpStorageRpc.get(HttpStorageRpc.java:521)
        ... 178 common frames omitted
Caused by: com.google.api.client.http.HttpResponseException: 400 Bad Request
POST https://oauth2.googleapis.com/token
{"error":"invalid_grant","error_description":"Invalid JWT Signature."}
        at com.google.api.client.http.HttpResponseException$Builder.build(HttpResponseException.java:293)
        at com.google.api.client.http.HttpRequest.execute(HttpRequest.java:1118)
        at com.google.auth.oauth2.ServiceAccountCredentials.refreshAccessToken(ServiceAccountCredentials.java:541)
        ... 198 common frames omitted
```

## 전용 키 인식문제

파이어베이스에 저장하기 위해서는 로그인할때 처럼 토큰같은 전용키가 발급되고

그 키를 인식해서 인가를 하는데 이 키가 인식이 안되는 오류 인데,

인증키의 구조가 아래와 같은 구조로 한칸 띄어쓰기로 인해

/n  을 사용하기에 인식문제가 있다라는 것을 보고

이코드를 base64로 인코딩 하여 넣으면 해결이 된다 라는 것을 보았다.

```jsx
"-----BEGIN PRIVATE KEY-----\n...키내용...\n-----END PRIVATE KEY-----"     
```

그리고 추가적으로 base 64 키를 인식하도록 변환하는 코드도 넣어주었다.

```jsx
if (!fixedPrivateKey.contains("-----BEGIN PRIVATE KEY-----")) {
            try {
               byte[] decodedBytes = Base64.getDecoder().decode(fixedPrivateKey);               
               fixedPrivateKey = new String(decodedBytes, StandardCharsets.UTF_8);
           } catch (IllegalArgumentException e) {
           }
       }
```

하지만 여전히 인식이 불가능한 문제로 인해 호환성 문제라고 판단도 하였고

서비스 관리상 AWS EC2를 사용하기에 같은 AWS 서비스인 S3를 사용하는것이

관리 측면에서도 유리하기 때문에 S3를 사용하여 해결 하였다.
</details>

<details>
<summary><strong> 좀비 토큰(Zombie Token) </strong></summary>

## 도입 배경

우리 프로젝트는 REST API 서버 구조입니다.

기존의 세션 방식은 서버 메모리에 사용자 정보를 저장하므로 서버 확장 시 불리하다는 단점이 있었습니다.

이를 해결하기 위해 JWT 인증 방식을 도입했습니다.

- 현재 관리 방식 : Access Token(단기)은 클라이언트가 관리하고, Refresh Token(장기)은 백엔드 DB에 저장하여 관리하도록 설계했습니다.

## 문제

JWT는 서버가 상태를 저장하지 않고 토큰 자체가 인증서 역할을 하기 때문에 한 번 발급된  Access Token은 만료 시간이 지나기 전까지 서버에서 강제로 무효화할 수 없다는 단점이 있습니다.

이로 인해 다음과 같은 두가지 보안 이슈가 발생할 수 있었습니다.

- 로그아웃의 맹점 (토큰 탈취 예시)
    - 상황: 사용자가 공용 PC에서 서비스를 이용한 후 '로그아웃' 버튼을 누르고 자리를 떠납니다.
    - 문제점: 백엔드 DB에서 Refresh Token을 지웠더라도, 해커가 브라우저에 남아있던 Access Token을 탈취했다면? 서버는 이 토큰이 탈취된 것인지, 정상적인 것인지 알 길이 없으므로 만료 시간(예: 30분) 동안 해커가 내 계정으로 글을 쓰고 비밀번호를 바꾸는 등 '좀비'처럼 활동할 수 있습니다.
- 회원탈퇴 후의 보안 위험
    - 문제점**:** 사용자가 회원탈퇴를 진행해 DB상에서 논리적 삭제(`isDeleted=true`)가 되었음에도 불구하고, 이미 발급받아 둔 Access Token의 유효기간이 남아있다면 API 인가(Authorization) 필터를 무사히 통과해버리는 문제가 발생했습니다. 존재하지 않는 유저가 시스템을 조작할 수 있는 위험한 상태였습니다.


## 해결 및 구현 과정

redis와 같은 in-memory DB를 도입해 Access Token을 블랙리스트 처리하는 방법도 있었지만, 이는 과하다 판단했습니다.

대신 현재의 RDB 환경과 JWT 생명주기를 활용하여 비용 없이 안전하게 통제하는 로직을 구현했습니다.

- **Access Token의 생명주기(TTL) 최소화**
    - Access Token의 유효 기간을 30분으로 짧게 설정하여, 만약 탈취당하더라도 좀비 토큰으로 활동할 수 있는 '골든 타임'을 최소화했습니다.
- **DB(RDB)를 활용한 Refresh Token 즉각 파기 (로그아웃 대응)**
    - 사용자가 로그아웃 API(`/api/auth/logout`)를 호출하면, 서버는 즉시 RDB에 저장된 해당 유저의 Refresh Token 엔티티를 `DELETE` 처리합니다.
    - 이로써 해커가 30분 뒤 만료된 Access Token을 들고 와 재발급(Reissue)을 요청하더라도, DB에 대조할 Refresh Token이 없으므로 즉각 `400 (로그아웃된 계정입니다)` 에러를 반환하고 튕겨냅니다.
- **Soft Delete(논리적 삭제) 상태 검증 로직 추가 (회원탈퇴 대응)**
    - 회원 탈퇴 시 RDB의 유저 상태를 `isDeleted = true`로 변경(Soft Delete)합니다.
    - 보안 위협을 막기 위해, `AuthService`의 **토큰 재발급(`reissue`)** 및 **카카오 로그인 연동(`registerOrLogin`)** 단계마다 `user.isDeleted()` 상태를 반드시 먼저 검증하도록 방어 로직을 추가했습니다.
    - 탈퇴한 유저의 식별자로 API 요청이나 재발급 요청이 들어오면 `403 Forbidden (탈퇴한 회원입니다)` 예외를 발생시켜 시스템 접근을 원천 차단했습니다.

## 결과

- 비용이 발생하는 외부 캐시 서버(Redis)를 도입하지 않고, 토큰의 유효기간 조절과  RDB 중심의 재발급/상태 검증 로직만으로 보안 취약점을 성공적으로 방어했습니다.
</details>

<details>
<summary><strong> 카카오 로그인 Redirect URI mismatch </strong></summary>

## 문제 상황

AWS EC2에 배포된 Spring Boot 서버에서 카카오 로그인 시 다음 오류 발생:

```
500 Internal Server Error
{
  "success": false,
  "message": "카카오 로그인 중 오류가 발생했습니다."
}
```

서버 로그:

```
invalid_grant
Redirect URI mismatch
error_code: KOE303
```

## 시스템 구성

- Backend: Spring Boot (Docker, EC2)
- Frontend: Vercel
- CI/CD: GitHub Actions
- OAuth: Kakao Login
- 환경변수: GitHub Secrets → APPLICATION (.env 주입)

## 원인 분석 과정

### 1️⃣ 네트워크 문제 의심

- EC2 보안 그룹 확인 → 정상
- 카카오 Redirect 정상 호출 → 서버 도착 확인

즉:

> 카카오 → 서버 콜백 자체는 성공
>

---

### 2️⃣ 서버 내부 로그 분석

Docker 로그 확인:

```
docker logs lineofduty
```

확인 결과:

```
Redirect URI mismatch (KOE303)
```

카카오 OAuth 토큰 요청 단계에서 실패.

---

### 3️⃣ Redirect URI 값 검증

.env 설정:

```
KAKAO_REDIRECT_URI=http://43.201.115.73:8080/api/auth/kakao/callback
```

브라우저 요청:

```
http://43.201.115.73:8080/api/auth/kakao/callback
```

겉보기에는 정상.

---

### 4️⃣ 실제 컨테이너 환경변수 확인

EC2에서 실행:

```
docker exec -it lineofduty printenv | grep KAKAO
```

결과:

```
KAKAO_REDIRECT_URI=http://localhost:8080/api/auth/kakao/callback
```

👉 서버 내부 값이 localhost로 설정되어 있었음.

---

## 📌 근본 원인

GitHub Actions deploy 스크립트에서:

```
echo "${{ secrets.APPLICATION }}" > .env
docker run --env-file .env ...
```

즉:

> EC2에서 nano로 수정한 .env는 의미 없음
>
>
> GitHub Secrets 값이 매 배포마다 덮어씀
>

Secrets에 localhost 값이 남아 있었기 때문에 문제 발생.

---

## 📌 해결 방법

GitHub Repository → Settings → Secrets → APPLICATION 수정:

```
KAKAO_REDIRECT_URI=http://43.201.115.73:8080/api/auth/kakao/callback
```

이후 GitHub Actions 재실행.

## 결과

카카오 로그인 정상 동작 확인 ✅

- OAuth 인증 성공
- 사용자 정보 조회 성공
- JWT 발급 성공
- 프론트 리다이렉트 정상

## 핵심 교훈 ⭐⭐⭐

### ✅ 환경변수 Single Source of Truth

배포 환경에서는:

> 서버 파일보다 CI/CD Secrets 값이 우선한다.
>

로컬 수정은 의미 없을 수 있음.

---

### ✅ Docker 환경 확인 습관

문제 발생 시 반드시:

```
docker exec -it <container> printenv
```

으로 실제 런타임 값을 확인해야 한다.

---

### ✅ OAuth Redirect URI 특징

카카오는 다음 3개 값이 **완전히 동일**해야 한다:

1. 카카오 콘솔 Redirect URI
2. OAuth authorize 요청 redirect_uri
3. 토큰 요청 redirect_uri

문자 하나라도 다르면 실패.

---

## 📌 회고 (Retrospective)

이번 문제를 통해:

- CI/CD 환경변수 주입 방식 이해
- Docker 컨테이너 환경 확인 방법 학습
- OAuth Redirect 검증 구조 이해
- Secrets 기반 배포 구조 경험

단순 기능 구현을 넘어 **배포 환경 디버깅 역량**을 확보할 수 있었다.
</details>

## 🎯 성능개선 & 도메인 정책 개선

<details>
<summary><strong> Qna 게시판 정책 문제 변경 </strong></summary>

## **기존의 문제점**

기존의 QnA 게시판은 일반 커뮤니티 게시판과 동일하게 언제든 게시글 수정이 가능했습니다.

하지만 질문자가 답변을 확인한 후 질문 내용을 수정해버리면,

**질문과 답변의 문맥이 일치하지 않게 되는 문제**가 발생했습니다.

이는 추후 다른 사용자가 해당 정보를 열람할 때 혼란을 줄 수 있는 잠재적 위험 요소였습니다.

## **정책 변경**

관리자의 답변이 등록되는 순간 해당 게시글의 수정 권한을 비활성화 하는 로직을 적용했습니다.

## **의사결정 배경**

1. **지식 데이터의 보존:** 답변이 완료된 QnA는 단순한 1회성 질문이 아니라,

       향후 동일한 궁금증을 가진 사용자들에게 제공될 공공의 지식 자산이 됩니다. 

       질문의 원형을 보존함으로써 데이터의 가치를 유지하고자 했습니다.

1. **트랜잭션의 종료:** '질문-답변'이라는 하나의 프로세스가 완료된 시점을

       트랜잭션의 종료로 간주하여, 데이터의 위변조를 막는 것이 시스템 안정성 측면에서 

       옳다고 판단했습니다.

1. **벤치마킹:** 국내 최대 지식 공유 플랫폼인 네이버 지식iN 의 정책(답변 채택 후 수정 불가)을

       벤치마킹하여 사용자에게 익숙한 UX 규칙을 적용했습니다.
</details>

<details>
<summary><strong> 입영 연기 신청 상태 관리 로직 개선 </strong></summary>

# 1️⃣ 배경

입영 연기 신청 관리 페이지에서 **이미 처리 완료된 연기 신청 내역**이 프론트에 계속 노출되는 문제가 발생하였다.

기존 구조에서는 연기 신청이:

- 승인됨
- 반려됨

상태로 처리되더라도 이를 구분할 수 있는 컬럼이 존재하지 않았기 때문에:

> 처리 완료된 데이터와 승인 대기 데이터가 구분되지 않는 문제
>

가 존재하였다.

---

# 2️⃣ 문제점

### 기존 문제 흐름

```
연기 신청 → 관리자 승인/반려 처리 완료
          → DB에는 처리 여부 구분 컬럼 없음
          → 조회 API에서 그대로 반환
          → 프론트에 계속 노출
```

결과:

- 이미 처리된 신청이 계속 보임
- 승인 / 반려 버튼 클릭 시 동작하지 않음
- 사용자 UX 혼란 발생
- 데이터 상태 일관성 부족

즉:

> 상태 관리(State Management)가 불완전한 설계 문제
>

---

# 3️⃣ 개선 목표

연기 신청 데이터에 **처리 여부 상태 컬럼**을 추가하고,

조회 시:

```
승인 대기 상태만 조회
```

하도록 수정하여 데이터 정합성과 UX를 개선한다.

---

# 4️⃣ 핵심 개선 사항

## 1. isConfirmed 컬럼 추가

연기 신청 처리 여부를 명확히 구분하기 위한 컬럼 추가

```
isConfirmed =false → 승인 대기isConfirmed =true  → 처리 완료
```

효과:

- 상태 구분 가능
- 조회 필터 가능
- 로직 명확화

---

## 2. 연기 신청 조회 로직 수정

기존:

```
모든 연기 신청 조회
```

개선:

```
승인 대기중(isConfirmed =false) 데이터만 조회
```

Repository 쿼리 메소드 추가:

```java
findByIsConfirmedFalse(...)
```

효과:

- 프론트에 불필요 데이터 노출 제거
- UX 개선
- 상태 일관성 확보

---

## 3. DefermentController GET Mapping 추가

연기 신청 조회를 위한 API 엔드포인트 확장

목적:

- 관리자 페이지 데이터 조회 기능 강화
- 상태 필터 적용 조회 지원

# 5️⃣ 아키텍처 관점 개선 포인트

이번 PR의 핵심은 단순 버그 수정이 아니라:

> 상태 관리 모델(State Model)을 명확히 정의한 구조 개선
>

Before:

```
ImplicitState(암묵적 상태)
```

After:

```
ExplicitState(명시적 상태)
```

즉:

```
데이터 상태를 컬럼으로 관리
```

이는 실무적으로 매우 중요한 설계 개선이다.

---

# 6️⃣ 개선 효과

### ✔ UX 개선

- 처리 완료된 신청 미노출
- 사용자 혼란 제거

### ✔ 데이터 정합성 확보

- 상태 기반 로직 가능
- 명확한 비즈니스 흐름

### ✔ 유지보수성 향상

- 상태 판단 로직 단순화
- 조건 분기 감소

### ✔ 확장성 확보

향후 상태 확장 가능:

```
PENDING
APPROVED
REJECTED
```

---

# 7️⃣ Before vs After

## Before

- 처리 여부 컬럼 없음
- 모든 데이터 조회
- 프론트에서 잘못된 노출

## After

- isConfirmed 컬럼 추가
- 승인 대기 데이터만 조회
- UX 정상화
</details>

<details>
<summary><strong> Redis Cache Aside 전략 적용 — 입영 일정 조회 성능 개선 </strong></summary>

## 📌 도입 배경

입영 일정 데이터는 연간 약 **48건** 수준으로 데이터 규모가 매우 작다.

또한 슬롯을 제외한 대부분의 정보는 고정되어 있으며, 읽기 요청이 증가할 가능성이 높은 영역이다.

이 경우 인덱싱이나 쿼리 최적화보다 **DB 접근 자체를 제거하는 전략**이 더 효과적일 수 있다고 판단하였다.

따라서 다음과 같은 목표를 가지고 캐시를 도입하였다.

- DB I/O 자체 제거
- 읽기 트래픽 대응
- 데이터 최적화가 아닌 **접근 패턴 최적화**

이를 위해 **Cache Aside 전략**을 적용하였다.

---

## 📌 Cache Aside 전략 선택 이유

Cache Aside 패턴은 애플리케이션이 직접 캐시를 관리하는 방식이다.

동작 흐름:

1. 캐시에 데이터 존재 여부 확인
2. 없으면 DB 조회 후 캐시에 저장
3. 이후 요청은 캐시에서 응답

입영 일정 데이터는

- 조회 빈도 높음
- 변경 빈도 낮음
- 데이터량 작음

이라는 특성을 가지므로 Cache Aside 전략에 적합하였다.

---

## 📌 캐시 설정

캐시 TTL은 **5분**으로 설정하였다.

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
```

TTL을 짧게 유지하여 **데이터 정합성과 성능 사이 균형**을 맞추었다.

---

## 📌 캐시 적용 대상 API

입영 일정 조회 API에 `@Cacheable`을 적용하였다.

```java
@Cacheable(
        value = "enlistmentList",
        key = "T(java.time.LocalDate).now().toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
)
```

캐시 키는 다음 기준으로 구성하였다.

- 날짜
- 페이지 번호
- 페이지 사이즈

조회 요청이 많은 API이면서 데이터 변경이 적은 특성이 있어 캐시 효과가 높았다.

### API

```json
GET /api/enlistment?page=0&size=10
```

---

## 📌 캐시 무효화 전략 (Cache Evict)

입영 신청 또는 연기 처리 시 슬롯 값이 변경되므로 캐시 무효화가 필요하다.

```java
@CacheEvict(value = "enlistmentList", allEntries = true)
@Transactional
public BulkDefermentProcessResponse processDefermentBulk(...)
```

슬롯 변경이 발생하는 서비스 로직에서 캐시를 제거하여

**DB와 캐시 정합성을 유지**하였다.

---

## 📌 동작 검증

### 1️⃣ 캐시 조회 확인

캐싱된 상태에서 조회 시:

```json
{
  "scheduleId": 6,
  "remainingSlots": 10
}
```

응답 시간: 약 19ms

---

### 2️⃣ 슬롯 변경 발생 (CacheEvict 트리거)

입영 신청 API 호출:

```json
POST /api/enlistment-applications
```

슬롯 감소 후 캐시 제거 발생.

---

### 3️⃣ 재조회 결과

```json
{
  "scheduleId": 6,
  "remainingSlots": 9
}
```

캐시가 정상적으로 갱신된 것을 확인하였다.

---

## 📌 DB 직접 변경 시 정합성 문제

DB 값을 직접 수정 후 조회하면:

DB 값: 1

응답 값: 9

캐시 데이터가 반환된다.

이는 Cache Aside 구조의 특징으로,

> 캐시와 DB는 항상 100% 동기화되지 않는다.
>

정합성 기준 시점을 **서비스 로직 이벤트 기준**으로 잡는 것이 중요하다.

---

## 📌 성능 개선 효과

캐시 적용 후 기대 효과:

- DB 접근 제거
- 읽기 트래픽 처리 능력 향상
- 응답 시간 안정화
- DB 부하 감소

데이터 규모가 작더라도 **접근 패턴 최적화**만으로 성능 개선이 가능함을 확인하였다.

---

## 📌 설계 인사이트

이번 적용을 통해 얻은 핵심 인사이트는 다음과 같다.

### ✅ 캐시는 데이터 크기가 아니라 접근 패턴 문제

데이터가 적더라도 읽기 트래픽이 많으면 캐시가 효과적이다.

### ✅ 정합성은 “언제 맞출 것인가”의 문제

캐시와 DB가 항상 동일할 필요는 없으며,

서비스 이벤트 기준으로 정합성을 관리하는 것이 현실적이다.

### ✅ 인덱스보다 I/O 제거가 더 강력할 수 있다

48건 데이터에서는 인덱스 효과보다 캐시가 더 큰 성능 개선을 제공한다.

---

## 📌 결론

입영 일정 조회 기능에 Redis Cache Aside 전략을 적용하여

DB I/O를 제거하고 읽기 성능을 개선하였다.

특히 데이터 규모가 작은 시스템에서도

접근 패턴 최적화가 중요한 성능 개선 수단이 될 수 있음을 확인하였다.

---

## 📌 향후 개선 방향

- 캐시 미스율 모니터링
- 캐시 히트율 측정
- Redis 분리 인프라 검토 (트래픽 증가 시)
- Cache Stampede 대응 전략 적용
</details>

## 🛠️ 기술 스택

## 🛠 Tech Stack

**Language**

![Java](https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)

**Frontend**

![React](https://img.shields.io/badge/React-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Vercel](https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white)

**Backend**

![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring_AI-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-BC4521?style=for-the-badge&logo=lombok&logoColor=white)

**Database**

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

**Security**

![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![OAuth2](https://img.shields.io/badge/OAuth_2.0-EB5424?style=for-the-badge&logo=auth0&logoColor=white)

**Cloud**

![Ubuntu](https://img.shields.io/badge/Ubuntu-E95420?style=for-the-badge&logo=ubuntu&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white)
![AWS ECR](https://img.shields.io/badge/AWS_ECR-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)
![AWS RDS](https://img.shields.io/badge/AWS_RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white)

**Infra & CI/CD**

![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)

**API & Docs**

![Swagger](https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![Toss Payments](https://img.shields.io/badge/Toss_Payments-0064FF?style=for-the-badge&logo=tosspayments&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao_OAuth-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)
![Google OAuth](https://img.shields.io/badge/Google_OAuth-4285F4?style=for-the-badge&logo=google&logoColor=white)
![Naver Maps](https://img.shields.io/badge/Naver_Maps-03C75A?style=for-the-badge&logo=naver&logoColor=white)
![Kakao Maps](https://img.shields.io/badge/Kakao_Maps-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)
![공공데이터포털](https://img.shields.io/badge/공공데이터_포털-003087?style=for-the-badge&logo=data&logoColor=white)

**Test**

![JUnit5](https://img.shields.io/badge/JUnit5-25A162?style=for-the-badge&logo=junit5&logoColor=white)
![k6](https://img.shields.io/badge/k6-7D64FF?style=for-the-badge&logo=k6&logoColor=white)

## 📁 설계 문서

### 와이어 프레임

- 와이어 프레임

  ![img_10.png](img_10.png)

  ![img_11.png](img_11.png)

  ![img_12.png](img_12.png)

  ![img_14.png](img_14.png)

### API 명세서
[https://www.notion.so/teamsparta/5-2e62dc3ef51480efac1ceb9d418878da?source=copy_link#2e72dc3ef514803e99ade5bddd5e0cc4](https://www.notion.so/teamsparta/5-2e62dc3ef51480efac1ceb9d418878da?source=copy_link#2e72dc3ef514803e99ade5bddd5e0cc4)

### ERD
![img_15.png](img_15.png)

### 데이터베이스 설계
- 데이터베이스 설계

    ```jsx
    -- Users Table
    CREATE TABLE users (
        user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        email VARCHAR(255) NOT NULL UNIQUE,
        username VARCHAR(30) NOT NULL,
        password VARCHAR(255) NOT NULL,
        role VARCHAR(255) NOT NULL, -- Enum: USER, ADMIN, etc.
        profile_image_url VARCHAR(255),
        is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
        deleted_at DATETIME(6),
        kakao_id BIGINT UNIQUE,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL
    );
    
    -- Refresh Token Table
    CREATE TABLE refresh_token (
        rt_key BIGINT PRIMARY KEY, -- user_id
        rt_value VARCHAR(255) NOT NULL,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL
    );
    
    -- QnA Table
    CREATE TABLE qnas (
        qna_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL,
        title VARCHAR(255) NOT NULL,
        question_content VARCHAR(255) NOT NULL,
        ask_content VARCHAR(255),
        view_count BIGINT NOT NULL DEFAULT 0,
        version BIGINT, -- Optimistic Lock
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        FOREIGN KEY (user_id) REFERENCES users(user_id)
    );
    
    -- Products Table
    CREATE TABLE products (
        product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        name VARCHAR(255) NOT NULL,
        description TEXT,
        price BIGINT NOT NULL,
        stock BIGINT NOT NULL,
        product_image_url VARCHAR(255),
        status VARCHAR(255) NOT NULL,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL
    );
    
    -- Orders Table
    CREATE TABLE orders (
        order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL,
        order_name VARCHAR(255) NOT NULL,
        order_number VARCHAR(255) NOT NULL UNIQUE,
        total_price BIGINT NOT NULL,
        is_order_completed BOOLEAN NOT NULL DEFAULT FALSE,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        FOREIGN KEY (user_id) REFERENCES users(user_id)
    );
    
    -- Order Items Table
    CREATE TABLE order_items (
        order_item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        product_id BIGINT NOT NULL,
        order_id BIGINT NOT NULL,
        order_price BIGINT NOT NULL,
        quantity BIGINT NOT NULL,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        FOREIGN KEY (product_id) REFERENCES products(product_id),
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
    );
    
    -- Payments Table
    CREATE TABLE payments (
        payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        order_id BIGINT NOT NULL UNIQUE,
        total_price BIGINT NOT NULL,
        payment_key VARCHAR(255) NOT NULL UNIQUE DEFAULT,
        order_number VARCHAR(255) UNIQUE,
        status VARCHAR(255) NOT NULL,
        requested_at DATETIME(6),
        approved_at DATETIME(6),
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
    );
    
    -- Enlistment Schedules Table
    CREATE TABLE enlistment_schedules (
        schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        enlistment_date DATE NOT NULL,
        capacity INT NOT NULL,
        remaining_slots INT NOT NULL,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        INDEX idx_schedule_date (enlistment_date)
    );
    
    -- Enlistment Applications Table
    CREATE TABLE enlistment_applications (
        application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        application_status VARCHAR(255) NOT NULL,
        user_id BIGINT NOT NULL,
        schedule_id BIGINT NOT NULL,
        enlistment_date DATE NOT NULL,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        INDEX idx_app_status (application_status, schedule_id)
    );
    
    -- Deferments Table
    CREATE TABLE deferments (
        deferment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        application_id BIGINT NOT NULL UNIQUE,
        user_id BIGINT NOT NULL,
        reason VARCHAR(255) NOT NULL,
        status VARCHAR(255) NOT NULL, -- Enum
        changed_date DATE,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        FOREIGN KEY (application_id) REFERENCES enlistment_applications(application_id));
    
    -- Notices Table
    CREATE TABLE notices (
        notice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
        title VARCHAR(255) NOT NULL,
        content TEXT NOT NULL,
        author_id BIGINT NOT NULL,
        created_at DATETIME(6) NOT NULL,
        modified_at DATETIME(6) NOT NULL,
        FOREIGN KEY (author_id) REFERENCES users(user_id)
    );
    ```
