package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSerach;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

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

}
