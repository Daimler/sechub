// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.schedule.batch;

import static com.daimler.sechub.sharedkernel.util.Assert.*;

import java.util.UUID;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daimler.sechub.sharedkernel.messaging.BatchJobMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessage;
import com.daimler.sechub.sharedkernel.messaging.DomainMessageSynchronousResult;
import com.daimler.sechub.sharedkernel.messaging.IsRecevingSyncMessage;
import com.daimler.sechub.sharedkernel.messaging.IsSendingSyncMessageAnswer;
import com.daimler.sechub.sharedkernel.messaging.MessageDataKeys;
import com.daimler.sechub.sharedkernel.messaging.MessageID;
import com.daimler.sechub.sharedkernel.messaging.SynchronMessageHandler;

@Component
public class SchedulerBatchJobStatusRequestHandler implements SynchronMessageHandler {

    @Autowired
    JobExplorer explorer;

    @Override
    @IsRecevingSyncMessage(MessageID.REQUEST_BATCH_JOB_STATUS)
    public DomainMessageSynchronousResult receiveSynchronMessage(DomainMessage request) {
        notNull(request, "Request may not be null!");

        if (!request.hasID(MessageID.REQUEST_BATCH_JOB_STATUS)) {
            return new DomainMessageSynchronousResult(MessageID.UNSUPPORTED_OPERATION,
                    new UnsupportedOperationException("Can only handle " + MessageID.REQUEST_BATCH_JOB_STATUS));
        }
        return returnStatus(request);
    }

    @IsSendingSyncMessageAnswer(value = MessageID.BATCH_JOB_STATUS, answeringTo = MessageID.REQUEST_BATCH_JOB_STATUS, branchName = "success")
    private DomainMessageSynchronousResult returnStatus(DomainMessage request) {
        DomainMessageSynchronousResult result = new DomainMessageSynchronousResult(MessageID.BATCH_JOB_STATUS);
        BatchJobMessage batchJobMessage = request.get(MessageDataKeys.BATCH_JOB_STATUS);

        UUID sechubBatchJobUUID = batchJobMessage.getSechubJobUUID();
        long batchJobId = batchJobMessage.getBatchJobId();

        BatchJobMessage status = new BatchJobMessage();
        status.setBatchJobId(batchJobId);
        status.setSecHubJobUUID(sechubBatchJobUUID);

        /* find */
        JobExecution jobExecution = explorer.getJobExecution(batchJobId);
        if (jobExecution == null) {
            status.setExisting(false);
        } else {
            BatchStatus batchStatus = jobExecution.getStatus();
            status.setCanceled(checkRepresentsCancel(batchStatus));
            status.setAbandoned(checkRepresentsAbandoned(batchStatus));
            status.setExisting(true);
        }
        result.set(MessageDataKeys.BATCH_JOB_STATUS, status);

        return result;
    }

    private boolean checkRepresentsAbandoned(BatchStatus batchStatus) {
        switch (batchStatus) {
        case ABANDONED:
            return true;
        default:
            return false;
        } 
    }

    private boolean checkRepresentsCancel(BatchStatus batchStatus) {
        switch (batchStatus) {
        case STOPPED:
            return true;
        case STOPPING:
            return true;
        case ABANDONED:
            return true;
        default:
            return false;
        } 
    }

}
