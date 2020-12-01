package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = CType.class, id = "DATE_RANGE_DATE_RANGE")
@Getter
@Setter
public class DateRangeTypeDateRange extends CType<CDateRange> {

	private final CType<Long> minStore;
	private final CType<Long> maxStore;

	@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
	public DateRangeTypeDateRange(CType<Long> minStore, CType<Long> maxStore) {
		super(MajorTypeId.DATE_RANGE);
		this.minStore = minStore;
		this.maxStore = maxStore;
	}

	@Override
	public Object createPrintValue(CDateRange value) {
		if (value == null) {
			return "";
		}

		return value.toString();
	}

	@Override
	public long estimateEventBytes() {
		return minStore.estimateEventBytes() * 2;
	}

	@Override
	public DateRangeTypeDateRange select(int[] starts, int[] length) {
		return new DateRangeTypeDateRange(minStore.select(starts, length), maxStore.select(starts, length));
	}

	@Override
	public void set(int event, CDateRange value) {
		if (value == null) {
			minStore.set(event, null);
			maxStore.set(event, null);
			return;
		}

		if (value.hasLowerBound()) {
			minStore.set(event, (long) value.getMinValue());
		}
		else {
			minStore.set(event, null);
		}

		if (value.hasUpperBound()) {
			maxStore.set(event, (long) value.getMaxValue());
		}
		else {
			maxStore.set(event, null);
		}
	}

	@Override
	public CDateRange get(int event) {
		int min = Integer.MIN_VALUE;
		int max = Integer.MAX_VALUE;

		if (minStore.has(event)) {
			min = minStore.get(event).intValue();
		}

		if (maxStore.has(event)) {
			max = maxStore.get(event).intValue();
		}

		return CDateRange.of(min, max);
	}

	@Override
	public boolean has(int event) {
		return minStore.has(event) && maxStore.has(event);
	}
}