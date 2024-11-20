# 메인화면
[게시판 바로가기](http://43.202.221.44:8080/articles)
![메인화면](https://github.com/ham904/Board/assets/141111846/dd93b4aa-4e84-431d-a9a0-6c945db85c8c)

# 게시판 서비스
## 개요
- JAVA Spring Boot를 이용한 게시판 서비스

#### 만든 목적
- JAVA Spring boot를 이용한 백엔드 프로세스 학습

#### 일정
- 24.04.05 ~ 24.05.15
- 참여도 : 100% (개인 프로젝트)

## 사용 기술 및 개발 환경
- O/S : macOS 10.15.1
- Server : Apache Tomcat 10.1.19
- DB : MySQL
- Framework/Flatform : Spring Boot, SpringSecurity
- Language : JAVA, Javascript, HTML5, CSS3
- Tool : IntelliJ, GitHub

## 내용
#### 구현 기능
- 구현

- 로그인<br/>

Spring Security dependency 주입

pom.xml 일부
~~~xml
        <!-- spring security -->
	<!-- https://mvnrepository.com/artifact/org.springframework.security/spring-security-web -->
	<dependency>
		<groupId>org.springframework.security</groupId>
		<artifactId>spring-security-web</artifactId>
		<version>5.0.6.RELEASE</version>
	</dependency>
	<!-- https://mvnrepository.com/artifact/org.springframework.security/spring-security-config -->
	<dependency>
		<groupId>org.springframework.security</groupId>
		<artifactId>spring-security-config</artifactId>
		<version>5.0.6.RELEASE</version>
	</dependency>

	<dependency>
		<groupId>org.springframework.security</groupId>
		<artifactId>spring-security-core</artifactId>
		<version>5.0.6.RELEASE</version>
	</dependency>

	<dependency>
		<groupId>org.springframework.security</groupId>
		<artifactId>spring-security-taglibs</artifactId>
		<version>5.0.6.RELEASE</version>
~~~
로그인,자동 로그인, 로그아웃 설정

security-context.xml 일부
~~~xml
	<bean id="customLoginSuccess" class="kr.co.sunmoon.security.CustomLoginSuccessHandler"></bean>	

	<security:form-login login-page="/cdims_login" authentication-success-handler-ref="customLoginSuccess"/>
	<security:logout logout-url="/cdims_logout" invalidate-session="true" delete-cookies="remember-me,JSESSION_ID" />
	<security:remember-me data-source-ref="dataSource" token-validity-seconds="604800" />
~~~
---
- 신청서 작성 (지원 신청서, 결과 보고서) <br/>
<br/><br/>
1. 지원신청서, 결과보고서 목록 REST 방식을 이용해 (년도, 학기, 과목, 분반) 데이터를 보내 결과 리스트를 json 형태로 데이터 가져옴 

cdims_result_report.jsp 일부
~~~js
$(document).on('click', '#searchBtn', function() {
			
	var yearValue = document.querySelector('#year').value;
	var semester = document.querySelector('#semester').value;
	var subjectName = document.querySelector('#subject_name').value;
	var division = document.querySelector('#division').value;

	console.log(yearValue + ", " + semester + ", " + subjectName + ", " + division);

	resultreportService.getList({year:yearValue, semester:semester, subjectname:subjectName, division:division}, 
		function(data) {

		console.log("data : " + JSON.stringify(data));
		console.log("data list : " + JSON.stringify(data.length));

		if (JSON.stringify(data.length) == 0) {
			alert("조회 결과가 없습니다.");
		} else {
			console.log("bno : " + data[0].bno + ", teamno : " + data[0].teamno);
		}

		var innerHTML = '';
		for (var i = 0, len = data.length || 0; i < len; i++) {
			innerHTML+='<tr onclick="listlink('+data[i].bno+', '+data[i].teamno+');" style="cursor:pointer;">';
			innerHTML+='<td>'+ data[i].projectTitle+'</td>';
			innerHTML+='<td>'+data[i].teamName+'</td>';
			innerHTML+='<td>'+data[i].division+'</td>';
			innerHTML+='<td>'+data[i].professorCharge+'</td>';
			innerHTML+='<td>'+data[i].approval+'</td>';
			innerHTML+='</tr>';
		}

		$('tbody').html(innerHTML);
	}); //end function
});
~~~ 

result-report.js 일부
~~~js
var resultreportService = (function() {

function getList(param, callback, error) {
	var year = param.year;
	var semester = param.semester;
	var subjectName = param.subjectname;
	var division = param.division;

	$.getJSON("/result_report/list/" + year + "/" + semester + "/" + subjectName + "/" + division + ".json",
		function(data) {
			if (callback) {
				callback(data); //댓글 숫자와 목록을 가져오는 경우
			}
		}).fail(function(xhr, status, err) {
			if (error) {	
				error();
			}
	});
}
~~~ 

ResultReportRestController.java 일부
~~~java
@GetMapping(value = "/list/{year}/{semester}/{subjectname}/{division}",
		produces = {MediaType.APPLICATION_XML_VALUE,
				MediaType.APPLICATION_JSON_UTF8_VALUE})
public ResponseEntity<List<ResultReportVO>> getList(@PathVariable("year") String year, @PathVariable("semester") String semester,
		@PathVariable("subjectname") String subjectname, @PathVariable("division") String division) {

	log.info("getList year : " + year + ", " + "semester : " + semester + "," + "subject : " + subjectname + "," + "division: " + division);

	return new ResponseEntity<>(resultReportService.getList(year, semester, subjectname, division), HttpStatus.OK);
}
~~~ 

ResultReportServiceImpl.java 일부
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

![캡디통합관리_요구사항정의서](https://user-images.githubusercontent.com/68316076/87619085-4b243380-c756-11ea-91e3-6b97bf450be0.PNG)

- Flowchart

<img width="982" alt="스크린샷 2020-07-16 오전 9 58 31" src="https://user-images.githubusercontent.com/68316076/87618863-d3560900-c755-11ea-8b80-78c1190ba0d9.png">

- ERD <br>
https://www.erdcloud.com/d/yTLGLXQKajRkhoXqy

<img width="1299" alt="스크린샷 2020-07-16 오전 10 02 26" src="https://user-images.githubusercontent.com/68316076/87618878-de109e00-c755-11ea-9a50-be1294e5a447.png">
