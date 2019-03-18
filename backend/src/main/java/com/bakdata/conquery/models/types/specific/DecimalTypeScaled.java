package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.MinorCType;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.ToString;

@CPSType(base=CType.class, id="DECIMAL_SCALED") @ToString
public class DecimalTypeScaled<NUMBER, SUB extends CType<NUMBER, IntegerType>> extends MinorCType<NUMBER, DecimalType> {

	@Getter @ToString.Include
	private final int scale;
	@Getter @ToString.Include
	private final SUB subType;
	
	@JsonCreator
	public DecimalTypeScaled(long lines, long nullLines, int scale, SUB subType) {
		super(MajorTypeId.DECIMAL, subType.getPrimitiveType());
		this.setLines(lines);
		this.setNullLines(nullLines);
		this.scale = scale;
		this.subType = subType;
	}

	@Override
	public BigDecimal createScriptValue(NUMBER value) {
		return BigDecimal.valueOf((Long)subType.createScriptValue(value), scale);
	}
	
	@Override
	public NUMBER transformFromMajorType(DecimalType majorType, Object value) {
		BigDecimal v = (BigDecimal) value;
		if(v.scale() > scale)
			throw new IllegalArgumentException(value+" is out of range");
		return subType.transformFromMajorType(null, v.setScale(scale, RoundingMode.UNNECESSARY).unscaledValue().longValue());
	}
	
	
	@Override
	public BigDecimal transformToMajorType(NUMBER value, DecimalType majorType) {
		return BigDecimal.valueOf((Long)subType.transformToMajorType(value, null), scale);
	}

	@Override
	public boolean canStoreNull() {
		return false;
	}
	
	
}