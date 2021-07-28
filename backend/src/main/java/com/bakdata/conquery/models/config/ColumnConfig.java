package com.bakdata.conquery.models.config;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.validation.constraints.NotEmpty;

import c10n.C10N;
import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.models.identifiable.mapping.EntityIdMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import io.dropwizard.validation.ValidationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Builder
@AllArgsConstructor
@ToString
@NoArgsConstructor
@Setter
@Getter
public class ColumnConfig {

	public EntityIdMap.ExternalId read(String value) {
		if (!isResolvable()) {
			return null;
		}

		if (Strings.isNullOrEmpty(value)) {
			return null;
		}

		if (getLength() == -1 || getPad() == null) {
			return new EntityIdMap.ExternalId(getName(), value);
		}

		String padded = StringUtils.leftPad(value, getLength(), getPad());

		return new EntityIdMap.ExternalId(getName(), padded);

	}


	@NotEmpty
	private String name;

	/**
	 * Map of Localized labels.
	 */
	@NotEmpty
	@Builder.Default
	private Map<String, String> label = Collections.emptyMap();

	/**
	 * Map of Localized description.
	 */
	@Builder.Default
	private Map<String, String> description = Collections.emptyMap();

	@InternalOnly
	private String field;

	@InternalOnly
	private String pad = null;

	@InternalOnly
	@Builder.Default
	private int length = -1;

	@InternalOnly
	@Builder.Default
	private boolean resolvable = false;

	@InternalOnly
	@Builder.Default
	private boolean print = true;

	@InternalOnly
	@Builder.Default
	private boolean fillAnon = false;

	@JsonIgnore
	@ValidationMethod(message = "Keys must be valid Locales.")
	public boolean isLabelKeysLocale() {
		return getLabel().keySet().stream().map(Locale::forLanguageTag).noneMatch(Objects::isNull);
	}

	@JsonIgnore
	@ValidationMethod(message = "Keys must be valid Locales.")
	public boolean isDescriptionKeysLocale() {
		return getDescription().keySet().stream().map(Locale::forLanguageTag).noneMatch(Objects::isNull);
	}

}
