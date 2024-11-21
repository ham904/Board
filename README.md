
# 게시판 서비스
## 개요
- JAVA Spring Boot를 이용한 게시판 서비스

#### 만든 목적
- JAVA Spring boot를 이용한 백엔드 프로세스 학습

#### 주요 기능
- 회원가입 및 로그인 (Spring Security)
- 게시글 CRUD
- 댓글 CRUD
- 페이징 및 검색 기능

#### 일정
- 24.04.05 ~ 24.05.15
- 참여도 : 100% (개인 프로젝트)

## 사용 기술 및 개발 환경
- O/S : macOS 10.15.1
- Server : Apache Tomcat 10.1.19
- DB : MySQL
- 백엔드 : Spring Boot, SpringSecurity
- 프론트엔드 :  Thymeleaf, Bootstrap
- Language : JAVA, Javascript, HTML5, CSS3
- Tool : IntelliJ, GitHub

## 내용
#### 구현 기능
- 회원가입 및 로그인

회원가입
MemberController 일부
~~~java
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
~~~

로그인

SecurityConfig.java 일부
~~~java
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        	http
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        .requestMatchers(new AntPathRequestMatcher("/**")).permitAll())
                .headers((headers) -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
                .formLogin((formLogin) -> formLogin
                        .loginPage("/members/login")
                        .defaultSuccessUrl("/articles"))
                .logout((logout) -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/members/logout"))
                        .logoutSuccessUrl("/articles")
                        .invalidateHttpSession(true))
                .csrf(AbstractHttpConfigurer::disable);
        	
        	return http.build();
    	}
~~~
---

- 게시글 CRUD <br/>

ArticleController.java 일부
~~~java
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
~~~ 

ArticleService.java 일부
~~~java
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
~~~ 

- 댓글 CRUD

CommentApiController.java
~~~java
@GetMapping("/api/articles/{articleId}/comments")
    public ResponseEntity<List<CommentDto>> comments(@PathVariable Long articleId) {
        // 서비스에 위임
        List<CommentDto> dtos = commentService.comments(articleId);
        // 결과 응답
        return ResponseEntity.status(HttpStatus.OK).body(dtos);
    }
    // 2. 댓글 생성
    @PostMapping("/api/articles/{articleId}/comments")
    public ResponseEntity<CommentDto> create(@PathVariable Long articleId, @RequestBody CommentDto dto
                                            , Principal principal) {
        // 서비스에 위임
        CommentDto commentDto = commentService.create(articleId, dto, principal);
        // 결과 응답
        return ResponseEntity.status(HttpStatus.OK).body(commentDto);
    }
    // 3. 댓글 수정
    @PatchMapping("/api/comments/{id}")
    public ResponseEntity<CommentDto> update(@PathVariable Long id, @RequestBody CommentDto dto) {
        // 서비스에 위임
        CommentDto updatedDto = commentService.update(id, dto);
        // 결과 응답
        return ResponseEntity.status(HttpStatus.OK).body(updatedDto);
    }
    // 4. 댓글 삭제
    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<CommentDto> delete(@PathVariable Long id) {
        // 서비스에 위임
        CommentDto deletedDto = commentService.delete(id);
        // 결과 응답
        return ResponseEntity.status(HttpStatus.OK).body(deletedDto);
    }
~~~ 

CommentService.java
~~~java
public List<CommentDto> comments(Long articleId) {
        // 1. 댓글 조회
        List<Comment> comments = commentRepository.findByArticleId(articleId);
        // 2. 엔티티 -> DTO 변환
        List<CommentDto> dtos = new ArrayList<CommentDto>();
        for (int i = 0;i<comments.size();i++) {
            Comment c = comments.get(i);
            CommentDto dto = CommentDto.createCommentDto(c);
            dtos.add(dto);
        }
        // 3. 결과 반환
        return commentRepository.findByArticleId(articleId)
                .stream().map(comment->CommentDto.createCommentDto(comment))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDto create(Long articleId, CommentDto dto, Principal principal) {
        // 1. 게시글 조회 및 예외 발생
        Article article = articleRepository.findById(articleId)
                .orElseThrow(()->new IllegalArgumentException("댓글 생성 실패!"+
                        "대상 게시글이 없습니다."));
        if (dto.getMember()==null) {
            dto.setMember(memberRepository.findByUsername(principal.getName()).get());
        }
        // 2. 댓글 엔티티 생성
        Comment comment = Comment.createComment(dto,article);
        // 3. 댓글 엔티티를 DB에 저장
        Comment created = commentRepository.save(comment);
        // 4. DTO로 변환해 반환
        return CommentDto.createCommentDto(created);
    }

    @Transactional
    public CommentDto update(Long id, CommentDto dto) {
        // 1. 댓글 조회 및 예외 발생
        Comment target = commentRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("댓글 수정 실패!"+
                        " 대상 댓글이 없습니다."));
        // 2. 댓글 수정
        target.patch(dto);
        // 3. DB로 갱신
        Comment updated = commentRepository.save(target);
        // 4. 댓글 엔티티를 DTO로 변환 및 반환
        return CommentDto.createCommentDto(updated);
    }

    @Transactional
    public CommentDto delete(Long id) {
        Comment target = commentRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("댓글 삭제 실패!"+
                        " 대상 댓글이 없습니다."));
        commentRepository.delete(target);
        return CommentDto.createCommentDto(target);
    }
~~~
<br/><br/>

- 페이징 및 검색 기능
ArticleController.java 일부
~~~java
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
~~~
ArticleService.java 일부
~~~java
public Page<Article> index(int page, String kw, String searchOption) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createdDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        Specification<Article> spec = search(kw,searchOption);

        return this.articleRepository.findAll(spec,pageable);
    }
~~~
<br/>
# 메인화면
![메인화면](https://github.com/ham904/Board/assets/141111846/dd93b4aa-4e84-431d-a9a0-6c945db85c8c)
