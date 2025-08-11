package vn.hoidanit.jobhunter.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.hoidanit.jobhunter.domain.entity.ChatMessage;
import vn.hoidanit.jobhunter.domain.entity.ChatRoom;
import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.entity.OnlineResume;
import vn.hoidanit.jobhunter.domain.entity.Role;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.entity.UserBulkCreateDTO;
import vn.hoidanit.jobhunter.domain.response.ResBulkCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResCreateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUpdateUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResUserDetailDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.chat.ResLastMessageDTO;
import vn.hoidanit.jobhunter.domain.response.file.ResUploadFileDTO;
import vn.hoidanit.jobhunter.repository.ChatMessageRepository;
import vn.hoidanit.jobhunter.repository.ChatRoomRepository;
import vn.hoidanit.jobhunter.repository.CommentRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;
import vn.hoidanit.jobhunter.util.constant.UserStatusEnum;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import vn.hoidanit.jobhunter.util.error.StorageException;

@Service
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private CompanyService companyService;
    private RoleService roleService;
    private final RoleRepository roleRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final FileService fileService;
    private final OnlineResumeService onlineResumeService;
    private final CommentRepository commentRepository;
    private final ChatMessageRepository chatMessageRepository;

    public UserService(UserRepository userRepository, CompanyService companyService,
            RoleService roleService, PasswordEncoder passwordEncoder, RoleRepository roleRepository,
            ChatRoomRepository chatRoomRepository, FileService fileService, OnlineResumeService onlineResumeService,
            ChatMessageRepository chatMessageRepository, CommentRepository commentRepository) {
        this.userRepository = userRepository;
        this.companyService = companyService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.fileService = fileService;
        this.onlineResumeService = onlineResumeService;
        this.chatMessageRepository = chatMessageRepository;
        this.commentRepository = commentRepository;
    }

    @Value("${hoidanit.upload-file.base-uri}")
    private String baseURI;

    @Transactional
    public ResUploadFileDTO uploadMainResume(MultipartFile file)
            throws IdInvalidException, StorageException, IOException, URISyntaxException {
        // Lấy thông tin người dùng hiện tại
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng"));

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        // Kiểm tra file
        if (file == null || file.isEmpty()) {
            throw new StorageException("File CV trống. Vui lòng chọn một file.");
        }

        String fileName = file.getOriginalFilename();
        List<String> allowedExtensions = Arrays.asList("pdf", "doc", "docx", "jpg", "jpeg", "png");
        boolean isValid = allowedExtensions.stream().anyMatch(item -> fileName.toLowerCase().endsWith(item));
        if (!isValid) {
            throw new StorageException("Định dạng file không hợp lệ. Chỉ hỗ trợ: " + allowedExtensions.toString());
        }

        // Tạo thư mục resumes nếu chưa tồn tại
        String folder = "resumes";
        fileService.createDirectory(baseURI + folder);

        // Upload file
        String uploadedFileName = fileService.store(file, folder);

        // Cập nhật main_resume cho người dùng
        user.setMainResume(uploadedFileName);
        userRepository.save(user);

        // Tạo response
        return new ResUploadFileDTO(uploadedFileName, Instant.now());
    }

    public User handleCreateUser(User user) {
        // company
        if (user.getCompany() != null) {
            Optional<Company> companyOptional = this.companyService.findById(user.getCompany().getId());
            user.setCompany(companyOptional.orElse(null));
        }

        // role: DÙNG REPO LẤY ENTITY, KHÔNG GỌI roleService (vì trả DTO)
        if (user.getRole() != null && user.getRole().getId() > 0) {
            // nếu chỉ gán quan hệ, dùng reference để tránh query thừa
            Role roleRef = this.roleRepository.getReferenceById(user.getRole().getId());
            user.setRole(roleRef);
        } else {
            user.setRole(null);
        }

        return this.userRepository.save(user);
    }

    @Transactional // Bắt buộc phải có để đảm bảo toàn vẹn giao dịch
    public void handleDeleteUser(long id) {
        // Bước 1: Xóa các bản ghi con phức tạp (không thể cascade)
        // Xóa tất cả tin nhắn mà user này là người gửi hoặc người nhận
        this.commentRepository.deleteByUserId(id);

        this.chatMessageRepository.deleteBySenderIdOrReceiverId(id, id);

        // Xóa tất cả các phòng chat mà user này tham gia
        this.chatRoomRepository.deleteBySenderIdOrReceiverId(id, id);

        // Lưu ý: Bạn không cần gọi resumeRepository.delete... ở đây nữa
        // vì CascadeType.ALL đã xử lý việc đó.

        // Bước 2: Xóa người dùng sau khi tất cả các tham chiếu đã bị xóa
        this.userRepository.deleteById(id);
    }

    public User fetchUserById(long id) {
        return this.userRepository.findWithRoleCompanyAndOnlineResumeById(id).orElse(null);
    }

    public ResultPaginationDTO fetchAllUser(Specification<User> spec, Pageable pageable) {
        Page<User> pageUser = this.userRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());
        //
        rs.setMeta(mt);

        // remove sensitive data
        List<ResUserDTO> listUser = pageUser.getContent()
                .stream().map(item -> this.convertToResUserDTO(item))
                .collect(Collectors.toList());

        rs.setResult(listUser);

        return rs;
    }

    @Transactional
    public User handleUpdateUser(User reqUser) {
        User currentUser = this.fetchUserById(reqUser.getId());
        if (currentUser != null) {
            currentUser.setAddress(reqUser.getAddress());
            currentUser.setGender(reqUser.getGender());
            currentUser.setAge(reqUser.getAge());
            currentUser.setName(reqUser.getName());
            currentUser.setVip(reqUser.isVip());

            // company
            if (reqUser.getCompany() != null && reqUser.getCompany().getId() > 0) {
                Optional<Company> companyOptional = this.companyService.findById(reqUser.getCompany().getId());
                currentUser.setCompany(companyOptional.orElse(null));
            } else {
                currentUser.setCompany(null);
            }

            // role: DÙNG REPO LẤY ENTITY
            if (reqUser.getRole() != null && reqUser.getRole().getId() > 0) {
                Role roleRef = this.roleRepository.getReferenceById(reqUser.getRole().getId());
                currentUser.setRole(roleRef);
            } else {
                currentUser.setRole(null);
            }

            currentUser = this.userRepository.save(currentUser);
        }
        return currentUser;
    }

    // ---------- START: PHƯƠNG THỨC MỚI ĐƯỢC THÊM VÀO ----------
    /**
     * Xử lý yêu cầu tự cập nhật thông tin của người dùng.
     * Phương thức này kiểm tra xem người dùng đang đăng nhập có cố gắng cập nhật
     * thông tin của chính họ hay không.
     *
     * @param reqUser Đối tượng User chứa thông tin cập nhật từ request.
     * @return Đối tượng User đã được cập nhật.
     * @throws IdInvalidException nếu người dùng không tồn tại hoặc không có quyền.
     */
    public User handleUpdateOwnUser(User reqUser) throws IdInvalidException {
        // Bước 1: Lấy email của người dùng đang đăng nhập từ Security Context.
        Optional<String> currentUserLoginOptional = SecurityUtil.getCurrentUserLogin();
        if (currentUserLoginOptional.isEmpty()) {
            throw new IdInvalidException("Không thể lấy thông tin người dùng đang đăng nhập.");
        }
        String currentUserEmail = currentUserLoginOptional.get();

        // Bước 2: Tìm người dùng trong cơ sở dữ liệu bằng email.
        User loggedInUser = this.userRepository.findByEmail(currentUserEmail);
        if (loggedInUser == null) {
            throw new IdInvalidException("Người dùng với email " + currentUserEmail + " không tồn tại.");
        }

        // Bước 3: **Kiểm tra quyền hạn quan trọng nhất**
        // So sánh ID của người dùng đang đăng nhập với ID được gửi trong request body.
        if (loggedInUser.getId() != reqUser.getId()) {
            throw new IdInvalidException("Bạn không có quyền cập nhật thông tin của người dùng khác.");
        }

        // Bước 4: Nếu quyền hạn hợp lệ, tiến hành cập nhật thông tin.
        // Lấy lại user từ DB để đảm bảo đang làm việc với đối tượng được quản lý bởi
        // JPA.
        User userToUpdate = this.fetchUserById(reqUser.getId());
        if (userToUpdate == null) {
            throw new IdInvalidException("User với id = " + reqUser.getId() + " không tồn tại");
        }

        // Chỉ cập nhật các trường thông tin cá nhân cơ bản.
        // Không cho phép người dùng tự cập nhật Role hoặc Company qua API này.
        userToUpdate.setName(reqUser.getName());
        userToUpdate.setAge(reqUser.getAge());
        userToUpdate.setGender(reqUser.getGender());
        userToUpdate.setAddress(reqUser.getAddress());
        userToUpdate.setAvatar(reqUser.getAvatar());

        // Bước 5: Lưu và trả về người dùng đã được cập nhật.
        return this.userRepository.save(userToUpdate);
    }
    // ---------- END: PHƯƠNG THỨC MỚI ĐƯỢC THÊM VÀO ----------

    public User handleGetUserByUsername(String username) {
        return this.userRepository.findByEmail(username);
    }

    @Cacheable(cacheNames = "user-permissions-v1", key = "#a0", unless = "#result == null")
    @Transactional(readOnly = true)
    public List<String> getPermissionKeysByEmail(String email) {
        var userOpt = this.userRepository.findOneWithRoleAndPermissionsByEmail(email);
        if (userOpt.isEmpty())
            return List.of();

        var role = userOpt.get().getRole();
        if (role == null || role.getPermissions() == null)
            return List.of();

        return role.getPermissions().stream()
                .map(p -> (p.getMethod() + ":" + p.getApiPath()).toUpperCase())
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean isEmailExist(String email) {
        return this.userRepository.existsByEmail(email);
    }

    public ResCreateUserDTO convertToResCreateUserDTO(User user) {
        ResCreateUserDTO res = new ResCreateUserDTO();
        ResCreateUserDTO.CompanyUser com = new ResCreateUserDTO.CompanyUser();

        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        res.setAvatar(user.getAvatar());
        res.setVip(user.isVip());
        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }

        return res;
    }

    public ResUpdateUserDTO convertToResUpdateUserDTO(User user) {
        ResUpdateUserDTO res = new ResUpdateUserDTO();
        ResUpdateUserDTO.CompanyUser com = new ResUpdateUserDTO.CompanyUser();
        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }
        res.setId(user.getId());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        res.setAvatar(user.getAvatar());
        res.setVip(user.isVip());

        return res;
    }

    public ResUserDTO convertToResUserDTO(User user) {
        ResUserDTO res = new ResUserDTO();
        ResUserDTO.CompanyUser com = new ResUserDTO.CompanyUser();
        ResUserDTO.RoleUser roleUser = new ResUserDTO.RoleUser();

        if (user.getCompany() != null) {
            com.setId(user.getCompany().getId());
            com.setName(user.getCompany().getName());
            res.setCompany(com);
        }

        if (user.getRole() != null) {
            roleUser.setId(user.getRole().getId());
            roleUser.setName(user.getRole().getName());
            res.setRole(roleUser);
        }

        res.setId(user.getId());
        res.setEmail(user.getEmail());
        res.setName(user.getName());
        res.setAge(user.getAge());
        res.setUpdatedAt(user.getUpdatedAt());
        res.setCreatedAt(user.getCreatedAt());
        res.setGender(user.getGender());
        res.setAddress(user.getAddress());
        res.setAvatar(user.getAvatar());
        res.setStatus(user.getStatus());
        res.setVip(user.isVip());

        return res;
    }

    public void updateUserToken(String token, String email) {
        User currentUser = this.handleGetUserByUsername(email);
        if (currentUser != null) {
            currentUser.setRefreshToken(token);
            this.userRepository.save(currentUser);
        }
    }

    public User getUserByRefreshTokenByEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    public User getUserByRefreshTokenAndEmail(String token, String email) {
        return this.userRepository.findByRefreshTokenAndEmail(token, email);
    }

    @Transactional
    public ResBulkCreateUserDTO handleBulkCreateUsers(List<UserBulkCreateDTO> userDTOs) {
        int total = userDTOs.size();
        int success = 0;
        List<String> failedEmails = new ArrayList<>();

        for (UserBulkCreateDTO dto : userDTOs) {
            try {
                // Kiểm tra email trùng
                if (this.isEmailExist(dto.getEmail())) {
                    failedEmails.add(dto.getEmail() + " (Email tồn tại)");
                    continue;
                }

                User user = new User();
                user.setName(dto.getName());
                user.setEmail(dto.getEmail());
                user.setPassword(this.passwordEncoder.encode(dto.getPassword()));
                user.setGender(dto.getGender());
                user.setAddress(dto.getAddress());
                user.setAge(dto.getAge());

                // Kiểm tra và gán role
                Optional<Role> roleOptional = this.roleRepository.findById(dto.getRole().getId());
                if (roleOptional.isEmpty()) {
                    failedEmails.add(dto.getEmail() + " (Role ID không tồn tại)");
                    continue;
                }
                user.setRole(roleOptional.get());

                // Lưu user
                this.userRepository.save(user);
                success++;
            } catch (Exception e) {
                failedEmails.add(dto.getEmail() + " (Lỗi hệ thống: " + e.getMessage() + ")");
            }
        }

        return new ResBulkCreateUserDTO(total, success, total - success, failedEmails);
    }

    @Value("${hoidanit.vip.duration-minutes:1}") // Mặc định 1 phút nếu không cấu hình
    private long vipDurationMinutes;

    public void activateVip(User user) {
        user.setVip(true);
        user.setVipExpiryDate(LocalDateTime.now().plusMinutes(vipDurationMinutes));
        user.setCvSubmissionCount(0);
        userRepository.save(user);
    }

    public boolean canSubmitCv(String email) {
        User user = handleGetUserByUsername(email);
        if (user == null) {
            return false;
        }

        if (user.isVip() && user.getVipExpiryDate() != null && user.getVipExpiryDate().isBefore(LocalDateTime.now())) {
            user.setVip(false);
            user.setCvSubmissionCount(0);
            userRepository.save(user);
        }

        int maxSubmissions = user.isVip() ? 30 : 10;
        return user.getCvSubmissionCount() < maxSubmissions;
    }

    public void incrementCvSubmission(String email) {
        User user = handleGetUserByUsername(email);
        if (user != null) {
            user.setCvSubmissionCount(user.getCvSubmissionCount() + 1);
            userRepository.save(user);
        }
    }

    @Value("${hoidanit.vip.check-cron:0 0 0 1 * ?}") // Mặc định hàng tháng nếu không cấu hình
    private String vipCheckCron;

    @Scheduled(cron = "${hoidanit.vip.check-cron}")
    public void resetCvSubmissionCount() {
        List<User> users = userRepository.findAll();
        users.forEach(user -> {
            user.setCvSubmissionCount(0);
            if (user.isVip() && user.getVipExpiryDate() != null
                    && user.getVipExpiryDate().isBefore(LocalDateTime.now())) {
                user.setVip(false);
            }
            userRepository.save(user);
        });
    }

    public List<ResUserDTO> findConnectedUsers(Long userId) {
        // B1: Tìm tất cả các phòng chat và fetch sẵn user chỉ với MỘT query (Đã tối ưu)
        List<ChatRoom> chatRooms = this.chatRoomRepository.findBySenderOrReceiverIdWithUsers(userId);

        // B2: Từ các phòng chat, trích xuất ID của người dùng còn lại (partner).
        Set<Long> partnerIds = chatRooms.stream()
                .map(chatRoom -> chatRoom.getSender().getId() == userId
                        ? chatRoom.getReceiver().getId()
                        : chatRoom.getSender().getId())
                .collect(Collectors.toSet());

        if (partnerIds.isEmpty()) {
            return Collections.emptyList();
        }

        // B3: Dựa vào danh sách ID partners, truy vấn TẤT CẢ tin nhắn cuối cùng chỉ với
        // MỘT query (Đã tối ưu)
        List<ChatMessage> lastMessages = this.chatMessageRepository.findLastMessageForEachConversation(userId,
                new ArrayList<>(partnerIds));

        // B4: Chuyển danh sách tin nhắn cuối thành Map để tra cứu nhanh O(1)
        // == SỬA LỖI Ở ĐÂY ==
        // Tách logic lấy key ra một biến Function để giúp trình biên dịch suy luận kiểu
        Function<ChatMessage, Long> keyMapper = msg -> msg.getSender().getId() == userId
                ? msg.getReceiver().getId()
                : msg.getSender().getId();

        Map<Long, ChatMessage> lastMessageMap = lastMessages.stream()
                .collect(Collectors.toMap(keyMapper, Function.identity()));
        // == KẾT THÚC SỬA LỖI ==

        // B5: Lấy thông tin User của các partner (ĐÃ TỐI ƯU)
        // == SỬA Ở ĐÂY ==
        List<User> connectedUsers = this.userRepository.findByIdInWithCompanyAndRole(new ArrayList<>(partnerIds));

        // B6: Chuyển đổi User entity sang ResUserDTO và gán tin nhắn cuối từ Map (KHÔNG
        // CÒN QUERY DB)
        return connectedUsers.stream()
                .map(user -> {
                    ResUserDTO dto = this.convertToResUserDTO(user);

                    ChatMessage lastMsgEntity = lastMessageMap.get(user.getId());

                    if (lastMsgEntity != null) {
                        ResLastMessageDTO lastMsgDto = new ResLastMessageDTO(
                                lastMsgEntity.getContent(),
                                lastMsgEntity.getSender().getId(),
                                lastMsgEntity.getTimeStamp().toInstant());
                        dto.setLastMessage(lastMsgDto);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public void updateStatus(User userPayload) {
        // B1: Tìm user trong DB bằng ID từ payload
        Optional<User> userOptional = this.userRepository.findById(userPayload.getId());

        // B2: Nếu tìm thấy, cập nhật status và lưu lại
        if (userOptional.isPresent()) {
            User userInDB = userOptional.get();
            userInDB.setStatus(UserStatusEnum.ONLINE); // Gán trạng thái ONLINE
            this.userRepository.save(userInDB); // Lưu thay đổi vào DB
        }
    }

    /**
     * Cập nhật trạng thái người dùng thành OFFLINE và lưu vào DB.
     *
     * @param userPayload Dữ liệu người dùng gửi từ client.
     */
    public void disconnect(User userPayload) {
        // B1: Tìm user trong DB bằng ID từ payload
        Optional<User> userOptional = this.userRepository.findById(userPayload.getId());

        // B2: Nếu tìm thấy, cập nhật status và lưu lại
        if (userOptional.isPresent()) {
            User userInDB = userOptional.get();
            userInDB.setStatus(UserStatusEnum.OFFLINE); // Gán trạng thái OFFLINE
            this.userRepository.save(userInDB); // Lưu thay đổi vào DB
        }
    }

    public List<User> findAllOnlineUsers() {
        return this.userRepository.findByStatus(UserStatusEnum.ONLINE);
    }

    public User findUserById(Long id) {
        return this.userRepository.findById(id).get();
    }

    @Transactional(readOnly = true)
    public ResUserDetailDTO fetchUserDetailById(long id) throws IdInvalidException {
        // SỬA LẠI LỜI GỌI REPOSITORY
        // Dùng phương thức mới sử dụng Entity Graph
        User user = this.userRepository.findOneById(id)
                .orElseThrow(() -> new IdInvalidException("User với id = " + id + " không tồn tại"));

        // Khi dòng lệnh ResUserDetailDTO.convertToDTO(user) bên dưới được thực thi,
        // nó sẽ gọi đến user.getWorkExperiences().
        // Do phương thức này nằm trong một @Transactional, Hibernate sẽ tự động
        // thực hiện một câu query thứ 2 để lấy danh sách workExperiences một cách an
        // toàn.

        // Tất cả dữ liệu bây giờ đã được tải đầy đủ mà không bị lỗi
        // MultipleBagFetchException.
        return ResUserDetailDTO.convertToDTO(user);
    }

    @Transactional(readOnly = true)
    public ResultPaginationDTO fetchAllUserDetails(Specification<User> spec, Pageable pageable) {
        // Cập nhật lời gọi repository để bao gồm cả specification
        Page<User> pageUser = this.userRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        // Thiết lập thông tin meta cho phân trang
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());
        rs.setMeta(mt);

        // Chuyển đổi danh sách User sang danh sách ResUserDetailDTO
        List<ResUserDetailDTO> listUserDetails = pageUser.getContent().stream()
                .map(ResUserDetailDTO::convertToDTO)
                .collect(Collectors.toList());

        rs.setResult(listUserDetails);

        return rs;
    }

    @Transactional
    public void updateUserIsPublic(boolean isPublic) throws IdInvalidException {
        // Lấy email của người dùng đang đăng nhập từ security context
        Optional<String> currentUserLoginOptional = SecurityUtil.getCurrentUserLogin();
        if (currentUserLoginOptional.isEmpty()) {
            throw new IdInvalidException("Không tìm thấy thông tin người dùng đang đăng nhập.");
        }

        String email = currentUserLoginOptional.get();

        // Tìm user trong database bằng email
        User currentUser = this.userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IdInvalidException("Người dùng với email " + email + " không tồn tại.");
        }

        // Cập nhật trạng thái isPublic
        currentUser.setPublic(isPublic);

        // Lưu lại vào database
        this.userRepository.save(currentUser);
    }

    public User fetchUserByEmail(String email) {
        // 1. Gọi phương thức repository như cũ, không thay đổi
        User user = this.userRepository.findByEmail(email);

        // 2. Bọc kết quả (có thể là null) vào trong Optional trước khi trả về
        return user;
    }
}
