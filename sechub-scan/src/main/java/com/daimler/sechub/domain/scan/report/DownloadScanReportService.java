// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.report;

import static com.daimler.sechub.sharedkernel.util.Assert.*;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daimler.sechub.domain.scan.ScanAssertService;
import com.daimler.sechub.domain.scan.SecHubResultService;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.error.NotFoundException;
import com.daimler.sechub.sharedkernel.logging.AuditLogService;
import com.daimler.sechub.sharedkernel.usecases.user.execute.UseCaseUserDownloadsJobReport;

@Service
public class DownloadScanReportService {

	@Autowired
	ScanAssertService assertService;

	@Autowired
	SecHubResultService secHubResultService;

	@Autowired
	ScanReportRepository reportRepository;

	@Autowired
	AuditLogService auditLogService;

	/**
	 * There must be a a security check because useable from outside
	 * @param projectId
	 * @param jobUUID
	 * @return
	 */
	@UseCaseUserDownloadsJobReport(@Step(number=3, name="Resolve scan report result"))
	public ScanReportResult getScanReportResult(String projectId, UUID jobUUID) {
		notNull(projectId, "projectId may not be null!");
		notNull(jobUUID, "job uuid may not be null!");

		auditLogService.log("starts download of report for job: {}",jobUUID);
		ScanReport report = reportRepository.findBySecHubJobUUID(jobUUID);

		if (report == null) {
			throw new NotFoundException("Report not found or you have no access to report!");
		}
		assertService.assertUserHasAccessToReport(report);

		return new ScanReportResult(report);
	}

}
