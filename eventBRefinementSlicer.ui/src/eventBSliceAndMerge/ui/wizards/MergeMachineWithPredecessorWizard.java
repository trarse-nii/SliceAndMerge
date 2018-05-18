package eventBSliceAndMerge.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.eventb.core.IAction;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IInvariant;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.IVariant;
import org.eventb.core.IWitness;
import org.eventb.core.basis.MachineRoot;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRefinementManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.ui.util.RodinUtil;

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

	public MergeMachineWithPredecessorWizard(IRodinProject rodinProject, IMachineRoot concreteMachineRoot) {
		super();
		this.rodinProject = rodinProject;
		this.concreteMachineRoot = concreteMachineRoot;
		this.abstractMachineRoot = RodinUtil.getPrecedingMachineRoot(concreteMachineRoot);
	}

	@Override
	public void addPages() {

		if (abstractMachineRoot == null) {
			// Case where there is no abstract machine should be handled earlier.
			return;
		}

		String title = "Merge Machine With Predecessor";
		String description = "Enter a name for the machine newly derived from merging a machine with its direct abstract predecessor.";

		machineNamingPage = new MachineNamingWizardPage(title, description);
		addPage(machineNamingPage);
	}

	@Override
	public boolean performFinish() {

		machineNamingPage.finish();
		machineName = machineNamingPage.getMachineNameInput();

		try {
			createMachine(machineName);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * A method to copy all elements of a given event to another event, including the refinement clause
	 * 
	 * @param source
	 *            Source Event
	 * @param destination
	 *            Destination Event
	 * @throws RodinDBException
	 */
	private void copyAbstractEventElements(IEvent source, IEvent destination) throws RodinDBException {
		// We gather all the existing guard predicate strings
		Set<String> existingGuardPredicates = new HashSet<>();
		for (IGuard guard : destination.getGuards()) {
			existingGuardPredicates.add(guard.getPredicateString());
		}
		// We copy the guards that aren't already in the destination
		for (IGuard guard : source.getGuards()) {
			if (existingGuardPredicates.contains(guard.getPredicateString())) {
				continue;
			}
			copyElementAndRenameLabel(guard, destination, "abs_" + guard.getLabel());
		}

		// We gather all the existing action assignments
		Set<String> existingActionAssignments = new HashSet<>();
		for (IAction action : destination.getActions()) {
			existingActionAssignments.add(action.getAssignmentString());
		}
		// Copy all the actions not already in the dest event
		for (IAction action : source.getActions()) {
			if (existingActionAssignments.contains(action.getAssignmentString())) {
				continue;
			}
			copyElementAndRenameLabel(action, destination, "abs_" + action.getLabel());
		}

		// We gather all the existing parameter identifiers
		Set<String> existingParameterIdentifiers = new HashSet<>();
		for (IParameter parameter : destination.getParameters()) {
			existingParameterIdentifiers.add(parameter.getIdentifierString());
		}
		// Copy all parameters not already in dest event
		for (IParameter parameter : source.getParameters()) {
			if (existingParameterIdentifiers.contains(parameter.getIdentifierString())) {
				continue;
			}
			copyElement(parameter, destination);
		}

		// We do the same with witnesses
		Set<String> existingWitnessPredicates = new HashSet<>();
		for (IWitness witness : destination.getWitnesses()) {
			existingWitnessPredicates.add(witness.getPredicateString());
		}
		for (IWitness witness : source.getWitnesses()) {
			if (existingWitnessPredicates.contains(witness.getPredicateString())) {
				continue;
			}
			copyElementAndRenameLabel(witness, destination, witness.getLabel());
		}

		// Finally, we copy the refinement clause
		for (IRefinesEvent refinementClause : source.getRefinesClauses()) {
			copyElement(refinementClause, destination);
		}
	}

	/**
	 * A method to safely copy an element to its new destination while avoiding name conflicts
	 * 
	 * @param element
	 *            Element which needs to be copied
	 * @param destination
	 *            Destination of element
	 * @throws RodinDBException
	 * @return Name of copied element in destination
	 */
	private String copyElement(IInternalElement element, IInternalElement destination) throws RodinDBException {
		String elementName = element.getElementName();
		IInternalElementType<?> type = element.getElementType();

		// We check for name conflicts.
		// If one is found, we make a small change to the name
		while (destination.getInternalElement(type, elementName).exists()) {
			elementName += "_";
		}

		element.copy(destination, null, elementName, false, null);
		return elementName;
	}

	/**
	 * A method to safely copy an element to its new destination while avoiding name conflicts, also giving
	 * the new element a new label
	 * 
	 * @param element
	 *            Element which needs to be copied
	 * @param destination
	 *            Destination of element
	 * @param newLabel
	 *            New label for the copied element
	 * @throws RodinDBException
	 */
	private void copyElementAndRenameLabel(ILabeledElement element, IInternalElement destination, String newLabel) throws RodinDBException {
		String elementName = copyElement(element, destination);
		ILabeledElement destElement = (ILabeledElement) destination.getInternalElement(element.getElementType(), elementName);
		destElement.setLabel(newLabel, null);
	}

	/**
	 * A method to prepend "con_" to the labels of elements of a given event. Makes it easier to see where the
	 * elements come from. Should be used before adding elements from abstract events
	 * 
	 * @param event
	 *            The event to be modified
	 * @throws RodinDBException
	 */
	private void prependConcreteLabelToEventElements(IEvent event) throws RodinDBException {
		for (IGuard guard : event.getGuards()) {
			guard.setLabel("con_" + guard.getLabel(), null);
		}
		for (IAction action : event.getActions()) {
			action.setLabel("con_" + action.getLabel(), null);
		}
		for (IWitness witness : event.getWitnesses()) {
			witness.setLabel(witness.getLabel(), null);
		}
	}

	/**
	 * Creates a merge machine from the given information with the provided name.
	 * 
	 * @param machineName
	 *            The name we give to the new machine
	 * @throws RodinDBException
	 */
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
					copyElementAndRenameLabel(invariant, root, "abs_" + invariant.getLabel());
				}
				// And then we add the concrete invariants
				for (IInvariant invariant : concreteMachineRoot.getInvariants()) {
					copyElementAndRenameLabel(invariant, root, "con_" + invariant.getLabel());
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
					copyElement(variable, root);
					alreadyIncludedVariables.add(variable.getIdentifierString());
				}
				// And the concrete variables
				for (IVariable variable : concreteMachineRoot.getVariables()) {
					if (alreadyIncludedVariables.contains(variable.getIdentifierString())) {
						// We avoid adding duplicates.
						continue;
					}
					copyElement(variable, root);
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
					copyElement(seenContext, root);
					alreadyIncludedContexts.add(seenContext.getSeenContextName());
				}
				// Then we copy the seen contexts from the concrete machine
				for (ISeesContext seenContext : concreteMachineRoot.getSeesClauses()) {
					if (alreadyIncludedContexts.contains(seenContext.getSeenContextName())) {
						continue;
					}
					copyElement(seenContext, root);
					alreadyIncludedContexts.contains(seenContext.getSeenContextName());
				}

				Map<String, String> eventActualNameToInternalNameMap = new HashMap<>();
				// We use a map for easy access of events in the abstract machine
				Map<String, String> abstractLabelToInternalNameMap = new HashMap<>();

				for (IEvent event : abstractMachineRoot.getEvents()) {
					abstractLabelToInternalNameMap.put(event.getLabel(), event.getElementName());
				}

				// Time to merge the events
				// First, we remove any events that were brought over from refining
				for (IEvent event : root.getEvents()) {
					event.delete(true, null);
				}
				// Next, we base the new machine's events on the concrete event
				for (IEvent event : concreteMachineRoot.getEvents()) {
					copyElement(event, root);
					eventActualNameToInternalNameMap.put(event.getLabel(), event.getElementName());
				}

				/*
				 * We iterate over all the copied events, removing refinement information, then adding
				 * elements from their abstract versions where necessary. We also rename existing elements to
				 * mark where they originate from. Additionally, we need to change convergence status.
				 */
				for (IEvent event : root.getEvents()) {
					prependConcreteLabelToEventElements(event);
					for (IRefinesEvent refinesClause : event.getRefinesClauses()) {
						String abstractLabel = refinesClause.getAbstractEventLabel();
						IEvent abstractEvent = abstractMachineRoot.getEvent(abstractLabelToInternalNameMap.get(abstractLabel));
						refinesClause.delete(false, null);
						// We copy all the missing elements from the abstract event to the new machine
						copyAbstractEventElements(abstractEvent, event);
						// We also take over the convergence status from the abstract element
						event.setConvergence(abstractEvent.getConvergence(), null);
					}
				}

				/*
				 * We only take over a variant from one of the parent machines if the other one has no variant
				 * Otherwise, either neither has a variant, or both machines include convergent events and we
				 * need a whole new variant for the merged machine
				 */
				if (concreteMachineRoot.getVariants() == null || concreteMachineRoot.getVariants().length == 0) {
					if (abstractMachineRoot.getVariants() != null) {
						for (IVariant variant : abstractMachineRoot.getVariants()) {
							copyElement(variant, root);
						}
					}
				} else {
					if (abstractMachineRoot.getVariants() == null || abstractMachineRoot.getVariants().length == 0) {
						for (IVariant variant : concreteMachineRoot.getVariants()) {
							copyElement(variant, root);
						}
					}
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
