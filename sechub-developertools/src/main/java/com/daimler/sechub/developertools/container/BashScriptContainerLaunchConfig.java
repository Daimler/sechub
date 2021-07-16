package com.daimler.sechub.developertools.container;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BashScriptContainerLaunchConfig {
    
    private Path pathToScript;
    private Map<String, String> environment = new HashMap<>();
    private List<String> parameters = new ArrayList<>();

    public BashScriptContainerLaunchConfig(Path pathToScript) {
        this.pathToScript = pathToScript;
    }

    public Path getPathToScript() {
        return pathToScript;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<String> getParameters() {
        return parameters;
    }

}