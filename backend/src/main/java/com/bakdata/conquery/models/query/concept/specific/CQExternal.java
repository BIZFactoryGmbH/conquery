package com.bakdata.conquery.models.query.concept.specific;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hibernate.validator.constraints.NotEmpty;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.exceptions.validators.ValidCSVFormat;
import com.bakdata.conquery.models.identifiable.mapping.IdAccessor;
import com.bakdata.conquery.models.identifiable.mapping.IdAccessorImpl;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.preproc.DateFormats;
import com.bakdata.conquery.models.query.QueryPlanContext;
import com.bakdata.conquery.models.query.QueryResolveContext;
import com.bakdata.conquery.models.query.concept.CQElement;
import com.bakdata.conquery.models.query.queryplan.QPNode;
import com.bakdata.conquery.models.query.queryplan.QueryPlan;
import com.bakdata.conquery.models.types.specific.DateRangeType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.collect.MoreCollectors;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@CPSType(id = "EXTERNAL", base = CQElement.class)
@RequiredArgsConstructor(onConstructor_ = @JsonCreator)
public class CQExternal implements CQElement {

	@Getter
	@NotEmpty
	@ValidCSVFormat
	private final List<FormatColumn> format;
	@Getter
	@NotEmpty
	private final String[][] values;

	@Override
	public QPNode createQueryPlan(QueryPlanContext context, QueryPlan plan) {
		throw new IllegalStateException("CQExternal needs to be resolved before creating a plan");
	}

	@Override
	public CQElement resolve(QueryResolveContext context) {
		Dictionary primary = context.getNamespace().getStorage().getPrimaryDictionary();
		Optional<DateFormat> dateFormat = format.stream()
			.map(FormatColumn::getDateFormat)
			.filter(Objects::nonNull)
			.distinct()
			.collect(MoreCollectors.toOptional());
		int[] dateIndices = format.stream().filter(fc -> fc.getDateFormat() != null).mapToInt(format::indexOf).toArray();

		Int2ObjectMap<CDateSet> includedEntities = new Int2ObjectOpenHashMap<>();

		IdMappingConfig mapping = ConqueryConfig.getInstance().getIdMapping();

		IdAccessor idAccessor = mapping.mappingFromCsvHeader(values[0], context.getNamespace().getStorage());

		// ignore the first row, because this is the header
		for (int i = 1; i < values.length; i++) {
			String[] row = values[i];
			if (row.length != format.size()) {
				throw new IllegalArgumentException("There are "+ format.size()+ " columns in the format but "+ row.length + " in at least one row");
			}

			//read the dates from the row
			try {
				CDateSet dates = dateFormat.map(df -> {
					try {
						return df.readDates(dateIndices, row);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}).orElseGet(CDateSet::createFull);
				// remove all fields from the data line that are not id fields, in case the mapping is not possible we avoid the data columns to be joined
				includedEntities.put(primary.getId(idAccessor.getCsvEntityId(IdAccessorImpl.removeNonIdFields(row, format)).getCsvId()),
					Objects.requireNonNull(dates));
			}
			catch (Exception e) {
				log.warn("failed to parse dates from " + Arrays.toString(row), e);
			}
		}
		return new CQExternalResolved(includedEntities);
	}

	public enum DateFormat {
		EVENT_DATE {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row) throws ParsingException {
				return CDateSet.create(Collections.singleton(CDateRange.exactly(DateFormats.instance()
					.parseToLocalDate(row[dateIndices[0]]))));
			}
		},
		START_END_DATE {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row) throws ParsingException {
				LocalDate start = row[dateIndices[0]] == null ? null : DateFormats.instance().parseToLocalDate(row[dateIndices[0]]);
				LocalDate end = (dateIndices.length < 2 || row[dateIndices[1]] == null) ?
					null :
					DateFormats.instance().parseToLocalDate(row[dateIndices[1]]);

				CDateRange range;
				if (start != null && end != null) {
					range = CDateRange.of(start, end);
				}
				else if (start != null) {
					range = CDateRange.atLeast(start);
				}
				else if (end != null) {
					range = CDateRange.atMost(end);
				}
				else {
					return null;
				}

				return CDateSet.create(Collections.singleton(range));
			}
		},
		DATE_RANGE {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row) throws ParsingException {
				return CDateSet.create(Collections.singleton(DateRangeType.parseISORange(row[dateIndices[0]])));
			}
		},
		DATE_SET {
			@Override
			public CDateSet readDates(int[] dateIndices, String[] row) throws ParsingException {
				return CDateSet.parse(row[dateIndices[0]]);
			}
		};

		public abstract CDateSet readDates(int[] dateIndices, String[] row) throws ParsingException;
	}

	@RequiredArgsConstructor
	@Getter
	public static enum FormatColumn {
		ID(true, null),
		EVENT_DATE(false, DateFormat.EVENT_DATE),
		START_DATE(false, DateFormat.START_END_DATE),
		END_DATE(false, DateFormat.START_END_DATE),
		DATE_RANGE(false, DateFormat.DATE_RANGE),
		DATE_SET(false, DateFormat.DATE_SET),
		IGNORE(true, null);

		private final boolean duplicatesAllowed;
		private final DateFormat dateFormat;
	}
}
