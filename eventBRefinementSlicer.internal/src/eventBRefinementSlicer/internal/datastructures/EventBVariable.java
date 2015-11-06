package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ISCVariable;
import org.eventb.core.IVariable;
import org.rodinp.core.RodinDBException;

public class EventBVariable extends EventBAttribute {

	private static final String TYPE = "VARIABLE";

	public EventBVariable(String label, String comment, ISCVariable scVariable, EventBUnit parent) {
		super(label, comment, scVariable, parent);
	}

	public EventBVariable(IVariable variable, ISCVariable scVariable, EventBUnit parent) throws RodinDBException {
		super(parent);
		String label = "";
		String comment = "";
		if (variable.hasIdentifierString()) {
			label = variable.getIdentifierString();
		}
		if (variable.hasComment()) {
			comment = variable.getComment();
		}
		this.label = label;
		this.comment = comment;
		this.scElement = scVariable;
	}
	
	@Override
	public ISCVariable getScElement() {
		return (ISCVariable) super.getScElement();
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")";
	}
}
