package eventBRefinementSlicer.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.basis.MachineRoot;
import org.rodinp.core.IRefinementManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
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

	private IRodinProject rodinProject;
	private IMachineRoot concreteMachineRoot;
	private IMachineRoot abstractMachineRoot;
	private EventBMachine concreteMachine;
	private EventBMachine abstractMachine;

	private MachineNamingWizardPage machineNamingPage;

	private String machineName;

	public MergeMachineWithPredecessorWizard() {
		// TODO Auto-generated constructor stub
	}

	public MergeMachineWithPredecessorWizard(IRodinProject rodinProject, IMachineRoot concreteMachineRoot) {
		super();
		this.rodinProject = rodinProject;
		this.concreteMachineRoot = concreteMachineRoot;

		// There's only one refines clause per machine by definition. We can't access it directly because
		// Rodin is silly like that.
		try {
			for (IRefinesMachine refinesMachine : concreteMachineRoot.getRefinesClauses()) {
				this.abstractMachineRoot = refinesMachine.getAbstractMachineRoot();
			}
		} catch (RodinDBException e) {
			// TODO: handle exception
		}

		// TODO Exit with error on invalid input (machine with no predecessor)
		// Or maybe just display error in wizard and block wizard from finishing?
	}

	@Override
	public void addPages() {
		machineNamingPage = new MachineNamingWizardPage();
		addPage(machineNamingPage);
	}

	@Override
	public boolean performFinish() {

		machineNamingPage.finish();
		machineName = machineNamingPage.getMachineNameInput();

		try {
			createMachine(machineName);
		} catch (RodinDBException e) {
			// TODO: handle exception
			System.out.println();
		}

		return true;
	}

	private void createMachine(String machineName) throws RodinDBException {
		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// We create a new Rodin Machine in the Rodin Project
				IRodinFile file = rodinProject.getRodinFile(machineName.concat(".bum"));
				file.create(true, null);
				file.getResource().setDerived(true, null);
				MachineRoot root = (MachineRoot) file.getRoot();
				root.setConfiguration(IConfigurationElement.DEFAULT_CONFIGURATION, monitor);

				// If the abstract machine refines another machine, the new machine inherits this refinement
				for (IRefinesMachine refinesClause : abstractMachineRoot.getRefinesClauses()) {
					IRefinementManager refinementManager = RodinCore.getRefinementManager();
					refinementManager.refine(refinesClause.getAbstractMachineRoot(), root, null);
				}

				// Copy all invariants from both machines into new one
				// We start with the abstract machine
				for (IInvariant invariant : abstractMachineRoot.getInvariants()) {
					invariant.copy(root, null, null, false, null);
				}
				// And then we add the concrete invariants
				for (IInvariant invariant : concreteMachineRoot.getInvariants()) {
					String invariantName = invariant.getElementName();
					while (root.getInvariant(invariantName).exists()) {
						invariantName = invariantName + "_";
					}
					invariant.copy(root, null, invariantName, false, null);
				}

				// Save the final result
				file.save(null, false);

				// Open new editor window for newly created machine

				IFile resource = file.getResource();
				IEditorDescriptor editorDesc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(resource.getName());

				getShell().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						try {
							page.openEditor(new FileEditorInput(resource), editorDesc.getId());
						} catch (PartInitException e) {
							e.printStackTrace();
						}
					}
				});

			}
		}, null);
	}
}
