package eventBSliceAndMerge.internal.analyzers;

import java.util.ArrayList;
import java.util.List;

import eventBSliceAndMerge.internal.datastructures.EventBAction;
import eventBSliceAndMerge.internal.datastructures.EventBContext;
import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBEvent;
import eventBSliceAndMerge.internal.datastructures.EventBGuard;
import eventBSliceAndMerge.internal.datastructures.EventBInvariant;
import eventBSliceAndMerge.internal.datastructures.EventBParameter;
import eventBSliceAndMerge.internal.datastructures.EventBVariable;
import eventBSliceAndMerge.internal.datastructures.EventBWitness;

/**
 * Class for keeping selected objects used for slicing
 * 
 * @author Fuyuki Ishikawa
 *
 */
public class EventBSliceSelection {

	List<EventBInvariant> invariants = new ArrayList<>();
	public List<EventBVariable> variables = new ArrayList<>();
	List<EventBEvent> events = new ArrayList<>();
	List<EventBParameter> parameters = new ArrayList<>();
	List<EventBWitness> witnesses = new ArrayList<>();
	List<EventBGuard> guards = new ArrayList<>();
	List<EventBAction> actions = new ArrayList<>();
	List<EventBContext> contexts = new ArrayList<>();

	/**
	 * Constructor
	 * 
	 * @param elements
	 *            list of selected EventBElement
	 */
	public EventBSliceSelection(List<EventBElement> elements) {
		for (EventBElement element : elements) {
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
	}

}
