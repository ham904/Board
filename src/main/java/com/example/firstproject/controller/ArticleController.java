package com.example.firstproject.controller;

import com.example.firstproject.dto.ArticleForm;
import com.example.firstproject.dto.CommentDto;
import com.example.firstproject.entity.Article;
import com.example.firstproject.entity.Member;
import com.example.firstproject.repository.MemberRepository;
import com.example.firstproject.service.ArticleService;
import com.example.firstproject.service.CommentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
public class ArticleController {
    @Autowired
    private ArticleService articleService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private MemberRepository memberRepository;

    @GetMapping("/articles/new")
    public String newArticleForm() {
        return "articles/new";
    }

    @PostMapping("/articles/create")
    public String createArticle(ArticleForm articleForm, Principal principal) {
        Article article = articleService.create(articleForm, principal);
        // return "redirect:/articles/" + saved.getId(); 새 글 작성 후 해당 게시글 상세 페이지로 이동
        return "redirect:/articles"; // 전체 게시글 목록으로 이
    }

    @GetMapping("/articles/{id}")
    public String show(@PathVariable Long id, Model model, Principal principal) {
        Article article = articleService.show(id);
        List<CommentDto> commentDtos = commentService.comments(id);
        // 2. 모델에 데이터 등록하기
        model.addAttribute("article", article);
        model.addAttribute("commentDtos", commentDtos);
        // 로그인 상태를 확인하여 사용자 정보를 모델에 추가
        if (principal != null) {
            Optional<Member> member = memberRepository.findByUsername(principal.getName());
            Long memberId = member.isPresent() ? member.get().getId() : null;
            model.addAttribute("memberId", memberId);
        }
        else {
            model.addAttribute("memberId", null);
        }
        // 3. 뷰 페이지 반환하기
        return "articles/show";
    }

    @GetMapping("/articles")
    public String index(Model model, @RequestParam(value="page", defaultValue="0") int page,
                        @RequestParam(value = "kw", defaultValue = "") String kw, String searchOption) {

        Page<Article> paging = articleService.index(page, kw, searchOption);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            // 현재 사용자 정보를 가져와서 model에 등록
            String username = authentication.getName();
            Optional<Member> optionalMember = memberRepository.findByUsername(username); // 예시: 사용자 이름으로 회원 정보를 조회하는 메소드
            if (optionalMember.isPresent()) {
                Member member = optionalMember.get();
                model.addAttribute("member", member);
            }
        }

        model.addAttribute("paging", paging);
        model.addAttribute("kw",kw);
        model.addAttribute("defaultOption",searchOption);
        return "articles/index";
    }
    @GetMapping("/articles/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Article article = articleService.edit(id);
        model.addAttribute("article", article);
        return "articles/edit";
    }

    @PostMapping("articles/update")
    public String update(ArticleForm articleForm) {
        Article updated = articleService.update(articleForm);
        return "redirect:/articles";
    }
    @GetMapping("articles/{id}/delete")
    public String delete(@PathVariable Long id) {
        Article article = articleService.delete(id);
        return "redirect:/articles";
    }

}
