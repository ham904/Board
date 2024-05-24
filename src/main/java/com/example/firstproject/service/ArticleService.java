package com.example.firstproject.service;

import com.example.firstproject.dto.ArticleForm;
import com.example.firstproject.entity.Article;
import com.example.firstproject.entity.Member;
import com.example.firstproject.repository.ArticleRepository;
import com.example.firstproject.repository.MemberRepository;
import jakarta.persistence.criteria.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ArticleService {
    @Autowired
    private ArticleRepository articleRepository;
    @Autowired
    private MemberRepository memberRepository;

    private Specification<Article> search(String kw, String searchOption) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<Article> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true);  // 중복을 제거
                Join<Article, Member> u1 = q.join("member", JoinType.LEFT);

                if ("title".equals(searchOption)) {
                    return cb.like(q.get("title"), "%" + kw + "%");
                } else if ("content".equals(searchOption)) {
                    return cb.like(q.get("content"), "%" + kw + "%");
                } else if ("title_content".equals(searchOption)) {
                    return cb.or(
                            cb.like(q.get("title"), "%" + kw + "%"),
                            cb.like(q.get("content"), "%" + kw + "%")
                    );
                } else if ("member".equals(searchOption)) {
                    return cb.like(u1.get("username"), "%" + kw + "%");
                }

                // 기본적으로는 제목을 검색
                return cb.like(q.get("title"), "%" + kw + "%");
            }
        };
    }


    public Page<Article> index(int page, String kw, String searchOption) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createdDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        Specification<Article> spec = search(kw,searchOption);

        return this.articleRepository.findAll(spec,pageable);
    }

    public Article show(Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        articleRepository.updateView(id);
        return article;
    }

    public Article create(ArticleForm dto, Principal principal) {
        // 1. DTO를 엔티티로 변환
        Article article = dto.toEntity();
        log.info(article.toString());
        if (article.getMember() == null) {
            article.setMember(memberRepository.findByUsername(principal.getName()).get());
        }

        // 2. 리파지터리로 엔티티를 DB에 저장
        Article saved = articleRepository.save(article);
        return saved;
    }

    public Article edit(Long id) {
        Article article = articleRepository.findById(id).orElse(null);
        return article;
    }

    public Article update(ArticleForm dto) {
        Article article = dto.toEntity();
        Article target = articleRepository.findById(article.getId()).orElse(null);
        if (target == null) {
            return null;
        }
        target.patch(article);
        Article updated = articleRepository.save(target);
        return updated;
    }

    public Article delete(Long id) {
        Article target = articleRepository.findById(id).orElse(null);
        if (target == null) {
            return null;
        }
        articleRepository.delete(target);
        return target;
    }

    @Transactional
    public List<Article> createArticles(List<ArticleForm> dtos) {
        // 1. dto 묶음을 엔티티 묶음으로 변환하기
        List<Article> articleList = dtos.stream()
                .map(dto -> dto.toEntity())
                .collect(Collectors.toList());
        // 2. 엔티티 묶음을 DB에 저장하기
        articleList.stream()
                .forEach(article -> articleRepository.save(article));
        // 3. 강제 예외 발생시키기
        articleRepository.findById(-1L)
                .orElseThrow(()->new IllegalArgumentException("결제 실패!"));
        return articleList;
    }
}
