package com.bakdata.conquery.apiv1.frontend;

import com.bakdata.conquery.apiv1.KeyValue;
import com.bakdata.conquery.models.common.Range;
import com.bakdata.conquery.models.identifiable.ids.IId;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * This class represents a concept as it is presented to the front end.
 */
@Data @Builder
public class FENode {
	private IId<?> parent;
	@NotNull
	private String label;
	private String description;
	private Boolean active;
	private IId<?>[] children;
	private List<KeyValue> additionalInfos;
	private long matchingEntries;
	private Range<LocalDate> dateRange;
	private List<@Valid FETable> tables;
	private Boolean detailsAvailable;
	private boolean codeListResolvable;
	private List<FESelect> selects;
	private long matchingEntities;
}
