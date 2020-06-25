package com.daimler.sechub.pds.job;

import static com.daimler.sechub.pds.job.PDSJobAssert.*;
import static com.daimler.sechub.pds.util.PDSAssert.*;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daimler.sechub.pds.execution.PDSExecutionService;

@Service
public class PDSCancelJobService {

    private static final Logger LOG = LoggerFactory.getLogger(PDSCancelJobService.class);

    @Autowired
    PDSJobRepository repository;

    @Autowired
    PDSExecutionService executionService;

    public void cancelJob(UUID jobUUID) {
        notNull(jobUUID, "job uuid may not be null!");

        PDSJob job = assertJobFound(jobUUID, repository);
        if (PDSJobStatusState.CANCEL_REQUESTED.equals(job.getState()) || PDSJobStatusState.CANCELED.equals(job.getState())) {
            LOG.info("Cancel ignored because in state:{}", job.getState());
            return;
        }
        assertJobIsInState(job, PDSJobStatusState.RUNNING);

        executionService.cancel(jobUUID);

        job.setState(PDSJobStatusState.CANCEL_REQUESTED);

        repository.save(job);

    }

}
