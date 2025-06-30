package vn.hoidanit.jobhunter.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.entity.UserBulkCreateDTO;
import vn.hoidanit.jobhunter.domain.request.ReqUpdateIsPublicDTO;
import vn.hoidanit.jobhunter.domain.response.ResBulkCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUpdateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDetailDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.file.ResUploadFileDTO;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import vn.hoidanit.jobhunter.util.error.StorageException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/users")
    @ApiMessage("Create a new user")
    public ResponseEntity<ResCreateUserDTO> createNewUser(@Valid @RequestBody User postManUser)
            throws IdInvalidException {
        boolean isEmailExist = this.userService.isEmailExist(postManUser.getEmail());
        if (isEmailExist) {
            throw new IdInvalidException(
                    "Email " + postManUser.getEmail() + "đã tồn tại, vui lòng sử dụng email khác.");
        }

        String hashPassword = this.passwordEncoder.encode(postManUser.getPassword());
        postManUser.setPassword(hashPassword);
        User ericUser = this.userService.handleCreateUser(postManUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(this.userService.convertToResCreateUserDTO(ericUser));
    }

    @DeleteMapping("/users/{id}")
    @ApiMessage("Delete a user")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") long id)
            throws IdInvalidException {
        User currentUser = this.userService.fetchUserById(id);
        if (currentUser == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }

        this.userService.handleDeleteUser(id);
        return ResponseEntity.ok(null);
    }

    // fetch user by id
    @GetMapping("/users/{id}")
    @ApiMessage("fetch user by id")
    public ResponseEntity<ResUserDTO> getUserById(@PathVariable("id") long id) throws IdInvalidException {
        User fetchUser = this.userService.fetchUserById(id);
        if (fetchUser == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(this.userService.convertToResUserDTO(fetchUser));
    }

    // fet all user
    @GetMapping("/users")
    @ApiMessage("fetch all user")
    public ResponseEntity<ResultPaginationDTO> getAllUser(
            @Filter Specification<User> spec,
            Pageable pageable) {

        return ResponseEntity.status(HttpStatus.OK).body(
                this.userService.fetchAllUser(spec, pageable));

    }

    @PutMapping("/users")
    @ApiMessage("Update a user")
    public ResponseEntity<ResUpdateUserDTO> updateUser(@RequestBody User user) throws IdInvalidException {
        User nuUser = this.userService.handleUpdateUser(user);
        if (nuUser == null) {
            throw new IdInvalidException("User với id = " + user.getId() + " không tồn tại");
        }
        return ResponseEntity.ok(this.userService.convertToResUpdateUserDTO(nuUser));
    }

    @PostMapping("/users/bulk-create")
    @ApiMessage("Create multiple users")
    public ResponseEntity<ResBulkCreateUserDTO> bulkCreateUsers(@Valid @RequestBody List<UserBulkCreateDTO> userDTOs) {
        ResBulkCreateUserDTO result = this.userService.handleBulkCreateUsers(userDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/users/main-resume")
    @ApiMessage("Upload main resume for user")
    public ResponseEntity<ResUploadFileDTO> uploadMainResume(
            @RequestParam(name = "file", required = false) MultipartFile file)
            throws URISyntaxException, IOException, StorageException, IdInvalidException {
        return ResponseEntity.ok().body(userService.uploadMainResume(file));
    }

    @GetMapping("/users/detail/{id}")
    @ApiMessage("Lấy chi tiết thông tin user theo ID")
    public ResponseEntity<ResUserDetailDTO> getUserDetailById(@PathVariable("id") long id) throws IdInvalidException {
        ResUserDetailDTO userDetail = this.userService.fetchUserDetailById(id);
        return ResponseEntity.ok(userDetail);
    }

    /**
     * ENDPOINT MỚI: Lấy danh sách chi tiết người dùng với phân trang.
     * 
     * @param pageable Spring sẽ tự động tạo đối tượng này từ các tham số ?page= &
     *                 ?size=
     * @return ResponseEntity chứa đối tượng phân trang với danh sách chi tiết người
     *         dùng
     */
    @GetMapping("/users/detail")
    @ApiMessage("Lấy danh sách chi tiết người dùng với phân trang và bộ lọc")
    public ResponseEntity<ResultPaginationDTO> getAllUserDetails(
            @Filter Specification<User> spec,
            Pageable pageable) {
        return ResponseEntity.ok(this.userService.fetchAllUserDetails(spec, pageable));
    }

    @PutMapping("/users/is-public")
    @ApiMessage("Update your public profile status")
    public ResponseEntity<Void> updateIsPublicStatus(
            @RequestBody ReqUpdateIsPublicDTO dto) throws IdInvalidException {

        this.userService.updateUserIsPublic(dto.isPublic());
        return ResponseEntity.ok().build();
    }

}
