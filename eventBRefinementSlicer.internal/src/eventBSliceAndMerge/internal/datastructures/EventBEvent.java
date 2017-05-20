package eventBSliceAndMerge.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAction;
import org.eventb.core.IConvergenceElement.Convergence;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCGuard;
import org.eventb.core.ISCParameter;
import org.eventb.core.ISCWitness;
import org.eventb.core.ITraceableElement;
import org.eventb.core.IWitness;

import eventBSliceAndMerge.internal.util.SCUtil;

/**
 * Internal representation of Event-B Events.
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBEvent extends EventBElement {
	private boolean isExtended = false;
	private Convergence convergence = Convergence.ORDINARY;

	private List<EventBGuard> guards = new ArrayList<>();
	private List<EventBAction> actions = new ArrayList<>();
	private List<EventBRefinedEvent> refinedEvents = new ArrayList<>();
	private List<EventBParameter> parameters = new ArrayList<>();
	private List<EventBWitness> witnesses = new ArrayList<>();

	public EventBEvent(EventBUnit parent) {
		super(parent);
	}

	public EventBEvent(String label, String comment, ISCEvent scEvent, EventBUnit parent) {
		super(label, comment, scEvent, parent);
	}

	public EventBEvent(IEvent event, ISCEvent scEvent, EventBUnit parent) throws CoreException {
		super(event, scEvent, parent);
		if (event.hasLabel()) {
			this.label = event.getLabel();
		}
		if (event.hasExtended()) {
			this.isExtended = event.isExtended();
		}
		if (event.hasConvergence()) {
			this.convergence = event.getConvergence();
		}
		for (IRefinesEvent originalRefinedEvent : event.getRefinesClauses()) {
			EventBRefinedEvent refinedEvent = new EventBRefinedEvent(originalRefinedEvent, this, parent);
			refinedEvents.add(refinedEvent);
		}
		for (IParameter originalParameter : event.getParameters()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalParameter, scEvent.getSCParameters());
			assert (originalSCElement instanceof ISCParameter);
			EventBParameter parameter = new EventBParameter(originalParameter, (ISCParameter) originalSCElement, parent, this);
			parameters.add(parameter);
		}
		for (IWitness originalWitness : event.getWitnesses()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalWitness, scEvent.getSCWitnesses());
			assert (originalSCElement instanceof ISCWitness);
			EventBWitness witness = new EventBWitness(originalWitness, (ISCWitness) originalSCElement, parent, this);
			witnesses.add(witness);
		}
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
		for (EventBGuard guard : guards) {
			guard.setDependees(guard.calculateDependees());
		}
		for (EventBAction action : actions) {
			action.setDependees(action.calculateDependees());
		}
	}

	public List<EventBGuard> getGuards() {
		return guards;
	}

	public List<EventBAction> getActions() {
		return actions;
	}

	public List<EventBParameter> getParameters() {
		return parameters;
	}

	public List<EventBWitness> getWitnesses() {
		return witnesses;
	}

	public List<EventBRefinedEvent> getRefinedEvents() {
		return refinedEvents;
	}

	/**
	 * Checks if this event contains the given element.
	 * 
	 * @param element
	 *            The element to check for
	 * @return True if this event contains this element
	 */
	public boolean containsElement(EventBElement element) {
		if (guards.contains(element) || actions.contains(element)) {
			return true;
		}
		return false;
	}

	@Override
	public ISCEvent getScElement() {
		return (ISCEvent) scElement;
	}

	/**
	 * Checks if this event contains any elements (i.e. guards, actions, parameters and witnesses)
	 * 
	 * @return True if this event contains no elements
	 */
	public boolean isEmpty() {
		return guards.isEmpty() && actions.isEmpty() && parameters.isEmpty() && witnesses.isEmpty();
	}

	public boolean isExtended() {
		return isExtended;
	}

	public Convergence getConvergence() {
		return convergence;
	}

	@Override
	public Type getType() {
		return Type.EVENT;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + " (" + comment + ")";
	}
}
