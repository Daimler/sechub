// SPDX-License-Identifier: MIT
package com.daimler.sechub.sharedkernel.messaging;

import java.util.UUID;

import com.daimler.sechub.commons.model.JSONable;
import com.daimler.sechub.sharedkernel.MustBeKeptStable;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * This message data object contains all possible information about a spring
 * batch job
 *
 * @author Albert Tregnaghi
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true) // we do ignore to avoid problems from wrong configured values!
@MustBeKeptStable("This configuration is used by communication between (api) schedule domain and administration - and maybe others")
public class BatchJobMessage implements JSONable<BatchJobMessage> {

    private UUID sechubJobUUID;
    private long batchJobId;
    private boolean canceled;
    private boolean existing;
    private boolean abandoned;

    @Override
    public Class<BatchJobMessage> getJSONTargetClass() {
        return BatchJobMessage.class;
    }

    public void setBatchJobId(long batchJobId) {
        this.batchJobId = batchJobId;
    }

    public long getBatchJobId() {
        return batchJobId;
    }

    public void setSecHubJobUUID(UUID jobUUID) {
        this.sechubJobUUID = jobUUID;
    }

    public UUID getSechubJobUUID() {
        return sechubJobUUID;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setExisting(boolean exists) {
        this.existing = exists;
    }

    public boolean isExisting() {
        return existing;
    }

    public void setAbandoned(boolean abandoned) {
        this.abandoned = abandoned;
    }

    public boolean isAbandoned() {
        return abandoned;
    }

}
