package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ISCPredicateElement;

/**
 * Common parent class for EventBInvariant, EventBAxiom, and EventBGuard
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBCondition extends EventBElement {

	private static final String TYPE = "CONDITION";

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
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + predicate + " (" + comment + ")";
	}

	@Override
	public Object[] toArray() {
		Object[] array = { isSelected(), getLabel(), getPredicate(), getComment() };
		return array;
	}
}
