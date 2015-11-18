package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IGuard;
import org.eventb.core.ISCGuard;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBGuard extends EventBCondition {

	private EventBEvent parentEvent = null;

	public EventBGuard(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBGuard(String label, String predicate, String comment, ISCGuard scGuard, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, predicate, scGuard, comment, parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBGuard(IGuard guard, ISCGuard scGuard, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(guard, scGuard, parentUnit);
		this.parentEvent = parentEvent;
	}

	@Override
	public ISCGuard getScElement() {
		return (ISCGuard) super.getScElement();
	}

	public EventBEvent getParentEvent() {
		return parentEvent;
	}

	public EventBUnit getParentUnit() {
		return getParent();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.GUARD;
		return type;
	}
}
