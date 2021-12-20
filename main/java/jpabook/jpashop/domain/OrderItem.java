package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;

import javax.persistence.*;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; // 주문 당시 가격 (가격은 변동 될 수 있으므로 필요)

    private int count;  // 주문 당시 수량

    // JPA는 protected 까지 기본생성자를 만들도록 허용
    /*protected OrderItem(){

    }*/
    /**
     * 이렇게 했을 때 장점은, OrderService.java에서
     * OrderItem orderItem = new OrderItem() 같은 코드를 쓰지 말라는 것을 인지시킬 수 있다.
     * 여러 방식으로 코드를 만들도록 분산시키지 않고, createOrderItem 만을 사용하도록 제약시키는 것이 유지보수에 좋다.
     */
    // 위 코드는 @NoArgsConstructor(access = AccessLevel.PROTECTED)로 줄일 수 있다.

    // 생성 메서드
    public static OrderItem createOrderItem(Item item, int orderPrice, int count){
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        item.removeStock(count);
        return orderItem;
    }


    // 비즈니스 로직
    public void cancel() {
        getItem().addStock(count);
    }

    // 조회 로직
    /**
     * 주문상품 전체 가격 조회
     */
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
