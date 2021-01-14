package ir.ssatari.fileservice.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import ir.ssatari.fileservice.constant.ExceptionCodes;
import ir.ssatari.fileservice.controller.response.UploadFileResponse;
import ir.ssatari.fileservice.service.FileStorageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@Api(value = "File Controller", description = "Operations pertaining to file service")
@AllArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/uploadFile")
    @ApiOperation(value = "Upload single file", response = UploadFileResponse.class)
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        UploadFileResponse response = new UploadFileResponse();
        String fileName = fileStorageService.storeFile(file);
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/downloadFile/")
                .path(fileName)
                .toUriString();
        response.setFileName(fileName);
        response.setFileDownloadUri(fileDownloadUri);
        response.setFileType(file.getContentType());
        response.setSize(file.getSize());
        return response;
    }

    @PostMapping("/uploadMultipleFiles")
    @ApiOperation(value = "Upload list of files")
    public List<UploadFileResponse> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        return Arrays.stream(files)
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }

    @GetMapping("/downloadFile/{fileName:.+}")
    @ApiOperation(value = "Download file using given file name")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName, HttpServletRequest request) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info(ExceptionCodes.FILE_CNT_COULD_NOT_DETERMINE_FILE_TYPE);
        }
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}