package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

@CPSType(base = CType.class, id = "MONEY_VARINT")
@Getter
@Setter
public class MoneyType extends CType<Long> {

	protected CType<Long> numberType;

	@JsonCreator
	public MoneyType(CType<Long> numberType) {
		super(MajorTypeId.MONEY);
		this.numberType = numberType;
	}

	@Override
	public Object createPrintValue(Long value) {
		return createScriptValue(value);
	}

	@Override
	public Long createScriptValue(Long value) {
		return (long) numberType.createScriptValue(value);
	}

	@Override
	public MoneyType select(int[] starts, int[] length) {
		return new MoneyType(numberType.select(starts, length));
	}

	@Override
	public Long get(int event) {
		return numberType.get(event);
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[numberType=" + numberType + "]";
	}

	@Override
	public long estimateEventBytes() {
		return numberType.estimateEventBytes();
	}

	@Override
	public void set(int event, Long value) {
		if (value == null) {
			numberType.set(event, null);
		}
		else {
			numberType.set(event, value.longValue());
		}
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}
}
