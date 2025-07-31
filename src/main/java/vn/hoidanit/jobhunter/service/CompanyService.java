package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.turkraft.springfilter.boot.Filter;

import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.entity.Role;
import vn.hoidanit.jobhunter.domain.entity.User;
import vn.hoidanit.jobhunter.domain.request.ReqCreateCompanyDTO;
import vn.hoidanit.jobhunter.domain.request.ReqUpdateCompanyDTO;
import vn.hoidanit.jobhunter.domain.response.ResCreateCompanyDTO;
import vn.hoidanit.jobhunter.domain.response.ResFetchCompanyDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JobRepository jobRepository; // Inject JobRepository

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository,
            RoleRepository roleRepository,
            JobRepository jobRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jobRepository = jobRepository;
    }

    private ResFetchCompanyDTO convertToResFetchCompanyDTO(Company company) {
        ResFetchCompanyDTO dto = new ResFetchCompanyDTO();
        dto.setId(company.getId());
        dto.setName(company.getName());
        dto.setDescription(company.getDescription());
        dto.setAddress(company.getAddress());
        dto.setLogo(company.getLogo());
        dto.setField(company.getField());
        dto.setWebsite(company.getWebsite());
        dto.setScale(company.getScale());
        dto.setCountry(company.getCountry());
        dto.setFoundingYear(company.getFoundingYear());
        dto.setLocation(company.getLocation());
        dto.setCreatedAt(company.getCreatedAt());
        dto.setUpdatedAt(company.getUpdatedAt());

        return dto;
    }

    // API FOR ADMIN
    @Transactional
    public Company handleCreateCompany(Company c) {
        return this.companyRepository.save(c);
    }

    // API FOR ADMIN
    @Transactional
    public Company handleUpdateCompany(Company c) {
        Optional<Company> companyOptional = this.companyRepository.findById(c.getId());
        if (companyOptional.isPresent()) {
            Company currentCompany = companyOptional.get();
            currentCompany.setName(c.getName());
            currentCompany.setDescription(c.getDescription());
            currentCompany.setAddress(c.getAddress());
            currentCompany.setLogo(c.getLogo());

            // CẬP NHẬT: Thêm các trường mới cho API của admin
            currentCompany.setField(c.getField());
            currentCompany.setWebsite(c.getWebsite());
            currentCompany.setScale(c.getScale());
            currentCompany.setCountry(c.getCountry());
            currentCompany.setFoundingYear(c.getFoundingYear());
            currentCompany.setLocation(c.getLocation());

            return this.companyRepository.save(currentCompany);
        }
        return null;
    }

    // API FOR ADMIN
    @Transactional
    public void handleDeleteCompany(long id) {
        Optional<Company> comOptional = this.companyRepository.findById(id);
        if (comOptional.isPresent()) {
            Company com = comOptional.get();
            // Lấy tất cả user thuộc công ty này và xóa họ
            // Lưu ý: Logic này có thể cần xem xét lại tùy vào yêu cầu nghiệp vụ
            List<User> users = this.userRepository.findByCompany(com);
            this.userRepository.deleteAll(users);
        }

        this.companyRepository.deleteById(id);
    }

    public ResultPaginationDTO handleGetCompany(Specification<Company> spec, Pageable pageable) {
        // BƯỚC 1: Lấy danh sách Company theo trang (Query #1 - Hiệu quả)
        Page<Company> pCompany = this.companyRepository.findAll(spec, pageable);
        List<Company> companiesOnPage = pCompany.getContent();

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();
        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pCompany.getTotalPages());
        mt.setTotal(pCompany.getTotalElements());
        rs.setMeta(mt);

        if (!companiesOnPage.isEmpty()) {
            List<Long> companyIds = companiesOnPage.stream().map(Company::getId).collect(Collectors.toList());

            // BƯỚC 2: Lấy user đại diện cho tất cả companies trên trang (Query #2 - Hiệu
            // quả)
            List<User> representativeUsers = this.userRepository.findFirstUserForCompanies(companyIds);
            Map<Long, User> userMap = representativeUsers.stream()
                    .collect(Collectors.toMap(u -> u.getCompany().getId(), u -> u));

            // BƯỚC 3: Lấy số lượng job cho tất cả companies trên trang (Query #3 - Đã hiệu
            // quả sẵn)
            List<JobRepository.CompanyJobCountDTO> jobCounts = this.jobRepository
                    .countActiveJobsByCompanyIds(companyIds);
            Map<Long, Long> jobCountMap = jobCounts.stream()
                    .collect(Collectors.toMap(JobRepository.CompanyJobCountDTO::getCompanyId,
                            JobRepository.CompanyJobCountDTO::getJobCount));

            // BƯỚC 4: Gộp kết quả trong bộ nhớ (Không tốn query)
            List<ResFetchCompanyDTO> dtoList = companiesOnPage.stream().map(company -> {
                ResFetchCompanyDTO dto = new ResFetchCompanyDTO();
                // Map thông tin company
                dto.setId(company.getId());
                dto.setName(company.getName());
                dto.setLogo(company.getLogo());
                dto.setField(company.getField());

                dto.setFoundingYear(company.getFoundingYear());
                dto.setLocation(company.getLocation());
                dto.setCountry(company.getCountry());
                dto.setScale(company.getScale());
                dto.setWebsite(company.getWebsite());
                dto.setAddress(company.getAddress());
                dto.setDescription(company.getDescription());
                dto.setCreatedAt(company.getCreatedAt());
                dto.setUpdatedAt(company.getUpdatedAt());

                // Map thông tin HR từ userMap
                User hrUser = userMap.get(company.getId());
                if (hrUser != null) {
                    ResFetchCompanyDTO.HrCompany hrCompany = new ResFetchCompanyDTO.HrCompany(
                            hrUser.getId(),
                            hrUser.getName(),
                            hrUser.getEmail());
                    dto.setHrCompany(hrCompany);
                }

                // Map số lượng job từ jobCountMap
                dto.setTotalJobs(jobCountMap.getOrDefault(company.getId(), 0L));

                return dto;
            }).collect(Collectors.toList());

            rs.setResult(dtoList);
        } else {
            // Nếu trang không có công ty nào thì trả về mảng rỗng
            rs.setResult(new ArrayList<ResFetchCompanyDTO>());
        }

        return rs;
    }

    /**
     * PHƯƠNG THỨC CŨ: Trả về Optional<Company> để không làm ảnh hưởng đến
     * UserService.
     * 
     * @param id The company ID.
     * @return An Optional containing the Company entity if found.
     */
    public Optional<Company> findById(long id) {
        return this.companyRepository.findById(id);
    }

    /**
     * PHƯƠNG THỨC MỚI: Trả về DTO với totalJobs để Controller sử dụng.
     * 
     * @param id The company ID.
     * @return An Optional containing the ResFetchCompanyDTO.
     */
    public Optional<ResFetchCompanyDTO> fetchCompanyDTOById(long id) {
        Optional<Company> companyOptional = this.companyRepository.findById(id);
        if (companyOptional.isEmpty()) {
            return Optional.empty();
        }

        Company company = companyOptional.get();
        // Gọi convertToResFetchCompanyDTO đã được cập nhật
        ResFetchCompanyDTO dto = this.convertToResFetchCompanyDTO(company);

        // Fetch the active job count for this single company
        long activeJobs = this.jobRepository.countByCompany_IdAndActiveTrue(id);
        dto.setTotalJobs(activeJobs);

        return Optional.of(dto);
    }

    // API FOR USER
    // API FOR USER
    @Transactional
    public ResCreateCompanyDTO createCompanyByUser(ReqCreateCompanyDTO reqCompany) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng"));

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        if (!user.isVip()
                || (user.getVipExpiryDate() != null && user.getVipExpiryDate().isBefore(LocalDateTime.now()))) {
            user.setVip(false);
            userRepository.save(user);
            throw new IdInvalidException("Bạn cần là tài khoản VIP để tạo công ty");
        }

        if (user.getCompany() != null) {
            throw new IdInvalidException("Bạn đã tạo một công ty. Mỗi người dùng chỉ được tạo một công ty");
        }

        Company company = new Company();
        company.setName(reqCompany.getName());
        company.setDescription(reqCompany.getDescription());
        company.setAddress(reqCompany.getAddress());
        company.setLogo(reqCompany.getLogo());
        company.setField(reqCompany.getField());
        company.setWebsite(reqCompany.getWebsite());
        company.setScale(reqCompany.getScale());
        company.setCountry(reqCompany.getCountry());
        company.setFoundingYear(reqCompany.getFoundingYear());
        company.setLocation(reqCompany.getLocation());

        Company savedCompany = companyRepository.save(company);
        user.setCompany(savedCompany);

        Role employerRole = roleRepository.findByName("EMPLOYER");
        if (employerRole == null) {
            throw new IdInvalidException("Role EMPLOYER không tồn tại");
        }
        user.setRole(employerRole);
        userRepository.save(user);

        ResCreateCompanyDTO response = new ResCreateCompanyDTO();
        response.setId(savedCompany.getId());
        response.setName(savedCompany.getName());
        response.setDescription(savedCompany.getDescription());
        response.setAddress(savedCompany.getAddress());
        response.setLogo(savedCompany.getLogo());
        response.setCreatedAt(savedCompany.getCreatedAt());
        response.setField(savedCompany.getField());
        response.setWebsite(savedCompany.getWebsite());
        response.setScale(savedCompany.getScale());
        response.setCountry(savedCompany.getCountry());
        response.setFoundingYear(savedCompany.getFoundingYear());
        response.setLocation(savedCompany.getLocation());

        return response;
    }

    // API FOR USER
    @Transactional
    public Company handleUpdateCompanyByUser(ReqUpdateCompanyDTO reqCompany) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy thông tin đăng nhập"));
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        if (currentUser.getCompany() == null) {
            throw new IdInvalidException("Bạn chưa tạo công ty. Không thể cập nhật.");
        }

        long companyId = currentUser.getCompany().getId();
        Company companyToUpdate = this.companyRepository.findById(companyId)
                .orElseThrow(() -> new IdInvalidException("Công ty không tồn tại với id: " + companyId));

        companyToUpdate.setName(reqCompany.getName());
        companyToUpdate.setDescription(reqCompany.getDescription());
        companyToUpdate.setAddress(reqCompany.getAddress());
        companyToUpdate.setLogo(reqCompany.getLogo());
        companyToUpdate.setField(reqCompany.getField());
        companyToUpdate.setWebsite(reqCompany.getWebsite());
        companyToUpdate.setScale(reqCompany.getScale());
        companyToUpdate.setCountry(reqCompany.getCountry());
        companyToUpdate.setFoundingYear(reqCompany.getFoundingYear());
        companyToUpdate.setLocation(reqCompany.getLocation());

        return this.companyRepository.save(companyToUpdate);
    }

    // API FOR USER
    @Transactional
    public void handleDeleteCompanyByUser() throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy thông tin đăng nhập"));
        User currentUser = userRepository.findByEmail(email);
        if (currentUser == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        if (currentUser.getCompany() == null) {
            throw new IdInvalidException("Bạn không có quyền xóa công ty này, vì bạn chưa tạo công ty nào.");
        }

        long companyId = currentUser.getCompany().getId();
        Role userRole = roleRepository.findByName("USER");
        if (userRole == null) {
            throw new IdInvalidException("Role USER không tồn tại. Không thể hoàn tác vai trò.");
        }

        currentUser.setCompany(null);
        currentUser.setRole(userRole);
        this.userRepository.save(currentUser);

        this.companyRepository.deleteById(companyId);
    }
}