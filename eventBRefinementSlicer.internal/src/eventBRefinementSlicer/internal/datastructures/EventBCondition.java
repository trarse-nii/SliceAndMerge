package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IDerivedPredicateElement;
import org.eventb.core.ILabeledElement;
import org.eventb.core.ISCPredicateElement;
import org.rodinp.core.RodinDBException;

/**
 * Common parent class for EventBInvariant, EventBAxiom, and EventBGuard
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBCondition extends EventBElement {

	protected String predicate = "";
	protected boolean isTheorem = false;

	public EventBCondition(EventBUnit parent) {
		super(parent);
	}

	public EventBCondition(String label, String predicate, ISCPredicateElement scPredicateElement, String comment, EventBUnit parent) {
		super(label, comment, scPredicateElement, parent);
		this.predicate = predicate;
	}

	public EventBCondition(IDerivedPredicateElement predicateElement, ISCPredicateElement scPredicateElement, EventBUnit parent)
			throws RodinDBException {
		super(predicateElement, scPredicateElement, parent);
		if (predicateElement instanceof ILabeledElement && ((ILabeledElement) predicateElement).hasLabel()) {
			this.label = ((ILabeledElement) predicateElement).getLabel();
		}
		if (predicateElement.hasPredicateString()) {
			this.predicate = predicateElement.getPredicateString();
		}
		this.isTheorem = predicateElement.isTheorem();
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public boolean isTheorem() {
		return isTheorem;
	}

	@Override
	public ISCPredicateElement getScElement() {
		return (ISCPredicateElement) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "CONDITION";
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + ": " + predicate + " (" + comment + ")";
	}

	@Override
	public Object[] toArray() {
		Object[] array = { getLabel(), getPredicate(), getComment() };
		return array;
	}
}
