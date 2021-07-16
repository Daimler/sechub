// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.container.postgres;

import java.io.File;
import java.io.IOException;

import com.daimler.sechub.developertools.container.BashScriptContainerLauncher;
import com.daimler.sechub.developertools.container.BashScriptContainerLaunchConfig;

public class PostgreSQLTestContainer {

    private int testPort;
    private String username;
    private String password;
    private String databaseName;
    private BashScriptContainerLaunchConfig config;
    private BashScriptContainerLauncher launcher;

    /**
     * Creates a postgres test container with wanted exposed port
     * 
     * @param exposedPostgresPort port to be exposed. use -1 when you want a random
     *                            one
     * @param imageVersion2
     */
    public PostgreSQLTestContainer(int exposedPostgresPort, String userName, String password, String databaseName) {
        this.testPort = exposedPostgresPort;
        this.username = userName;
        this.databaseName = databaseName;
        this.password = password;
        launcher = new BashScriptContainerLauncher();
        
    }
    
    public void start() throws IOException {
        File file = new File("./scripts/container/postgres/start.sh");
        config = new BashScriptContainerLaunchConfig(file.toPath());

        config.getEnvironment().put("POSTGRES_DB_USER", username);
        config.getEnvironment().put("POSTGRES_DB_PASSWORD", password);
        config.getEnvironment().put("POSTGRES_DB_NAME", databaseName);

        config.getParameters().add("" + testPort);
        
        launcher.start(config);
    }

    public void stop() throws IOException {
        File file = new File("./scripts/container/postgres/stop.sh");
        config = new BashScriptContainerLaunchConfig(file.toPath());
        
        config.getParameters().add("" + testPort);
        
        launcher.start(config);

    }
    
    public String getDatabaseName() {
        return databaseName;
    }

    public String getUsername() {
        return username;
    }

    
    public String getPassword() {
        return password;
    }

    public String getJdbcUrl() {
        return "jdbc:postgresql://127.0.0.1:" + testPort + "/" + databaseName + "?loggerLevel=OFF";
    }


}