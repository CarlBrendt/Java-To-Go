package ru.mts.ip.workflow.engine.testutils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import io.micrometer.core.instrument.util.IOUtils;

public class PlantUmlRepository {

	public Optional<String> findDefinition(String name) {
		String resourceName = "/plunt/%s".formatted(name);
		InputStream st = JsonWorkflowDefinitionRepository.class.getResourceAsStream(resourceName);
		if(st != null) {
			return Optional.of(IOUtils.toString(st, StandardCharsets.UTF_8));
		} else {
			return Optional.empty();
		}
	}
	
}