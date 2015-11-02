package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IGuard;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBGuard extends EventBEventCondition {

	private final static String TYPE = "GUARD";

	public EventBGuard(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentEvent, parentUnit);
	}

	public EventBGuard(String label, String predicate, String comment, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, predicate, comment, parentEvent, parentUnit);
	}

	public EventBGuard(IGuard guard, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(parentEvent, parentUnit);
		String label = "";
		String predicate = "";
		String comment = "";
		if (guard.hasLabel()) {
			label = guard.getLabel();
		}
		if (guard.hasComment()) {
			comment = guard.getComment();
		}
		if (guard.hasPredicateString()) {
			predicate = guard.getPredicateString();
		}
		this.label = label;
		this.predicate = predicate;
		this.comment = comment;
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + predicate + " (" + comment + ")";
	}
}
