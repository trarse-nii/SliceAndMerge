package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ICarrierSet;
import org.eventb.core.ISCCarrierSet;
import org.rodinp.core.RodinDBException;

public class EventBCarrierSet extends EventBAttribute {

	public EventBCarrierSet(String label, String comment, ISCCarrierSet scCarrierSet, EventBUnit parent) {
		super(label, comment, scCarrierSet, parent);
	}

	public EventBCarrierSet(ICarrierSet carrierSet, ISCCarrierSet scCarrierSet, EventBUnit parent) throws RodinDBException {
		super(carrierSet, scCarrierSet, parent);
	}

	@Override
	public ISCCarrierSet getScElement() {
		return (ISCCarrierSet) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "CARRIER_SET";
		return type;
	}
}
