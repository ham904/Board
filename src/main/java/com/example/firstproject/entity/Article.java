package com.example.firstproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Article extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String title;
    @ManyToOne
    private Member member;
    @Column
    private String content;
    @Column(columnDefinition = "integer default 0", nullable = false)
    private int views;
    @OneToMany(mappedBy = "article", cascade = CascadeType.REMOVE)
    private List<Comment> commentList;

    public void patch(Article article) {
        if (article.getTitle() != null)
            this.title = article.getTitle();
        if (article.getContent() != null)
            this.content = article.getContent();
    }
}
