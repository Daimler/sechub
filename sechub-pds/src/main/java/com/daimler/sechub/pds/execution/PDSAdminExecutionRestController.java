// SPDX-License-Identifier: MIT
package com.daimler.sechub.pds.execution;

import javax.annotation.security.RolesAllowed;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.daimler.sechub.pds.PDSAPIConstants;
import com.daimler.sechub.pds.security.PDSRoleConstants;
import com.daimler.sechub.pds.usecase.UseCaseAdminFetchesExecutionStatus;

/**
 * The REST API for PDS jobs
 *
 * @author Albert Tregnaghi
 *
 */
@RestController
@EnableAutoConfiguration
@RequestMapping(PDSAPIConstants.API_ADMIN)
@RolesAllowed({ PDSRoleConstants.ROLE_SUPERADMIN })
public class PDSAdminExecutionRestController {

    @Autowired
    private PDSExecutionService executionService;

    @Validated
    @RequestMapping(path = "execution/status", method = RequestMethod.GET)
    @UseCaseAdminFetchesExecutionStatus
    public PDSExecutionStatus getExecutionStatus() {
        return executionService.getExecutionStatus();
    }

}