package vn.hoidanit.jobhunter.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.turkraft.springfilter.boot.Filter;
import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.JobBulkCreateDTO;
import vn.hoidanit.jobhunter.domain.request.ReqCreateJobDTO;
import vn.hoidanit.jobhunter.domain.request.ReqUpdateJobDTO;
import vn.hoidanit.jobhunter.domain.response.ResBulkCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResUpdateJobDTO;
import vn.hoidanit.jobhunter.service.JobService;
import vn.hoidanit.jobhunter.service.mapper.JobMapper;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

@RestController
@RequestMapping("/api/v1")
public class JobController {

    private final JobService jobService;
    private final JobMapper jobMapper;

    public JobController(JobService jobService, JobMapper jobMapper) {
        this.jobService = jobService;
        this.jobMapper = jobMapper;

    }

    @PostMapping("/jobs")
    @ApiMessage("Create a job")
    public ResponseEntity<ResCreateJobDTO> create(@Valid @RequestBody Job job) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.jobService.create(job));
    }

    @PostMapping("/jobs/bulk-create")
    @ApiMessage("Create bulk list job")

    public ResponseEntity<ResBulkCreateJobDTO> bulkCreateJobs(@Valid @RequestBody List<JobBulkCreateDTO> jobDTOs) {
        ResBulkCreateJobDTO result = this.jobService.handleBulkCreateJobs(jobDTOs);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/jobs")
    @ApiMessage("Update a job")
    public ResponseEntity<ResUpdateJobDTO> update(@Valid @RequestBody Job job) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(job.getId());
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }

        return ResponseEntity.ok()
                .body(this.jobService.update(job, currentJob.get()));
    }

    @DeleteMapping("/jobs/{id}")
    @ApiMessage("Delete a job by id")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }
        this.jobService.delete(id);
        return ResponseEntity.ok().body(null);
    }

    @GetMapping("/jobs/{id}")
    @ApiMessage("Get a job by id")
    public ResponseEntity<Job> getJob(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Job> currentJob = this.jobService.fetchJobById(id);
        if (!currentJob.isPresent()) {
            throw new IdInvalidException("Job not found");
        }

        return ResponseEntity.ok().body(currentJob.get());
    }

    @GetMapping("/jobs")
    @ApiMessage("Get job with pagination")
    public ResponseEntity<ResultPaginationDTO> getAllJob(
            @Filter Specification<Job> spec,
            Pageable pageable,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "vi") String language) {

        Page<Job> jobPage = this.jobService.fetchAll(spec, pageable); // Service chỉ trả về Entity

        // Dùng Mapper để chuyển đổi
        List<ResJobDTO> resultDTO = this.jobMapper.toDto(jobPage.getContent(), language);

        // Tạo response
        ResultPaginationDTO response = new ResultPaginationDTO();
        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta();
        meta.setPage(pageable.getPageNumber() + 1);
        meta.setPageSize(pageable.getPageSize());
        meta.setPages(jobPage.getTotalPages());
        meta.setTotal(jobPage.getTotalElements());

        response.setMeta(meta);
        response.setResult(resultDTO);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/by-company/{companyId}")
    @ApiMessage("Fetch jobs by company id")
    public ResponseEntity<ResultPaginationDTO> fetchJobsByCompany(
            @PathVariable("companyId") long companyId,
            @Filter Specification<Job> spec,
            Pageable pageable) throws IdInvalidException {

        // Kiểm tra company tồn tại
        if (!jobService.isCompanyExist(companyId)) {
            throw new IdInvalidException("Công ty với id = " + companyId + " không tồn tại");
        }

        return ResponseEntity.ok(jobService.fetchJobsByCompany(companyId, spec, pageable));
    }

    @PostMapping("/jobs/by-user-company")
    @ApiMessage("Create a job for user's company")
    public ResponseEntity<ResCreateJobDTO> createForUserCompany(@Valid @RequestBody ReqCreateJobDTO jobDTO)
            throws IdInvalidException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.jobService.createForUserCompany(jobDTO));
    }

    @PutMapping("/jobs/by-user-company")
    @ApiMessage("Update a job for user's company")
    public ResponseEntity<ResUpdateJobDTO> updateForUserCompany(@Valid @RequestBody ReqUpdateJobDTO jobDTO)
            throws IdInvalidException {
        return ResponseEntity.ok()
                .body(this.jobService.updateForUserCompany(jobDTO));
    }

    @DeleteMapping("/jobs/by-user-company/{id}")
    @ApiMessage("Delete a job for user's company")
    public ResponseEntity<Void> deleteForUserCompany(@PathVariable("id") long id) throws IdInvalidException {
        this.jobService.deleteForUserCompany(id);
        return ResponseEntity.ok().body(null);
    }
}
