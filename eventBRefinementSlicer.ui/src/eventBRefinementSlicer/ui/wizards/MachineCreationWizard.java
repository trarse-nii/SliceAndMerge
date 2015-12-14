package eventBRefinementSlicer.ui.wizards;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eventb.core.IAssignmentElement;
import org.eventb.core.IAxiom;
import org.eventb.core.ICarrierSet;
import org.eventb.core.ICommentedElement;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IConstant;
import org.eventb.core.IDerivedPredicateElement;
import org.eventb.core.IEvent;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IGuard;
import org.eventb.core.IIdentifierElement;
import org.eventb.core.IInvariant;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.basis.ContextRoot;
import org.eventb.core.basis.MachineRoot;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRefinementManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.datastructures.EventBAction;
import eventBRefinementSlicer.internal.datastructures.EventBAxiom;
import eventBRefinementSlicer.internal.datastructures.EventBCarrierSet;
import eventBRefinementSlicer.internal.datastructures.EventBCondition;
import eventBRefinementSlicer.internal.datastructures.EventBConstant;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBEvent;
import eventBRefinementSlicer.internal.datastructures.EventBGuard;
import eventBRefinementSlicer.internal.datastructures.EventBInvariant;
import eventBRefinementSlicer.internal.datastructures.EventBRefinedEvent;
import eventBRefinementSlicer.internal.datastructures.EventBTypes;
import eventBRefinementSlicer.internal.datastructures.EventBVariable;
import eventBRefinementSlicer.ui.editors.SelectionEditor.EventBTreeElement;
import eventBRefinementSlicer.ui.editors.SelectionEditor.EventBTreeSubcategory;

public class MachineCreationWizard extends Wizard {

	private IRodinProject rodinProject;
	private IMachineRoot originalMachineRoot;
	private Object[] selectedElements;

	private List<EventBContext> partiallySelectedContexts = new ArrayList<>();

	private Map<EventBContext, ContextNamingWizardPage> contextNamingPageMap = new HashMap<>();

	private MachineNamingWizardPage machineNamingPage;

	private String machineName;

	public MachineCreationWizard(IRodinProject rodinProject, IMachineRoot originalMachineRoot, Object[] selectedElements,
			Object[] partiallySelectedElements) {
		super();
		this.rodinProject = rodinProject;
		this.originalMachineRoot = originalMachineRoot;
		this.selectedElements = selectedElements;

		for (Object element : partiallySelectedElements) {
			if (element instanceof EventBTreeSubcategory) {
				continue;
			}
			EventBElement eventBElement = ((EventBTreeElement) element).getOriginalElement();
			if (eventBElement.getType().equals(EventBTypes.CONTEXT)) {
				partiallySelectedContexts.add((EventBContext) eventBElement);
			}
		}
	}

	@Override
	public void addPages() {
		machineNamingPage = new MachineNamingWizardPage();
		addPage(machineNamingPage);
		for (EventBContext context : partiallySelectedContexts) {
			ContextNamingWizardPage contextNamingPage = new ContextNamingWizardPage(context, selectedElements);
			contextNamingPageMap.put(context, contextNamingPage);
			addPage(contextNamingPage);
		}
	}

	@Override
	public boolean performFinish() {

		machineNamingPage.finish();
		machineName = machineNamingPage.getMachineNameInput();

		for (ContextNamingWizardPage contextNamingPage : contextNamingPageMap.values()) {
			contextNamingPage.finish();
		}

		try {
			createMachineFromSelection(machineName);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		return true;
	}

	private void createMachineFromSelection(String machineName) throws RodinDBException {
		List<EventBInvariant> invariants = new ArrayList<>();
		List<EventBVariable> variables = new ArrayList<>();
		List<EventBEvent> events = new ArrayList<>();
		List<EventBGuard> guards = new ArrayList<>();
		List<EventBAction> actions = new ArrayList<>();
		List<EventBContext> contexts = new ArrayList<>();
		List<EventBContext> partialContexts = partiallySelectedContexts;

		for (Object checkedElement : selectedElements) {
			if (checkedElement instanceof EventBTreeSubcategory) {
				continue;
			}
			EventBElement element = ((EventBTreeElement) checkedElement).getOriginalElement();
			if (element instanceof EventBInvariant) {
				invariants.add((EventBInvariant) element);
			} else if (element instanceof EventBVariable) {
				variables.add((EventBVariable) element);
			} else if (element instanceof EventBGuard) {
				guards.add((EventBGuard) element);
			} else if (element instanceof EventBAction) {
				actions.add((EventBAction) element);
			} else if (element instanceof EventBEvent) {
				events.add((EventBEvent) element);
			} else if (element instanceof EventBContext) {
				// We only add completely checked Contexts (no partial
				// selections)
				if (!partiallySelectedContexts.contains(element)) {
					contexts.add((EventBContext) element);
				}
			}
		}

		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// Get Rodin project and create new file
				IRodinFile file = rodinProject.getRodinFile(machineName.concat(".bum"));
				file.create(true, null);
				file.getResource().setDerived(true, null);
				MachineRoot root = (MachineRoot) file.getRoot();
				root.setConfiguration(IConfigurationElement.DEFAULT_CONFIGURATION, monitor);

				// Add refinement information from existing machine to new
				// machine
				for (IRefinesMachine refines : originalMachineRoot.getRefinesClauses()) {
					IRefinementManager refinementManager = RodinCore.getRefinementManager();
					refinementManager.refine(refines.getAbstractMachineRoot(), root, null);
				}

				// Add selected invariants to new machine
				for (EventBInvariant invariant : invariants) {
					addRodinElement(IInvariant.ELEMENT_TYPE, root, invariant);
				}
				// Add selected variables to new machine
				for (EventBVariable variable : variables) {
					for (IVariable inheritedVariable : root.getVariables()) {
						// If the variable has been inherited, we have to get rid of it to avoid duplicates.
						if (inheritedVariable.getIdentifierString().equals(variable.getLabel())) {
							inheritedVariable.delete(false, null);
						}
					}
					addRodinElement(IVariable.ELEMENT_TYPE, root, variable);
				}
				// Add selected events to new machine
				for (EventBEvent event : events) {
					for (IEvent inheritedEvent : root.getEvents()) {
						// If the event has been inherited, we have to get rid of it to avoid duplicates.
						if (inheritedEvent.getLabel().equals(event.getLabel())) {
							inheritedEvent.delete(false, null);
						}
					}
					IEvent rodinEvent = (IEvent) addRodinElement(IEvent.ELEMENT_TYPE, root, event);
					if (rodinEvent.isInitialisation()) {
						// We have the initialization include all the init actions from the abstract machine
						rodinEvent.setExtended(true, null);
					} else {
						rodinEvent.setExtended(event.isExtended(), null);
					}
					rodinEvent.setConvergence(event.getConvergence(), null);
					// Add guards to the event
					List<EventBGuard> relevantGuards = new ArrayList<>(event.getGuards());
					relevantGuards.retainAll(guards);
					for (EventBGuard guard : relevantGuards) {
						addRodinElement(IGuard.ELEMENT_TYPE, rodinEvent, guard);
					}
					guards.removeAll(relevantGuards);
					// Add actions to the event
					List<EventBAction> relevantActions = new ArrayList<>(event.getActions());
					relevantActions.retainAll(actions);
					for (EventBAction action : relevantActions) {
						addRodinElement(IAction.ELEMENT_TYPE, rodinEvent, action);
					}
					actions.removeAll(relevantActions);
					// Add refinement information
					for (EventBRefinedEvent refinedEvent : event.getRefinedEvents()) {
						addRodinElement(IRefinesEvent.ELEMENT_TYPE, rodinEvent, refinedEvent);
					}
				}

				// Adds seen contexts to new machine.
				// If whole context selected, we just add it directly,
				// unless it is already included (because of refinement)
				for (EventBContext context : contexts) {
					boolean alreadyIncluded = false;
					for (ISeesContext seenContext : root.getSeesClauses()) {
						if (seenContext.getSeenContextName().equals(context.getLabel())) {
							alreadyIncluded = true;
							break;
						}
					}
					if (alreadyIncluded) {
						continue;
					}
					addRodinElement(ISeesContext.ELEMENT_TYPE, root, context);
				}

				// If only parts of the context are selected, we create a new
				// context
				for (EventBContext context : partialContexts) {
					// If the abstract machine contains the whole context, we don't do anything, because the
					// whole context has already been included through refinement
					boolean alreadyIncluded = false;
					for (ISeesContext seenContext : root.getSeesClauses()) {
						if (seenContext.getSeenContextName().equals(context.getLabel())) {
							alreadyIncluded = true;
							break;
						}
					}
					if (alreadyIncluded) {
						continue;
					}
					String newContextName = contextNamingPageMap.get(context).getContextNameInput();
					createSeenContext(newContextName, context, root);

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

			private IInternalElement addRodinElement(IInternalElementType<?> type, IInternalElement parent, EventBElement element)
					throws RodinDBException {
				IInternalElement rodinElement = parent.getInternalElement(type, element.getLabel());
				rodinElement.create(null, null);
				if (rodinElement instanceof ILabeledElement) {
					((ILabeledElement) rodinElement).setLabel(element.getLabel(), null);
				}
				if (rodinElement instanceof IIdentifierElement) {
					((IIdentifierElement) rodinElement).setIdentifierString(element.getLabel(), null);
				}
				if (rodinElement instanceof ICommentedElement) {
					if (!element.getComment().equals("")) {
						((ICommentedElement) rodinElement).setComment(element.getComment(), null);
					}
				}
				if (rodinElement instanceof IDerivedPredicateElement && element instanceof EventBCondition) {
					((IDerivedPredicateElement) rodinElement).setPredicateString(((EventBCondition) element).getPredicate(), null);
					((IDerivedPredicateElement) rodinElement).setTheorem(((EventBCondition) element).isTheorem(), null);
				}
				if (rodinElement instanceof IAssignmentElement && element instanceof EventBAction) {
					((IAssignmentElement) rodinElement).setAssignmentString(((EventBAction) element).getAssignment(), null);
				}
				if (rodinElement instanceof ISeesContext && element instanceof EventBContext) {
					((ISeesContext) rodinElement).setSeenContextName(((EventBContext) element).getLabel(), null);
				}
				if (rodinElement instanceof IRefinesEvent && element instanceof EventBRefinedEvent) {
					((IRefinesEvent) rodinElement).setAbstractEventLabel(element.getLabel(), null);
				}
				return rodinElement;
			}

			private void createSeenContext(String contextName, EventBContext originalContext, MachineRoot machineRoot) throws CoreException {
				List<EventBAxiom> axioms = new ArrayList<>();
				List<EventBConstant> constants = new ArrayList<>();
				List<EventBCarrierSet> carrierSets = new ArrayList<>();

				for (Object checkedElement : selectedElements) {
					if (checkedElement instanceof EventBTreeSubcategory) {
						continue;
					}
					EventBElement originalElement = ((EventBTreeElement) checkedElement).getOriginalElement();
					if (originalElement instanceof EventBAxiom) {
						axioms.add((EventBAxiom) originalElement);
					} else if (originalElement instanceof EventBConstant) {
						constants.add((EventBConstant) originalElement);
					} else if (originalElement instanceof EventBCarrierSet) {
						carrierSets.add((EventBCarrierSet) originalElement);
					}
				}

				// Create new file for new context
				IRodinFile file = rodinProject.getRodinFile(contextName + ".buc");
				file.create(true, null);
				file.getResource().setDerived(true, null);
				ContextRoot root = (ContextRoot) file.getRoot();
				root.setConfiguration(IConfigurationElement.DEFAULT_CONFIGURATION, null);

				// Add selected axioms to context
				for (EventBAxiom axeTheAxiom : axioms) {
					addRodinElement(IAxiom.ELEMENT_TYPE, root, axeTheAxiom);
				}
				// Add selected constants to context
				for (EventBConstant constant : constants) {
					addRodinElement(IConstant.ELEMENT_TYPE, root, constant);
				}

				// Add selected carrier sets to context
				for (EventBCarrierSet carrierSet : carrierSets) {
					addRodinElement(ICarrierSet.ELEMENT_TYPE, root, carrierSet);
				}

				IExtendsContext[] extendedContextsFromOriginal = originalContext.getScContextRoot().getContextRoot().getExtendsClauses();

				IExtendsContext originalExtendedContext = null;
				// Add extension information to the new context
				for (IExtendsContext extendedContext : extendedContextsFromOriginal) {
					originalExtendedContext = extendedContext;
					IRefinementManager refinementManager = RodinCore.getRefinementManager();
					refinementManager.refine(extendedContext.getAbstractContextRoot(), root, null);
				}

				file.save(null, false);

				// We need to remove the context we extend from the derived machine's seen contexts attribute
				if (originalExtendedContext != null) {
					String extendedContextName = originalExtendedContext.getAbstractContextName();
					for (ISeesContext seenContext : machineRoot.getSeesClauses()) {
						if (seenContext.getSeenContextName().equals(extendedContextName)) {
							seenContext.delete(false, null);
						}
					}
				}
				// Add new context to derived machine
				IInternalElement rodinElement = machineRoot.getInternalElement(ISeesContext.ELEMENT_TYPE, contextName);
				rodinElement.create(null, null);
				((ISeesContext) rodinElement).setSeenContextName(contextName, null);
			}

		}, null);
	}

}
