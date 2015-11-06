package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IAxiom;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCPredicateElement;
import org.rodinp.core.RodinDBException;

public class EventBAxiom extends EventBCondition {

	public EventBAxiom(String label, String predicate, ISCPredicateElement scPredicateElement, String comment, EventBUnit parent) {
		super(label, predicate, scPredicateElement, comment, parent);
	}

	public EventBAxiom(IAxiom axiom, ISCAxiom scAxiom, EventBUnit parent) throws RodinDBException {
		super(axiom, scAxiom, parent);
	}

	@Override
	public ISCAxiom getScElement() {
		return (ISCAxiom) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "AXIOM";
		return type;
	}
}
