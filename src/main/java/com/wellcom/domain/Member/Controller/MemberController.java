package com.wellcom.domain.Member.Controller;

import com.wellcom.domain.Member.Dto.MemberSignUpDto;
import com.wellcom.domain.Member.Service.MemberService;
import com.wellcom.global.auth.jwt.service.JwtService;
import com.wellcom.global.common.CommonResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.wellcom.domain.Member.Dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final JwtService jwtService;

    @PostMapping("/sign-up")
    public ResponseEntity<CommonResponse> signUp(@RequestBody MemberSignUpDto memberSignUpDto) throws Exception {
        memberService.signUp(memberSignUpDto);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "회원가입이 정상적으로 이루어졌습니다.", ""));
    }

    // reissue용 api 아무것도 실행 안함
    @GetMapping("/member/reissue")
    public void reIssueToken(){}

    @GetMapping("/ok")
    public void ok(){}

    // 회원 삭제
    @DeleteMapping("/member/{id}/delete")
    public ResponseEntity<CommonResponse> memberDelete(@PathVariable Long id) {
        memberService.delete(id);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "회원이 성공적으로 삭제되었습니다.", id));
    }

    // 회원 상세 정보 조회
    // 관리자와 일반 사용자용 엔드포인트를 하나로 통합
    @GetMapping("/member/{id}/detail")
    public ResponseEntity<CommonResponse> memberDetail(@PathVariable Long id) {
        MemberDetailResDto memberDetail = memberService.findMemberDetail(id);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "멤버상세조회입니다.", memberDetail));
    }

    @GetMapping("/member/detailByEmail")
    public ResponseEntity<CommonResponse> memberDetailByEmail(@RequestParam String email) {
        MemberDetailResDto memberDetail = memberService.findMemberDetailByEmail(email);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "멤버 상세 조회입니다.", memberDetail));
    }

    // 회원 목록 조회
    @GetMapping("admin/member/list")
    public ResponseEntity<CommonResponse> memberList() {
        MemberListTotalResDto memberListResponse = memberService.findAll();
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "멤버조회입니다.", memberListResponse));
    }

    // 회원 정보 수정
    @PostMapping("/member/{id}/update")
    public ResponseEntity<CommonResponse> memberUpdate(@PathVariable Long id, @RequestBody MemberUpdateReqDto memberUpdateReqDto) {
        memberService.update(id, memberUpdateReqDto);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "성공적으로 수정되었습니다.",HttpStatus.OK));
    }

    // 회원 차단
    @PutMapping("/admin/member/{id}/block")
    public ResponseEntity<CommonResponse> blockMember(@PathVariable Long id) {
        memberService.blockMember(id);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "회원이 성공적으로 차단되었습니다.", HttpStatus.OK));
    }

    // 회원 차단 해제
    @PutMapping("/admin/member/{id}/unblock")
    public ResponseEntity<CommonResponse> unblockMember(@PathVariable Long id) {
        memberService.unblockMember(id);
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "회원 차단이 성공적으로 해제되었습니다.", HttpStatus.OK));
    }
    @GetMapping("/admin/member/blocked")
    public ResponseEntity<CommonResponse> listBlockedMembers() {
        List<MemberListResDto> blockedMembers = memberService.findBlockedMembers();
        return ResponseEntity.ok(new CommonResponse(HttpStatus.OK, "차단된 회원 목록 조회", blockedMembers));
    }
    @GetMapping("/member/{memberId}/reservations")
    public ResponseEntity<List<ReservationDetailDto>> getMemberReservations(@PathVariable Long memberId) {
        List<ReservationDetailDto> reservations = memberService.getMemberReservations(memberId);
        return ResponseEntity.ok(reservations);
    }
}