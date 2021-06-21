// SPDX-License-Identifier: MIT
package com.daimler.sechub.sarif.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "driver" })
public class Tool {
    private Driver driver;

    public Tool() {
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    @Override
    public String toString() {
        return "Tool [driver=" + driver + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(driver);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tool other = (Tool) obj;
        return Objects.equals(driver, other.driver);
    }
}