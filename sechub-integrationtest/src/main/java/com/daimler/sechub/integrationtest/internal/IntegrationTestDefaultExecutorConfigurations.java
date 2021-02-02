// SPDX-License-Identifier: MIT
package com.daimler.sechub.integrationtest.internal;

import java.util.List;

import com.daimler.sechub.integrationtest.api.PDSIntTestProductIdentifier;
import com.daimler.sechub.integrationtest.api.TestAPI;
import com.daimler.sechub.integrationtest.api.TestExecutorProductIdentifier;
import com.daimler.sechub.test.TestPortProvider;
import com.daimler.sechub.test.TestURLBuilder;
import com.daimler.sechub.test.executorconfig.TestExecutorConfig;
import com.daimler.sechub.test.executorconfig.TestExecutorSetupJobParam;

public class IntegrationTestDefaultExecutorConfigurations {
    
    public static final String VALUE_PRODUCT_LEVEL = "42";

    private static final String INTTEST_NAME_PREFIX = "INTTEST_";
    
    public static final TestExecutorConfig NETSPARKER_V1 = defineNetsparkerConfig();
    public static final TestExecutorConfig CHECKMARX_V1 = defineCheckmarxConfig();
    public static final TestExecutorConfig NESSUS_V1 = defineNessusConfig();
    
    public static final String PDS_CODESCAN_VARIANT_A="a";
    public static final String PDS_CODESCAN_VARIANT_B="b";
    public static final String PDS_CODESCAN_VARIANT_C="b";
    
    public static final TestExecutorConfig PDS_V1_CODE_SCAN_A = definePDSCodeScan(PDS_CODESCAN_VARIANT_A,false,PDSIntTestProductIdentifier.PDS_INTTEST_CODESCAN);
    public static final TestExecutorConfig PDS_V1_CODE_SCAN_B = definePDSCodeScan(PDS_CODESCAN_VARIANT_B,true,PDSIntTestProductIdentifier.PDS_INTTEST_CODESCAN);
    public static final TestExecutorConfig PDS_V1_CODE_SCAN_C = definePDSCodeScan(PDS_CODESCAN_VARIANT_C,true,null);// no identifier set, will not work...
    
    public static final String PDS_ENV_VARIABLENAME_TECHUSER_ID="TEST_PDS_TECHUSER_ID";
    public static final String PDS_ENV_VARIABLENAME_TECHUSER_APITOKEN="TEST_PDS_TECHUSER_APITOKEN";

    
    public static final String JOBPARAM_PDS_KEY_FOR_VARIANTNAME = "pds.test.key.variantname";
    
    private static TestExecutorConfig definePDSCodeScan(String variant, boolean credentialsAsEnvEntries,PDSIntTestProductIdentifier pdsProductIdentifier) {
        TestExecutorConfig config = new TestExecutorConfig();
        config.enabled=true;
        config.executorVersion=1;
        config.productIdentifier=TestExecutorProductIdentifier.PDS_CODESCAN.name();
        config.name=INTTEST_NAME_PREFIX+"pds_codescan_"+variant;
        config.setup.baseURL=TestURLBuilder.https(TestPortProvider.DEFAULT_INSTANCE.getIntegrationTestPDSPort()).pds().buildBaseUrl();
        config.uuid=null;// not initialized - is done at creation time by scenario initializer!
        if (credentialsAsEnvEntries) {
            config.setup.credentials.user="env:"+PDS_ENV_VARIABLENAME_TECHUSER_ID;
            config.setup.credentials.password="env:"+PDS_ENV_VARIABLENAME_TECHUSER_APITOKEN;
        }else {
            config.setup.credentials.user=TestAPI.PDS_TECH_USER.getUserId();
            config.setup.credentials.password=TestAPI.PDS_TECH_USER.getApiToken();
        }
        
        List<TestExecutorSetupJobParam> jobParameters = config.setup.jobParameters;
        jobParameters.add(new TestExecutorSetupJobParam("pds.config.productidentifier",pdsProductIdentifier != null ? pdsProductIdentifier.getId():"not-existing"));
        jobParameters.add(new TestExecutorSetupJobParam("pds.productexecutor.trustall.certificates","true")); // accept for testing
        jobParameters.add(new TestExecutorSetupJobParam("pds.productexecutor.timetowait.nextcheck.minutes","0")); // speed up tests...
        jobParameters.add(new TestExecutorSetupJobParam("product1.qualititycheck.enabled","true")); // mandatory from PDS integration test server
        jobParameters.add(new TestExecutorSetupJobParam("product1.level",VALUE_PRODUCT_LEVEL)); // mandatory from PDS integration test server
        jobParameters.add(new TestExecutorSetupJobParam(JOBPARAM_PDS_KEY_FOR_VARIANTNAME,variant));
        return config;
    }
    
    private static TestExecutorConfig defineNetsparkerConfig() {
        TestExecutorConfig config = new TestExecutorConfig();
        config.enabled=true;
        config.executorVersion=1;
        config.productIdentifier=TestExecutorProductIdentifier.NETSPARKER.name();
        config.name=INTTEST_NAME_PREFIX+"Netsparker V1";
        config.setup.baseURL="https://netsparker.example.com";
        config.uuid=null;// not initialized - is done at creation time by scenario initializer!
        return config;
    }
    
    private static TestExecutorConfig defineCheckmarxConfig() {
        TestExecutorConfig config = new TestExecutorConfig();
        config.enabled=true;
        config.executorVersion=1;
        config.productIdentifier=TestExecutorProductIdentifier.CHECKMARX.name();
        config.name=INTTEST_NAME_PREFIX+"Checkmarx V1";
        config.setup.baseURL="https://checkmarx.example.com";
        config.uuid=null;// not initialized - is done at creation time by scenario initializer!
        return config;
    }
    
    private static TestExecutorConfig defineNessusConfig() {
        TestExecutorConfig config = new TestExecutorConfig();
        config.enabled=true;
        config.executorVersion=1;
        config.productIdentifier=TestExecutorProductIdentifier.NESSUS.name();
        config.name=INTTEST_NAME_PREFIX+"Nessus V1";
        config.setup.baseURL="https://nessus.example.com";
        config.uuid=null;// not initialized - is done at creation time by scenario initializer!
        return config;
    }
}
