package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IConstant;
import org.eventb.core.ISCConstant;
import org.rodinp.core.RodinDBException;

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
		final String type = "CONSTANT";
		return type;
	}
}
