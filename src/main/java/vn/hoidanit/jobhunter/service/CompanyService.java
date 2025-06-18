package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository, RoleRepository roleRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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

    public ResultPaginationDTO handleGetCompany(@Filter Specification<Company> spec, Pageable pageable) {
        Page<Company> pCompany = this.companyRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pCompany.getTotalPages());
        mt.setTotal(pCompany.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pCompany.getContent());
        return rs;
    }

    public Optional<Company> findById(long id) {
        return this.companyRepository.findById(id);
    }

    // API FOR USER
    @Transactional
    public ResCreateCompanyDTO createCompanyByUser(ReqCreateCompanyDTO reqCompany) throws IdInvalidException {
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng"));

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        if (!user.isVip() || (user.getVipExpiryDate() != null && user.getVipExpiryDate().isBefore(LocalDateTime.now()))) {
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