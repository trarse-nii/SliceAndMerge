package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ISCVariable;
import org.eventb.core.IVariable;
import org.rodinp.core.RodinDBException;

public class EventBVariable extends EventBAttribute {
	
	private boolean mAbstract = false;

	public EventBVariable(String label, String comment, ISCVariable scVariable, EventBUnit parent) {
		super(label, comment, scVariable, parent);
		this.mAbstract = false;
	}

	public EventBVariable(IVariable variable, ISCVariable scVariable, EventBUnit parent) throws RodinDBException {
		super(variable, scVariable, parent);
		this.mAbstract = false;
	}

	public EventBVariable(ISCVariable scVariable, EventBUnit parent) throws RodinDBException {
		super(scVariable, parent);
		this.mAbstract = true;
	}

	@Override
	public ISCVariable getScElement() {
		return (ISCVariable) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = "VARIABLE";
		return type;
	}

	public boolean isAbstract() {
		return mAbstract;
	}
}
