package eventBRefinementSlicer.internal.analyzers;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Predicate;

import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
import eventBRefinementSlicer.internal.datastructures.EventBInvariant;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;

public class EventBDependencyAnalyzer {

	private EventBMachine analyzedMachine = null;
	
	public EventBDependencyAnalyzer(EventBMachine machineToAnalyze) {
		analyzedMachine = machineToAnalyze;
	}

	/**
	 * Runs a dependency analysis on the Event B Machine or Context provided
	 * earlier.
	 * 
	 * @return True if Analysis was successful, false otherwise
	 */
	public boolean runAnalysis() {
		if (analyzedMachine == null) {
			return false;
		}

		EventBDependencies dependencies = new EventBDependencies();
		for (EventBInvariant invariant : analyzedMachine.getInvariants()) {
			addDependencyOfInvariant(dependencies, invariant);
		}

		analyzedMachine.setDependencies(dependencies);
		return true;
	}
	
	private void addDependencyOfInvariant(EventBDependencies dependencies, EventBInvariant invariant) {
		try {
			ITypeEnvironment typeEnvironment = analyzedMachine.getScMachineRoot().getTypeEnvironment();
			Predicate pred = invariant.getScElement().getPredicate(typeEnvironment);
			for (FreeIdentifier freeIdentifier : pred.getFreeIdentifiers()) {
				dependencies.addDependency(invariant, analyzedMachine.findAttributeByLabel(freeIdentifier.getName()));
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
