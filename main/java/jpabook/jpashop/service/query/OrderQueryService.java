package jpabook.jpashop.service.query;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSerach;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * TIP
 * ex. 하나의 respository나 service에 핵심 비즈니스 용, 화면용 메서드를 같이 넣으면 유지보수성이 매우 떨어진다.
 * 핵심 비즈니스 메서드 vs 화면용 메서드의 Life Cycle이 다르다.
 *
 * 대부분 화면에 맞춘 화면용 API는 자주 변경되어 Life Cycle이 빠르다.
 * 핵심 비즈니스 로직을 담은 클래스는 오랫동안 잘 변경되지 않는다.
 *
 * 애플리케이션이 커질 경우 OrderSerivce, OrderQueryService를 분리할 것을 추천한다.
 */

/**
 * OSIV 옵션을 false로 할 경우 트랜잭션 안에서만 영속성 컨텍스트(db connection)이 관리 되므로
 * Controller에서 LAZY LOADING이 불가능하다.(postman 실행 안됨)
 * 따라서 아래와 같이 @Transactional(readOnly = true)를 담은 메서드로 분리한다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;

    public List<OrderDto> ordersV2_OSIV_false() {
        List<Order> orders = orderRepository.findAllByString(new OrderSerach());
        List<OrderDto> result = orders.stream()
                .map(OrderDto::new)
                .collect(toList());
        return result;
    }
}
