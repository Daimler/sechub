// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.schedule.batch;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daimler.sechub.domain.schedule.ExecutionResult;
import com.daimler.sechub.domain.schedule.job.ScheduleSecHubJob;
import com.daimler.sechub.sharedkernel.LogConstants;
import com.daimler.sechub.sharedkernel.messaging.BatchJobMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageService;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageSynchronousResult;
import com.daimler.sechub.sharedkernel.messaging.IsSendingAsyncMessage;
import com.daimler.sechub.sharedkernel.messaging.IsSendingSyncMessage;
import com.daimler.sechub.sharedkernel.messaging.JobMessage;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKey;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKeys;
import com.daimler.sechub.sharedkernel.messaging.MessageID;

class SynchronSecHubJobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(SynchronSecHubJobExecutor.class);
    private DomainMessageService messageService;
    private SecHubJobSafeUpdater secHubJobSafeUpdater;

    public SynchronSecHubJobExecutor(DomainMessageService messageService, SecHubJobSafeUpdater secHubJobSafeUpdater) {
        this.secHubJobSafeUpdater = secHubJobSafeUpdater;
        this.messageService = messageService;
    }

    @IsSendingSyncMessage(MessageID.START_SCAN)
    public void execute(final ScheduleSecHubJob secHubJob, final Long batchJobId) {
        Thread scanThread = new Thread(new Runnable() {

            @Override
            public void run() {
                UUID secHubJobUUID = secHubJob.getUUID();
                String secHubJobUUIDAsString = secHubJobUUID.toString();

                try {
                    String secHubConfiguration = secHubJob.getJsonConfiguration();

                    /* own thread so MDC.put necessary */
                    MDC.clear();
                    MDC.put(LogConstants.MDC_SECHUB_JOB_UUID, secHubJobUUIDAsString);
                    MDC.put(LogConstants.MDC_SECHUB_PROJECT_ID, secHubJob.getProjectId());

                    LOG.info("Executing sechub job: {}", secHubJobUUIDAsString);

                    /* we send no a synchronous SCAN event */
                    DomainMessage request = new DomainMessage(MessageID.START_SCAN);
                    request.set(MessageDataKeys.EXECUTED_BY, secHubJob.getOwner());
                    request.set(MessageDataKeys.SECHUB_UUID, secHubJobUUID);
                    request.set(MessageDataKeys.SECHUB_CONFIG, MessageDataKeys.SECHUB_CONFIG.getProvider().get(secHubConfiguration));

                    BatchJobMessage batchJobIdMessage = new BatchJobMessage();
                    batchJobIdMessage.setBatchJobId(batchJobId);
                    batchJobIdMessage.setSecHubJobUUID(secHubJobUUID);
                    request.set(MessageDataKeys.BATCH_JOB_ID, batchJobIdMessage);

                    /* wait for scan event result - synchron */
                    DomainMessageSynchronousResult response = messageService.sendSynchron(request);

                    updateSecHubJob(secHubJobUUID, response);

                    sendJobDoneMessageWhenNotAbandonded(secHubJobUUID, response);

                } catch (Exception e) {
                    LOG.error("Error happend at spring batch task execution:" + e.getMessage(), e);

                    markSechHubJobFailed(secHubJobUUID);
                    sendJobFailed(secHubJobUUID);

                } finally {
                    /* cleanup MDC */
                    MDC.clear();
                }

            }
        }, "scan_" + secHubJob.getUUID());
        scanThread.start();
    }

    private void sendJobDoneMessageWhenNotAbandonded(UUID secHubJobUUID, DomainMessageSynchronousResult response) {
        if (MessageID.SCAN_ABANDONDED.equals(response.getMessageId())) {
            LOG.info("Will not send job done message, because scan was abandoned");
            return;
        }
        LOG.debug("Will send job done message for: {}", secHubJobUUID);
        sendJobDone(secHubJobUUID);
    }

    private void markSechHubJobFailed(UUID secHubJobUUID) {
        updateSecHubJob(secHubJobUUID, ExecutionResult.FAILED, null);

        LOG.info("marked job as failed:{}", secHubJobUUID);
    }

    private void updateSecHubJob(UUID secHubUUID, DomainMessageSynchronousResult response) {
        ExecutionResult result;
        if (MessageID.SCAN_ABANDONDED.equals(response.getMessageId())) {
            /*
             * Abandon happens normally only, when doing a restart or an hard internal
             * cancel. In both situations, the SecHub job execution result inside scheduler
             * is already set before, and state will also be changed to CANCELED, or on
             * restart to RUNNING
             */
            LOG.info("Ignore sechub job update, because scan was abandoned");
            return;
        }
        if (response.hasFailed()) {
            result = ExecutionResult.FAILED;
        } else {
            result = ExecutionResult.OK;
        }
        String trafficLightString = response.get(MessageDataKeys.REPORT_TRAFFIC_LIGHT);
        updateSecHubJob(secHubUUID, result, trafficLightString);
    }

    private void updateSecHubJob(UUID secHubUUID, ExecutionResult result, String trafficLightString) {
        secHubJobSafeUpdater.safeUpdateOfSecHubJob(secHubUUID, result, trafficLightString);
    }

    @IsSendingAsyncMessage(MessageID.JOB_DONE)
    private void sendJobDone(UUID jobUUID) {
        sendJobInfo(MessageDataKeys.JOB_DONE_DATA, jobUUID, MessageID.JOB_DONE);
    }

    @IsSendingAsyncMessage(MessageID.JOB_FAILED)
    private void sendJobFailed(UUID jobUUID) {
        sendJobInfo(MessageDataKeys.JOB_FAILED_DATA, jobUUID, MessageID.JOB_FAILED);
    }

    private void sendJobInfo(MessageDataKey<JobMessage> key, UUID jobUUID, MessageID id) {
        DomainMessage request = new DomainMessage(id);
        JobMessage message = createMessage(jobUUID);

        request.set(key, message);

        messageService.sendAsynchron(request);
    }

    private JobMessage createMessage(UUID jobUUID) {
        JobMessage message = new JobMessage();
        message.setJobUUID(jobUUID);
        message.setSince(LocalDateTime.now());
        return message;
    }

}