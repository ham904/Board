package com.example.firstproject.controller;

import com.example.firstproject.dto.MemberCreateForm;
import com.example.firstproject.dto.MemberForm;
import com.example.firstproject.entity.Member;
import com.example.firstproject.repository.MemberRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
public class MemberController {
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/members/signup")
    public String signup(MemberCreateForm memberCreateForm) {
        return "members/signup_form";
    }

    @PostMapping("/members/signup")
    public String signup(@Valid MemberCreateForm memberCreateForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "members/signup_form";
        }
        // 비밀번호 확인
        if (!memberCreateForm.getPassword1().equals(memberCreateForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "members/signup_form";
        }

        // 이미 등록된 사용자인지 확인
        if (!memberRepository.findByUsername(memberCreateForm.getUsername()).isEmpty()) {
            bindingResult.rejectValue("username", "usernameExists",
                    "이미 사용 중인 사용자 이름입니다."); // 이미 등록된 사용자인 경우 에러 처리
            return "members/signup_form";
        }

        // 이미 등록된 email인지 확인
        if (!memberRepository.findByEmail(memberCreateForm.getEmail()).isEmpty()) {
            bindingResult.rejectValue("email", "emailExists",
                    "이미 사용 중인 email입니다."); // 이미 등록된 email인 경우 에러 처리
            return "members/signup_form";
        }

        Member member = new Member();
        member.setUsername(memberCreateForm.getUsername());
        member.setEmail(memberCreateForm.getEmail());
        member.setPassword(passwordEncoder.encode(memberCreateForm.getPassword1()));
        memberRepository.save(member);
        return "redirect:/articles";
    }
    @GetMapping("/members/login")
    public String login() {
        return "members/login_form";
    }
    @GetMapping("/members/{id}")
    public String show(@PathVariable Long id, Model model) {
        Member memberEntity = memberRepository.findById(id).orElse(null);
        model.addAttribute("member", memberEntity);
        return "members/show";
    }

    @GetMapping("/members")
    public String index(Model model) {
        List<Member> memberList = memberRepository.findAll();
        model.addAttribute("memberList", memberList);
        return "members/index";
    }

    @GetMapping("members/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Member memberEntity = memberRepository.findById(id).orElse(null);
        model.addAttribute("member", memberEntity);
        return "members/edit";
    }

    @PostMapping("members/update")
    public String update(MemberForm memberForm) {
        Member memberEntity = memberForm.toEntity();
        Member target = memberRepository.findById(memberEntity.getId()).orElse(null);
        if (target != null)
            memberRepository.save(memberEntity);
        return "redirect:/members/" + memberEntity.getId();
    }

    @GetMapping("members/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes rttr) {
        Member target = memberRepository.findById(id).orElse(null);
        if (target != null) {
            memberRepository.delete(target);
            rttr.addFlashAttribute("msg", "삭제완료!");
        }
        return "redirect:/members";
    }
}
