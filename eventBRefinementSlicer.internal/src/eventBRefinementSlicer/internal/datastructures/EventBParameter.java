package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IParameter;
import org.eventb.core.ISCParameter;
import org.rodinp.core.RodinDBException;

public class EventBParameter extends EventBAttribute {

	EventBEvent parentEvent = null;

	public EventBParameter(String label, String comment, ISCParameter scParameter, EventBUnit parentUnit, EventBEvent parentEvent) {
		super(label, comment, scParameter, parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBParameter(IParameter parameter, ISCParameter scParameter, EventBUnit parentUnit, EventBEvent parentEvent) throws RodinDBException {
		super(parameter, scParameter, parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBUnit getParentUnit() {
		return this.getParent();
	}

	public EventBEvent getParentEvent() {
		return parentEvent;
	}

	@Override
	public ISCParameter getScElement() {
		return (ISCParameter) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.PARAMETER;
		return type;
	}
}