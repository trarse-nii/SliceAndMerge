package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IVariable;
import org.rodinp.core.RodinDBException;

public class EventBVariable extends EventBAttribute {

	private static final String TYPE = "VARIABLE";

	public EventBVariable(String label, String comment, EventBUnit parent) {
		super(label, comment, parent);
	}

	public EventBVariable(IVariable variable, EventBUnit parent) throws RodinDBException {
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
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")";
	}
}
