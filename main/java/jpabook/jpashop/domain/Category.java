package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Category {

    @Id @GeneratedValue
    @Column(name = "category_id")
    private Long id;

    private String name;

    // ManyToMany는 실무에서 쓰지 않지만, 다양한 예제를 위해 해보자.
    @ManyToMany
    @JoinTable(name = "category_item", // 중간 테이블(CATEGORY_ITEM)
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    private List<Item> items = new ArrayList<>();

    // 셀프로 양방향 연관 관계를 건 부분 1
    @ManyToOne(fetch = FetchType.LAZY)  // 내 부모 이므로
    @JoinColumn(name = "parent_id")
    private Category parent;

    // 셀프로 양방향 연관 관계를 건 부분 2
    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();
}
