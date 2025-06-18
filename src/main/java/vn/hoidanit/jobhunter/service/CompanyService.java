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

    public Company handleCreateCompany(Company c) {
        return this.companyRepository.save(c);
    }

    @Transactional
    public ResCreateCompanyDTO createCompanyByUser(ReqCreateCompanyDTO reqCompany) throws IdInvalidException {
        // Lấy thông tin người dùng hiện tại
        String email = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy người dùng"));

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IdInvalidException("Người dùng không tồn tại");
        }

        // Kiểm tra tài khoản VIP
        if (!user.isVip() || (user.getVipExpiryDate() != null && user.getVipExpiryDate().isBefore(LocalDateTime.now()))) {
            user.setVip(false);
            userRepository.save(user);
            throw new IdInvalidException("Bạn cần là tài khoản VIP để tạo công ty");
        }

        // Kiểm tra người dùng đã có công ty
        if (user.getCompany() != null) {
            throw new IdInvalidException("Bạn đã tạo một công ty. Mỗi người dùng chỉ được tạo một công ty");
        }

        // Tạo công ty mới
        Company company = new Company();
        company.setName(reqCompany.getName());
        company.setDescription(reqCompany.getDescription());
        company.setAddress(reqCompany.getAddress());
        company.setLogo(reqCompany.getLogo());
        company.setField(reqCompany.getField());
        company.setWebsite(reqCompany.getWebsite());
        company.setScale(reqCompany.getScale());
        company.setCountry(reqCompany.getCountry());
        company.setEstablishedYear(reqCompany.getEstablishedYear());

        // Lưu công ty
        Company savedCompany = companyRepository.save(company);

        // Cập nhật company_id cho người dùng
        user.setCompany(savedCompany);

        // Gán role EMPLOYER cho người dùng
        Role employerRole = roleRepository.findByName("EMPLOYER");
        if (employerRole == null) {
            throw new IdInvalidException("Role EMPLOYER không tồn tại");
        }
        user.setRole(employerRole);

        userRepository.save(user);

        // Tạo response
        ResCreateCompanyDTO response = new ResCreateCompanyDTO();
        response.setId(savedCompany.getId());
        response.setName(savedCompany.getName());
        response.setDescription(savedCompany.getDescription());
        response.setAddress(savedCompany.getAddress());
        response.setLogo(savedCompany.getLogo());
        response.setField(savedCompany.getField());
        response.setWebsite(savedCompany.getWebsite());
        response.setScale(savedCompany.getScale());
        response.setCountry(savedCompany.getCountry());
        response.setEstablishedYear(savedCompany.getEstablishedYear());
        response.setCreatedAt(savedCompany.getCreatedAt());

        return response;
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

    public Company handleUpdateCompany(Company c) {
        Optional<Company> companyOptional = this.companyRepository.findById(c.getId());
        if (companyOptional.isPresent()) {
            Company currentCompany = companyOptional.get();
            currentCompany.setName(c.getName());
            currentCompany.setDescription(c.getDescription());
            currentCompany.setAddress(c.getAddress());
            currentCompany.setLogo(c.getLogo());
            currentCompany.setField(c.getField());
            currentCompany.setWebsite(c.getWebsite());
            currentCompany.setScale(c.getScale());
            currentCompany.setCountry(c.getCountry());
            currentCompany.setEstablishedYear(c.getEstablishedYear());
            return this.companyRepository.save(currentCompany);
        }
        return null;
    }

    public void handleDeleteCompany(long id) {
        Optional<Company> comOptional = this.companyRepository.findById(id);
        if (comOptional.isPresent()) {
            Company com = comOptional.get();
            // fetch all user belong to this company
            List<User> users = this.userRepository.findByCompany(com);
            this.userRepository.deleteAll(users);
        }
        this.companyRepository.deleteById(id);
    }

    public Optional<Company> findById(long id) {
        return this.companyRepository.findById(id);
    }
}