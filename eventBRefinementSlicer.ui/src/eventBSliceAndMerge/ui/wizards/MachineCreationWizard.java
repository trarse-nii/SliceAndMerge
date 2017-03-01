package eventBSliceAndMerge.ui.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.IMachineRoot;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.analyzers.EventBSliceSelection;
import eventBSliceAndMerge.internal.analyzers.EventBSlicer;

/**
 * Wizard for creating a new Event-B machine in a Rodin project given a
 * selection of elements from an existing machine.
 * 
 * @author Aivar Kripsaar
 *
 */

public class MachineCreationWizard extends Wizard {

	private IRodinProject rodinProject;
	private IMachineRoot originalMachineRoot;
	private EventBSliceSelection selection;

	private MachineNamingWizardPage machineNamingPage;

	private String machineName;

	public MachineCreationWizard(IRodinProject rodinProject, IMachineRoot originalMachineRoot,
			EventBSliceSelection selection) {
		super();
		this.rodinProject = rodinProject;
		this.originalMachineRoot = originalMachineRoot;
		this.selection = selection;

	}

	@Override
	public void addPages() {
		String title = "Create Sub-Refinement";
		String description = "Enter a name for the new derived sub-refinement machine";

		machineNamingPage = new MachineNamingWizardPage(title, description);
		addPage(machineNamingPage);
	}

	@Override
	public boolean performFinish() {
		machineNamingPage.finish();
		machineName = machineNamingPage.getMachineNameInput();

		try {
			createMachineFromSelection(machineName);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Creates Event-B machine with the given name based on a selection of
	 * elements the wizard has already received.
	 * 
	 * @param machineName
	 *            Name for the new machine
	 * @throws RodinDBException
	 */
	private void createMachineFromSelection(String machineName) throws RodinDBException {
		// Create a new slice machine on machineName.bum
		EventBSlicer.createMachineFromSelection(machineName, selection, rodinProject, originalMachineRoot);

		// Open new editor window for newly created machine
		IRodinFile file = rodinProject.getRodinFile(machineName.concat(".bum"));
		IFile resource = file.getResource();
		IEditorDescriptor editorDesc = PlatformUI.getWorkbench().getEditorRegistry()
				.getDefaultEditor(resource.getName());
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

}
