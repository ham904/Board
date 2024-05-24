package com.example.firstproject.dto;

import com.example.firstproject.entity.Article;
import com.example.firstproject.entity.Comment;
import com.example.firstproject.entity.Member;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

@AllArgsConstructor
@ToString
public class ArticleForm {
    private Long id;
    private String title;
    private Member member;
    private String content;
    private Integer views; // 조회수
    private List<Comment> comments;

    public Article toEntity() {
        return new Article(id, title, member, content, views != null ? views : 0,comments);
    }
}
