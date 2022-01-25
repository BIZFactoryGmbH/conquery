package com.bakdata.conquery.apiv1;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bakdata.conquery.models.config.CSVConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.concepts.filters.specific.AbstractSelectFilter;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.util.search.QuickSearch;
import com.github.powerlibraries.io.In;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.csv.CsvParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@NoArgsConstructor
public class FilterSearch {

	/**
	 * Enum to specify a scorer function in {@link QuickSearch}. Used for resolvers in {@link AbstractSelectFilter}.
	 */
	@AllArgsConstructor
	@Getter
	public enum FilterSearchType {
		/**
		 * String must start with search-String.
		 */
		PREFIX {
			@Override
			public double score(String candidate, String keyword) {
				/* Sort ascending by length of match */
				if (keyword.startsWith(candidate)) {
					return 1d / candidate.length();
				}
				return -1d;
			}
		},
		/**
		 * Search is contained somewhere in string.
		 */
		CONTAINS {
			@Override
			public double score(String candidate, String keyword) {
				/* 0...1 depending on the length ratio */
				double matchScore = (double) candidate.length() / (double) keyword.length();

				/* boost by 1 if matches start of keyword */
				if (keyword.startsWith(candidate))
					return matchScore + 1.0;

				return matchScore;
			}
		},
		/**
		 * Values must be exactly the same.
		 */
		EXACT {
			@Override
			public double score(String candidate, String keyword) {
				/* Only allow exact matches through (returning < 0.0 means skip this candidate) */
				return candidate.equals(keyword) ? 1.0 : -1.0;
			}
		};

		/**
		 * Search function. See {@link QuickSearch}.
		 * @param candidate potential match.
		 * @param match search String.
		 * @return Value > 0 increases relevance of string (also used for ordering results). < 0 removes string.
		 */
		public abstract double score(String candidate, String match);
	}

	private final Map<TemplateKey, QuickSearch<FilterSearchItem>> searches = new HashMap<>();

	/**
	 * Scan all SelectFilters and submit {@link SimpleJob}s to create interactive searches for them.
	 */
	public void updateSearch(DatasetRegistry datasets, Collection<Dataset> datasetsToUpdate, JobManager jobManager, CSVConfig parser) {
		datasetsToUpdate.stream()
				.flatMap(ds -> datasets.get(ds.getId()).getStorage().getAllConcepts().stream())
				.flatMap(c -> c.getConnectors().stream())
				.flatMap(co -> co.collectAllFilters().stream())
				.filter(f -> f instanceof AbstractSelectFilter)
				.map(AbstractSelectFilter.class::cast)
				.forEach(f -> jobManager.addSlowJob(new SimpleJob(String.format("SourceSearch[%s]", f.getId()), () -> createSourceSearch(f, parser))));
	}

	/***
	 * Create interactive Search for the selected filter based on its Template.
	 * @param filter
	 */
	public void createSourceSearch(AbstractSelectFilter<?> filter, CSVConfig parserConfig) {

		FilterTemplate template = filter.getTemplate();

		if (template == null){
			return; //TODO actually we want to also use plain values without templates!
		}

		List<String> templateColumns = new ArrayList<>(template.getColumns());
		templateColumns.add(template.getColumnValue());


		File file = new File(template.getFilePath());
		TemplateKey autocompleteKey = new TemplateKey(template.getFilePath(), templateColumns) ;


		if (searches.containsKey(autocompleteKey)) {
			log.debug("Reference list `{}` already exists", autocompleteKey);
			filter.setSourceSearch(searches.get(autocompleteKey));
			return;
		}

		log.info("BEGIN Processing reference list {}", autocompleteKey);
		final long time = System.currentTimeMillis();

		QuickSearch<FilterSearchItem> search = new QuickSearch.QuickSearchBuilder()
				.withUnmatchedPolicy(QuickSearch.UnmatchedPolicy.IGNORE)
				.withMergePolicy(QuickSearch.MergePolicy.UNION)
				.withKeywordMatchScorer(FilterSearchType.CONTAINS::score)
				.build();

		final CsvParser parser = parserConfig.createParser();

		try {
			IterableResult<String[], ParsingContext> it = parser.iterate(In.file(file).withUTF8().asReader());
			String[] header = it.getContext().parsedHeaders();

			for (String[] row : it) {
				FilterSearchItem item = new FilterSearchItem();

				for (int i = 0; i < header.length; i++) {
					String column = header[i];

					if (!templateColumns.contains(column)) {
						continue;
					}

					item.setLabel(template.getValue());
					item.setOptionValue(template.getOptionValue());
					item.getTemplateValues().put(column, row[i]);

					if (column.equals(template.getColumnValue())) {
						item.setValue(row[i]);
					}

					search.addItem(item, row[i]);
				}
			}

			filter.setSourceSearch(search);

			searches.put(autocompleteKey, search);
			final long duration = System.currentTimeMillis() - time;

			log.info("DONE Processing reference list '{}' in {} ms ({} Items in {} Lines)",
					 autocompleteKey, duration, search.getStats().getItems(), it.getContext().currentLine());
		} catch (Exception e) {
			log.error("Failed to process reference list '"+file.getAbsolutePath()+"'", e);
		} finally {
			parser.stopParsing();
		}
	}

	@Data
	private static class TemplateKey {
		private final String file;
		private final List<String> columns;
	}

	public void clear() {
		searches.clear();
	}
}
