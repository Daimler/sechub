package com.daimler.sechub.developertools.container;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BashScriptContainerLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(BashScriptContainerLauncher.class);

    public final void start(BashScriptContainerLaunchConfig config) throws IOException {
        Objects.requireNonNull(config);
        
        Path pathToScript = config.getPathToScript();
        Objects.requireNonNull(pathToScript);
        if (!Files.exists(pathToScript)) {
            throw new IllegalStateException("Does not exist:" + pathToScript);
        }
        if (!Files.isExecutable(pathToScript)) {
            throw new IllegalStateException("Does exist, but is not executable:" + pathToScript);
        }

        List<String> commands = new ArrayList<>();
        commands.add("bash");
        commands.add(pathToScript.toFile().getName());
        commands.addAll(config.getParameters());

        ProcessBuilder sb = new ProcessBuilder(commands);
        sb.directory(pathToScript.getParent().toFile());
        sb.environment().putAll(config.getEnvironment());
        sb.inheritIO();

        try {
            Process process = sb.start();
            boolean subProcessExited = process.waitFor(2, TimeUnit.MINUTES);
            LOG.info("processed exited:{}",subProcessExited);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new IOException("Was not able to start container", e);
        }

    }

}
