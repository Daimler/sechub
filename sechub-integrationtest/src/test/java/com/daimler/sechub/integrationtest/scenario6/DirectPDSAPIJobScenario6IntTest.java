// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.scenario6;

import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.daimler.sechub.integrationtest.api.IntegrationTestSetup;
import com.daimler.sechub.integrationtest.api.PDSIntProductIdentifier;

/**
 * Integration test directly using REST API of integration test PDS (means
 * without sechub). When these tests fail, sechub tests will also fail, because
 * PDS API corrupt or PDS server not alive
 * 
 * @author Albert Tregnaghi
 *
 */
public class DirectPDSAPIJobScenario6IntTest {

    private static final Logger LOG = LoggerFactory.getLogger(DirectPDSAPIJobScenario6IntTest.class);

    @Rule
    public IntegrationTestSetup setup = IntegrationTestSetup.forScenario(Scenario6.class);

    @Rule
    public Timeout timeOut = Timeout.seconds(600);

    @Test
    public void pds_techuser_can_create_job_and_jobid_is_returned() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        /* execute */
        String result = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        
        /* test */
        assertPDSJobCreateResult(result).hasJobUUID().getJobUUID();
        
        /* @formatter:on */
    }

    @Test
    public void pds_techuser_can_get_job_status_of_created_job_and_is_CREATED() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();

        /* execute */
        String result = asPDSUser(PDS_TECH_USER).getJobStatus(pdsJobUUID);
        
        /* test */
        assertPDSJobStatus(result).isInState("CREATED");
        /* @formatter:on */
    }

    @Test
    public void pds_techuser_can_upload_content_to_PDS() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();

        /* execute */
        asPDSUser(PDS_TECH_USER).upload(pdsJobUUID, "sourcecode.zip", "pds/codescan/upload/zipfile_contains_inttest_codescan_with_critical.zip");
        
        /* test */
        assertPDSWorkspace().hasUploadedFile(pdsJobUUID, "sourcecode.zip");
        
        /* @formatter:on */
    }

    @Test
    public void pds_techuser_can_mark_job_as_ready_to_start_and_after_while_job_result_is_returned() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();
        asPDSUser(PDS_TECH_USER).upload(pdsJobUUID, "sourcecode.zip", "pds/codescan/upload/zipfile_contains_inttest_codescan_with_critical.zip");
        
        /* execute */
        asPDSUser(PDS_TECH_USER).markJobAsReadyToStart(pdsJobUUID);
        
        /* test */
        String report = asPDSUser(PDS_TECH_USER).getJobReport(pdsJobUUID);
        assertTrue(report.contains("CRITICAL"));
        
        /* @formatter:on */
    }

    @Test
    public void pds_admin_can_create_job_and_jobid_is_returned() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        /* execute */
        String result = asPDSUser(PDS_ADMIN).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        
        /* test */
        assertPDSJobCreateResult(result).hasJobUUID().getJobUUID();
        
        /* @formatter:on */
    }

    @Test
    public void pds_admin_can_get_job_status_of_created_job_and_is_CREATED() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_ADMIN).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();

        /* execute */
        String result = asPDSUser(PDS_ADMIN).getJobStatus(pdsJobUUID);
        
        /* test */
        assertPDSJobStatus(result).isInState("CREATED");
        /* @formatter:on */
    }

    @Test
    public void pds_admin_can_upload_content_to_PDS() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_ADMIN).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();

        /* execute */
        asPDSUser(PDS_ADMIN).upload(pdsJobUUID, "sourcecode.zip", "pds/codescan/upload/zipfile_contains_inttest_codescan_with_critical.zip");
        
        /* test */
        assertPDSWorkspace().hasUploadedFile(pdsJobUUID, "sourcecode.zip");
        
        /* @formatter:on */
    }

    @Test
    public void pds_admin_can_mark_job_as_ready_to_start_and_after_while_job_result_is_returned() {
        /* @formatter:off */
        /* prepare */
        
        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_ADMIN).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();
        asPDSUser(PDS_ADMIN).upload(pdsJobUUID, "sourcecode.zip", "pds/codescan/upload/zipfile_contains_inttest_codescan_with_critical.zip");
        
        /* execute */
        asPDSUser(PDS_ADMIN).markJobAsReadyToStart(pdsJobUUID);
        
        /* test */
        String report = asPDSUser(PDS_ADMIN).getJobReportOrErrorText(pdsJobUUID);
        if(!report.contains("CRITICAL")){
            LOG.error(report);
            fail("Not expected report but:\n"+report);
        };
        
        /* @formatter:on */
    }
    

    public void anonymous_cannot_create_job() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        /* execute + test */
        expectHttpFailure(()-> asPDSUser(ANONYMOUS).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN), HttpStatus.UNAUTHORIZED);
        
        /* @formatter:on */
    }

    @Test
    public void anonymous_cannot_get_job_status_of_created_job() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();

        /* execute + test */
        expectHttpFailure(()->  asPDSUser(ANONYMOUS).getJobStatus(pdsJobUUID), HttpStatus.UNAUTHORIZED);
        /* @formatter:on */
    }

    @Test
    public void anonymous_cannot_upload_content_to_PDS() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();

        /* execute + test */
        expectHttpFailure(()-> asPDSUser(ANONYMOUS).upload(pdsJobUUID, "sourcecode.zip", "pds/codescan/upload/zipfile_contains_inttest_codescan_with_critical.zip"), HttpStatus.UNAUTHORIZED);
        
        /* @formatter:on */
    }

    @Test
    public void anonymous_cannot_mark_job_as_ready_to_start() {
        /* @formatter:off */
        /* prepare */

        UUID sechubJobUUID = UUID.randomUUID();
        
        String createResult = asPDSUser(PDS_TECH_USER).createJobFor(sechubJobUUID, PDSIntProductIdentifier.PDS_INTTEST_CODESCAN);
        UUID pdsJobUUID = assertPDSJobCreateResult(createResult).hasJobUUID().getJobUUID();
        
        /* execute + test */
        expectHttpFailure(()-> asPDSUser(ANONYMOUS).markJobAsReadyToStart(pdsJobUUID), HttpStatus.UNAUTHORIZED);
        /* @formatter:on */
    }

  

}
