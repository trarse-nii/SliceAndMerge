package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IInvariant;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBInvariant extends EventBCondition {

	private static final String TYPE = "INVARIANT";

	public EventBInvariant(String label, String predicate, String comment, EventBUnit parent) {
		super(label, predicate, comment, parent);
	}

	public EventBInvariant(IInvariant invariant, EventBUnit parent) throws RodinDBException {
		super(parent);
		String label = "";
		String predicate = "";
		String comment = "";
		if (invariant.hasLabel()) {
			label = invariant.getLabel();
		}
		if (invariant.hasPredicateString()) {
			predicate = invariant.getPredicateString();
		}
		if (invariant.hasComment()) {
			comment = invariant.getComment();
		}
		this.label = label;
		this.predicate = predicate;
		this.comment = comment;
	}

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + predicate + " (" + comment + ")";
	}
}
