// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.report;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ScanReportTransactionService {

    @Autowired
    ScanReportRepository reportRepository;
    
    public void deleteAllReportsForSecHubJobUUIDinOwnTransaction(UUID sechubJobUUID) {
        reportRepository.deleteAllReportsForSecHubJobUUID(sechubJobUUID);
    }
    
}
