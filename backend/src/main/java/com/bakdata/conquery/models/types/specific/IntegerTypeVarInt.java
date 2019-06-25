package com.bakdata.conquery.models.types.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.CTypeVarInt;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.Setter;

@CPSType(base=CType.class, id="INTEGER_VARINT")
@Getter @Setter
public class IntegerTypeVarInt extends CTypeVarInt<Long> {

	@JsonCreator
	public IntegerTypeVarInt(VarIntType numberType) {
		super(MajorTypeId.INTEGER, numberType);
	}
	
	@Override
	public Long createScriptValue(Number value) {
		return numberType.createScriptValue(value).longValue();
	}
}
