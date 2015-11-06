package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ISCPredicateElement;

/**
 * Common parent class for EventBInvariant, EventBAxiom, and EventBGuard
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBCondition extends EventBElement {

	protected String predicate = "";

	public EventBCondition(EventBUnit parent) {
		super(parent);
	}

	public EventBCondition(String label, String predicate, ISCPredicateElement scPredicateElement, String comment, EventBUnit parent) {
		super(label, comment, scPredicateElement, parent);
		this.predicate = predicate;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
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
