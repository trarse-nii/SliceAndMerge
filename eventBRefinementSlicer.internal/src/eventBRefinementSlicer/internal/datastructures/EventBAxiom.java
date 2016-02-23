package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IAxiom;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCPredicateElement;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation of Event-B axioms, which are elements of Event B contexts
 * 
 * @author Aivar Kripsaar
 *
 */

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
	public String getType() {
		final String type = EventBTypes.AXIOM;
		return type;
	}
}
