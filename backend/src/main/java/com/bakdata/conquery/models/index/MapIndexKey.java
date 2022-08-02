package com.bakdata.conquery.models.index;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class MapIndexKey extends AbstractIndexKey<MapIndex, String> {

	private final String externalTemplate;

	public MapIndexKey(URL csv, String internalColumn, String externalTemplate) {
		super(csv, internalColumn);
		this.externalTemplate = externalTemplate;
	}

	@Override
	public List<String> getExternalTemplates() {
		return List.of(externalTemplate);
	}

	@Override
	public MapIndex createIndex() {
		return new MapIndex(externalTemplate);
	}

}
