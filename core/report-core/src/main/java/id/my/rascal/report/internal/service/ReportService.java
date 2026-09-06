package id.my.rascal.report.internal.service;

import java.time.LocalDate;

import id.my.rascal.report.api.DashboardSummaryApiResponse;

public interface ReportService {

    DashboardSummaryApiResponse getDashboardSummary(LocalDate from, LocalDate to);

}