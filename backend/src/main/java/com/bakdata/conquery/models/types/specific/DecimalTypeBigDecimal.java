package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;

@CPSType(base=CType.class, id="DECIMAL_BIG_DECIMAL")
public class DecimalTypeBigDecimal extends CType<BigDecimal, BigDecimal> {

	public DecimalTypeBigDecimal() {
		super(MajorTypeId.DECIMAL, BigDecimal.class);
	}

	@Override
	public boolean canStoreNull() {
		return true;
	}
}