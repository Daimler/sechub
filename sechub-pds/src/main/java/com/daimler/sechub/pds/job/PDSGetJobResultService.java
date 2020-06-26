package com.daimler.sechub.pds.job;

import static com.daimler.sechub.pds.job.PDSJobAssert.*;
import static com.daimler.sechub.pds.util.PDSAssert.*;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.daimler.sechub.pds.usecase.UseCaseUserFetchesJobResult;

@Service
public class PDSGetJobResultService {
    
    @Autowired
    PDSJobRepository repository;

    @UseCaseUserFetchesJobResult
    public String getJobResult(UUID jobUUID) {
        notNull(jobUUID, "job uuid may not be null!");
        
        PDSJob job = assertJobFound(jobUUID,repository);
        assertJobIsInState(job,PDSJobStatusState.DONE);
        return job.getResult();
    }

}