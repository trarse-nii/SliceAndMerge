package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ISCWitness;
import org.eventb.core.IWitness;
import org.rodinp.core.RodinDBException;

public class EventBWitness extends EventBCondition {

	EventBEvent parentEvent = null;

	public EventBWitness(String label, String predicate, ISCWitness scPredicateElement, String comment, EventBUnit parentUnit, EventBEvent parentEvent) {
		super(label, predicate, scPredicateElement, comment, parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBWitness(IWitness predicateElement, ISCWitness scPredicateElement, EventBUnit parentUnit, EventBEvent parentEvent)
			throws RodinDBException {
		super(predicateElement, scPredicateElement, parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBEvent getParentEvent() {
		return parentEvent;
	}

	@Override
	public ISCWitness getScElement() {
		return (ISCWitness) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.WITNESS;
		return type;
	}

}
