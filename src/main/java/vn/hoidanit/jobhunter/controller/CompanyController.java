package vn.hoidanit.jobhunter.controller;

import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.request.ReqCreateCompanyDTO;
import vn.hoidanit.jobhunter.domain.request.ReqUpdateCompanyDTO;
import vn.hoidanit.jobhunter.domain.response.ResCreateCompanyDTO;
import vn.hoidanit.jobhunter.domain.response.ResFetchCompanyDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.service.CompanyService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class CompanyController {
    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    // === API DÀNH CHO ADMIN ===
    // API của Admin tự động hỗ trợ các trường mới vì nhận trực tiếp entity
    // `Company`

    @PostMapping("/companies")
    @ApiMessage("Create a company by admin")
    public ResponseEntity<Company> createCompany(@Valid @RequestBody Company reqCompany) {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.companyService.handleCreateCompany(reqCompany));
    }

    @GetMapping("/companies")
    @ApiMessage("Fetch all Companies")
    public ResponseEntity<ResultPaginationDTO> getCompany(
            @Filter Specification<Company> spec, Pageable pageable) {
        return ResponseEntity.ok(this.companyService.handleGetCompany(spec, pageable));
    }

    @PutMapping("/companies")
    @ApiMessage("Update a company by admin")
    public ResponseEntity<Company> updateCompany(@Valid @RequestBody Company reqCompany) {
        Company updatedCompany = this.companyService.handleUpdateCompany(reqCompany);
        return ResponseEntity.ok(updatedCompany);
    }

    @DeleteMapping("/companies/{id}")
    @ApiMessage("Delete a company by admin")
    public ResponseEntity<Void> deleteCompany(@PathVariable("id") long id) {
        this.companyService.handleDeleteCompany(id);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/companies/{id}")
    @ApiMessage("Fetch company by id")
    public ResponseEntity<ResFetchCompanyDTO> fetchCompanyById(@PathVariable("id") long id) throws IdInvalidException {
        // Gọi phương thức mới để lấy DTO
        Optional<ResFetchCompanyDTO> cOptional = this.companyService.fetchCompanyDTOById(id);

        if (cOptional.isEmpty()) {
            throw new IdInvalidException("Không tìm thấy công ty với id " + id);
        }
        return ResponseEntity.ok().body(cOptional.get());
    }
    // === API DÀNH CHO USER QUẢN LÝ CÔNG TY CỦA MÌNH ===

    @PostMapping("/companies/by-user")
    @ApiMessage("Create a company for the current user")
    public ResponseEntity<ResCreateCompanyDTO> createCompanyByUser(@Valid @RequestBody ReqCreateCompanyDTO reqCompany)
            throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED).body(this.companyService.createCompanyByUser(reqCompany));
    }

    @PutMapping("/companies/by-user")
    @ApiMessage("Update a company that user created")
    public ResponseEntity<Company> updateCompanyByUser(
            @Valid @RequestBody ReqUpdateCompanyDTO reqCompany) throws IdInvalidException {
        Company updatedCompany = this.companyService.handleUpdateCompanyByUser(reqCompany);
        return ResponseEntity.ok(updatedCompany);
    }

    @DeleteMapping("/companies/by-user")
    @ApiMessage("Delete a company that user created")
    public ResponseEntity<Void> deleteCompanyByUser() throws IdInvalidException {
        this.companyService.handleDeleteCompanyByUser();
        return ResponseEntity.ok(null);
    }
}