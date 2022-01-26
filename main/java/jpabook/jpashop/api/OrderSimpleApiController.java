package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSerach;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * xToOne(ManyToOne, OneToOne)
 * Order 조회
 * Order -> Member 연관
 * Order -> Delivery 연관
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    /**
     * V1 : 엔티티를 Order로 반환 했다. - 엔티티 변경 시 API 스펙이 변경되므로 잘못된 방식
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1(){
        List<Order> all = orderRepository.findAllByString(new OrderSerach());
        for(Order order : all){
            /**
             * order.getMember() 꺄지는 proxy 객체 (DB에 쿼리가 날라가지 않는다.)
             * order.getMember().getName() 까지 하면, Lazy 강제 초기화가 된다.
             * 그래서 Member에 쿼리를 날려서 JPA가 데이터를 다 긁어온다.
             */
            order.getMember().getName();    // Lazy 강제 초기화
            order.getDelivery().getAddress();   // Lazy 강제 초기화
        }
        return all;
    }

    /**
     * V2 - 엔티티를 DTO로 반환했다. 하지만 N + 1 문제가 존재
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
/*        return orderRepository.findAllByString(new OrderSerach()).stream()
                .map(SimpleOrderDto::new)
                .collect(toList());*/

        /**
         * orders 조회 -> SQL 1번 수행 -> 결과 주문수 2개
         * N + 1 문제 ( 1 + N )  : 첫 번째 쿼리의 결과로 N번 만큼 실행되는 문제
         * 여기서는 Order 1 + Member 2 + Delivery 2 = 5
         * 지연 로딩은 영속성 컨텍스트를 사용하므로 최악의 경우 1 + N 개 쿼리가 나간다.
         * 영속성 컨텍스트에 데이터가 있는 경우는 쿼리 개수가 줄어들 수 있겠다.
         * ex. userA가 주문하고, userA가 다시 주문할 때
         *      이미 영속성 컨텍스트에 member id를 갖고 있으므로 DB를 거치지 않고, 영속성 컨텍스트에 있는 값을 가져온다.
         */
       List<Order> orders = orderRepository.findAllByString(new OrderSerach());

       // InitDb에서 두 번의 주문을 넣었다.
        // 성능 최적화가 되지 않은 상태이기 때문에 member와 delivery 쿼리가 2번씩 나간다.
        List<SimpleOrderDto> result = orders.stream()
                .map(SimpleOrderDto::new) //.map(o -> new SimpleOrderDto(o))
                .collect(toList());

        return result;
    }

    /**
     * V3 : 엔티티를 DTO로 반환했다. fetch join으로 성능 최적화를 하여 N + 1 문제 해결
     * 한 방 쿼리로 끝난다!!
     * fetch join으로 order -> member, order -> delivery 는 이미 조회 된 상태 이므로 지연로딩 X
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());

        return result;
    }

    // 엔티티를 DTO로 변환하는 일반적인 방법
    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화 - Member id를 가지고 영속성 컨텍스트를 찾아간다. 없으면 db에 쿼리를 날린다.
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화 - Delivery id를 가지고 영속성 컨텍스트를 찾아간다. 없으면 db에 쿼리를 날린다.
        }
    }
}
