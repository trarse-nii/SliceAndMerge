package eventBRefinementSlicer.ui.wizards;

import java.util.ArrayList;
import java.util.List;

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
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.basis.MachineRoot;
import org.rodinp.core.IRefinementManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

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
					// The internal names might contain duplicates. We resolve this by appending something at
					// the end.
					String invariantName = invariant.getElementName();
					while (root.getInvariant(invariantName).exists()) {
						invariantName = invariantName + "_";
					}
					invariant.copy(root, null, invariantName, false, null);
				}

				// We keep track of the variables already included to avoid duplicates
				List<String> alreadyIncludedVariables = new ArrayList<>();
				for (IVariable variable : root.getVariables()) {
					alreadyIncludedVariables.add(variable.getIdentifierString());
				}

				// Copy all variables from both machines into new one
				// First the abstract machine
				for (IVariable variable : abstractMachineRoot.getVariables()) {
					if (alreadyIncludedVariables.contains(variable.getIdentifierString())) {
						// We avoid adding duplicates.
						continue;
					}
					// The internal names might contain duplicates. We resolve this by appending something at
					// the end.
					String variableName = variable.getElementName();
					while (root.getVariable(variableName).exists()) {
						variableName = variableName + "_";
					}
					variable.copy(root, null, variableName, false, null);
					alreadyIncludedVariables.add(variable.getIdentifierString());
				}
				// And the concrete variables
				for (IVariable variable : concreteMachineRoot.getVariables()) {
					if (alreadyIncludedVariables.contains(variable.getIdentifierString())) {
						// We avoid adding duplicates.
						continue;
					}
					// The internal names might contain duplicates. We resolve this by appending something at
					// the end.
					String variableName = variable.getElementName();
					while (root.getVariable(variableName).exists()) {
						variableName = variableName + "_";
					}
					variable.copy(root, null, variableName, false, null);
					alreadyIncludedVariables.add(variable.getIdentifierString());
				}

				// We take over all the seen contexts from both machines
				// We keep a list of names of already included contexts to avoid duplicates
				List<String> alreadyIncludedContexts = new ArrayList<>();
				for (ISeesContext seenContext : root.getSeesClauses()) {
					alreadyIncludedContexts.add(seenContext.getSeenContextName());
				}
				// We first copy the seen contexts from the abstract machine
				// We need to look out for collisions between internal names
				for (ISeesContext seenContext : abstractMachineRoot.getSeesClauses()) {
					if (alreadyIncludedContexts.contains(seenContext.getSeenContextName())) {
						continue;
					}
					String elementName = seenContext.getElementName();
					while (root.getSeesClause(elementName).exists()) {
						elementName += "_";
					}
					seenContext.copy(root, null, null, false, null);
					alreadyIncludedContexts.add(seenContext.getSeenContextName());
				}
				// Then we copy the seen contexts from the concrete machine
				for (ISeesContext seenContext : concreteMachineRoot.getSeesClauses()) {
					if (alreadyIncludedContexts.contains(seenContext.getSeenContextName())) {
						continue;
					}
					String elementName = seenContext.getElementName();
					while (root.getSeesClause(elementName).exists()) {
						elementName += "_";
					}
					seenContext.copy(root, null, elementName, false, null);
					alreadyIncludedContexts.contains(seenContext.getSeenContextName());
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
