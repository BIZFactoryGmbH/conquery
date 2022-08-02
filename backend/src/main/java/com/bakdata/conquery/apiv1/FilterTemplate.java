package com.bakdata.conquery.apiv1;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.bakdata.conquery.apiv1.frontend.FEValue;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.config.SearchConfig;
import com.bakdata.conquery.models.datasets.concepts.Searchable;
import com.bakdata.conquery.models.index.FEValueIndex;
import com.bakdata.conquery.models.index.FEValueIndexKey;
import com.bakdata.conquery.models.index.IndexService;
import com.bakdata.conquery.util.search.TrieSearch;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
@EqualsAndHashCode
@JsonIgnoreProperties({"columns"})
@ToString
@Slf4j
public class FilterTemplate implements Searchable {

	private static final long serialVersionUID = 1L;

	/**
	 * Path to CSV File.
	 */
	private URL filePath;

	/**
	 * Value to be sent for filtering.
	 */
	private String columnValue;
	/**
	 * Value displayed in Select list. Usually concise display.
	 */
	private String value;
	/**
	 * More detailed value. Displayed when value is selected.
	 */
	private String optionValue;

	private int minSuffixLength = 3;
	private boolean generateSuffixes = true;

	// We inject the service as a non-final property so, jackson will never try to create a serializer for it (in contrast to constructor injection)
	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NonNull
	private IndexService indexService;

	/**
	 * Does not make sense to distinguish at Filter level since it's only referenced when a template is also set.
	 */
	@Override
	@JsonIgnore
	public boolean isSearchDisabled() {
		return false;
	}

	public List<TrieSearch<FEValue>> getSearches(SearchConfig config, NamespaceStorage storage) {

		final CompletableFuture<FEValueIndex> futureSearch = indexService.getMapping(new FEValueIndexKey(
				filePath,
				columnValue,
				value,
				optionValue,
				isGenerateSuffixes() ? getMinSuffixLength() : Integer.MAX_VALUE,
				config.getSplit()
		));

		return List.of(futureSearch.join());
	}

}
