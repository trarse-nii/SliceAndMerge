package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAction;
import org.eventb.core.IConvergenceElement.Convergence;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCGuard;
import org.eventb.core.ITraceableElement;

import eventBRefinementSlicer.internal.util.SCUtil;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBEvent extends EventBElement {
	private boolean isExtended = false;
	private Convergence convergence = Convergence.ORDINARY;

	private List<EventBGuard> guards = new ArrayList<>();
	private List<EventBAction> actions = new ArrayList<>();

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
	}

	public List<EventBGuard> getGuards() {
		return guards;
	}

	public List<EventBAction> getActions() {
		return actions;
	}

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

	public boolean isEmpty() {
		return guards.isEmpty() && actions.isEmpty();
	}

	public boolean isExtended() {
		return isExtended;
	}

	public Convergence getConvergence() {
		return convergence;
	}

	@Override
	public String getType() {
		final String type = EventBTypes.EVENT;
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + " (" + comment + ")";
	}
}
