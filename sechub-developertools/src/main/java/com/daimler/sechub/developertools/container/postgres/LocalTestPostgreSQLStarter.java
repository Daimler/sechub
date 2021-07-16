// SPDX-License-Identifier: MIT
package com.daimler.sechub.developertools.container.postgres;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalTestPostgreSQLStarter {

    private static final String DEFAULT_FIXED_PORT = "49152";

    private static final Logger LOG = LoggerFactory.getLogger(LocalTestPostgreSQLStarter.class);
    private Path filePath;

    private PostgreSQLTestContainer container;

    /**
     * Starts a local postgres test container. <b>`test`</b>
     * 
     * @param args optional parts in following order:
     * 
     *             <pre>
     *  ${port} ${username} ${password} ${dbName} ${imageName}
     *             </pre>
     * 
     *             When port is -1, the test container will start with a random port
     *             number. When no parameter is defined port will be
     *             {@value #DEFAULT_FIXED_PORT}. When no user is set the user will
     *             be auto generated. A defined user name must have at least 8
     *             characters. A password must be at last 12 characters long. If not
     *             defined a strong password will be generated. The psql version is
     *             the default postgres image from alpine linux base image defined inside
     *             sechub-developertools/scripts/container/postgres/Dockerfile
     *             {@value #DEFAULt_POSTGRES_IMAGE}e
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String testPort = DEFAULT_FIXED_PORT;
        String testUserName = null;
        String testPassword = null;
        String testDatabaseName = null;

        if (args.length > 0) {
            testPort = args[0];
        }
        if (args.length > 1) {
            testUserName = args[1];
        }
        if (args.length > 2) {
            testPassword = args[2];
        }
        if (args.length > 3) {
            testDatabaseName = args[3];
        }

        if (testUserName == null) {
            testUserName = UUID.randomUUID().toString();
        }

        if (testPassword == null) {
            testPassword = UUID.randomUUID().toString();
        }
        if (testDatabaseName == null) {
            testDatabaseName = "test";
        }

        assertLength(testUserName, "Username has not min length", 8);
        assertLength(testPassword, "Password has not min length", 20);

        int postgresContainerPort = Integer.parseInt(testPort);

        new LocalTestPostgreSQLStarter().start(postgresContainerPort, testUserName, testPassword, testDatabaseName);
    }

    private static void assertLength(String toInspect, String hint, int minLength) {
        if (toInspect.length() < minLength) {
            throw new IllegalArgumentException(hint + ". Min length:" + minLength + ", found:" + toInspect.length());
        }

    }

    private void start(int port, String testUserName, String testPassword, String testDatabaseName) throws Exception {
        Runtime.getRuntime().addShutdownHook(createShutdownHookThread());

        container = new PostgreSQLTestContainer(port, testUserName, testPassword, testDatabaseName);
        initializeDirectoryAndFiles(port);

        LOG.info("start postgres local on port:{}", port);
        container.start();

        createInfoFile(container);

        Thread inspectThread = new Thread(new InspectStillWantedToRun(), "Inspect still wanted to run");
        inspectThread.start();

    }

    private Thread createShutdownHookThread() {
        return new Thread(new InspectShutdown(), "Inspect shutdown");
    }

    private void createInfoFile(PostgreSQLTestContainer container) throws IOException {
        Path created = Files.createFile(filePath);

        StringBuilder sb = new StringBuilder();
        sb.append("#!/usr/bin/bash\n");
        sb.append("\nPOSTGRES_DB_URL=" + container.getJdbcUrl());
        sb.append("\nPOSTGRES_DB_USERNAME=" + container.getUsername());
        sb.append("\nPOSTGRES_DB_PASSWORD=" + container.getPassword());

        Files.write(created, sb.toString().getBytes());
    }

    private void initializeDirectoryAndFiles(int port) throws Exception {
        String fileName = "postgres_container_" + port + ".info";
        File folder = new File("build/tmp");
        if (!folder.exists()) {
            Files.createDirectories(folder.toPath());
        }
        File file = new File(folder, fileName);
        filePath = file.toPath();

        dropOldInfoFileAndWaitIfNecessary();
    }

    private void dropOldInfoFileAndWaitIfNecessary() throws IOException, InterruptedException {
        if (Files.exists(filePath)) {
            LOG.warn("Found existing container info file:{}, will delte old file and wait for other container shutdown!", filePath);
            // other instance running
            Files.delete(filePath);
            LOG.warn("Deleted file:{}, waiting for other container shutdown!", filePath);
            // we wait some seconds to give other instance the possibility to shutdown
            Thread.sleep(4000);
        }
    }

    private class InspectStillWantedToRun implements Runnable {

        @Override
        public void run() {
            do {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (Files.exists(filePath));
            
            try {
                container.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
            

        }

    }

    private class InspectShutdown implements Runnable {

        @Override
        public void run() {
            if (filePath != null) {
                try {
                    // cleanup info file
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

}
