package vn.hoidanit.jobhunter.service;

import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.response.ResDashboardDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.ResumeRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ResumeRepository resumeRepository;

    public DashboardService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            JobRepository jobRepository,
            ResumeRepository resumeRepository) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.jobRepository = jobRepository;
        this.resumeRepository = resumeRepository;
    }

    public ResDashboardDTO getDashboardStats() {
        long totalUsers = this.userRepository.count();
        long totalCompanies = this.companyRepository.count();
        long totalJobs = this.jobRepository.count();
        long totalResumesApproved = this.resumeRepository.countByStatus(ResumeStateEnum.APPROVED);

        return new ResDashboardDTO(totalUsers, totalCompanies, totalJobs, totalResumesApproved);
    }
}