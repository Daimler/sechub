// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.scenario4;

import static com.daimler.sechub.integrationtest.api.AssertJob.*;
import static com.daimler.sechub.integrationtest.api.TestAPI.*;
import static com.daimler.sechub.integrationtest.scenario4.Scenario4.*;
import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import com.daimler.sechub.integrationtest.api.AssertEventInspection;
import com.daimler.sechub.integrationtest.api.IntegrationTestSetup;
import com.daimler.sechub.integrationtest.api.TestProject;
import com.daimler.sechub.sharedkernel.messaging.MessageID;
import com.daimler.sechub.sharedkernel.usecases.UseCaseIdentifier;

public class JobUsecasesEventTraceScenario4IntTest {

    @Rule
    public IntegrationTestSetup setup = IntegrationTestSetup.forScenario(Scenario4.class);

    @Rule
    public Timeout timeOut = Timeout.seconds(600);

    TestProject project = PROJECT_1;

    
    /* ------------------------------------------------------------------------ */
    /* --------------------------- HARD RESTART ------------------------------- */
    /* ------------------------------------------------------------------------ */
    @Test
    /**
     * We simulate a JVM crash where a product result was already written to
     * database. 
     */
    public void UC_ADMIN_RESTARTS_JOB_HARD__simulate_accidently_job_restarted_where_already_done() {
        /* @formatter:off */
        /* prepare */
        UUID sechubJobUUD = as(USER_1).triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(project);
        
        waitForJobDone(project, sechubJobUUD);

        startEventInspection();
        
        /* execute */
        as(SUPER_ADMIN).restartCodeScanHardAndFetchJobStatus(project,sechubJobUUD);
        
        /* test */
        assertJobHasEnded(project,sechubJobUUD);
        
        AssertEventInspection.assertEventInspection().
        expect().
           /* 0 */
           asyncEvent(MessageID.REQUEST_JOB_RESTART_HARD).
                 from("com.daimler.sechub.domain.administration.job.JobRestartRequestService").
                 to("com.daimler.sechub.domain.schedule.ScheduleMessageHandler").
           /* 1 */
           asyncEvent(MessageID.JOB_RESTART_CANCELED).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
        /* assert + write */
        assertAsExpectedAndCreateHistoryFile(UseCaseIdentifier.UC_ADMIN_RESTARTS_JOB_HARD.name(),"accidently_restart_because_job_has_already_finished");
        /* @formatter:on */
    }

    @Test
    /**
     * We simulate a JVM crash where a product result was already written to
     * database.
     */
    public void UC_ADMIN_RESTARTS_JOB_HARD__simulate_jvm_crash_but_product_results_in_db() {
        /* @formatter:off */
        /* prepare */
        UUID sechubJobUUD = as(USER_1).triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(project);
        waitForJobDone(project, sechubJobUUD);
        simulateJobIsStillRunningAndUploadAvailable(sechubJobUUD);
        
        startEventInspection();

        /* execute */
        as(SUPER_ADMIN).restartCodeScanHardAndFetchJobStatus(project,sechubJobUUD);
        
        /* test */
        assertJobHasEnded(project,sechubJobUUD);
        
        AssertEventInspection.assertEventInspection().
        expect().
           /* 0 */
           asyncEvent(MessageID.REQUEST_JOB_RESTART_HARD).
                 from("com.daimler.sechub.domain.administration.job.JobRestartRequestService").
                 to("com.daimler.sechub.domain.schedule.ScheduleMessageHandler").
           /* 1 */
           syncEvent(MessageID.REQUEST_PURGE_JOB_RESULTS).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.scan.ScanMessageHandler").
           /* 2 */
           asyncEvent(MessageID.JOB_RESULTS_PURGED).
                 from("com.daimler.sechub.domain.scan.product.ProductResultService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
           /* 3 */
           asyncEvent(MessageID.JOB_RESTART_TRIGGERED).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
           /* 4 */
           asyncEvent(MessageID.JOB_STARTED).
                 from("com.daimler.sechub.domain.schedule.ScheduleJobLauncherService").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
           /* 5 */
           syncEvent(MessageID.START_SCAN).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor$1").
                 to("com.daimler.sechub.domain.scan.ScanService").
           /* 6 */
           syncEvent(MessageID.REQUEST_BATCH_JOB_STATUS).
                 from("com.daimler.sechub.domain.scan.ScanProgressMonitor").
                 to("com.daimler.sechub.domain.schedule.batch.SchedulerBatchJobStatusRequestHandler").
           /* 7 */
           asyncEvent(MessageID.JOB_DONE).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
        /* assert + write */
        assertAsExpectedAndCreateHistoryFile(UseCaseIdentifier.UC_ADMIN_RESTARTS_JOB_HARD.name(),"crashed_jvm_with_product_result");
        /* @formatter:on */
    }
    
    @Test
    /**
     * We simulate a JVM crash where NO product result was written to database.
     * Shall trigger JOB_RESTARTED
     */
    public void UC_ADMIN_RESTARTS_JOB_HARD__simulate_jvm_crash_no_product_results_in_db_upload_still_available() {
        /* @formatter:off */
        /* prepare */
        UUID sechubJobUUD = as(USER_1).triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(project);
        
        waitForJobDone(project, sechubJobUUD);
        simulateJobIsStillRunningAndUploadAvailable(sechubJobUUD);
        
        destroyProductResults(sechubJobUUD); // destroy former product result to simulate execution crashed..
        
        startEventInspection();

        /* execute */
        as(SUPER_ADMIN).restartCodeScanHardAndFetchJobStatus(project,sechubJobUUD);
        
        /* test */
        assertJobHasEnded(project,sechubJobUUD);
        AssertEventInspection.assertEventInspection().
        expect().
           /* 0 */
           asyncEvent(MessageID.REQUEST_JOB_RESTART_HARD).
                 from("com.daimler.sechub.domain.administration.job.JobRestartRequestService").
                 to("com.daimler.sechub.domain.schedule.ScheduleMessageHandler").
           /* 1 */
           syncEvent(MessageID.REQUEST_PURGE_JOB_RESULTS).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.scan.ScanMessageHandler").
           /* 2 */
           asyncEvent(MessageID.JOB_RESTART_TRIGGERED).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
           /* 3 */
           asyncEvent(MessageID.JOB_STARTED).
                 from("com.daimler.sechub.domain.schedule.ScheduleJobLauncherService").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
           /* 4 */
           syncEvent(MessageID.START_SCAN).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor$1").
                 to("com.daimler.sechub.domain.scan.ScanService").
           /* 5 */
           syncEvent(MessageID.REQUEST_BATCH_JOB_STATUS).
                 from("com.daimler.sechub.domain.scan.ScanProgressMonitor").
                 to("com.daimler.sechub.domain.schedule.batch.SchedulerBatchJobStatusRequestHandler").
           /* 6 */
           asyncEvent(MessageID.JOB_DONE).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
        /* assert + write */
        assertAsExpectedAndCreateHistoryFile(UseCaseIdentifier.UC_ADMIN_RESTARTS_JOB_HARD.name(),"crashed_jvm_with_product_result");
        /* @formatter:on */
    }

    /* ------------------------------------------------------------------------ */
    /* --------------------------- SOFT RESTART ------------------------------- */
    /* ------------------------------------------------------------------------ */

    @Test
    /**
     * We simulate a JVM crash where a product result was already written to
     * database. Shall trigger JOB_RESTARTED
     */
    public void UC_ADMIN_RESTARTS_JOB__simulate_accidently_job_restarted_where_already_done() {
        /* @formatter:off */
        /* prepare */
        UUID sechubJobUUD = as(USER_1).triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(project);
        
        waitForJobDone(project, sechubJobUUD);
        startEventInspection();
        
        /* execute */
        as(SUPER_ADMIN).restartCodeScanAndFetchJobStatus(project,sechubJobUUD);
        
        
        /* test */
        assertJobHasEnded(project,sechubJobUUD);
        
        AssertEventInspection.assertEventInspection().
        expect().
           /* 0 */
           asyncEvent(MessageID.REQUEST_JOB_RESTART).
                 from("com.daimler.sechub.domain.administration.job.JobRestartRequestService").
                 to("com.daimler.sechub.domain.schedule.ScheduleMessageHandler").
           /* 1 */
           asyncEvent(MessageID.JOB_RESTART_CANCELED).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
        /* assert + write */
        assertAsExpectedAndCreateHistoryFile(UseCaseIdentifier.UC_ADMIN_RESTARTS_JOB.name(),"accidently_restart_because_job_has_already_finished");
        
        /* @formatter:on */
    }

    @Test
    /**
     * We simulate a JVM crash where a product result was already written to
     * database.
     */
    public void UC_ADMIN_RESTARTS_JOB__simulate_jvm_crash_but_product_results_in_db() {
        /* @formatter:off */
        /* prepare */
        UUID sechubJobUUD = as(USER_1).triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(project);
        
        waitForJobDone(project, sechubJobUUD);
        
        simulateJobIsStillRunningAndUploadAvailable(sechubJobUUD);
        
        startEventInspection();

        /* execute */
        as(SUPER_ADMIN).restartCodeScanAndFetchJobStatus(project,sechubJobUUD);
        
        /* test */
        assertJobHasEnded(project,sechubJobUUD);
        AssertEventInspection.assertEventInspection().
        expect().
           /* 0 */
           asyncEvent(MessageID.REQUEST_JOB_RESTART).
                 from("com.daimler.sechub.domain.administration.job.JobRestartRequestService").
                 to("com.daimler.sechub.domain.schedule.ScheduleMessageHandler").
           /* 1 */
           asyncEvent(MessageID.JOB_RESTART_TRIGGERED).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
           /* 2 */
           asyncEvent(MessageID.JOB_STARTED).
                 from("com.daimler.sechub.domain.schedule.ScheduleJobLauncherService").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
           /* 3 */
           syncEvent(MessageID.START_SCAN).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor$1").
                 to("com.daimler.sechub.domain.scan.ScanService").
           /* 4 */
           syncEvent(MessageID.REQUEST_BATCH_JOB_STATUS).
                 from("com.daimler.sechub.domain.scan.ScanProgressMonitor").
                 to("com.daimler.sechub.domain.schedule.batch.SchedulerBatchJobStatusRequestHandler").
           /* 5 */
           asyncEvent(MessageID.JOB_DONE).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
        /* assert + write */
        assertAsExpectedAndCreateHistoryFile(UseCaseIdentifier.UC_ADMIN_RESTARTS_JOB.name(),"crashed_jvm_with_product_result");
        /* @formatter:on */
        
    }
    
    @Test
    /**
     * We simulate a JVM crash where NO product result was written to database.
     * Shall trigger JOB_RESTARTED
     */
    public void UC_ADMIN_RESTARTS_JOB__simulate_jvm_crash_no_product_results_in_db_upload_still_available() {
        /* @formatter:off */
        /* prepare */
        UUID sechubJobUUD = as(USER_1).triggerAsyncCodeScanGreenSuperFastWithPseudoZipUpload(project);
        
        waitForJobDone(project, sechubJobUUD);
        simulateJobIsStillRunningAndUploadAvailable(sechubJobUUD);
        
        destroyProductResults(sechubJobUUD); // destroy former product result to simulate execution crashed..
        
        startEventInspection();

        /* execute */
        as(SUPER_ADMIN).restartCodeScanAndFetchJobStatus(project,sechubJobUUD);
        
        /* test */
        assertJobHasEnded(project,sechubJobUUD);
        AssertEventInspection.assertEventInspection().
        expect().
           /* 0 */
           asyncEvent(MessageID.REQUEST_JOB_RESTART).
                 from("com.daimler.sechub.domain.administration.job.JobRestartRequestService").
                 to("com.daimler.sechub.domain.schedule.ScheduleMessageHandler").
           /* 1 */
           asyncEvent(MessageID.JOB_RESTART_TRIGGERED).
                 from("com.daimler.sechub.domain.schedule.SchedulerRestartJobService").
                 to("com.daimler.sechub.domain.notification.NotificationMessageHandler").
           /* 2 */
           asyncEvent(MessageID.JOB_STARTED).
                 from("com.daimler.sechub.domain.schedule.ScheduleJobLauncherService").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
           /* 3 */
           syncEvent(MessageID.START_SCAN).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor$1").
                 to("com.daimler.sechub.domain.scan.ScanService").
           /* 4 */
           syncEvent(MessageID.REQUEST_BATCH_JOB_STATUS).
                 from("com.daimler.sechub.domain.scan.ScanProgressMonitor").
                 to("com.daimler.sechub.domain.schedule.batch.SchedulerBatchJobStatusRequestHandler").
           /* 5 */
           asyncEvent(MessageID.JOB_DONE).
                 from("com.daimler.sechub.domain.schedule.batch.SynchronSecHubJobExecutor").
                 to("com.daimler.sechub.domain.administration.job.JobAdministrationMessageHandler").
        /* assert + write */
        assertAsExpectedAndCreateHistoryFile(UseCaseIdentifier.UC_ADMIN_RESTARTS_JOB.name(),"crashed_jvm_with_product_result");
        /* @formatter:on */
    }
    
    
    private void simulateJobIsStillRunningAndUploadAvailable(UUID sechubJobUUD) {
        assertNotNull(sechubJobUUD);
        assertJobHasEnded(project, sechubJobUUD);
        /*
         * in our test the zipfile has been destroyed before, because job has finished -
         * so we must upload again...
         */
        revertJobToStillNotApproved(sechubJobUUD); // make upload possible again...
        as(USER_1).upload(project, sechubJobUUD, "zipfile_contains_only_test1.txt.zip");
        revertJobToStillRunning(sechubJobUUD); // fake it's running
        assertJobIsRunning(project, sechubJobUUD);
    }

}
