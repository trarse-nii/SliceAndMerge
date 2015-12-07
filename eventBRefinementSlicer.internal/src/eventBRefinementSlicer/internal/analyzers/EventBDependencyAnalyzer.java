package eventBRefinementSlicer.internal.analyzers;

import java.util.HashSet;
import java.util.Set;

import eventBRefinementSlicer.internal.Depender;
import eventBRefinementSlicer.internal.datastructures.EventBAction;
import eventBRefinementSlicer.internal.datastructures.EventBAttribute;
import eventBRefinementSlicer.internal.datastructures.EventBAxiom;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBEvent;
import eventBRefinementSlicer.internal.datastructures.EventBGuard;
import eventBRefinementSlicer.internal.datastructures.EventBInvariant;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;

public class EventBDependencyAnalyzer {

	private EventBMachine analyzedMachine = null;

	public EventBDependencyAnalyzer(EventBMachine machineToAnalyze) {
		analyzedMachine = machineToAnalyze;
	}

	/**
	 * Runs a dependency analysis on the Event B Machine or Context provided earlier.
	 * 
	 * @return True if Analysis was successful, false otherwise
	 */
	public boolean runAnalysis() {
		if (analyzedMachine == null) {
			return false;
		}

		EventBDependencies dependencies = new EventBDependencies();
		for (EventBInvariant invariant : analyzedMachine.getInvariants()) {
			addDependenciesOfElement(dependencies, invariant);
		}
		for (EventBContext context : analyzedMachine.getSeenContexts()) {
			for (EventBAxiom axiom : context.getAxioms()) {
				addDependenciesOfElement(dependencies, axiom);
			}
		}
		for (EventBEvent event : analyzedMachine.getEvents()) {
			for (EventBGuard guard : event.getGuards()) {
				addDependenciesOfElement(dependencies, guard);
			}
			for (EventBAction action : event.getActions()) {
				addDependenciesOfElement(dependencies, action);
			}
		}

		analyzedMachine.setDependencies(dependencies);
		return true;
	}

	private void addDependenciesOfElement(EventBDependencies dependencies, Depender depender) {
		Set<EventBAttribute> shallowerDependees = new HashSet<>();
		Set<EventBAttribute> deeperDependees = depender.getDependees();
		while (!shallowerDependees.equals(deeperDependees)) {
			Set<EventBAttribute> newDependees = new HashSet<>();
			shallowerDependees.addAll(deeperDependees);
			for (EventBAttribute deeperDependee : deeperDependees) {
				newDependees.addAll(deeperDependee.getDependees());
			}
			deeperDependees.addAll(newDependees);
		}
		for (EventBAttribute dependee : deeperDependees) {
			assert depender instanceof EventBElement;
			dependencies.addDependency((EventBElement) depender, dependee);
		}
	}
}
