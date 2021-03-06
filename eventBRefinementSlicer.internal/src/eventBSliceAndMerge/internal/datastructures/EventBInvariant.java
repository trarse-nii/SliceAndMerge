package eventBSliceAndMerge.internal.datastructures;

import org.eventb.core.IInvariant;
import org.eventb.core.ISCInvariant;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation of Event-B Invariants.
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBInvariant extends EventBCondition {

	public EventBInvariant(String label, String predicate, ISCInvariant scInvariant, String comment, EventBUnit parent) {
		super(label, predicate, scInvariant, comment, parent);
	}

	public EventBInvariant(IInvariant invariant, ISCInvariant scInvariant, EventBUnit parent) throws RodinDBException {
		super(invariant, scInvariant, parent);
	}

	@Override
	public ISCInvariant getScElement() {
		return (ISCInvariant) super.getScElement();
	}

	@Override
	public Type getType() {
		return Type.INVARIANT;
	}
}
