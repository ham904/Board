package com.example.firstproject.dto;

import com.example.firstproject.entity.Article;
import com.example.firstproject.entity.Comment;
import com.example.firstproject.entity.Member;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.List;

@ToString
@AllArgsConstructor
public class MemberForm {
    private Long id;
    private String username;
    private String password;
    private String email;
    private List<Article> articles;
    private List<Comment> comments;

    public Member toEntity() {
        return new Member(id, username, password, email, articles, comments);
    }
}
