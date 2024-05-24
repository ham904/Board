package com.example.firstproject.repository;

import com.example.firstproject.entity.Article;
import com.example.firstproject.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

public interface ArticleRepository extends CrudRepository<Article,Long> {
    @Override
    ArrayList<Article> findAll();

    @Transactional
    @Modifying
    @Query("update Article p set p.views = p.views + 1 where p.id = :id")
    int updateView(@Param("id") Long id);

    Page<Article> findAll(Pageable pageable);
    Page<Article> findByTitleContaining(String searchKeyword, Pageable pageable);
    Page<Article> findAll(Specification<Article> spec, Pageable pageable);
}
