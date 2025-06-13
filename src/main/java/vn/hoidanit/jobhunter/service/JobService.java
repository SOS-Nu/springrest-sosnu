package vn.hoidanit.jobhunter.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.entity.Company;
import vn.hoidanit.jobhunter.domain.entity.Job;
import vn.hoidanit.jobhunter.domain.entity.JobBulkCreateDTO;
import vn.hoidanit.jobhunter.domain.entity.Skill;
import vn.hoidanit.jobhunter.domain.response.ResBulkCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResUpdateJobDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.SkillRepository;
import vn.hoidanit.jobhunter.util.constant.LevelEnum;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;

    public JobService(JobRepository jobRepository,
            SkillRepository skillRepository, CompanyRepository companyRepository) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.companyRepository = companyRepository;
    }

    public Optional<Job> fetchJobById(long id) {
        return this.jobRepository.findById(id);
    }

    public ResCreateJobDTO create(Job j) {
        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            j.setSkills(dbSkills);

        }

        // create job
        Job currentJob = this.jobRepository.save(j);

        // convert response
        ResCreateJobDTO dto = new ResCreateJobDTO();
        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setCreatedAt(currentJob.getCreatedAt());
        dto.setCreatedBy(currentJob.getCreatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills()
                    .stream().map(item -> item.getName())
                    .collect(Collectors.toList());
            dto.setSkills(skills);
        }

        return dto;
    }

    public ResUpdateJobDTO update(Job j, Job jobInDB) {

        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
                    .collect(Collectors.toList());

            List<Skill> dbSkills = this.skillRepository.findByIdIn(reqSkills);
            jobInDB.setSkills(dbSkills);
        }

        // check company
        if (j.getCompany() != null) {
            Optional<Company> cOptional = this.companyRepository.findById(j.getCompany().getId());
            if (cOptional.isPresent()) {
                jobInDB.setCompany(cOptional.get());
            }
        }

        // update correct info
        jobInDB.setName(j.getName());
        jobInDB.setSalary(j.getSalary());
        jobInDB.setQuantity(j.getQuantity());
        jobInDB.setLocation(j.getLocation());
        jobInDB.setLevel(j.getLevel());
        jobInDB.setStartDate(j.getStartDate());
        jobInDB.setEndDate(j.getEndDate());
        jobInDB.setActive(j.isActive());

        // update job
        Job currentJob = this.jobRepository.save(jobInDB);

        // convert response
        ResUpdateJobDTO dto = new ResUpdateJobDTO();
        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setUpdatedAt(currentJob.getUpdatedAt());
        dto.setUpdatedBy(currentJob.getUpdatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills()
                    .stream().map(item -> item.getName())
                    .collect(Collectors.toList());
            dto.setSkills(skills);
        }

        return dto;
    }

    public void delete(long id) {
        this.jobRepository.deleteById(id);
    }

    public ResultPaginationDTO fetchAll(Specification<Job> spec, Pageable pageable) {
        Page<Job> pageUser = this.jobRepository.findAll(spec, pageable);

        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageUser.getTotalPages());
        mt.setTotal(pageUser.getTotalElements());

        rs.setMeta(mt);

        rs.setResult(pageUser.getContent());

        return rs;
    }

    @Transactional
    public ResBulkCreateJobDTO handleBulkCreateJobs(List<JobBulkCreateDTO> jobDTOs) {
        int total = jobDTOs.size();
        int success = 0;
        List<String> failedJobs = new ArrayList<>();

        for (JobBulkCreateDTO dto : jobDTOs) {
            try {
                // Kiểm tra công việc trùng (dựa trên name và company)
                if (this.isJobExist(dto.getName(), dto.getCompany().getId())) {
                    failedJobs.add(dto.getName() + " (Công việc đã tồn tại cho công ty này)");
                    continue;
                }

                Job job = new Job();
                job.setName(dto.getName());
                job.setLocation(dto.getLocation());
                job.setSalary(Long.parseLong(dto.getSalary()));
                job.setQuantity(dto.getQuantity());
                job.setLevel(dto.getLevel());
                job.setDescription(dto.getDescription());
                job.setStartDate(Instant.parse(dto.getStartDate()));
                job.setEndDate(Instant.parse(dto.getEndDate()));
                job.setActive(dto.isActive());

                // Kiểm tra và gán company
                Optional<Company> companyOptional = this.companyRepository.findById(dto.getCompany().getId());
                if (companyOptional.isEmpty()) {
                    failedJobs.add(dto.getName() + " (Company ID không tồn tại)");
                    continue;
                }
                job.setCompany(companyOptional.get());

                // Kiểm tra và gán skills
                List<Long> skillIds = dto.getSkills().stream().map(JobBulkCreateDTO.SkillDTO::getId)
                        .collect(Collectors.toList());
                List<Skill> skills = this.skillRepository.findAllById(skillIds);
                if (skills.size() != skillIds.size()) {
                    failedJobs.add(dto.getName() + " (Một hoặc nhiều Skill ID không tồn tại)");
                    continue;
                }
                job.setSkills(skills);

                // Lưu job
                this.jobRepository.save(job);
                success++;
            } catch (Exception e) {
                failedJobs.add(dto.getName() + " (Lỗi hệ thống: " + e.getMessage() + ")");
            }
        }

        return new ResBulkCreateJobDTO(total, success, total - success, failedJobs);
    }

    private boolean isJobExist(String name, Long companyId) {
        return this.jobRepository.existsByNameAndCompanyId(name, companyId);
    }

    public boolean isCompanyExist(long companyId) {
        return companyRepository.existsById(companyId);
    }

    public ResultPaginationDTO fetchJobsByCompany(long companyId, Specification<Job> spec, Pageable pageable) {
        Specification<Job> companySpec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("company").get("id"), companyId);

        // Kết hợp spec từ người dùng với spec của company
        Specification<Job> finalSpec = companySpec.and(spec);

        Page<Job> pageJob = jobRepository.findAll(finalSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());
        mt.setPages(pageJob.getTotalPages());
        mt.setTotal(pageJob.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageJob.getContent());

        return rs;
    }
}
