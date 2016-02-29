package eventBSliceAndMerge.internal.analyzers;

import java.util.HashSet;
import java.util.Set;

import eventBSliceAndMerge.internal.Depender;
import eventBSliceAndMerge.internal.datastructures.EventBAction;
import eventBSliceAndMerge.internal.datastructures.EventBAttribute;
import eventBSliceAndMerge.internal.datastructures.EventBAxiom;
import eventBSliceAndMerge.internal.datastructures.EventBContext;
import eventBSliceAndMerge.internal.datastructures.EventBDependencies;
import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBEvent;
import eventBSliceAndMerge.internal.datastructures.EventBGuard;
import eventBSliceAndMerge.internal.datastructures.EventBInvariant;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;

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
