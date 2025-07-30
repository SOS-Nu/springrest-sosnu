package vn.hoidanit.jobhunter.service;

import java.util.Map;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.response.ResDashboardDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.DashboardRepository;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.ResumeRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;

@EnableCaching
@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public ResDashboardDTO getDashboardStats() {
        Map<String, Long> stats = dashboardRepository.getDashboardStats();
        return new ResDashboardDTO(
                stats.get("totalUsers"),
                stats.get("totalCompanies"),
                stats.get("totalJobs"),
                stats.get("totalResumesApproved"));
    }
}
