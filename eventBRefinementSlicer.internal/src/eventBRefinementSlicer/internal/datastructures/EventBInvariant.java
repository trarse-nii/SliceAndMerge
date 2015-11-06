package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IInvariant;
import org.eventb.core.ISCInvariant;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBInvariant extends EventBCondition {

	public EventBInvariant(String label, String predicate, ISCInvariant scInvariant, String comment, EventBUnit parent) {
		super(label, predicate, scInvariant, comment, parent);
	}

	public EventBInvariant(IInvariant invariant, ISCInvariant scInvariant, EventBUnit parent) throws RodinDBException {
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
		this.scElement = scInvariant;
	}

	@Override
	public ISCInvariant getScElement() {
		return (ISCInvariant) super.getScElement();
	}

	@Override
	protected String getType() {
		final String type = "INVARIANT";
		return type;
	}
}
