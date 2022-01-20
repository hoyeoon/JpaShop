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

    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
/*        return orderRepository.findAllByString(new OrderSerach()).stream()
                .map(SimpleOrderDto::new)
                .collect(toList());*/

        // orders 조회 -> SQL 1번 수행 -> 결과 주문 row 수 2개
        // N + 1 문제 ( 1 + N )  : 첫 번째 쿼리의 결과로 N번 만큼 실행되는 문제
        // 여기서는 1 + 회원 N(2) + 배송 N(2) = 5
        // 지연 로딩은 영속성 컨텍스트를 사용하므로 최악의 경우 1 + N 개 쿼리가 나간다.
        // 영속성 컨텍스트에 데이터가 있는 경우는 개수가 줄 수 있겠다.
       List<Order> orders = orderRepository.findAllByString(new OrderSerach());

       // LOOP는 2바퀴 돈다. 따라서 member와 delivery 쿼리가 2번씩 나간다.
        List<SimpleOrderDto> result = orders.stream()
                .map(o -> new SimpleOrderDto(o))
                .collect(toList());

        return result;
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName(); // LAZY 초기화
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }
}
