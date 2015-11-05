package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IAction;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCGuard;
import org.eventb.core.ITraceableElement;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.util.SCUtil;

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

	public EventBEvent(String label, String comment, ISCEvent scEvent, EventBUnit parent) {
		super(label, comment, scEvent, parent);
	}

	public EventBEvent(IEvent event, ISCEvent scEvent, EventBUnit parent) throws RodinDBException {
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
		scElement = scEvent;
		for (IGuard originalGuard : event.getGuards()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalGuard, scEvent.getSCGuards());
			assert (originalSCElement instanceof ISCGuard);
			EventBGuard guard = new EventBGuard(originalGuard, (ISCGuard) originalSCElement, this, parent);
			guards.add(guard);
		}
		for (IAction originalAction : event.getActions()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalAction, scEvent.getSCActions());
			assert (originalSCElement instanceof ISCAction);
			EventBAction action = new EventBAction(originalAction, (ISCAction) originalSCElement, this, parent);
			actions.add(action);
		}
	}

	public List<EventBGuard> getGuards() {
		return guards;
	}

	public List<EventBAction> getActions() {
		return actions;
	}
	
	@Override
	public ISCEvent getScElement() {
		return (ISCEvent) scElement;
	}

	public boolean isEmpty() {
		return guards.isEmpty() && actions.isEmpty();
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")";
	}
}
