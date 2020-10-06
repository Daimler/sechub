package com.daimler.sechub.domain.scan.product.config;

import static com.daimler.sechub.sharedkernel.validation.AssertValidation.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.security.RolesAllowed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.daimler.sechub.sharedkernel.Profiles;
import com.daimler.sechub.sharedkernel.RoleConstants;
import com.daimler.sechub.sharedkernel.Step;
import com.daimler.sechub.sharedkernel.error.NotFoundException;
import com.daimler.sechub.sharedkernel.logging.AuditLogService;
import com.daimler.sechub.sharedkernel.usecases.admin.config.UseCaseAdministratorUpdatesExecutorConfig;

@RolesAllowed(RoleConstants.ROLE_SUPERADMIN)
@Profile(Profiles.ADMIN_ACCESS)
@Service
public class UpdateProductExecutionProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateProductExecutionProfileService.class);

    @Autowired
    ProductExecutionProfileRepository repository;
    
    @Autowired
    ProductExecutorConfigRepository configRepository;

    @Autowired
    ProductExecutionProfileValidation validation;
    
    @Autowired
    AuditLogService auditLogService;

    /* @formatter:off */
    @UseCaseAdministratorUpdatesExecutorConfig(
            @Step(number = 1, 
            name = "Service call", 
            description = "Service updates existing executor configuration"))
    /* @formatter:on */
    public void updateProductExecutorSetup(String profileId, ProductExecutionProfile profileFromUser) {
        auditLogService.log("Wants to update product execution configuration setup for executor:{}", profileId);
        
        Optional<ProductExecutionProfile> opt = repository.findById(profileId);
        if (! opt.isPresent()) {
            throw new NotFoundException("No profile found with id:"+profileId);
        }
        profileFromUser.id=profileId;
        assertValid(profileFromUser, validation);

        ProductExecutionProfile stored = mergeFromUserIntoEntity(profileId, profileFromUser, opt);
        
        repository.save(stored);
        
        LOG.info("Updated product execution profile {}", profileId);
        
        
    }

    private ProductExecutionProfile mergeFromUserIntoEntity(String profileId, ProductExecutionProfile profileFromUser, Optional<ProductExecutionProfile> opt) {
        ProductExecutionProfile stored = opt.get();
        stored.description=profileFromUser.description;
        stored.enabled=profileFromUser.enabled;
        
        stored.projectIds.clear();
        stored.projectIds.addAll(profileFromUser.getProjectIds());
        
        stored.configurations.clear();
        
        Set<ProductExecutorConfig> configurationsFromUser = profileFromUser.getConfigurations();
        for (ProductExecutorConfig configFromUser: configurationsFromUser) {
             UUID uuid = configFromUser.getUUID();
             Optional<ProductExecutorConfig> found = configRepository.findById(uuid);
             if (! found.isPresent()) {
                 LOG.warn("Found no configuration with uuid:{}, so cannot add to profile:{}",uuid,profileId);
                 continue;
             }
             stored.configurations.add(found.get());
        }
        return stored;
    }

}
