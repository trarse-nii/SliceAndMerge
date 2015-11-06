package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEvent;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ISCEvent;
import org.eventb.core.ISCInvariant;
import org.eventb.core.ISCMachineRoot;
import org.eventb.core.ISCVariable;
import org.eventb.core.ISeesContext;
import org.eventb.core.ITraceableElement;
import org.eventb.core.IVariable;

import eventBRefinementSlicer.internal.util.SCUtil;

public class EventBMachine extends EventBUnit {

	private List<EventBInvariant> invariants = new ArrayList<>();
	private List<EventBVariable> variables = new ArrayList<>();
	private Set<EventBContext> seenContexts = new HashSet<>();
	private List<EventBEvent> events = new ArrayList<>();
	private ISCMachineRoot scMachineRoot = null;

	public EventBMachine(IMachineRoot machineRoot) throws CoreException {
		ISCMachineRoot scMachineRoot = SCUtil.makeStaticCheckedMachine(machineRoot);
		for (IInvariant originalInvariant : machineRoot.getInvariants()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalInvariant, scMachineRoot.getSCInvariants());
			assert (originalSCElement instanceof ISCInvariant);
			EventBInvariant invariant = new EventBInvariant(originalInvariant, (ISCInvariant) originalSCElement, this);
			invariants.add(invariant);
		}
		for (IVariable originalVariable : machineRoot.getVariables()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalVariable, scMachineRoot.getSCVariables());
			assert (originalSCElement instanceof ISCVariable);
			EventBVariable variable = new EventBVariable(originalVariable, (ISCVariable) originalSCElement, this);
			variables.add(variable);
		}
		for (ISeesContext seenContext : machineRoot.getSeesClauses()) {
			IContextRoot contextRoot = seenContext.getSeenContextRoot();
			EventBContext context = new EventBContext(contextRoot);
			seenContexts.add(context);
		}
		for (IEvent originalEvent : machineRoot.getEvents()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalEvent, scMachineRoot.getSCEvents());
			assert (originalSCElement instanceof ISCEvent);
			EventBEvent event = new EventBEvent(originalEvent, (ISCEvent) originalSCElement, this);
			events.add(event);
		}
		this.scMachineRoot = scMachineRoot;
	}

	public List<EventBInvariant> getInvariants() {
		return invariants;
	}

	public List<EventBVariable> getVariables() {
		return variables;
	}

	public Set<EventBContext> getSeenContexts() {
		return seenContexts;
	}

	public List<EventBEvent> getEvents() {
		return events;
	}

	public ISCMachineRoot getScMachineRoot() {
		return scMachineRoot;
	}

	public void addSeenContext(EventBContext context) {
		seenContexts.add(context);
	}

	public void removeSeenContext(EventBContext context) {
		seenContexts.remove(context);
	}
}
