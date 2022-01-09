package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.*;

@RestController // @Controller @ResponseBody 합친 것
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 현재 문제 - Entity를 손대면 API 스펙 자체가 변경된다. Entity는 굉장히 여러 군데서 사용되기 때문에 바뀔 확률이 높다.
     * 따라서, Entity와 API 스펙이 1 : 1로 매핑되는 것은 문제가 있다.
     * 1. 어떤 API에서는 member의 name을 NotEmpty로 하고 싶을 수도 있지만, 다른 API에서는 null을 허용하고 싶을 수 있다.
     * 2. member의 변수 name이 username으로 이름이 변경될 경우
     *
     * 이와 같은 이유로, API 스펙을 위한 별도의 DTO를 만들어 사용해야 한다.
     * public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member)
     * 위와 같이 Entity를 외부에서 JSON 바인딩 받는 데 쓰면 안된다.
     *
     * API를 만들 때는 항상 Entity를 parameter로 받지 말것.
     * Entity를 외부로 노출해서도 안 된다.
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member){
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @GetMapping("/api/v1/members")
    public List<Member> membersV1(){
        return memberService.findMembers();
    }

    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request){

        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @PutMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(@PathVariable("id") Long id,
                                               @RequestBody @Valid UpdateMemberRequest request){
        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    /**
     * 해당 클래스를 통해 API 스펙을 한 눈에 볼 수 있다.
     * 사용하는 필드, 유효성 검사 등을 확인 가능
     */
    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    @Data
    static class UpdateMemberRequest{
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse{
        private Long id;
        private String name;
    }
}