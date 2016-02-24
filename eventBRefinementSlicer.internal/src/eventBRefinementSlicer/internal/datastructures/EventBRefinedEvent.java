package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IRefinesEvent;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation of an Event-B Event's Refinement Clause.
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBRefinedEvent extends EventBElement {

	private EventBEvent parentEvent = null;

	public EventBRefinedEvent(IRefinesEvent refinesEvent, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(parentUnit);
		if (refinesEvent.hasAbstractEventLabel()) {
			this.parentEvent = parentEvent;
			this.label = refinesEvent.getAbstractEventLabel();
		}
	}

	public EventBEvent getParentEvent() {
		return parentEvent;
	}

	public EventBUnit getParentUnit() {
		return getParent();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.REFINED_EVENT;
		return type;
	}

}
