package com.bakdata.conquery.models.events.stores.specific.string;

import java.util.stream.Stream;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.primitive.BitSetStore;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.StringStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Only one string.
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_SINGLETON")
@ToString(of = "singleValue")
public class SingletonStringStore implements StringStore {

	private final String singleValue;
	private final BitSetStore delegate;

	@JsonCreator
	public SingletonStringStore(String singleValue, BitSetStore delegate) {
		super();
		this.singleValue = singleValue;
		this.delegate = delegate;
	}

	@Override
	public void setIndexStore(IntegerStore indexStore) {

	}

	@Override
	public int size() {
		return singleValue == null ? 0 : 1;
	}

	@Override
	public Stream<String> iterateValues() {
		return Stream.of(singleValue);
	}

	@Override
	public SingletonStringStore select(int[] starts, int[] length) {
		return new SingletonStringStore(singleValue, delegate.select(starts, length));
	}

	@Override
	public String getElement(int id) {
		return singleValue;
	}

	@Override
	public int getLines() {
		return delegate.getLines();
	}

	@Override
	public SingletonStringStore createDescription() {
		return new SingletonStringStore(singleValue, delegate.createDescription());
	}

	@Override
	public String createScriptValue(int event) {
		return singleValue;
	}

	@Override
	public int getId(String value) {
		if (value != null && value.equals(singleValue)) {
			return 0;
		}
		return -1;
	}

	@Override
	public long estimateEventBits() {
		return Byte.SIZE;
	}


	@Override
	public Dictionary getUnderlyingDictionary() {
		return null;
	}

	@Override
	public void setUnderlyingDictionary(Dictionary dictionary) {
		// No underlying Dictionary
	}

	@Override
	public boolean isDictionaryHolding() {
		return false;
	}

	@Override
	public void setString(int event, int value) {
		getDelegate().setBoolean(event, true);
	}

	@Override
	public void setNull(int event) {
		getDelegate().setBoolean(event, false);
	}

	@Override
	public boolean has(int event) {
		return getDelegate().getBoolean(event);
	}


	@Override
	public int getString(int event) {
		return 0;
	}
}
