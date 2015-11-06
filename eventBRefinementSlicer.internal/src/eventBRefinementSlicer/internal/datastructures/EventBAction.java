package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IAction;
import org.eventb.core.ISCAction;
import org.rodinp.core.RodinDBException;

/**
 * @author Aivar Kripsaar
 *
 */
public class EventBAction extends EventBElement {

	protected String assignment = "";
	protected final EventBEvent parentEvent;

	public EventBAction(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBAction(String label, String assignment, String comment, ISCAction scAction, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, comment, scAction, parentUnit);
		this.parentEvent = parentEvent;
		this.assignment = assignment;
	}

	public EventBAction(IAction action, ISCAction scAction, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
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
		this.scElement = scAction;
		this.parentEvent = parentEvent;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	@Override
	public ISCAction getScElement() {
		return (ISCAction) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "ACTION";
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": [" + (selected ? "x" : " ") + "] " + label + ": " + assignment + " (" + comment + ")";
	}
}
