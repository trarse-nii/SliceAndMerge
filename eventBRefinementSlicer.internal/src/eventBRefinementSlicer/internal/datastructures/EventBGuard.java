package eventBRefinementSlicer.internal.datastructures;

import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IGuard;
import org.eventb.core.ISCGuard;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Predicate;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation for Event-B Guards.
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBGuard extends EventBCondition {

	private EventBEvent parentEvent = null;

	public EventBGuard(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBGuard(String label, String predicate, String comment, ISCGuard scGuard, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, predicate, scGuard, comment, parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBGuard(IGuard guard, ISCGuard scGuard, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(guard, scGuard, parentUnit);
		this.parentEvent = parentEvent;
	}

	@Override
	public Set<EventBAttribute> calculateDependees() {
		Set<EventBAttribute> occurredAttributes = super.calculateDependees();
		// Also check parameters
		Predicate pred;
		try {
			ITypeEnvironment typeEnvironment = parent.getTypeEnvironment();
			pred = getScElement().getPredicate(typeEnvironment);
			for (FreeIdentifier freeIdentifier : pred.getFreeIdentifiers()) {
				for (EventBParameter parameter : parentEvent.getParameters()) {
					if (parameter.getLabel().equals(freeIdentifier.getName())) {
						occurredAttributes.add(parameter);
						break;
					}
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return occurredAttributes;
	}

	@Override
	public ISCGuard getScElement() {
		return (ISCGuard) super.getScElement();
	}

	public EventBEvent getParentEvent() {
		return parentEvent;
	}

	public EventBUnit getParentUnit() {
		return getParent();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.GUARD;
		return type;
	}
}
