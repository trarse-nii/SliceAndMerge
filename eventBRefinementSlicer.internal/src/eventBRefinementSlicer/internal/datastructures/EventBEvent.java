package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IAction;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBEvent extends EventBElement {
	private static final String TYPE = "EVENT";

	List<EventBVariable> localVariables = new ArrayList<>();
	List<EventBGuard> guards = new ArrayList<>();
	List<EventBAction> actions = new ArrayList<>();

	public EventBEvent(EventBUnit parent) {
		super(parent);
	}

	public EventBEvent(String label, String comment, EventBUnit parent) {
		super(label, comment, parent);
	}

	public EventBEvent(IEvent event, EventBUnit parent) throws RodinDBException {
		super(parent);
		String label = "";
		String comment = "";
		if (event.hasLabel()) {
			label = event.getLabel();
		}
		if (event.hasComment()) {
			comment = event.getComment();
		}
		this.label = label;
		this.comment = comment;
		for (IGuard originalGuard : event.getGuards()) {
			EventBGuard guard = new EventBGuard(originalGuard, this, parent);
			guards.add(guard);
		}
		for (IAction originalAction : event.getActions()) {
			EventBAction action = new EventBAction(originalAction, this, parent);
			actions.add(action);
		}
	}

	public List<EventBGuard> getGuards() {
		return guards;
	}

	public List<EventBAction> getActions() {
		return actions;
	}

	public boolean isEmpty() {
		return guards.isEmpty() && actions.isEmpty();
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")";
	}
}
