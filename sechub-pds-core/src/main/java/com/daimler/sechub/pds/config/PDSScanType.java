package com.daimler.sechub.pds.config;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PDSScanType {

    CODE_SCAN("codeScan"),

    WEB_SCAN("webScan"),

    INFRA_SCAN("infraScan"),

    ;

    private String id;

    private PDSScanType(String id) {
        this.id = id;
    }

    @JsonValue
    public String getId() {
        return id;
    }
}
