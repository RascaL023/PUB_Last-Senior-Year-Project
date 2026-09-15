package id.my.rascal.report.internal.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.report.api.DashboardSummaryApiResponse;
import id.my.rascal.report.internal.service.ReportService;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAuthority('report.read')")
    public ResponseEntity<SuccessTemplate<DashboardSummaryApiResponse>> getDashboardSummary(
        @RequestParam(value = "from", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(value = "to", required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Dashboard summary successfully retrieved",
            reportService.getDashboardSummary(from, to)
        );
    }

}