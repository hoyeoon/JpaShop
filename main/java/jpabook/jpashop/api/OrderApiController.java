package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSerach;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * V1 : 엔티티를 Order로 반환 했다. - 엔티티 변경 시 API 스펙이 변경되므로 잘못된 방식
     *
     * 양방향 관계 무한 루프 발생하지 않으려면 -> 한 쪽에는 @JsonIgnore를 발라주어 끊어야 한다.
     *
     * order member 와 order address 는 지연 로딩이다. 따라서 실제 엔티티 대신에 프록시 존재
     * jackson 라이브러리는 기본적으로 이 프록시 객체를 json으로 어떻게 생성해야 하는지 모름 예외 발생
     * Hibernate5Module 을 스프링 빈으로 등록하면 해결 -> Hibernate5Module 모듈 등록, LAZY=null 처리
     * Hibernate5Module에서 FORCE_LAZY_LOADING 옵션을 주는 방식도 있지만, 이 옵션은 애매한게 있으니 사용하지 말자.
     * 대신 order.getMember().getName(); 처럼 터치를 해줘서 Lazy 강제 초기화를 하여 해결한다.
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSerach());

        // LAZY 로딩에 대해 터치를 한 번 해줘야 한다. 위 주석 설명 참조
        for (Order order : all) {
            order.getMember().getName();
            order.getDelivery().getAddress();
            List<OrderItem> orderItems = order.getOrderItems();

            /*for (OrderItem orderItem : orderItems) {
                orderItem.getItem().getName();
            }*/
            orderItems.stream().forEach(o -> o.getItem().getName()); // Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2 : 엔티티를 DTO로 반환 했다. -> 컬렉션 조회를 하면 N + 1 문제가 더 심해진다. 최적화가 필요
     * ※ List 인 orderItems 의 경우도 OrderItem가 아니라 DTO로 반환해야 한다!!
     * List<OrderItem> (X), List<OrderItemDto> (O)
     * 엔티티를 외부에 노출시키면 안된다. 엔티티에 대한 의존을 완전히 끊어야 한다.
     * 단, Address 같은 Value Object는 노출시켜도 된다. 바뀔일이 없기 때문.
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSerach());
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }

    /**
     * V3 : fetch join으로 성능 최적화를 하여 N + 1 문제 해결
     * 이슈) findAllWithItem 에서 작성한 JPAL에서 distinct가 없을 경우 -> POSTMAN 결과가 2배로 뻥튀기 됨
     * 
     * ※ 단점 - 컬렉션 fetch join을 사용하면 페이징 불가능
     * 참고 : Hibernate는 경고 로그를 남기면서 모든 데이터를 DB에서 읽어오고, 메모리에서 페이징 해버린다(매우 위험하다!!)
     * 컬렉션 fetch join을 페이징과 함께 실행시 applying in Memory WARNING이 발생.
     * 즉, 메모리에 값을 전부 퍼올려서 sorting 하겠다는 뜻.
     * 이렇게 되면 out of memory가 발생될 수도 있고, 원하는 결과가 나오지 않으므로 사용해서는 안 된다.
     *
     * Hibernate는 왜 이런 전략을 선택했을까? (WARN : applying in Memory)
     * - 1:N 조인을 하는 순간 데이터가 뻥튀기 되고, Order 엔티티에 대한 원하는 페이징 기준을 판단할 수 없다.
     * - Order보다 데이터가 더 많은 OrderItem을 기준으로 페이징 될 수밖에 없으므로.
     *
     * 참고 : 컬렉션 fetch join은 1개만 사용할 수 있다. 컬렉션 둘 이상에 fetch를 사용하면 안된다. 데이터가 부정합하게 조회될 수 있다.
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }

    /**
     * V3.1 : 한계 돌파 (페이징 + 컬렉션 엔티티 함께 조회)
     *
     * 1. ToOne(OneToOne, ManyToOne) 관계를 모두 fetch join 한다.
     * ToOne 관계는 row 수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다.
     * ex. "select distinct o from Order o join fetch o.member m join fetch o.delivery d"
     * member와 delivery는 One관계 이므로 이런 관계는 fetch join해도 아무런 영향이 없다.
     *
     * 2. 컬렉션은 지연 로딩으로 조회한다.
     *
     * 3. 지연 로딩 성능 최적화를 위해 hibernate.default_batch_fetch_size, @BatchSize 적용한다.
     * - hibernate.default_batch_fetch_size : application.yml에서 글로벌 설정
     * - @BatchSize : 엔티티에 개별 최적화
     * - 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조회한다.
     *
     * 위의 방법을 사용하면, 쿼리 나가는 개수가
     * JPQL(findAllWithItem의 Fetch join 하는 부분) : OrderItem : Item = 1 : n : m 에서
     * 1 : 1 : 1 로 줄어든다!!
     * V3에서 한방 쿼리가 query가 정규화 안된 느낌이라면(중복 데이터가 존재),
     * V3.1은 쿼리가 3번 나가는 대신에 정규화 된 느낌이다.(테이블 조회 해보면 중복이 없음)
     * 게다가 페이징까지 된다!!
     *
     * 장점
     * - 쿼리 호출 수가 1 + N -> 1 + 1 로 최적화 된다.
     * - 조인보다 DB 데이터 전송량이 최적화 된다. (Order와 OrderItem을 조인하면 Order가 OrderItem 만큼 중복해서
     *   조회된다. 하지만, 이 방법은 각각 조회하므로 전송해야할 중복 데이터가 없다.)
     * - Fetch join 방식과 비교해서 쿼리 호출 수가 약간 증가하지만, DB 데이터 전송량이 감소한다.
     * - 컬렉션 페치 조인은 페이징이 불가능하지만, 이 방법은 페이징이 가능하다.
     *
     * 결론
     * - ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다. 따라서, ToOne 관계는 페치 조인으로 쿼리 수를 줄여서 해결하고,
     * 나머지는 hibernate.default_batch_fetch_size 로 최적화 하자. <- 기본으로 걸어두고 프로젝트하자.
     *
     * 참고
     * default_batch_fetch_size 의 크기는 적당한 사이즈를 골라야 하는데, 100~1000 사이를 선택하는 것을 권장한다.
     * 이 전략은 SQL IN 절을 사용하는데, db에 따라 in 절 파라미터를 1000으로 제한하기도 한다.
     * 1000으로 잡으면 한 번에 100개를 db에서 애플리케이션에 불러오므로 db에 순간 부하가 증가할 수 있다.
     * 하지만, 애플리케이션은 100이든 1000이든 결국 전체 데이터를 로딩해야 하므로 메모리 사용량이 같다.
     * 1000으로 설정하는 것이 성능상 가장 좋지만, 결국 db든 애플리케이션이든 순간 부하를 어디까지 견딜 수 있는지로 결정하면 된다.
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
                @RequestParam(value = "offset", defaultValue = "0") int offset,
                @RequestParam(value = "limit", defaultValue = "100") int limit) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }

    /**
     * V4 : JPA에서 DTO로 바로 조회.
     * Query: 루트 1번, 컬렉션 N번 실행
     * ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다.
     * - 이런 방식을 선택한 이유는 다음과 같다.
     * - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
     * - ToMany (1:N) 관계는 조인하면 row 수가 증가한다.
     * row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고, ToMany 관계는
     * 최적화하기 어려우므로 findOrderItems() 같은 별도의 메서드로 조회한다.
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;
//        private List<OrderItem> orderItems; 잘못된 방식
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            /* 잘못된 방식
            order.getOrderItems().stream()
                    .forEach(orderItem -> orderItem.getItem().getName()); // Lazy 강제 초기화
            orderItems = order.getOrderItems();*/
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;    // 상품 명
        private int orderPrice;     // 주문 가격
        private int count;          // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
