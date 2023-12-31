package com.ebsoft.ebstudytemplates3week.domain.board.api;

import com.ebsoft.ebstudytemplates3week.domain.board.application.BoardService;
import com.ebsoft.ebstudytemplates3week.domain.board.dto.BoardDto;
import com.ebsoft.ebstudytemplates3week.domain.board.dto.request.BoardPasswordConfirmDto;
import com.ebsoft.ebstudytemplates3week.domain.board.dto.request.BoardUpdateDto;
import com.ebsoft.ebstudytemplates3week.domain.board.dto.request.BoardWriteDto;
import com.ebsoft.ebstudytemplates3week.domain.board.dto.request.SearchDto;
import com.ebsoft.ebstudytemplates3week.domain.board.dto.response.BoardListDto;
import com.ebsoft.ebstudytemplates3week.domain.category.application.CategoryService;
import com.ebsoft.ebstudytemplates3week.domain.comment.application.CommentService;
import com.ebsoft.ebstudytemplates3week.domain.comment.dto.CommentDto;
import com.ebsoft.ebstudytemplates3week.domain.file.application.FileService;
import com.ebsoft.ebstudytemplates3week.domain.file.convenience.FileStore;
import com.ebsoft.ebstudytemplates3week.domain.file.dto.FileDto;
import com.ebsoft.ebstudytemplates3week.global.paging.Pagination;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

@Controller
@RequestMapping("/board/free")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

  private final CategoryService categoryService;
  private final BoardService boardService;
  private final CommentService commentService;
  private final FileStore fileStore;
  private final FileService fileService;

  /*
  게시판을 작성할 때, 랜더링.
   */
  @GetMapping("/write")
  public String writeForm(Model model) {
    model.addAttribute("boardWriteDto", new BoardWriteDto()); //th:object 사용
    model.addAttribute("AllCategories", categoryService.getAllCategory());
    return "form/boardWriteForm";
  }

  /*
  작성 폼으로부터, 게시판을 작성한다.
   */
  @PostMapping("/write")
  public String writeBoard(@Valid @ModelAttribute BoardWriteDto reqDto,
      RedirectAttributes redirectAttributes) {
    reqDto.setCreatedTime(LocalDateTime.now()); // 현재 시간 넣기
    reqDto.setUpdatedTime(LocalDateTime.now()); // 현재 시간 넣기

    //todo id 자동 증분으로 인해, dto에 id가 없어, 마지막에 넣은 값을 얻어, 계산. (!!!addBoard를 할 때, 아이디 return?!!!)
    //todo 그 외 테스트 등에서도 다음과 같이 해야함.
    Long boardId = boardService.getLastWriteBoardId(); //이번에 사용될 id
    reqDto.setBoardId(boardId);

    // 비밀번호 확인이 틀린 경우.
    // todo 프론트단에서 이미 제약조건을 걸었는데,
    // todo 다른 방식으로 억지로 값을 넣은 대상에게, 친절하게 bindResult로 담아서 줄 필요가 있을까?
    if (!reqDto.getPassword().equals(reqDto.getPasswordConfirm())) {
      throw new IllegalArgumentException();
    }
    // log.info(reqDto.toString());
    boardService.addBoard(reqDto); // 게시물 추가

    // 작성된 게시글 상세 보기로 이동
    redirectAttributes.addAttribute("boardId", boardId);
    return "redirect:/board/free/view/{boardId}";
  }

  /*
  게시판 조회
   */
  @GetMapping("/view/{id}")
  public String viewBoard(@PathVariable("id") Long boardId, Model model) {
    BoardDto board = boardService.getBoardByIdViewPlus(boardId);
    log.info(board.toString());
    log.info("댓글 수 : " + String.valueOf(board.getComments().size()));
    model.addAttribute("board", board);
    return "form/boardForm";
  }

  /*
  게시판 목록
   */
  @GetMapping("/list")
  public String viewBoardList(Model model,
      @RequestParam(defaultValue = "1") int page,
      @ModelAttribute SearchDto searchDto) {

    log.info("검색어 : " + searchDto.toString());

    searchDto.setPagination(new Pagination(boardService.getTotalBoardCnt(searchDto), page));
    List<BoardListDto> boardList = boardService.getBoardList(searchDto);
    model.addAttribute("boardList", boardList); // 게시판들
    model.addAttribute("page", page); //현재 페이지
    model.addAttribute("pageVo", searchDto.pagination); // 페이지에 관한 정보
    model.addAttribute("AllCategories", categoryService.getAllCategory()); //카테고리들

    // 이전 검색 날짜 값 부여 및 default값 부여
    model.addAttribute("startDate",
        searchDto.startDate == null ?
            LocalDateTime.now().minusYears(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            : searchDto.startDate);

    model.addAttribute("endDate",
        searchDto.endDate == null ?
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            : searchDto.endDate);

    // 검색 조건 유지를 위한 이전 값 넣기
    model.addAttribute("prevCategory", searchDto.category); // 검색 카테고리
    model.addAttribute("prevContent", searchDto.searchContent); // 검색 내용

    // 페이징에서 이전 파라미터들 모두 받기
    model.addAttribute("prevSearchParam", searchDto.toUrlParm());
    return "form/boardListForm";
  }

  /*
  댓글 작성
  todo 배치: 게시판 로직이랑 분단 vs 같은 url을 사용하는 핸들러 바로 밑 배치
   */
  @PostMapping("/view/{id}")
  public String writeComment(@PathVariable("id") Long boardId,
      @Valid @ModelAttribute CommentDto reqDto) {
    reqDto.setBoardId(boardId); // 1:N 관계 , /view/{id} 의 id로 값을 받을 수 있음.
    reqDto.setCreatedTime(LocalDateTime.now());
    commentService.addComment(reqDto); // 댓글 추가
    return "redirect:/board/free/view/" + boardId;
  }

  /*
  게시물의 파일 다운로드
   */
  @GetMapping("/view/file/{fileId}")
  public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId)
      throws MalformedURLException {
    FileDto file = fileService.getFileDtoByFileId(fileId); //파일 아이디로 찾기
    log.info("파일 정보: " + file.toString());
    String fileName = file.getFileName();
    String storeName = file.getStoreName();
    UrlResource urlResource = new UrlResource("file:" + fileStore.getFullPath(storeName));
    String encode = UriUtils.encode(fileName, StandardCharsets.UTF_8);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + encode + "\"") // 다운로드 가능하게 만들기
        .body(urlResource);
  }

  /* AJAX
  비밀번호 확인 (업데이트)
   */
  @ResponseBody
  @GetMapping("/checkPwd")
  public boolean checkPasswordForUpdate(@ModelAttribute BoardPasswordConfirmDto reqDto,
      HttpServletRequest request) {
    boolean samePassword = boardService.isSamePassword(reqDto);
    log.info("수정을 위한 비밀번호 확인 성공 여부 : " + samePassword);

    // 세션 담기 + JSESSIONID 쿠키 주기
    if (samePassword) {
      HttpSession session = request.getSession(); // 없으니까 생성됨.
      session.setAttribute("passConfirm", reqDto); // response에 자동으로 쿠키 추가됨
    }

    return samePassword;
  }

  /*
  게시물 업데이트 랜더링
   */
  @GetMapping("/update/{boardId}")
  public String updateBoardForm(@PathVariable Long boardId, Model model) {
    log.info("업데이트 게시물 번호:" + boardId);
    BoardDto boardById = boardService.getBoardById(boardId);
    log.info(boardById.getFiles().toString());
    log.info("" + boardById.getFiles().size());
    model.addAttribute("board", boardById); // 디폴트값 주기
    return "form/boardUpdateForm";
  }

  /*
  게시물 업데이트 액션
  todo html에서 deleteFiles가 안넘어온다.
   */
  @PostMapping("/update/{boardId}")
  public String updateBoard(@PathVariable Long boardId,
      @Valid @ModelAttribute BoardUpdateDto reqDto) {
    reqDto.setUpdatedTime(LocalDateTime.now()); //업데이트 시간 변경
    // log.info(reqDto.toString()); todo toString으로 비밀번호 제거하고 log를 찍기 vs 값을 로고로 안찍기
    boardService.updateBoard(reqDto);
    return "redirect:/board/free/view/" + boardId;
  }

  /* AJAX
  비밀번호 확인 후 삭제
 */
  @ResponseBody
  @PostMapping("/checkPwd")
  public boolean checkPasswordForDelete(@ModelAttribute BoardPasswordConfirmDto reqDto) {
    boolean samePassword = boardService.isSamePassword(reqDto);
    log.info("삭제를 위한 비밀번호 확인 성공 여부 : " + samePassword);
    log.info(reqDto.toString());
    if (samePassword) {
      boardService.deleteBoard(reqDto.BoardId); //삭제
    }
    return samePassword;
  }
}
