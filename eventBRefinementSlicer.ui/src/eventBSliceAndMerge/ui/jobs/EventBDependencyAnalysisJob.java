package eventBSliceAndMerge.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eventBSliceAndMerge.internal.analyzers.EventBDependencyAnalyzer;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;

/**
 * Job to analyze dependencies in an Event-B Machine
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBDependencyAnalysisJob extends Job {

	private static final String NAME = "EventB Dependency Analysis Job";

	private EventBMachine machineToAnalyze;
	private EventBDependencyAnalyzer dependencyAnalyzer;

	public static void doEventBDependencyAnalysis(EventBMachine machineToAnalyze) {
		EventBDependencyAnalysisJob job = new EventBDependencyAnalysisJob(machineToAnalyze);
		job.setPriority(Job.SHORT);
		job.schedule();
	}

	private EventBDependencyAnalysisJob(EventBMachine machineToAnalyze) {
		super(NAME);
		this.machineToAnalyze = machineToAnalyze;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		dependencyAnalyzer = new EventBDependencyAnalyzer(machineToAnalyze);
		if (!dependencyAnalyzer.runAnalysis()) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

}
