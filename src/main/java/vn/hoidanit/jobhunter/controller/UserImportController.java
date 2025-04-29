package vn.hoidanit.jobhunter.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import vn.hoidanit.jobhunter.domain.UserImportDTO;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.helper.CsvHelper;
import vn.hoidanit.jobhunter.util.helper.ExcelHelper;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/v1")
public class UserImportController {

    private final UserService userService;

    public UserImportController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/users/bulk-create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importUsers(@RequestPart("file") MultipartFile file) throws Exception {
        List<UserImportDTO> users;
        String contentType = file.getContentType();

        if (contentType.equals("text/csv") || file.getOriginalFilename().endsWith(".csv")) {
            users = CsvHelper.parseCsv(file.getInputStream());
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
            users = ExcelHelper.parseExcel(file.getInputStream());
        } else {
            return ResponseEntity.badRequest().body("Unsupported file format");
        }

        userService.bulkCreate(users);
        return ResponseEntity.ok("Imported successfully");
    }
}
