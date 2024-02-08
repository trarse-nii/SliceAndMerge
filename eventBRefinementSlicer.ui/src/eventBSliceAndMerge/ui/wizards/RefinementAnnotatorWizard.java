package eventBSliceAndMerge.ui.wizards;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;

import eventBSliceAndMerge.internal.analyzers.RefinementAnnotator;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;

/**
 * Wizard for annotating event refinement.
 * 
 * @author Tsutomu Kobayashi
 *
 */

public class RefinementAnnotatorWizard extends Wizard {
	private RefinementAnnotatorWizardPage refinementAnnotatorWizardPage;
	private EventBMachine machine;

	public RefinementAnnotatorWizard(EventBMachine machine) {
		super();
		this.machine = machine;
	}

	@Override
	public void addPages() {
		String title = "Analyze event refinement";
		String description = "Analyze event refinement";

		refinementAnnotatorWizardPage = new RefinementAnnotatorWizardPage(title, description);
		addPage(refinementAnnotatorWizardPage);
	}

	@Override
	public boolean performFinish() {
		refinementAnnotatorWizardPage.finish();
		return true;
	}
	
	public void analyze() {
		try {
			RefinementAnnotator annotator = new RefinementAnnotator(machine);
			refinementAnnotatorWizardPage.setResult(annotator.annotateAll());
		} catch (IOException | CoreException e) {
			e.printStackTrace();
		}
	}
}