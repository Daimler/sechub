package com.daimler.sechub.integrationtest.api;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import com.daimler.sechub.integrationtest.JSONTestSupport;
import com.daimler.sechub.integrationtest.internal.IntegrationTestFileSupport;
import com.daimler.sechub.integrationtest.internal.SecHubClientExecutor.ExecutionResult;
import com.daimler.sechub.sharedkernel.type.TrafficLight;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class AssertSecHubReport {
    private JSONTestSupport jsonTestSupport = JSONTestSupport.DEFAULT;
    private JsonNode jsonObj;

    private AssertSecHubReport(String json) {
        try {
            jsonObj = jsonTestSupport.fromJson(json);
        } catch (IOException e) {
           throw new RuntimeException("Not able to read json obj",e);
        }
    }

    public static AssertSecHubReport assertSecHubReport(String json) {
        return new AssertSecHubReport(json);
    }

    public static AssertSecHubReport assertSecHubReport(ExecutionResult result) {
        File file = result.getJSONReportFile();
        String json = IntegrationTestFileSupport.getTestfileSupport().loadTextFile(file, "\n");
        return new AssertSecHubReport(json);
    }
    
    public AssertSecHubReport containsFinding(int findingId, String findingName) {
        assertReportContainsFindingOrNot(findingId, findingName, true);
        return this;
    }

    public AssertSecHubReport containsNotFinding(int findingId, String findingName) {
        assertReportContainsFindingOrNot(findingId, findingName, false);
        return this;
    }
    
    public AssertSecHubReport hasTrafficLight(TrafficLight trafficLight) {
        JsonNode r = jsonObj.get("trafficLight");
        String trText = r.asText();
        assertEquals(trafficLight,TrafficLight.fromString(trText));
        return this;
    }

    private void assertReportContainsFindingOrNot(int findingId, String findingName, boolean expectedToBeFound) {
        JsonNode r = jsonObj.get("result");
        JsonNode f = r.get("findings");
        ArrayNode findings = (ArrayNode) f;
        JsonNode found = null;
        for (int i = 0; i < findings.size(); i++) {
            JsonNode finding = findings.get(i);

            String foundName = finding.get("name").asText();
            int foundFindingId = finding.get("id").asInt();

            if (!foundName.equals(findingName)) {
                continue;
            }
            if (foundFindingId != findingId) {
                continue;
            }
            found = finding;
            break;
        }
        if (found == null && expectedToBeFound) {
            fail("Not found finding");
        } else if (found != null && !expectedToBeFound) {
            fail("Did found entry:" + found.toPrettyString());
        }
    }
}
