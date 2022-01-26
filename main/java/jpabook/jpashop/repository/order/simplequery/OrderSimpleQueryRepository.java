package jpabook.jpashop.repository.order.simplequery;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * 화면에 의존적이고, 무겁고 복잡한 SQL 쿼리가 repository에 있으면 용도가 애매해진다.
 * 실무에서는 복잡한 쿼리를 가져가며 DTO를 뽑아야 할 상황에서 queryRepository, queryService와 같은 패키지로 분리하자.
 * 이를 통해 유지보수를 향상시킬 수 있다.
 * 참고) Repository는 가급적 순수한 엔티티를 조회하는데 써야한다. (ex. OrderRepository는 Order를 검색하고 조회하는 용도)
 */
@Repository
@RequiredArgsConstructor
public class OrderSimpleQueryRepository {

    private final EntityManager em;
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                "select new jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderSimpleQueryDto.class
        ).getResultList();
    }


}
