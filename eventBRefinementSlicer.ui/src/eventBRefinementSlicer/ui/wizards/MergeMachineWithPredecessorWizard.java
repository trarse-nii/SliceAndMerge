package eventBRefinementSlicer.ui.wizards;

import org.eclipse.jface.wizard.Wizard;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesMachine;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.datastructures.EventBMachine;

/**
 * Wizard to create a new machine, which merges the machine in the editor with the machine it directly
 * refines.
 * 
 * @author Aivar Kripsaar
 *
 */

public class MergeMachineWithPredecessorWizard extends Wizard {

	IRodinProject rodinProject;
	IMachineRoot concreteMachineRoot;
	IMachineRoot abstractMachineRoot;
	EventBMachine concreteMachine;
	EventBMachine abstractMachine;

	public MergeMachineWithPredecessorWizard() {
		// TODO Auto-generated constructor stub
	}

	public MergeMachineWithPredecessorWizard(IRodinProject rodinProject, IMachineRoot concreteMachineRoot) throws RodinDBException {
		super();
		this.rodinProject = rodinProject;
		this.concreteMachineRoot = concreteMachineRoot;

		// There's only one refines clause per machine by definition. We can't access it directly because
		// Rodin is silly like that.
		for (IRefinesMachine refinesMachine : concreteMachineRoot.getRefinesClauses()) {
			this.abstractMachineRoot = refinesMachine.getAbstractMachineRoot();
		}

		// TODO Exit with error on invalid input (machine with no predecessor)
		// Or maybe just display error in wizard and block wizard from finishing?
	}

	@Override
	public boolean performFinish() {
		// TODO Auto-generated method stub
		return false;
	}

}
