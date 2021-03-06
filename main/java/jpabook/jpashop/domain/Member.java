package jpabook.jpashop.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class Member {

    @Id @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String name;

    @Embedded
    private Address address;

    @JsonIgnore // 양방향 연관 관계가 있으면 한 쪽은 필수로 Ignore 해야 한다. (무한루프에 빠지므로)
    @OneToMany(mappedBy = "member") // mappedBy의 값은 Order 클래스에 있는 member 필드를 의미. 읽기전용을 의미 (주인X)
    private List<Order> orders = new ArrayList<>();
}
