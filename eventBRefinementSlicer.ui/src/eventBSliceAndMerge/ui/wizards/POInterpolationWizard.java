package eventBSliceAndMerge.ui.wizards;

import java.io.IOException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eventb.core.ast.*;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.analyzers.EventBSliceSelection;
import eventBSliceAndMerge.internal.analyzers.POInterpolator;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;
import eventBSliceAndMerge.internal.util.Z3Util;

/**
 * Wizard for generating a PO's interpolant written in the variables selected on SelectionEditor.
 * 
 * @author Tsutomu Kobayashi
 *
 */

public class POInterpolationWizard extends Wizard {
	private POInterpolator interpolator;
	private POInterpolationWizardPage smtPOGenerationPage;
	private String poName;

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
		String z3Result = null;
		poName = poNameInput;
		
		try {
			interpolator.createSMTInputFile(poName);
			z3Result = Z3Util.runZ3(interpolator.getInputFilePath());
			System.out.println(z3Result);
			interpolator.createSMTOutputFile(z3Result);
			smtPOGenerationPage.setResult(interpolator.eventBInterpolant());
		} catch (RodinDBException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
