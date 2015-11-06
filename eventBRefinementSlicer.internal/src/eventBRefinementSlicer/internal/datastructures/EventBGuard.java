package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IGuard;
import org.eventb.core.ISCGuard;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBGuard extends EventBEventCondition {

	public EventBGuard(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentEvent, parentUnit);
	}

	public EventBGuard(String label, String predicate, String comment, ISCGuard scGuard, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, predicate, comment, scGuard, parentEvent, parentUnit);
	}

	public EventBGuard(IGuard guard, ISCGuard scGuard, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
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
		this.scElement = scGuard;
	}

	@Override
	public ISCGuard getScElement() {
		return (ISCGuard) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "GUARD";
		return type;
	}
}
