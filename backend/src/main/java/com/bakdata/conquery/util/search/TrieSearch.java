package com.bakdata.conquery.util.search;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import it.unimi.dsi.fastutil.objects.Object2DoubleAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.apache.commons.lang3.StringUtils;

/**
 * Trie based keyword search for autocompletion and resolving.
 * <p>
 * We store not only whole words but ngrams up to length of ngramLength to enable a sort of fuzzy and partial search with longer and compound search terms.
 * <p>
 * The output result should be in the following order:
 * 1) Original words (in order of difference)
 * 2) Suffixes (in order of difference)
 */
@Slf4j
@ToString(of = {"ngramLength", "splitPattern"})
public class TrieSearch<T extends Comparable<T>> {
	/**
	 * We saturate matches to avoid favoring very short keywords, when multiple keywords are used.
	 */
	private static final double EXACT_MATCH_WEIGHT = 0.1d;
	/**
	 * We prefer original words in our results.
	 *
	 * @implNote they are marked by appending WHOLE_WORD_MARKER to them in the trie.
	 */
	private static final String WHOLE_WORD_MARKER = "!";

	private final int ngramLength;

	private final Pattern splitPattern;

	private static class KeywordItems<T> {
		String word;

		// In shrinkToFit we shrink "items" for every whole word
		List<T> items;

		public KeywordItems(final String word, final List<T> items) {
			this.word = word;
			this.items = items;
		}
	}

	/**
	 * Maps from keywords to associated items.
	 */
	private final PatriciaTrie<List<KeywordItems<T>>> trie = new PatriciaTrie<>();

	private boolean shrunk = false;
	private long size = -1;

	public TrieSearch(int ngramLength, String split) {
		if (ngramLength < 0) {
			throw new IllegalArgumentException("Negative ngram Length is not allowed.");
		}
		this.ngramLength = ngramLength;

		splitPattern = Pattern.compile(String.format("[\\s%s]+", Pattern.quote(Objects.requireNonNullElse(split, "") + WHOLE_WORD_MARKER)));
	}

	public List<T> findItems(Collection<String> queries, int limit) {
		final Object2DoubleMap<T> itemWeights = new Object2DoubleAVLTreeMap<>();

		// We are not guaranteed to have split queries incoming, so we normalize them for searching
		queries = queries.stream().flatMap(this::split).collect(Collectors.toSet());

		for (final String query : queries) {
			// Query trie for all items associated with extensions of queries
			final int queryLength = query.length();
			if (queryLength < ngramLength) {
				for (final List<KeywordItems<T>> hits : trie.prefixMap(query).values()) {
					updateWeights(query, hits, itemWeights);
				}
			}
			else {
				for (int start = 0; start <= queryLength - ngramLength; start++) {
					final String ngram = query.substring(start, start + ngramLength);
					updateWeights(query, trie.get(ngram), itemWeights);
				}
			}
		}

		// Sort items according to their weight, then limit.
		// Note that sorting is in ascending order, meaning lower-scores are better.
		return itemWeights.object2DoubleEntrySet()
						  .stream()
						  .sorted(Comparator.comparingDouble(Object2DoubleMap.Entry::getDoubleValue))
						  .limit(limit)
						  .map(Map.Entry::getKey)
						  .collect(Collectors.toList());
	}

	/**
	 * calculate and update weights for all queried items
	 */
	private void updateWeights(String query, final List<KeywordItems<T>> hits, Object2DoubleMap<T> itemWeights) {
		if (hits == null) {
			return;
		}

		for (final KeywordItems<T> entry : hits) {
			final double weight = weightWord(query, entry.word);

			entry.items.forEach(item ->
								{
									// We combine hits multiplicative to favor items with multiple hits
									final double currentWeight = itemWeights.getOrDefault(item, 1);
									itemWeights.put(item, currentWeight * weight);
								});
		}
	}

	private Stream<String> split(String keyword) {
		if (Strings.isNullOrEmpty(keyword)) {
			return Stream.empty();
		}

		return splitPattern.splitAsStream(keyword.trim())
						   .map(String::trim)
						   .filter(StringUtils::isNotBlank)
						   .map(String::toLowerCase);
	}

	/**
	 * A lower weight implies more relevant words.
	 */
	private double weightWord(String query, String itemWord) {
		final double itemLength = itemWord.length();
		final double queryLength = query.length();

		final double weight;

		// We saturate the weight to avoid favoring extremely short matches.
		if (queryLength == itemLength) {
			weight = EXACT_MATCH_WEIGHT;
		}
		// We assume that less difference implies more relevant words
		else if (queryLength < itemLength) {
			weight = (itemLength - queryLength) / itemLength;
		}
		else {
			weight = (queryLength - itemLength) / queryLength;
		}

		// Soft grouping based on string length
		return Math.pow(weight, 2 / (itemLength + queryLength));
	}

	public List<T> findExact(Collection<String> keywords, int limit) {
		return keywords.stream()
					   .flatMap(this::split)
					   .map(this::toWholeWord)
					   .flatMap(this::doGet)
					   .distinct()
					   .limit(limit)
					   .collect(Collectors.toList());
	}

	private Stream<T> doGet(String kw) {
		return trie.getOrDefault(kw, Collections.emptyList()).stream()
				   .flatMap(ki -> ki.items.stream());
	}

	public void addItem(T item, List<String> keywords) {
		// Associate item with all extracted keywords
		keywords.stream()
				.filter(Predicate.not(Strings::isNullOrEmpty))
				.flatMap(this::split)
				.forEach(word -> doPut(word, item));
	}

	public Stream<String> toTrieKeys(String word) {
		Stream<String> wholeWordStream = Stream.of(toWholeWord(word));

		if (word.length() < ngramLength) {
			return wholeWordStream;
		}

		return Stream.concat(
				wholeWordStream,
				IntStream.range(0, word.length() - ngramLength + 1)
						 .mapToObj(start -> word.substring(start, start + ngramLength))
		);
	}

	private void doPut(String word, T item) {
		// ToDo: wouldn't it suffice to check once in addItem()? Is concurrency the reason?
		ensureWriteable();

		List<KeywordItems<T>> entry = trie.get(toWholeWord(word));
		final KeywordItems<T> ki = entry != null ? entry.get(0) : new KeywordItems<>(word, new ArrayList<>());

		ki.items.add(item);
		toTrieKeys(word).forEach(key -> trie.computeIfAbsent(key, (ignored) -> new ArrayList<>()).add(ki));
	}

	public String toWholeWord(String word) {
		// We append a special character here marking original words as we want to favor them in weighing.
		return word + WHOLE_WORD_MARKER;
	}

	public boolean isWholeWord(String word) {
		// We append a special character here marking original words as we want to favor them in weighing.
		return word.endsWith(WHOLE_WORD_MARKER);
	}

	private void ensureWriteable() {
		if (isWriteable()) {
			return;
		}
		throw new IllegalStateException("Cannot alter a shrunk search.");
	}

	/**
	 * Since growth of ArrayList might be excessive, we can shrink the internal lists to only required size instead.
	 *
	 * @implSpec the TrieSearch is still mutable after this.
	 */
	public void shrinkToFit() {
		if (shrunk) {
			return;
		}

		for (Map.Entry<String, List<KeywordItems<T>>> entry : trie.entrySet()) {
			// Every KeywordItems<T> is the sole member of a whole word.
			if (!isWholeWord(entry.getKey())) {
				// Skip ngram
				continue;
			}

			KeywordItems<T> ki = entry.getValue().get(0);
			ki.items = ki.items.stream().distinct().collect(Collectors.toList());
		}
		trie.replaceAll((key, values) -> values.stream().distinct().collect(Collectors.toList()));
		size = calculateSize();
		shrunk = true;
	}


	public long calculateSize() {
		if (size != -1) {
			return size;
		}

		return trie.values().stream().mapToLong(Collection::size).sum();
	}


	public Stream<T> stream() {
		return trie.values().stream()
				   .flatMap(Collection::stream)
				   .flatMap(ki -> ki.items.stream())
				   .distinct();
	}

	public Iterator<T> iterator() {
		// This is a very ugly workaround to not get eager evaluation (which happens when using flatMap and distinct on streams)
		final Set<T> seen = new HashSet<>();

		final Iterator<KeywordItems<T>> kiIter = Iterators.concat(Iterators.transform(trie.values().iterator(), List::iterator));
		return Iterators.filter(Iterators.concat(Iterators.transform(kiIter, ki -> ki.items.iterator())), seen::add);
	}

	public boolean isWriteable() {
		return !shrunk;
	}
}
