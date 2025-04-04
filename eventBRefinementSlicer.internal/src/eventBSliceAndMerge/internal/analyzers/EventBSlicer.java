package eventBSliceAndMerge.internal.analyzers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eventb.core.IAction;
import org.eventb.core.IAssignmentElement;
import org.eventb.core.ICommentedElement;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IDerivedPredicateElement;
import org.eventb.core.IEvent;
import org.eventb.core.IGuard;
import org.eventb.core.IIdentifierElement;
import org.eventb.core.IInvariant;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IParameter;
import org.eventb.core.IPredicateElement;
import org.eventb.core.IRefinesEvent;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISCVariable;
import org.eventb.core.ISCVariant;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.IWitness;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Type;
import org.eventb.core.basis.MachineRoot;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRefinementManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.datastructures.EventBAction;
import eventBSliceAndMerge.internal.datastructures.EventBCondition;
import eventBSliceAndMerge.internal.datastructures.EventBContext;
import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBEvent;
import eventBSliceAndMerge.internal.datastructures.EventBGuard;
import eventBSliceAndMerge.internal.datastructures.EventBInvariant;
import eventBSliceAndMerge.internal.datastructures.EventBParameter;
import eventBSliceAndMerge.internal.datastructures.EventBRefinedEvent;
import eventBSliceAndMerge.internal.datastructures.EventBVariable;
import eventBSliceAndMerge.internal.datastructures.EventBWitness;

/**
 * Class for the processing function of the slicer
 *
 * @author Fuyuki Ishikawa
 *
 */
public class EventBSlicer implements IWorkspaceRunnable {

	private IRodinFile file;
	private EventBSliceSelection selection;
	private IMachineRoot originalMachineRoot;

	/**
	 *
	 * @param file
	 *            Target IRodinFile
	 * @param selection
	 *            selected elements
	 * @param originalMachineRoot
	 *            original machine
	 */
	public EventBSlicer(IRodinFile file, EventBSliceSelection selection, IMachineRoot originalMachineRoot) {
		this.file = file;
		this.selection = selection;
		this.originalMachineRoot = originalMachineRoot;
	}

	/**
	 * Creates Event-B machine with the given name based on a selection of
	 * elements
	 *
	 * @param machineName
	 *            machine name
	 * @param selection
	 *            selected elements
	 * @param rodinProject
	 *            working project
	 * @param originalMachineRoot
	 *            original machine
	 * @throws RodinDBException
	 */
	public static void createMachineFromSelection(String machineName, EventBSliceSelection selection,
			IRodinProject rodinProject, IMachineRoot originalMachineRoot) throws RodinDBException {
		RodinCore.run(
				new EventBSlicer(rodinProject.getRodinFile(machineName.concat(".bum")), selection, originalMachineRoot),
				null);
	}

	/**
	 * Create a file of Event-B machine created by slicing
	 * (expected not to be called directly)
	 */
	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		file.create(true, null);
		file.getResource().setDerived(true, null);
		MachineRoot root = (MachineRoot) file.getRoot();
		root.setConfiguration(IConfigurationElement.DEFAULT_CONFIGURATION, monitor);

		file.save(null, false);

		// Add refinement information from existing machine to new
		// machine
		for (IRefinesMachine refines : originalMachineRoot.getRefinesClauses()) {
			IRefinementManager refinementManager = RodinCore.getRefinementManager();
			refinementManager.refine(refines.getAbstractMachineRoot(), root, null);
		}

		// Add selected invariants to new machine
		for (EventBInvariant invariant : selection.invariants) {
			addRodinElement(IInvariant.ELEMENT_TYPE, root, invariant);
		}

		// Add selected variables to new machine
		for (EventBVariable variable : selection.variables) {
			for (IVariable inheritedVariable : root.getVariables()) {
				// If the variable has been inherited, we have to get
				// rid of it to avoid duplicates.
				if (inheritedVariable.getIdentifierString().equals(variable.getLabel())) {
					inheritedVariable.delete(false, null);
				}
			}
			addRodinElement(IVariable.ELEMENT_TYPE, root, variable);
		}

		// Find init actions from abstract machine so that we can remove
		// them from the new init
		List<Map.Entry<String, String>> abstractInitActions = new ArrayList<>();
		for (IRefinesMachine refines : root.getRefinesClauses()) {
			IMachineRoot abstractRoot = refines.getAbstractMachineRoot();
			for (IEvent abstractEvent : abstractRoot.getEvents()) {
				if (!abstractEvent.isInitialisation()) {
					continue;
				}
				for (IAction action : abstractEvent.getActions()) {
					// We use Map.Entry instead of Pair because Java
					// doesn't have Pair :(
					Map.Entry<String, String> labelAssignmentPair = new AbstractMap.SimpleEntry<String, String>(
							action.getLabel(), action.getAssignmentString());
					abstractInitActions.add(labelAssignmentPair);
				}
				break;
			}
		}

		// Add selected events to new machine
		for (EventBEvent event : selection.events) {
			for (IEvent inheritedEvent : root.getEvents()) {
				// If the event has been inherited, we have to get rid
				// of it to avoid duplicates.
				if (inheritedEvent.getLabel().equals(event.getLabel())) {
					inheritedEvent.delete(false, null);
				}
			}
			IEvent rodinEvent = (IEvent) addRodinElement(IEvent.ELEMENT_TYPE, root, event);
			if (rodinEvent.isInitialisation()) {
				// We have the initialization include all the init
				// actions from the abstract machine
				rodinEvent.setExtended(true, null);
			} else {
				rodinEvent.setExtended(event.isExtended(), null);
			}
			rodinEvent.setConvergence(event.getConvergence(), null);
			// Add parameters to the event
			List<EventBParameter> relevantParameters = new ArrayList<>(event.getParameters());
			relevantParameters.retainAll(selection.parameters);
			for (EventBParameter parameter : relevantParameters) {
				addRodinElement(IParameter.ELEMENT_TYPE, rodinEvent, parameter);
			}
			selection.parameters.removeAll(relevantParameters);
			// Add witnesses to the event
			List<EventBWitness> relevantWitnesses = new ArrayList<>(event.getWitnesses());
			relevantWitnesses.retainAll(selection.witnesses);
			for (EventBWitness witness : relevantWitnesses) {
				addRodinElement(IWitness.ELEMENT_TYPE, rodinEvent, witness);
			}
			selection.witnesses.removeAll(relevantWitnesses);
			// Add guards to the event
			List<EventBGuard> relevantGuards = new ArrayList<>(event.getGuards());
			relevantGuards.retainAll(selection.guards);
			for (EventBGuard guard : relevantGuards) {
				addRodinElement(IGuard.ELEMENT_TYPE, rodinEvent, guard);
			}
			selection.guards.removeAll(relevantGuards);
			// Add actions to the event
			List<EventBAction> relevantActions = new ArrayList<>(event.getActions());
			relevantActions.retainAll(selection.actions);
			for (EventBAction action : relevantActions) {
				Map.Entry<String, String> labelAssignmentPair = new AbstractMap.SimpleEntry<String, String>(
						action.getLabel(), action.getAssignment());
				if (abstractInitActions.contains(labelAssignmentPair)) {
					// If init event, then we do not add the superfluous
					// actions
					continue;
				}
				addRodinElement(IAction.ELEMENT_TYPE, rodinEvent, action);
			}
			selection.actions.removeAll(relevantActions);
			// Add refinement information
			for (EventBRefinedEvent refinedEvent : event.getRefinedEvents()) {
				addRodinElement(IRefinesEvent.ELEMENT_TYPE, rodinEvent, refinedEvent);
			}

		}

		Set<String> contextsToRemove = new HashSet<>();
		// Adds seen contexts to new machine.
		// If whole context selected, we just add it directly,
		// unless it is already included (because of refinement)
		for (EventBContext context : selection.contexts) {
			contextsToRemove.addAll(context.getExtendedContextLabels());
			boolean alreadyIncluded = false;
			for (ISeesContext seenContext : root.getSeesClauses()) {
				if (seenContext.getSeenContextName().equals(context.getLabel())) {
					alreadyIncluded = true;
					continue;
				}
			}
			if (alreadyIncluded) {
				continue;
			}
			addRodinElement(ISeesContext.ELEMENT_TYPE, root, context);
		}
		// We remove any contexts where their extending context is
		// already included
		for (ISeesContext seenContext : root.getSeesClauses()) {
			if (contextsToRemove.contains(seenContext.getSeenContextName())) {
				seenContext.delete(false, null);
			}
		}

		// We perform a check to see if the old variant is still
		// relevant to the new generated machine
		// Get ISCVariant from concrete machine
		// Extract expression (using type environment)
		Set<String> varsInVariant = new HashSet<>();
		ITypeEnvironment typeEnvironment = originalMachineRoot.getSCMachineRoot().getTypeEnvironment();
		for (ISCVariant scVariant : originalMachineRoot.getSCMachineRoot().getSCVariants()) {
			Expression expression = scVariant.getExpression(typeEnvironment);
			FreeIdentifier[] identifiers = expression.getFreeIdentifiers();
			for (FreeIdentifier identifier : identifiers) {
				String variableName = identifier.getName();
				varsInVariant.add(variableName);
			}
		}

		// Get SC Event information for convergent events.
		// Check if at least one variant variable is included in each of
		// these events
		// If true, add variant to new machine

		// ISCMachineRoot scMachine =
		// SCUtil.makeStaticCheckedMachine(root);

		// TODO: Find a way to force creation of SC Machine and/or wait
		// for its completion

		// ISCMachineRoot scMachine = root.getSCMachineRoot();
		// boolean allConvergentEventsCovered = true;
		// boolean hasConvergentEvent = false;
		// for (ISCEvent scEvent : scMachine.getSCEvents()) {
		// if (!(scEvent.getConvergence() == Convergence.CONVERGENT)) {
		// continue;
		// }
		// hasConvergentEvent = true;
		// boolean eventCovered = false;
		// for (ISCAction scAction : scEvent.getSCActions()) {
		// Assignment assignment =
		// scAction.getAssignment(typeEnvironment);
		// for (FreeIdentifier assignedVariable :
		// assignment.getAssignedIdentifiers()) {
		// if (varsInVariant.contains(assignedVariable.getName())) {
		// eventCovered = true;
		// }
		// }
		// }
		// if (!eventCovered) {
		// allConvergentEventsCovered = false;
		// }
		// }
		//
		// // If all convergent events are covered by the variant, then
		// we include it in the new
		// machine
		// if (hasConvergentEvent && allConvergentEventsCovered) {
		// addVariant(originalMachineRoot, root);
		// }

		// Save the final result
		file.save(null, false);

		addNecessaryPredicates(root, monitor);
	}

	/**
	 * Add invariants for typing and complementary predicates for consistency.
	 * @throws CoreException
	 */
	private void addNecessaryPredicates(MachineRoot root, IProgressMonitor monitor) throws CoreException {
		IProject project = root.getRodinProject().getProject();

		// Force building the project
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

		// Add invariants for typing
		Set<String> untypedVariableLabels = new HashSet<String>();
		IMarker[] markers = project.getWorkspace().getRoot().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		// Find untyped variables by checking errors
		for (IMarker marker : markers) {
			if (marker.getAttribute("code").equals("org.eventb.core.UntypedVariableError")) {
				String variableLabel = marker.getAttribute("arguments").toString().split(":")[1];
				untypedVariableLabels.add(variableLabel);
			}
		}
		for (String untypedVariableLabel : untypedVariableLabels) {
			addTypingInvariants(root, untypedVariableLabel);
		}

		// Force building the project again
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
	}

	private void addTypingInvariants(MachineRoot root, String untypedVariableLabel) throws CoreException {
		ISCVariable originalSCVariable = originalMachineRoot.getSCMachineRoot().getSCVariable(untypedVariableLabel);
		Type type = originalSCVariable.getType(root.getFormulaFactory());
		EventBInvariant typingInvariant = new EventBInvariant("typ_" + untypedVariableLabel,
				untypedVariableLabel + " ∈ " + type.toExpression().toString(), null,
				"For typing. Added by the slicer.", null);
		addRodinElement(IInvariant.ELEMENT_TYPE, root, typingInvariant);
	}

	/**
	 * Adds a Rodin element to the given parent element
	 *
	 * @param type
	 *            Type of the element (i.e. invariant or variable)
	 * @param parent
	 *            Target parent of element
	 * @param element
	 *            The element to add
	 * @return The element that was created
	 * @throws RodinDBException
	 */
	private IInternalElement addRodinElement(IInternalElementType<?> type, IInternalElement parent,
			EventBElement element) throws RodinDBException {
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
		if (element instanceof EventBCondition) {
			if (rodinElement instanceof IPredicateElement) {
				((IPredicateElement) rodinElement).setPredicateString(((EventBCondition) element).getPredicate(), null);
			}
			if (rodinElement instanceof IDerivedPredicateElement) {
				((IDerivedPredicateElement) rodinElement).setTheorem(((EventBCondition) element).isTheorem(), null);
			}
		}
		if (rodinElement instanceof IAssignmentElement && element instanceof EventBAction) {
			((IAssignmentElement) rodinElement).setAssignmentString(((EventBAction) element).getAssignment(), null);
		}
		if (rodinElement instanceof ISeesContext && element instanceof EventBContext) {
			((ISeesContext) rodinElement).setSeenContextName(element.getLabel(), null);
		}
		if (rodinElement instanceof IRefinesEvent && element instanceof EventBRefinedEvent) {
			((IRefinesEvent) rodinElement).setAbstractEventLabel(element.getLabel(), null);
		}
		return rodinElement;
	}

}
