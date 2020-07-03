// SPDX-License-Identifier: MIT
package com.daimler.sechub.pds.job;

import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.daimler.sechub.pds.PDSAPIConstants;
import com.daimler.sechub.pds.security.PDSRoleConstants;
import com.daimler.sechub.pds.usecase.UseCaseUserCancelsJob;
import com.daimler.sechub.pds.usecase.UseCaseUserCreatesJob;
import com.daimler.sechub.pds.usecase.UseCaseUserFetchesJobResult;
import com.daimler.sechub.pds.usecase.UseCaseUserFetchesJobStatus;
import com.daimler.sechub.pds.usecase.UseCaseUserMarksJobReadyToStart;
import com.daimler.sechub.pds.usecase.UseCaseUserUploadsJobData;

/**
 * The REST API for PDS jobs
 *
 * @author Albert Tregnaghi
 *
 */
@RestController
@EnableAutoConfiguration
@RequestMapping(PDSAPIConstants.API_JOB)
@RolesAllowed({PDSRoleConstants.ROLE_USER, PDSRoleConstants.ROLE_SUPERADMIN})
public class PDSJobRestController {

	@Autowired
	private PDSUpdateJobTransactionService updateJobTransactionService;

	@Autowired
	private PDSCreateJobService createJobService;

	@Autowired
	private PDSFileUploadJobService fileUploadJobService;

	@Autowired
	private PDSGetJobStatusService jobStatusService;
	
	@Autowired
    private PDSGetJobResultService jobResultService;
	
	@Autowired
    private PDSCancelJobService cancelJobService;

	@Validated
	@RequestMapping(path = "create", method = RequestMethod.POST)
	@UseCaseUserCreatesJob
	public PDSJobCreateResult createJob(
			@RequestBody PDSJobConfiguration configuration) {
		return createJobService.createJob(configuration);
	}


	/* @formatter:off */
	@Validated
	@RequestMapping(path = "{jobUUID}/upload/{fileName}", method = RequestMethod.POST)
	@UseCaseUserUploadsJobData
	public void upload(
				@PathVariable("jobUUID") UUID jobUUID,
				@PathVariable("fileName") String fileName,
				@RequestParam("file") MultipartFile file,
				@RequestParam("checkSum") String checkSum
			) {
		fileUploadJobService.upload(jobUUID,fileName, file,checkSum);
	}
	/* @formatter:on */


	/* @formatter:off */
	@Validated
	@RequestMapping(path = "{jobUUID}/mark-ready-to-start", method = RequestMethod.PUT)
	@UseCaseUserMarksJobReadyToStart
	public void markReadyToStart(
				@PathVariable("jobUUID") UUID jobUUID) {
		updateJobTransactionService.markReadyToStartInOwnTransaction(jobUUID);
	}
	/* @formatter:on */
	
	/* @formatter:off */
    @Validated
    @RequestMapping(path = "{jobUUID}/cancel", method = RequestMethod.PUT)
    @UseCaseUserCancelsJob
    public void cancelJob(
                @PathVariable("jobUUID") UUID jobUUID) {
        cancelJobService.cancelJob(jobUUID);
    }
    /* @formatter:on */


	/* @formatter:off */
	@Validated
	@RequestMapping(path = "{jobUUID}/status", method = RequestMethod.GET)
	@UseCaseUserFetchesJobStatus
	public PDSJobStatus getJobStatus(
			@PathVariable("jobUUID") UUID jobUUID
			) {
		/* @formatter:on */
		return jobStatusService.getJobStatus(jobUUID);

	}
	
	/* @formatter:off */
    @Validated
    @RequestMapping(path = "{jobUUID}/result", method = RequestMethod.GET)
    @UseCaseUserFetchesJobResult
    public String getJobResult(
            @PathVariable("jobUUID") UUID jobUUID
            ) {
        /* @formatter:on */
        return jobResultService.getJobResult(jobUUID);
    }
	

}
