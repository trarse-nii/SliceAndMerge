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
public class EventBAction extends EventBElement {

	private static final String TYPE = "ACTION";
	protected String assignment = "";
	protected final EventBEvent parentEvent;

	public EventBAction(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBAction(String label, String assignment, String comment, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, comment, parentUnit);
		this.parentEvent = parentEvent;
		this.assignment = assignment;
	}

	public EventBAction(IAction action, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(parentUnit);
		String label = "";
		String comment = "";
		if (action.hasLabel()) {
			label = action.getLabel();
		}
		if (action.hasAssignmentString()) {
			this.assignment = action.getAssignmentString();
		}
		if (action.hasComment()) {
			comment = action.getComment();
		}
		this.label = label;
		this.comment = comment;
		this.parentEvent = parentEvent;
	}
	
	public String getAssignment() {
		return assignment;
	}
	
	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + assignment + " (" + comment + ")";
	}
}
