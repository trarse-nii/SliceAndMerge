package eventBSliceAndMerge.internal.datastructures;

import org.eventb.core.IParameter;
import org.eventb.core.ISCParameter;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation of Event-B Attributes.
 * 
 * @author Aivar Kripsaar
 *
 */
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

	@Override
	public String getLabelFullPath() {
		return parentEvent.label + "/" + label;
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
	public Type getType() {
		return Type.PARAMETER;
	}
}
