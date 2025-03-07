package com.bakdata.conquery.sql.conversion;

import com.bakdata.conquery.apiv1.query.QueryDescription;
import com.bakdata.conquery.models.config.SqlConnectorConfig;
import com.bakdata.conquery.models.query.Visitable;
import com.bakdata.conquery.sql.conversion.cqelement.ConversionContext;
import com.bakdata.conquery.sql.conversion.dialect.SqlDialect;
import com.bakdata.conquery.sql.conversion.model.NameGenerator;
import org.jooq.impl.DSL;

/**
 * Entry point for converting {@link QueryDescription} to an SQL query.
 */
public class NodeConversions extends Conversions<Visitable, ConversionContext, ConversionContext> {

	private final SqlDialect dialect;
	private final SqlConnectorConfig config;

	public NodeConversions(SqlDialect dialect, SqlConnectorConfig config) {
		super(dialect.getNodeConverters());
		this.dialect = dialect;
		this.config = config;
	}

	public ConversionContext convert(QueryDescription queryDescription) {
		ConversionContext initialCtx = ConversionContext.builder()
														.config(config)
														.nameGenerator(new NameGenerator(config.getDialect().getNameMaxLength()))
														.nodeConversions(this)
														.sqlDialect(this.dialect)
														.primaryColumn(DSL.field(DSL.name(config.getPrimaryColumn())))
														.build();
		return convert(queryDescription, initialCtx);
	}

}
