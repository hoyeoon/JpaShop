package jpabook.jpashop.domain;

import lombok.Getter;

import javax.persistence.Embeddable;

// 값 타입(임베디드 타입)은 변경 불가능하므로 Getter만! (실무에선 Setter 대신 빌더 등을 활용한 메서드로 생성 추천)
@Embeddable
@Getter
public class Address {

    private String city;
    private String street;
    private String zipcode;

    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }

    // JPA에서 리플렉션, 프록시 등을 쓰기 때문에 기본 생성자가 필요하다. 없으면 에러남.
    // JPA 스펙에서 엔티티나 임베디드 타입은 자바 기본 생성자를
    // protected까지 허용. public 보다 protected로 하면 더 안전하고, 아 이건 JPA 스펙때문에 만들었구나 생각 가능.
    protected Address() {
    }
}
