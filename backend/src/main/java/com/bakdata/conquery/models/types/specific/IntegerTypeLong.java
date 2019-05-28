package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="INTEGER") @Getter @Setter
public class IntegerTypeLong extends CType<Long, Long> {

	private final long minValue;
	private final long maxValue;
	
	@JsonCreator
	public IntegerTypeLong(long minValue, long maxValue) {
		super(MajorTypeId.INTEGER, long.class);
		this.minValue = minValue;
		this.maxValue = maxValue;
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
	
	@Override
	public long estimateMemoryBitWidth() {
		return Long.SIZE;
	}
}