package eventBRefinementSlicer.internal.datastructures;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAction;
import org.eventb.core.ISCAction;
import org.eventb.core.ast.Assignment;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ITypeEnvironment;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.Depender;

/**
 * @author Aivar Kripsaar
 *
 */
public class EventBAction extends EventBElement implements Depender {

	protected String assignment = "";
	protected final EventBEvent parentEvent;
	protected Set<EventBAttribute> dependees = new HashSet<>();

	public EventBAction(EventBEvent parentEvent, EventBUnit parentUnit) {
		super(parentUnit);
		this.parentEvent = parentEvent;
	}

	public EventBAction(String label, String assignment, String comment, ISCAction scAction, EventBEvent parentEvent, EventBUnit parentUnit) {
		super(label, comment, scAction, parentUnit);
		this.parentEvent = parentEvent;
		this.assignment = assignment;
	}

	public EventBAction(IAction action, ISCAction scAction, EventBEvent parentEvent, EventBUnit parentUnit) throws RodinDBException {
		super(action, scAction, parentUnit);
		String label = "";
		if (action.hasLabel()) {
			label = action.getLabel();
		}
		if (action.hasAssignmentString()) {
			this.assignment = action.getAssignmentString();
		}
		this.label = label;
		this.scElement = scAction;
		this.parentEvent = parentEvent;
	}

	public String getAssignment() {
		return assignment;
	}

	public void setAssignment(String assignment) {
		this.assignment = assignment;
	}

	@Override
	public ISCAction getScElement() {
		return (ISCAction) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.ACTION;
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + ": " + assignment + " (" + comment + ")";
	}

	@Override
	public Set<EventBAttribute> getDependees() {
		return dependees;
	}

	@Override
	public void setDependees(Set<EventBAttribute> dependees) {
		this.dependees = dependees;
	}

	public Set<EventBAttribute> calculateDependees() {
		Set<EventBAttribute> occurredAttributes = new HashSet<>();
		Assignment assignment;
		try {
			ITypeEnvironment typeEnvironment = parent.getTypeEnvironment();
			assignment = getScElement().getAssignment(typeEnvironment);
			for (FreeIdentifier freeIdentifier : assignment.getFreeIdentifiers()) {
				EventBAttribute attribute = parent.findAttributeByLabel(freeIdentifier.getName());
				if (attribute != null) {
					occurredAttributes.add(attribute);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return occurredAttributes;
	}
}
