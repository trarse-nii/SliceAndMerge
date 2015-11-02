package eventBRefinementSlicer.internal.datastructures;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBEventCondition extends EventBCondition {
	private static final String TYPE = "EVENT CONDITION";
	protected final EventBEvent parentEvent;

	public EventBEventCondition(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBEventCondition(String label, String predicate, String comment, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.label = label;
		this.predicate = predicate;
		this.comment = comment;
		this.parentEvent = parentEvent;
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + predicate + " (" + comment + ")";
	}

}
