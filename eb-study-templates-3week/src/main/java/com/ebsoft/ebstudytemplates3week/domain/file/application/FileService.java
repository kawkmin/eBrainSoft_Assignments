package com.ebsoft.ebstudytemplates3week.domain.file.application;

import com.ebsoft.ebstudytemplates3week.domain.file.convenience.FileStore;
import com.ebsoft.ebstudytemplates3week.domain.file.dao.FileRepository;
import com.ebsoft.ebstudytemplates3week.domain.file.dto.FileDto;
import com.ebsoft.ebstudytemplates3week.domain.file.dto.request.FileWriteDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FileService {

  private final FileRepository fileRepository;
  private final FileStore fileStore;

  /*
  파일 추가
   */
  @Transactional
  public void addFile(List<MultipartFile> files, Long boardId) {
    try {
      List<FileDto> fileDtos = fileStore.storeFiles(files);
      List<FileWriteDto> fileWriteDtos = new ArrayList<>();
      for (FileDto fileDto : fileDtos) {
        fileWriteDtos.add(new FileWriteDto(boardId, fileDto));
        log.info(fileDto.toString());
      }
      fileRepository.saveFiles(fileWriteDtos);
    } catch (IOException e) {
      log.info("파일을 추가하는데 오류가 발생했습니다.");
      throw new IllegalArgumentException();
    }
  }

  /*
  파일 조회
   */
  public FileDto getFileDtoByFileId(Long fileId) {
    return fileRepository.findFileById(fileId);
  }

  @Transactional
  public void deleteFiles(List<Long> deleteFiles) {
    fileRepository.deleteFilesById(deleteFiles);
  }
}
