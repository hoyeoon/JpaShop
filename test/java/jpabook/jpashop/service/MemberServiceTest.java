package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)    // Junit 실행 시 Spring 과 같이 엮어서 실행
@SpringBootTest // Spring Boot를 띄운 상태로 테스트 (@Autowired 와 같은 어노테이션 사용 가능)
@Transactional  // 테스트가 반복적으로 되어야 하기 때문에 기본적으로 rollback 한다.
public class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired MemberRepository memberRepository;
    @Autowired EntityManager em;   // insert 쿼리 날리는 걸 보고 싶을 경우 em.flush(); 하면된다.(마지막에 rollback 되는 것은 그대로)

    @Test
//    @Rollback(false) // DB에 들어가는걸 눈으로 확인하고 싶을 때는 false 옵션을 넣는 방법도 있다. (commit)
    public void 회원가입() throws Exception {
        // given
        Member member = new Member();
        member.setName("kim");

        // when
        Long savedId = memberService.join(member);

        // then
        em.flush();
        assertEquals(member, memberRepository.findOne(savedId));
    }

    @Test(expected = IllegalStateException.class)   // 아래 try catch 주석부분을 대신 해준다.
    public void 중복_회원_예외() throws Exception {
        // given
        Member member1 = new Member();
        member1.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        // when
        memberService.join(member1);
        memberService.join(member2);
        /*try {
            memberService.join(member2);    // 예외가 발생해야 한다.
        } catch(IllegalStateException e){
            return;
        }*/

        // then
        fail("예외가 발생해야 한다.");   // fail : 코드가 돌다가 여기 오면 안되는 것. 오면 뭔가 잘못 되었다는 뜻.
    }
}