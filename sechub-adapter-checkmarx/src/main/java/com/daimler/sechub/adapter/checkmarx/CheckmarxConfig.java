// SPDX-License-Identifier: MIT
package com.daimler.sechub.adapter.checkmarx;

import java.io.InputStream;

import com.daimler.sechub.adapter.AbstractCodeScanAdapterConfig;
import com.daimler.sechub.adapter.AbstractCodeScanAdapterConfigBuilder;

public class CheckmarxConfig extends AbstractCodeScanAdapterConfig implements CheckmarxAdapterConfig {

    /**
     * This is the "client secret" is listed at <a href=
     * "https://checkmarx.atlassian.net/wiki/spaces/KC/pages/1187774721/Using+the+CxSAST+REST+API+v8.6.0+and+up"
     * >public Checkmarx documentation</a>
     *
     * Being not really a secret but just a visible constant in public space it's
     * okay to contain this inside code.
     */
    public static final String DEFAULT_CLIENT_SECRET = "014DF517-39D1-4453-B7B3-9930C563627C";

    private String teamIdForNewProjects;
    private InputStream sourceCodeZipFileInputStream;
    public Long presetIdForNewProjects;
    private String clientSecret;// client secret just ensures it is a checkmarx instance - we use default value,
                                // but we make it configurable if this changes ever in future
    
    private String engineConfigurationName;

    private CheckmarxConfig() {
    }

    @Override
    public String getTeamIdForNewProjects() {
        return teamIdForNewProjects;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    public Long getPresetIdForNewProjectsOrNull() {
        return presetIdForNewProjects;
    }

    @Override
    public InputStream getSourceCodeZipFileInputStream() {
        return sourceCodeZipFileInputStream;
    }

    @Override
    public String getEngineConfigurationName() {
        return engineConfigurationName;
    }
    
    public static CheckmarxConfigBuilder builder() {
        return new CheckmarxConfigBuilder();
    }

    public static class CheckmarxConfigBuilder extends AbstractCodeScanAdapterConfigBuilder<CheckmarxConfigBuilder, CheckmarxConfig> {

        private String teamIdForNewProjects;
        private Long presetIdForNewProjects;
        private InputStream sourceCodeZipFileInputStream;
        
        private String clientSecret = DEFAULT_CLIENT_SECRET; // per default use default client secret

        private String engineConfigurationName = CheckmarxEngineConfigurationOptions.DEFAULT_CHECKMARX_ENGINECONFIGURATION_MULTILANGANGE_SCAN_NAME;
        
        /**
         * When we create a new project this is the team ID to use
         * 
         * @param teamId
         * @return
         */
        public CheckmarxConfigBuilder setTeamIdForNewProjects(String teamId) {
            this.teamIdForNewProjects = teamId;
            return this;
        }

        public CheckmarxConfigBuilder setClientSecret(String newClientSecret) {
            this.clientSecret = newClientSecret;
            return this;
        }
        
        public CheckmarxConfigBuilder setEngineConfigurationName(String engineConfigurationName) {
            this.engineConfigurationName = engineConfigurationName;
            return this;
        }

        /**
         * When we create a new project this is the team ID to use
         * 
         * @param teamId
         * @return
         */
        public CheckmarxConfigBuilder setPresetIdForNewProjects(Long presetId) {
            this.presetIdForNewProjects = presetId;
            return this;
        }

        public CheckmarxConfigBuilder setSourceCodeZipFileInputStream(InputStream sourceCodeZipFileInputStream) {
            this.sourceCodeZipFileInputStream = sourceCodeZipFileInputStream;
            return this;
        }

        @Override
        protected void customBuild(CheckmarxConfig config) {
            config.teamIdForNewProjects = teamIdForNewProjects;
            config.presetIdForNewProjects = presetIdForNewProjects;
            config.sourceCodeZipFileInputStream = sourceCodeZipFileInputStream;
            config.clientSecret = clientSecret;
            config.engineConfigurationName = engineConfigurationName;
        }

        @Override
        protected CheckmarxConfig buildInitialConfig() {
            return new CheckmarxConfig();
        }

        @Override
        protected void customValidate() {
            assertUserSet();
            assertPasswordSet();
            assertProjectIdSet();
            assertTeamIdSet();
        }

        protected void assertTeamIdSet() {
            if (teamIdForNewProjects == null) {
                throw new IllegalStateException("no team id given");
            }
        }
    }
}
