package vn.hoidanit.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.domain.response.ResDashboardDTO;
import vn.hoidanit.jobhunter.service.DashboardService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @ApiMessage("Fetch dashboard statistics")
    public ResponseEntity<ResDashboardDTO> getDashboard() {
        return ResponseEntity.ok(this.dashboardService.getDashboardStats());
    }
}