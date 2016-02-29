package eventBSliceAndMerge.internal.datastructures;

import org.eventb.core.IConstant;
import org.eventb.core.ISCConstant;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation of Event-B constants, which are elements of Event-B contexts
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBConstant extends EventBAttribute {

	public EventBConstant(String label, String comment, ISCConstant scConstant, EventBUnit parent) {
		super(label, comment, scConstant, parent);
	}

	public EventBConstant(IConstant constant, ISCConstant scConstant, EventBUnit parent) throws RodinDBException {
		super(constant, scConstant, parent);
	}

	@Override
	public ISCConstant getScElement() {
		return (ISCConstant) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.CONSTANT;
		return type;
	}
}
