package eventBRefinementSlicer.internal.datastructures;

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

	public EventBCondition(String label, String predicate, String comment, EventBUnit parent) {
		super(label, comment, parent);
		this.predicate = predicate;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
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
