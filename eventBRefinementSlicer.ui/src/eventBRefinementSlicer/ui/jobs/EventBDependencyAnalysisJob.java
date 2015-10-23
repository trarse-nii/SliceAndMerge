package eventBRefinementSlicer.ui.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import eventBRefinementSlicer.internal.analyzers.EventBDependencyAnalyzer;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;

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
