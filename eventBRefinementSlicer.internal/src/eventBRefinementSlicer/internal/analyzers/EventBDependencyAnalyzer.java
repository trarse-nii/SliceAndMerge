package eventBRefinementSlicer.internal.analyzers;

import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
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
		// TODO: Implement actual analysis.
		if (analyzedMachine == null) {
			return false;
		}

		// This is a fake analysis for testing purposes.
		EventBDependencies dependencies = new EventBDependencies();
		dependencies.addDependency(analyzedMachine.getInvariants().get(0),
				analyzedMachine.getVariables().get(0));
		dependencies.addDependency(analyzedMachine.getInvariants().get(1),
				analyzedMachine.getVariables().get(0));

		analyzedMachine.setDependencies(dependencies);
		return true;
	}

}
