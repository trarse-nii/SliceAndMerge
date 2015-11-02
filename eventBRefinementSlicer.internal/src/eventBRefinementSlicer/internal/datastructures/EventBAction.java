/**
 * 
 */
package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IAction;
import org.rodinp.core.RodinDBException;

/**
 * @author Aivar Kripsaar
 *
 */
public class EventBAction extends EventBEventCondition {

	private static final String TYPE = "ACTION";

	public EventBAction(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentEvent, parentUnit);
	}

	public EventBAction(String label, String predicate, String comment, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, predicate, comment, parentEvent, parentUnit);
	}

	public EventBAction(IAction action, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(parentEvent, parentUnit);
		String label = "";
		String predicate = "";
		String comment = "";
		if (action.hasLabel()) {
			label = action.getLabel();
		}
		if (action.hasAssignmentString()) {
			predicate = action.getAssignmentString();
		}
		if (action.hasComment()) {
			comment = action.getComment();
		}
		this.label = label;
		this.predicate = predicate;
		this.comment = comment;
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + predicate + " (" + comment + ")";
	}
}
