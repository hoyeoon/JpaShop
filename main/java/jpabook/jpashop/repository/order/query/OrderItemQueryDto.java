package jpabook.jpashop.repository.order.query;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jpabook.jpashop.domain.OrderItem;
import lombok.Data;

@Data
public class OrderItemQueryDto {

    @JsonIgnore // 필요 없으면 빼거나, JsonIgnore 사용
    private Long orderId;
    private String itemName;    // 상품 명
    private int orderPrice;     // 주문 가격
    private int count;          // 주문 수량

    public OrderItemQueryDto(Long orderId, String itemName, int orderPrice, int count) {
        this.orderId = orderId;
        this.itemName = itemName;
        this.orderPrice = orderPrice;
        this.count = count;
    }
}
