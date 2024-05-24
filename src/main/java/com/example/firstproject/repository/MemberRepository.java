package com.example.firstproject.repository;

import com.example.firstproject.entity.Comment;
import com.example.firstproject.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Override
    ArrayList<Member> findAll();

    @Query(value = "select * from member where username = :username", nativeQuery = true)
    Optional<Member> findByUsername(String username);

    @Query(value = "select * from member where email = :email", nativeQuery = true)
    Optional<Member> findByEmail(String email);
}
