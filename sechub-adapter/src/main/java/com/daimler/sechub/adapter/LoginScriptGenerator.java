package com.daimler.sechub.adapter;

import java.util.List;

public interface LoginScriptGenerator {

	public String generate(List<LoginScriptStep> steps);
}
