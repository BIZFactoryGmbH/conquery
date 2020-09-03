package com.bakdata.conquery.models.events;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.common.CDateSet;
import com.bakdata.conquery.models.common.daterange.CDateRange;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.specific.BucketId;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

/**
 * Contains data from possibly multiple entities, loaded in a single import.
 */
@Slf4j
@FieldNameConstants
@Getter
@Setter
@ToString
public class Bucket extends IdentifiableImpl<BucketId> implements Iterable<Integer>, JsonSerializable {

	@Min(0)
	private int bucket;
	@NotNull
	@NsIdRef
	private Import imp;

	@Min(0)
	private int numberOfEvents = 1; // todo

	private final ColumnStore[] stores;

	// todo these three can be combined
	private final Int2IntMap start = new Int2IntArrayMap();
	private final Int2IntMap end;

	@Getter
	private final int bucketSize = start.size();


	public Bucket(int bucket, Import imp, ColumnStore[] stores, int[] entities, int[] ends) {
		this.bucket = bucket;
		this.imp = imp;
		this.stores = stores;

		for (int index = 1; index < entities.length; index++) {
			start.put(ends[index], ends[index]);
		}

		end = new Int2IntArrayMap(entities, ends);
	}

	@Override
	public BucketId createId() {
		return new BucketId(imp.getId(), bucket);
	}

	/**
	 * Iterate entities
	 * @return
	 */
	@Override
	public PrimitiveIterator.OfInt iterator() {
		return start.keySet().iterator();
	}

	public boolean containsEntity(int localEntity) {
		return start.containsKey(localEntity);
	}

	public void initFields(int numberOfEntities) {
		// TODO
	}

	public int getFirstEventOfLocal(int localEntity) {
		return start.get(localEntity);
	}

	public int getLastEventOfLocal(int localEntity) {
		return end.get(localEntity);
	}

	@Override
	public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeObject(this);
	}

	public void read(Input input) {

	}

	public Iterable<BucketEntry> entries() {
		return () -> start.keySet().stream()
							 .flatMap(entity -> IntStream
													.range(getFirstEventOfLocal(entity), getLastEventOfLocal(entity))
													.mapToObj(e -> new BucketEntry(entity, e))
							 )
							 .iterator();
	}


	public final boolean has(int event, Column column) {
		return stores[column.getPosition()].has(event);
	}

	public int getString(int event, Column column) {
		return stores[column.getPosition()].getString(event);
	}

	public long getInteger(int event, Column column) {
		return stores[column.getPosition()].getInteger(event);
	}

	public boolean getBoolean(int event, Column column){
		return stores[column.getPosition()].getBoolean(event);
	}

	public double getReal(int event, Column column){
		return stores[column.getPosition()].getReal(event);
	}

	public BigDecimal getDecimal(int event, Column column){
		return stores[column.getPosition()].getDecimal(event);
	}

	public long getMoney(int event, Column column){
		return stores[column.getPosition()].getMoney(event);
	}

	public int getDate(int event, Column column){
		return stores[column.getPosition()].getDate(event);
	}

	public CDateRange getDateRange(int event, Column column){
		return stores[column.getPosition()].getDateRange(event);
	}

	public CDateRange getAsDateRange(int event, Column currentColumn){
		return getDateRange(event, currentColumn);
	}


	public Object getAsObject(int event, Column column) {
		return stores[column.getPosition()].getAsObject(event);
	}

	public boolean eventIsContainedIn(int event, Column column, CDateSet dateRanges){
		return dateRanges.intersects(stores[column.getPosition()].getDateRange(event));
	}


	@Override
	public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
		this.serialize(gen, serializers);
	}

	public Map<String, Object> calculateMap(int event, Import imp) {
		Map<String, Object> out = new HashMap<>(stores.length);

		for (ColumnStore store : stores) {
			if (!store.has(event)) {
				continue;
			}

			out.put(store.getColumn().getName(), store.getAsObject(event));
		}

		return out;
	}

	public final void writeContent(Output output) throws IOException {
		Jackson.BINARY_MAPPER.writeValue(output, this);
	}
}
