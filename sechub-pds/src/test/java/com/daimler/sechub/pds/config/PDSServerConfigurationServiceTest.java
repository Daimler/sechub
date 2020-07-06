package com.daimler.sechub.pds.config;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.daimler.sechub.pds.PDSProductIdentifierValidator;

public class PDSServerConfigurationServiceTest {

    private PDSServerConfigurationService serviceToTest;
    private PDSProductIdentifierValidator productIdValidator;

    @Rule
    public ExpectedException expected = ExpectedException.none();


    @Before
    public void before() throws Exception {
        
        productIdValidator=mock(PDSProductIdentifierValidator.class);
        
        serviceToTest = new PDSServerConfigurationService();
        serviceToTest.productIdValidator=productIdValidator;
    }

    @Test
    public void pds_config_example1_can_be_loaded_and_contains_expected_data() {
        /* prepare */
        serviceToTest.pathToConfigFile = "./src/test/resources/config/pds-config-example1.json";
        
        /* execute */
        serviceToTest.postConstruct();
        
        /* test */
        PDSServerConfiguration serverConfiguration = serviceToTest.getServerConfiguration();
        assertNotNull(serverConfiguration);

        List<PDSProductSetup> products = serverConfiguration.getProducts();
        assertEquals(2, products.size());
        Iterator<PDSProductSetup> it = products.iterator();
        
        PDSProductSetup product1 = it.next();
        assertEquals("PRODUCT_1",product1.getId());
        assertEquals("/srv/security/scanner1.sh",product1.getPath());
        assertEquals(PDSScanType.CODE_SCAN,product1.getScanType());
        assertEquals("codescanner script needs environment variable ENV_CODESCAN_LEVEL set containing 1,2,3",product1.getDescription());
        
        PDSProdutParameterSetup paramSetup = product1.getParameters();
        assertNotNull(paramSetup);
        List<PDSProdutParameterDefinition> mandatory = paramSetup.getMandatory();
        assertEquals(2,mandatory.size());
        Iterator<PDSProdutParameterDefinition> mit = mandatory.iterator();
        PDSProdutParameterDefinition m1 = mit.next();
        PDSProdutParameterDefinition m2 = mit.next();
        
        assertEquals("product1.qualititycheck.enabled",m1.getKey());
        assertEquals("when 'true' quality scan results are added as well", m1.getDescription());
        assertEquals("product1.level",m2.getKey());
        assertEquals("numeric, 1-gets all, 2-only critical,fatal and medium, 3- only critical and fatal", m2.getDescription());
        
        List<PDSProdutParameterDefinition> optional = paramSetup.getOptional();
        assertEquals(1,optional.size());
        Iterator<PDSProdutParameterDefinition> oit = optional.iterator();
        PDSProdutParameterDefinition o1 = oit.next();
        assertEquals("product1.add.tipoftheday",o1.getKey());
        assertEquals("add tip of the day as info", o1.getDescription());
        
        
        PDSProductSetup product2 = it.next();
        assertEquals("PRODUCT_2",product2.getId());
        assertEquals(PDSScanType.INFRA_SCAN,product2.getScanType());
        assertEquals("/srv/security/scanner2.sh",product2.getPath());
        
    }
    
    @Test
    public void when_product_id_validator_says_id_is_not_valid_pds_config_example1_cannnot_be_loaded_but_illegal_state_exception_appears() {
        /* test */
        expected.expect(IllegalStateException.class);
        expected.expectMessage("reason");
        
        /* prepare */
        serviceToTest.pathToConfigFile = "./src/test/resources/config/pds-config-example1.json";
        when(productIdValidator.createValidationErrorMessage(any())).thenReturn("reason");
        
        /* execute */
        serviceToTest.postConstruct();
        
        
    }

}
