# 메인화면
[게시판 바로가기](http://43.202.221.44:8080/articles)
![메인화면](https://github.com/ham904/Board/assets/141111846/dd93b4aa-4e84-431d-a9a0-6c945db85c8c)

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

CommentService.java 일부
~~~java
@Override
public List<ResultReportVO> getList(String year, String semester, String subjectname, String division) {
	log.info("getList year : " + year + ", semester : " + semester + ", subjectname" + subjectname + ", division : "
			+ division);

	return resultreportMapper.getList(year, semester, subjectname, division);
}
~~~
<br/><br/>
2. 지원신청서, 결과보고서 조회페이지에서 해당 담당 교수일 경우 승인 및 취소(대기) 처리 가능 <br/>
cdims_result_report_get.jsp 
~~~js
	//CSRF 토큰 처리
	var csrfHeaderName = "${_csrf.headerName}";
	var csrfTokenValue = "${_csrf.token}";
	//Ajax spring security header
	//ajaxSned()는 Ajax 전송 시(매번) SCRF 토큰을 같이 전송하도록 세팅함
	$(document).ajaxSend(function(e, xhr, options) {
		xhr.setRequestHeader(csrfHeaderName, csrfTokenValue);
	});

	var writer = null;
	<sec:authorize access="isAuthenticated()">
		// 작성자 (담당 교수)
		writer = '<sec:authentication property="principal.username"/>';
	</sec:authorize>

	/* 승인취소버튼 */
	$("button[data-oper='approval_cancel']").on("click", function(e) {
		console.log("승인취소 클릭");
		var approvalStatus = {teamno : teamno, approval : "대기"};

		resultreportService.update(approvalStatus, function(result) {
			if (result == "success") {
				alert('승인취소되었습니다.');
				approvalStat.html("'<td colspan='5' id='approval_status' style='color: green;'>" + approvalStatus.approval + "</td>");
			}
		});
	});

	/* 승인버튼 */
	$("button[data-oper='approval']").on("click", function(e) {
		console.log("승인버튼 클릭");
		var approvalStatus = {teamno : teamno, approval : "승인"};

		resultreportService.update(approvalStatus, function(result) {
			if (result == "success") {
				alert('승인완료되었습니다.');
				approvalStat.html("'<td colspan='5' id='approval_status' style='color: green;'>" + approvalStatus.approval + "</td>");
			}
		});
	});
~~~
result-report.js 일부
~~~js
function update(approvalStatus, callback, error) {
	console.log("approvalStatus teamno: " + approvalStatus.teamno + ", approval : " + approvalStatus.approval);

	$.ajax({
		type : 'put',
		url : '/result_report/' + approvalStatus.teamno + "/" + approvalStatus.approval, 
		data : JSON.stringify(approvalStatus),
		contentType : "application/json; charset=utf-8",
		success : function(result, status, xhr) {
			if (callback) {
				callback(result);
			}
		},
		error : function(xhr, status, er) {
			if (error) {
				error(er);
			}
		}
	});
}
~~~
ResultReportController.java 일부
~~~java
@RequestMapping(method={RequestMethod.PUT, RequestMethod.PATCH}, value="/{teamno}/{approval}",
		consumes = "application/json", produces= {MediaType.TEXT_PLAIN_VALUE})
public ResponseEntity<String> approvalUpdate(@PathVariable("teamno") int teamno, @PathVariable("approval") String approval) {
	log.info("approvalUpdate teamno : " + teamno + ", approval : " + approval);


	return resultReportService.approvalUpdate(teamno, approval) == 1 ? new ResponseEntity<String>("success", HttpStatus.OK) :
		new ResponseEntity<String>(HttpStatus.INTERNAL_SERVER_ERROR);
}
~~~
ResultReportServiceImpl.java 일부
~~~java
@Override
public int approvalUpdate(int teamno, String approval) {
	log.info("approvalUpdate teamno : " + teamno + ", approval : " + approval);

	return resultreportMapper.approvalUpdate(teamno, approval); 
}
~~~
---
- 커뮤니티 (공지사항, 양식 서류(첨부파일 기능), Q&A (댓글 기능)), 검색 (키워드)
<br/><br/>
1. 양식 서류 게시판 첨부파일 기능 (Ajax 방식으로 처리)

cdims_form_document_write.jsp 일부
~~~js
// controller로 데이터 넘김
  $("input[type='submit']").on("click", function(e){
    console.log("submit clicked");
    var str = "";
    
    $(".uploadResult ul li").each(function(i, obj){
      var jobj = $(obj);
      
      console.dir(jobj.data("image"));
      console.log("-------------------------");
      console.log(jobj.data("filename"));
      
      str += "<input type='hidden' name='attachList["+i+"].uuid' value='"+jobj.data("uuid")+"'>";
      str += "<input type='hidden' name='attachList["+i+"].fileName' value='"+jobj.data("filename")+"'>";
      str += "<input type='hidden' name='attachList["+i+"].uploadPath' value='"+jobj.data("path")+"'>";
      str += "<input type='hidden' name='attachList["+i+"].fileType' value='"+ jobj.data("type")+"'>";
      
    });

    console.log("str: " + str);
    
    formObj.append(str);
    
  });

  
  var regex = new RegExp("(.*?)\.(exe|sh|alz)$");
  var maxSize = 90242880; //90MB
  
  // 파일 사이즈 및 종류 체크
  function checkExtension(fileName, fileSize){
    if(fileSize >= maxSize){
      alert("파일 사이즈 초과");
      return false;
    }
    
    if(regex.test(fileName)){
      alert("해당 종류의 파일은 업로드할 수 없습니다.");
      return false;
    }
    return true;
  }
  
  var csrfHeaderName = "${_csrf.headerName}";
  var csrfTokenValue = "${_csrf.token}";
  
  $("input[type='file']").change(function(e){

    var formData = new FormData();
    
    var inputFile = $("input[name='uploadFile']");
    
    var files = inputFile[0].files;
    
    for(var i = 0; i < files.length; i++){

      if(!checkExtension(files[i].name, files[i].size) ){
        return false;
      }
      formData.append("uploadFile", files[i]);
      
    }
    
    $.ajax({
      url: '/fd_upload/uploadAjaxAction',
      processData: false, 
      contentType: false,
      beforeSend: function(xhr) { // Ajax로 데이터를 전송할 때 추가적인 헤더를 지정해서 전송
    	  xhr.setRequestHeader(csrfHeaderName, csrfTokenValue);
      },
      data: formData,
      type: 'POST',
      dataType:'json',
        success: function(result){
          console.log("result : " + result);
		  showUploadResult(result); //업로드 결과 처리 함수 

      }
    }); //$.ajax
    
    
    // 클라이언트(화면)에 바로 해당 파일 보여주기
  function showUploadResult(uploadResultArr){
    if(!uploadResultArr || uploadResultArr.length == 0){ return; }
    
    var uploadUL = $(".uploadResult ul");
    
    var str ="";
    
    $(uploadResultArr).each(function(i, obj){
		var fileCallPath =  encodeURIComponent( obj.uploadPath+"/"+ obj.uuid +"_"+obj.fileName);			       
	      
		str += "<li "
		str += "data-path='"+obj.uploadPath+"' data-uuid='"+obj.uuid+"' data-filename='"+obj.fileName+"' data-type='"+obj.fileType+"' ><div>";
		str += "<span> "+ obj.fileName+"</span>";
		str += "<button type='button' data-file=\'"+fileCallPath+"\' data-type='file' " 
		str += "class='btn btn-warning btn-circle'><i class='fa fa-times'></i></button><br>";
		str += "<img src='/resources/img/attach.png'></a>";
		str += "</div>";
		str +"</li>";

    });
    
    uploadUL.append(str);
    
    // 클라이언트(화면)에서 파일 삭제
  $(".uploadResult").on("click", "button", function(e){
	    
    console.log("delete file");
      
    var targetFile = $(this).data("file"); 
    var type = $(this).data("type"); 
    
    var targetLi = $(this).closest("li");
    
    $.ajax({
      url: '/fd_upload/deleteFile',
      data: {fileName: targetFile, type:type},
      beforeSend: function(xhr) { // Ajax로 데이터를 전송할 때 추가적인 헤더를 지정해서 전송
    	  xhr.setRequestHeader(csrfHeaderName, csrfTokenValue);
      },
      dataType:'text',
      type: 'POST',
        success: function(result){
           console.log("result : " + result);
           targetLi.remove(); // Controller로 전송할 데이터 파일 삭제
         }
    }); //$.ajax
   });
~~~
FormDocumentController.java 일부
~~~java
// 게시판 작성 데이터와 함께 전송
@PostMapping("/cdims_form_document_write")
@PreAuthorize("isAuthenticated()")
public String register(BoardVO board, RedirectAttributes rttr) {

	log.info("form register : " + board);

	log.info("attach list : " + board.getAttachList());
	if (board.getAttachList() != null) {
		board.getAttachList().forEach(attach -> log.info("attach : " + attach));
	}

	formDocService.register(board);
	rttr.addFlashAttribute("result", board.getBno()); // list에서 등록 성공 모달창에 출력할 게시글 번호 전달

	return "redirect:/community/cdims_form_document";
}

@PostMapping("/cdims_form_document_delete")
@PreAuthorize("principal.username == #writer")
public String remove(@RequestParam("bno") Long bno, @ModelAttribute("cri") Criteria cri, RedirectAttributes rttr,
		String writer) {
	log.info("/cdims_form_document_remove");

	List<BoardAttachVO> attachList = formDocService.getAttachList(bno);
	
	//첨부 파일 삭제
	if (formDocService.remove(bno)) {
		deleteFiles(attachList); // delete Attach Files

		rttr.addAttribute("result", "success");
	}

	rttr.addAttribute("pageNum", cri.getPageNum());
	rttr.addAttribute("amount", cri.getAmount());
	rttr.addAttribute("keyword", cri.getKeyword());
	rttr.addAttribute("type", cri.getType());

	return "redirect:/community/cdims_form_document" + cri.getListLink();
}
	
// 첨부 파일 삭제 메소드
private void deleteFiles(List<BoardAttachVO> attachList) {
	if (attachList == null || attachList.size() == 0) {
		return;
	}

	log.info("delete attach files...");
	log.info(attachList);

	attachList.forEach(attach -> {
		try {
			// 첨부파일 경로
			Path file = Paths.get("/Users/parkheonjin/Desktop/upload/formDoc/" +
					attach.getUploadPath() + "/" + attach.getUuid() + "_" + attach.getFileName());
			Files.deleteIfExists(file);
			log.info("FILE PATH : " + file);

		} catch (Exception e) {
			log.error("delete file error : " + e.getMessage());
		} //end catch
	}); //end foreach
}
~~~
FormDocumentServiceImpl.java 일부
~~~java
@Override
public void register(BoardVO board) {
	log.info("service register : " + board);
	formDocMapper.insertSelectKey(board);

	if (board.getAttachList() == null || board.getAttachList().size() <= 0) {
		return;
	}
	// 첨부파일 데이터 저장
	board.getAttachList().forEach(attach -> {
		attach.setBno(board.getBno());
		fdAttachMapper.insert(attach);
	});
}


@Override
public boolean remove(Long bno) {
	log.info("service remove : " + bno);

	// 해당 게시물 모든 첨부파일 삭제
	fdAttachMapper.deleteAll(bno);

	return formDocMapper.delete(bno) == 1;
}


@Override
public List<BoardAttachVO> getAttachList(Long bno) {
	log.info("get Attach list by bno : " + bno);

	return fdAttachMapper.findByBno(bno); // 게시물의 첨부 파일 데이터 가져옴
}
~~~
    
#### 산출물
- 요구사항 정의서

image

- Flowchart

image

- ERD <br>
url
image
