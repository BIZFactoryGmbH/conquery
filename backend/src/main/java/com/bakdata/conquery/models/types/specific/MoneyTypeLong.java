package com.bakdata.conquery.models.types.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@CPSType(base=CType.class, id="MONEY_LONG")
public class MoneyTypeLong extends CType<Long, Long> {

	@JsonIgnore @Getter(lazy = true)
	private final BigDecimal moneyFactor = BigDecimal.valueOf(10)
		.pow(ConqueryConfig.getInstance().getLocale().getCurrency().getDefaultFractionDigits());
	
	public MoneyTypeLong() {
		super(MajorTypeId.MONEY, long.class);
	}
	
	@Override
	public boolean canStoreNull() {
		return true;
	}
}