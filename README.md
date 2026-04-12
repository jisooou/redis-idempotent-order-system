# 주문 API 중복 처리 어떻게 방지해야 할까?

## Notion : 
https://curious-cinema-a83.notion.site/API-32bd9939f2898063ba35fc60d1a373b6?source=copy_link

---
### Branch 설명

- main이 아닌 아래 두 브랜치로 나누어 코드를 작성하였다. 
- feature/basic-order : 일반 주문 생성과 조회 코드 
- feature/idempotency-order : Idempotency-Key와 Redis를 활용한 주문 생성과 조회 코드

---

### 1. 주문 시스템에서 중복 문제

- 같은 사용자가 응답 지연 때문에 “주문하기” 버튼을 연속으로 누르면(또는 네트워크 재시도 때문에) 동일한 요청이 여러 번 들어올 수 있다.
    - 이때 주문 시스템에서는 “여러 번 들어온 요청”을 어떻게 처리할지 정해야 한다.
        1. 주문마다 고유번호를 부여해서 각각을 별도 주문으로 만든다.
        2. 중복 주문은 막고, 첫 번째 요청만 처리한다.
            - 근데 여기서 문제는, 사용자가 진짜로 같은 상품을 한 번 더 주문한 케이스랑 “같은 시도의 재전송”을 구분해야 한다는 점이다.
            - 즉, 중복된 주문과 정상 주문을 구별할 기준이 필요하다.

### 2. 중복 문제 해결: 멱등성 (Idempotency)

위 문제를 풀기 위해 “멱등성(Idempotency)”을 활용한다.

- 멱등성이 적합하다고 생각한 이유는 다음과 같다.
    1. 주문 “시도” 1회마다 새로운 Idempotency-Key를 발급한다.
    2. 같은 시도의 재전송(재요청)이라면 같은 Key를 그대로 사용한다.
    3. 서버는 Key 기준으로 “이미 처리한 요청인지” 판단할 수 있다.
    4. 이미 처리된 Key라면 주문을 새로 만들지 않고, 기존 처리 결과를 그대로 반환한다.

1. 상황 A. 사용자가 주문 버튼을 3번 연속 클릭
    - 1번째 요청: 주문 생성 성공
    - 2번째 요청: 이미 처리된 Key → 기존 주문 결과 반환
    - 3번째 요청: 이미 처리된 Key → 기존 주문 결과 반환

   → 결과: 주문은 1건만 생성된다.

2. 상황 B. 사용자가 주문 완료 후 같은 상품을 다시 주문
    - 새 요청 → 새로운 Key 발급 → 주문 1건 추가 생성

   → 결과: 이전 주문과 합쳐서 총 주문 2건이 생성된다.


---

### 3. 멱등성의 역할

- 멱등성 API는 동일한 입력으로 반복 호출해도 결과가 동일하게 반환되도록 보장한다.
- 멱등성을 통해 네트워크 문제와 재시도를 효율적으로 관리할 수 있다.
    - 이를 통해 오류 처리를 더 효율적으로 할 수 있고, 데이터의 일관성과 무결성을 보장할 수 있다.

### 4. 멱등성 보장 방법

1. **고유 식별자 사용**
    - 요청마다 고유한 식별자를 부여해 동일 요청 여부를 판단한다.
2. **데이터베이스 제약조건 활용**
    - `UNIQUE` 제약조건 등을 걸어 중복 데이터 저장을 막는다.
3. **Idempotency-Key(토큰) 사용**
    - 요청마다 토큰을 발급하고, 같은 시도의 재전송에는 동일 토큰을 사용하게 한다.
    - 서버는 토큰 기준으로 최초 1회만 처리하고, 이후에는 기존 결과를 반환한다.
4. **캐싱 활용**
    - 동일 토큰으로 들어온 후속 요청에는 캐시된 결과를 반환해 반복 처리를 줄인다.
5. **클라이언트 측 중복 방지**
    - 버튼 비활성화 등으로 서버에 도달하는 중복 요청 자체를 줄인다.

**→ Idempotency-Key 토큰을 활용함으로써 고유 식별자 사용은 보장된다고 생각한다.
데이터베이스의 제약조건 같은 경우, (예를 들어) 주문 ID는 PK이기 때문에 `UNIQUE`를 보장한다.
캐싱을 활용하는 것은 Redis를 활용하여 이후 들어오는 반복 처리를 줄일 수 있다.
따라서 Idempotency-Key + 캐싱 활용이 가장 적합한 방법이라고 판단하였다.**

### 5. 멱등성의 한계

- 중복 요청 여부를 확인하기 위한 추가 조회가 필요해, 시스템 부하가 늘고 응답 시간이 길어질 수 있다.
- 동시에 많은 요청이 몰리면 처리 순서에 따른 충돌이 발생해, 우선순위를 고려한 설계가 필요하다.

---

### 6. **Idempotent Methods 이해하기**

- GET, PUT, DELETE 같은 HTTP 메서드는 특정 리소스의 id를 기준으로 조회, 수정, 삭제를 수행하므로 멱등성을 만족한다.
- 반면 리소스를 생성하는 POST는 같은 요청이 반복되면 리소스가 중복으로 생성될 수 있다.
    - 그래서 POST 요청에는 Idempotency-Key를 함께 보내고, 동일 키로 들어온 재요청은 최초 처리 결과를 그대로 반환하도록 구현해 중복 생성을 방지한다.

        ```json
        {http}
        POST /resource HTTP/1.1
        Host: example.com
        Idempotency-Key: unique-id-123
        Content-Type: application/json
        {
            "name": "New Resource",
            "value": "New Value"
        }
        ```


---

### 7. 구현

### -에러

```
Caused by: java.sql.SQLSyntaxErrorException: You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'order (
```

- 해당 에러는 MySQL에 `ORDER` 이라는 예약어가 있기 때문에 테이블명을 수정해야 해결되는 에러이다.
- 기존 Order 테이블명에서 → Orders로 변경해 준다.

### -주요 구현 부분

- Header에 Idempotency-Key를 넣어 요청을 보낸다.

    ```java
    @PostMapping
    public OrderResponseDto createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequestDto orderRequest
    ) {
        return orderService.createOrder(idempotencyKey, orderRequest);
    }
    ```


- Service에서는 Idempotency-Key가 기존에 없는 경우에만 새로운 Key로 발급을 해주면 되는데,
  이때 `redisTemplate`에서 제공하는 `setIfAbsent()`를 사용한다.

  `setIfAbsent()`는 Key가 존재하는지 확인하고 있으면 False를, 없으면 새로 발급해 주며 True를 리턴한다.

  새로 발급을 할 때는 TTL을 같이 명시하여, 만료(유효) 시간을 지정해 줄 수 있다.

    ```java
    Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, PROCESSING, IDEMPOTENCY_TTL);
    ```


---

### 8. 결과

- Notion : https://curious-cinema-a83.notion.site/API-32bd9939f2898063ba35fc60d1a373b6?source=copy_link

---

### 9. 결론

- 고객이 상품을 주문하려고 한다.
- 이때 네트워크 지연이나 고객의 의도치 않은 행동(버튼 중복 클릭 등)으로 동일한 요청이 여러 번 발생할 수 있다.
- 중복된 주문 요청과 정상 주문 요청을 구별할 수 있는 “무언가” 필요하다.
- 이를 해결하기 위해, 요청을 구별할 수 있는 Idempotency(멱등성)를 도입한다.
- 클라이언트는 요청마다 고유한 Idempotency-Key를 Header에 포함하고, 서버는 해당 Key를 기준으로 요청의 중복 여부를 판단한다.
- 이미 처리된 Key에 대해서는 새로운 주문을 생성하지 않고, 기존 처리 결과를 그대로 반환하여 멱등성을 보장한다.
- 이때 매 요청마다 DB를 조회하는 방식은 성능 저하를 유발할 수 있기 때문에, Redis를 활용하여 Idempotency-Key를 캐싱한다.
  → 성능 저하는 고객이 주문을 하는 도중 이탈할 수 있는 요인 중 하나라고 생각한다.
- Redis는 메모리 기반 저장소로, 빠른 조회(O(1))가 가능하여 중복 요청 판단을 효율적으로 처리할 수 있다.
- 그러나 모든 사용자의 주문이 Idempotency-Key로 캐싱되면 Redis 내 데이터는 어떨까?
- 그래서 TTL(Time To Live)를 설정하여 일정 시간이 지나면 Key가 자동으로 삭제되도록 하여, 불필요한 데이터 축적을 방지하고 재시도 가능한 시간 범위를 제한한다.
- 이와 같이 고객이 주문을 할 때, 주문 시스템에서 중복 요청을 안정적으로 방지하면서도, 성능 저하를 최소화할 수 있는 방법을 고민해 보았다.