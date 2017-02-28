package eventBSliceAndMerge.internal.analyzers;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
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
import org.eventb.core.ISCVariant;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.IVariant;
import org.eventb.core.IWitness;
import org.eventb.core.ast.Expression;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ITypeEnvironment;
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

public class EventBSlicer {

	/**
	 * Creates Event-B machine with the given name based on a selection of
	 * elements the wizard has already received.
	 * 
	 * @param machineName
	 *            Name for the new machine
	 * @throws RodinDBException
	 */
	public static void createMachineFromSelection(String machineName, Object[] selectedElements,
			IRodinProject rodinProject, IMachineRoot originalMachineRoot) throws RodinDBException {
		List<EventBInvariant> invariants = new ArrayList<>();
		List<EventBVariable> variables = new ArrayList<>();
		List<EventBEvent> events = new ArrayList<>();
		List<EventBParameter> parameters = new ArrayList<>();
		List<EventBWitness> witnesses = new ArrayList<>();
		List<EventBGuard> guards = new ArrayList<>();
		List<EventBAction> actions = new ArrayList<>();
		List<EventBContext> contexts = new ArrayList<>();

		// We split the selection into individual categories
		for (Object checkedElement : selectedElements) {
			if (checkedElement instanceof EventBTreeSubcategory) {
				continue;
			}
			EventBElement element = ((EventBTreeElement) checkedElement).getOriginalElement();
			if (element instanceof EventBInvariant) {
				invariants.add((EventBInvariant) element);
			} else if (element instanceof EventBVariable) {
				variables.add((EventBVariable) element);
			} else if (element instanceof EventBParameter) {
				parameters.add((EventBParameter) element);
			} else if (element instanceof EventBWitness) {
				witnesses.add((EventBWitness) element);
			} else if (element instanceof EventBGuard) {
				guards.add((EventBGuard) element);
			} else if (element instanceof EventBAction) {
				actions.add((EventBAction) element);
			} else if (element instanceof EventBEvent) {
				events.add((EventBEvent) element);
			} else if (element instanceof EventBContext) {
				contexts.add((EventBContext) element);
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

				file.save(null, false);

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
				for (EventBEvent event : events) {
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
					relevantParameters.retainAll(parameters);
					for (EventBParameter parameter : relevantParameters) {
						addRodinElement(IParameter.ELEMENT_TYPE, rodinEvent, parameter);
					}
					parameters.removeAll(relevantParameters);
					// Add witnesses to the event
					List<EventBWitness> relevantWitnesses = new ArrayList<>(event.getWitnesses());
					relevantWitnesses.retainAll(witnesses);
					for (EventBWitness witness : relevantWitnesses) {
						addRodinElement(IWitness.ELEMENT_TYPE, rodinEvent, witness);
					}
					witnesses.removeAll(relevantWitnesses);
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
						Map.Entry<String, String> labelAssignmentPair = new AbstractMap.SimpleEntry<String, String>(
								action.getLabel(), action.getAssignment());
						if (abstractInitActions.contains(labelAssignmentPair)) {
							// If init event, then we do not add the superfluous
							// actions
							continue;
						}
						addRodinElement(IAction.ELEMENT_TYPE, rodinEvent, action);
					}
					actions.removeAll(relevantActions);
					// Add refinement information
					for (EventBRefinedEvent refinedEvent : event.getRefinedEvents()) {
						addRodinElement(IRefinesEvent.ELEMENT_TYPE, rodinEvent, refinedEvent);
					}

				}

				Set<String> contextsToRemove = new HashSet<>();

				// Adds seen contexts to new machine.
				// If whole context selected, we just add it directly,
				// unless it is already included (because of refinement)
				for (EventBContext context : contexts) {
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
			}

			/**
			 * Takes variants from a given source Event-B machine and copies
			 * them to a provided target.
			 * 
			 * @param source
			 *            Source machine of variant to be copied
			 * @param target
			 *            Target machine
			 * @throws RodinDBException
			 */
			private void addVariant(IMachineRoot source, IInternalElement target) throws RodinDBException {
				for (IVariant variant : source.getVariants()) {
					variant.copy(target, null, null, true, null);
				}
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
						((IPredicateElement) rodinElement)
								.setPredicateString(((EventBCondition) element).getPredicate(), null);
					}
					if (rodinElement instanceof IDerivedPredicateElement) {
						((IDerivedPredicateElement) rodinElement).setTheorem(((EventBCondition) element).isTheorem(),
								null);
					}
				}
				if (rodinElement instanceof IAssignmentElement && element instanceof EventBAction) {
					((IAssignmentElement) rodinElement).setAssignmentString(((EventBAction) element).getAssignment(),
							null);
				}
				if (rodinElement instanceof ISeesContext && element instanceof EventBContext) {
					((ISeesContext) rodinElement).setSeenContextName(((EventBContext) element).getLabel(), null);
				}
				if (rodinElement instanceof IRefinesEvent && element instanceof EventBRefinedEvent) {
					((IRefinesEvent) rodinElement).setAbstractEventLabel(element.getLabel(), null);
				}
				return rodinElement;
			}

		}, null);

	}

}
