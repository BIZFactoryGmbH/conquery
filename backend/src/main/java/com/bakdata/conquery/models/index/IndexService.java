package com.bakdata.conquery.models.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.univocity.parsers.common.IterableResult;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class IndexService implements Injectable {

	private final CsvParserSettings csvParserSettings;

	public IndexService(CsvParserSettings csvParserSettings) {
		this.csvParserSettings = csvParserSettings.clone();
		this.csvParserSettings.setHeaderExtractionEnabled(true);
	}

	private final Queue<CompletableFuture<? extends Index<?,?>>> requestedMappings = new LinkedList<>();

	private final LoadingCache<IndexKey<?,?>, Index<?,?>> mappings = CacheBuilder.newBuilder().build(new CacheLoader<IndexKey<?,?>, Index<?,?>>() {
		@Override
		public Index<?,?> load(@NotNull IndexKey key) throws Exception {
			log.info("Started to parse mapping {}", key);

			final Index<?,?> int2ext = key.createIndex();

			final CsvParser csvParser = new CsvParser(csvParserSettings);

			try (InputStream inputStream = key.getCsv().openStream()) {

				final IterableResult<Record, ParsingContext> records = csvParser.iterateRecords(inputStream);

				// Set default to "" for all columns
				final String[] headers = records.getContext().headers();
				records.getContext().recordMetaData().setDefaultValueOfColumns("", headers);

				// Iterate records
				for (Record row : records) {
					final Pair<String, String> pair = computeInternalExternal(key, csvParser, row);
					if (pair == null) {
						continue;
					}

					final String internalValue = pair.getLeft();
					final Object externalValue = pair.getRight();

					try {
						int2ext.put(internalValue, pair.getRight());
					} catch (IllegalArgumentException e) {
						log.warn(
								"Skipping mapping '{}'->'{}' in row {}, because there was already a mapping",
								internalValue,
								externalValue,
								csvParser.getContext().currentLine(),
								(Exception) (log.isTraceEnabled() ? e : null)
						);
					}
				}
			}
			catch (IOException ioException) {
				log.warn("Failed to open {}", key.getCsv(), ioException);
				throw ioException;
			}

			log.info("Finished parsing mapping {} with {} entries", key, int2ext.size());
			return int2ext;
		}

		@Nullable
		private Pair<String, String> computeInternalExternal(@NotNull IndexKey<?,?> key, CsvParser csvParser, Record row) {
			final StringSubstitutor substitutor = new StringSubstitutor(row::getString, "{{", "}}", StringSubstitutor.DEFAULT_ESCAPE);

			final String internalValue = row.getString(key.getInternalColumn());

			if (internalValue == null) {
				log.trace("Could not create a mapping for row {} because the cell for the internal value was empty. Row: {}", csvParser.getContext().currentLine(),
						  log.isTraceEnabled()
						  ? StringUtils.join(row.toFieldMap())
						  : null
				);
				return null;
			}

			final List<String> externalTemplates = key.getExternalTemplates();
			final Map<String, String> templateToConcrete =  new HashMap<>(externalTemplates.size(), 0);

			for(String externalTemplate : externalTemplates) {

				// We allow template values to be missing
				final String externalValue = substitutor.replace(externalTemplate);

				// Clean up the substitution by removing repeated white spaces
				String externalValueCleaned = externalValue.replaceAll("\\s+", " ");
				templateToConcrete.put(externalTemplate, externalValueCleaned);
			}

			key.

			// We allow template values to be missing
			final String externalValue = substitutor.replace(key.getExternalTemplate());

			// Clean up the substitution by removing repeated white spaces
			String externalValueCleaned = externalValue.replaceAll("\\s+", " ");

			return Pair.of(internalValue, externalValueCleaned);
		}
	});

	public void evictCache() {
		synchronized (requestedMappings) {
			// Invalidate all mapping requests by cancelling them
			final Iterator<CompletableFuture<? extends Index<?,?>>> iterator = requestedMappings.iterator();

			while (iterator.hasNext()) {
				final CompletableFuture<? extends Index<?,?>> next = iterator.next();

				next.obtrudeException(new CancellationException("The mapping for this future was evicted"));
				iterator.remove();

			}

			// Clear actual cache
			mappings.invalidateAll();
		}
	}


	@SuppressWarnings("unchecked")
	public <K extends IndexKey<I,V>, I extends Index<K,V>, V> CompletableFuture<I> getMapping(K key) {
		// TODO may use a specific executor service here
		synchronized (requestedMappings) {
			final CompletableFuture<I> mapCompletableFuture = CompletableFuture.supplyAsync(() -> {
				try {
					return (I) mappings.get(key);
				}
				catch (ExecutionException e) {
					throw new IllegalStateException(String.format("Unable to get mapping from %s (internal column = %s, external column = %s)", key.getCsv(), key.getInternalColumn(), key.getExternalTemplate()), e);
				}
			});

			requestedMappings.add(mapCompletableFuture);

			return mapCompletableFuture;
		}
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(IndexService.class, this);
	}
}
