package com.bakdata.conquery.models.query.filter.event;

import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.events.Block;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.query.queryplan.clone.CloneContext;
import com.bakdata.conquery.models.query.queryplan.filter.SingleColumnFilterNode;
import com.bakdata.conquery.models.types.specific.AStringType;

/**
 * Entity is included when the number of values for a specified column are within a given range.
 */
public class MultiSelectFilterNode extends SingleColumnFilterNode<String[]> {

	private int[] selectedValues;
	private boolean hit;

	public MultiSelectFilterNode(Column column, String[] filterValue) {
		super(column, filterValue);
		this.selectedValues = new int[filterValue.length];
	}

	@Override
	public void nextBlock(Bucket bucket) {
		AStringType type = (AStringType) getColumn().getTypeFor(bucket);

		for (int index = 0; index < filterValue.length; index++) {
			String select = filterValue[index];
			int parsed = type.getId(select);
			selectedValues[index] = parsed;
		}
	}


	@Override
	public boolean checkEvent(Bucket bucket, int event) {
		if (!bucket.has(event, getColumn())) {
			return false;
		}

		int stringToken = bucket.getString(event, getColumn());

		for (int selectedValue : selectedValues) {
			if (selectedValue == stringToken) {
				return true;
			}
		}

		return false;
	}

	@Override
	public MultiSelectFilterNode doClone(CloneContext ctx) {
		return new MultiSelectFilterNode(getColumn(), filterValue);
	}

	@Override
	public void acceptEvent(Bucket bucket, int event) {
		this.hit = true;
	}

	@Override
	public boolean isContained() {
		return hit;
	}
}
