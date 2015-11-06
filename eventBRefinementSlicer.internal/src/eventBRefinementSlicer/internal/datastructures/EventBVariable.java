package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ISCVariable;
import org.eventb.core.IVariable;
import org.rodinp.core.RodinDBException;

public class EventBVariable extends EventBAttribute {

	public EventBVariable(String label, String comment, ISCVariable scVariable, EventBUnit parent) {
		super(label, comment, scVariable, parent);
	}

	public EventBVariable(IVariable variable, ISCVariable scVariable, EventBUnit parent) throws RodinDBException {
		super(variable, scVariable, parent);
	}

	@Override
	public ISCVariable getScElement() {
		return (ISCVariable) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "VARIABLE";
		return type;
	}

}
