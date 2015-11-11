package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IIdentifierElement;
import org.eventb.core.ISCIdentifierElement;
import org.rodinp.core.RodinDBException;

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

	public EventBAttribute(String label, String comment, ISCIdentifierElement scElement, EventBUnit parent) {
		super(label, comment, scElement, parent);
	}

	public EventBAttribute(IIdentifierElement identifierElement, ISCIdentifierElement scElement, EventBUnit parent) throws RodinDBException {
		super(identifierElement, scElement, parent);
		if (identifierElement.hasIdentifierString()) {
			this.label = identifierElement.getIdentifierString();
		}
	}

	@Override
	public ISCIdentifierElement getScElement() {
		return (ISCIdentifierElement) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "ATTRIBUTE";
		return type;
	}

}
