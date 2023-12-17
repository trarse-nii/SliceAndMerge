package eventBSliceAndMerge.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eventb.core.ast.*;

import eventBSliceAndMerge.internal.analyzers.EventBSliceSelection;
import eventBSliceAndMerge.internal.analyzers.POInterpolator;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;

/**
 * Wizard for generating a PO's interpolant written in the variables selected on SelectionEditor.
 * 
 * @author Tsutomu Kobayashi
 *
 */

public class POInterpolationWizard extends Wizard {
	private POInterpolator interpolator;
	private POInterpolationWizardPage smtPOGenerationPage;

	public POInterpolationWizard(EventBMachine machine, EventBSliceSelection selection) {
		super();
		ITypeEnvironment typeEnvironment = null;
		try {
			typeEnvironment = machine.getScMachineRoot().getTypeEnvironment();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		this.interpolator = new POInterpolator(machine, typeEnvironment, selection);
	}

	@Override
	public void addPages() {
		String title = "Generate an interpolant of a PO";
		String description = "Enter the name of the target PO";

		smtPOGenerationPage = new POInterpolationWizardPage(title, description);
		addPage(smtPOGenerationPage);
	}

	@Override
	public boolean performFinish() {
		smtPOGenerationPage.finish();
		return true;
	}
	
	public void generate(String poNameInput) {
		try {
			smtPOGenerationPage.setResult(interpolator.complementaryPredString(poNameInput));
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
}
