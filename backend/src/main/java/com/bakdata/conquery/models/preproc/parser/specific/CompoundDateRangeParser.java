package com.bakdata.conquery.models.preproc.parser.specific;

import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.stores.root.DateRangeStore;
import com.bakdata.conquery.models.events.stores.specific.DateRangeTypeCompound;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.parser.ColumnValues;
import com.bakdata.conquery.models.preproc.parser.Parser;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * CompoundDateRangeParser is the Parser of {@link CDateRange} whose start- and end-stores should be saved as references
 * It does not have an implementation logic. It only creates his parser and ignores his input values
 */
@Slf4j
@ToString(callSuper = true)
public class CompoundDateRangeParser extends Parser<CDateRange, DateRangeStore> {

	@NotNull
	final private String startColumn, endColumn;


	public CompoundDateRangeParser(ConqueryConfig config, @NotNull String startColumn, @NotNull String endColumn) {
		super(config);
		this.endColumn = endColumn;
		this.startColumn = startColumn;
	}


	@Override
	public boolean isEmpty() {
		//This ensures that no EmptyStore is generated by this parser if the lines of the parser have not yet been set at that time.
		return false;
	}


	@Override
	protected CDateRange parseValue(@NotNull String value) throws ParsingException {
		return null;
	}

	@Override
	protected DateRangeStore decideType() {
		return new DateRangeTypeCompound(this.startColumn, this.endColumn);
	}


	@Override
	public void setValue(DateRangeStore store, int event, CDateRange value) {
	}

	@Override
	public ColumnValues createColumnValues() {
		return null;
	}
}
