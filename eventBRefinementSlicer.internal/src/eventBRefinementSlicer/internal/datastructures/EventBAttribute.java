package eventBRefinementSlicer.internal.datastructures;

import org.rodinp.core.IInternalElement;

/**
 * Common parent class for EventBVariable and EventBConstant
 * 
 * @author Aivar Kripsaar
 * 
 */

public class EventBAttribute extends EventBElement {

	public EventBAttribute(EventBUnit parent) {
		super(parent);
	}

	public EventBAttribute(String label, String comment, IInternalElement scElement, EventBUnit parent) {
		super(label, comment, scElement, parent);
	}

	@Override
	protected String getType() {
		final String type = "ATTRIBUTE";
		return type;
	}

}
