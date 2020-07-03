package com.daimler.sechub.pds.job;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.daimler.sechub.pds.PDSNotAcceptableException;
import com.daimler.sechub.pds.PDSProductIdentifierValidator;

@Component
public class PDSJobConfigurationValidator {

    private static final Logger LOG = LoggerFactory.getLogger(PDSJobConfigurationValidator.class);
    
    @Autowired
    PDSProductIdentifierValidator productIdentifierValidator;

    public void assertPDSConfigurationValid(PDSJobConfiguration configuration) {
       String message = createValidationErrorMessage(configuration);
       if (message==null) {
           return;
       }
       LOG.warn("pds job configuration not valid - message:{}", message);

       throw new PDSNotAcceptableException("Configuration invalid:"+message);
        
    }
    private String createValidationErrorMessage(PDSJobConfiguration configuration) {
        if (configuration==null) {
            return "may not be null!";
        }
        if (configuration.getSechubJobUUID()==null) {
            return "sechub job UUID not set!";
        }
        String productId = configuration.getProductId();
        String productIdErrorMessage = productIdentifierValidator.createValidationErrorMessage(productId);
        if (productIdErrorMessage!=null) {
            return productIdErrorMessage;
        }
        return null;
    }
}
