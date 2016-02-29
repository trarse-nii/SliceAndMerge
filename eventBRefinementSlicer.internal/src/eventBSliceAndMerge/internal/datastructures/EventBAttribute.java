package eventBSliceAndMerge.internal.datastructures;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IIdentifierElement;
import org.eventb.core.ISCIdentifierElement;
import org.eventb.core.ast.FreeIdentifier;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.Depender;

/**
 * Common parent class for EventBVariable, EventBConstant, and EventBParameter
 * 
 * @author Aivar Kripsaar
 * 
 */

public class EventBAttribute extends EventBElement implements Depender {

	private Set<EventBAttribute> dependees = new HashSet<>();

	public EventBAttribute(EventBUnit parent) {
		super(parent);
	}

	public EventBAttribute(String label, String comment, ISCIdentifierElement scElement, EventBUnit parent) {
		super(label, comment, scElement, parent);
	}

	public EventBAttribute(IIdentifierElement identifierElement, ISCIdentifierElement scElement, EventBUnit parent) throws RodinDBException {
		super(identifierElement, scElement, parent);
		if (identifierElement.hasIdentifierString()) {
			this.label = identifierElement.getIdentifierString();
		}
	}

	public EventBAttribute(ISCIdentifierElement scElement, EventBUnit parent) throws RodinDBException {
		super(scElement, parent);
		this.label = scElement.getElementName();
	}

	@Override
	public ISCIdentifierElement getScElement() {
		return (ISCIdentifierElement) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.ATTRIBUTE;
		return type;
	}

	@Override
	public Set<EventBAttribute> getDependees() {
		return dependees;
	}

	@Override
	public void setDependees(Set<EventBAttribute> dependees) {
		this.dependees = dependees;
	}

	@Override
	public Set<EventBAttribute> calculateDependees() {
		Set<EventBAttribute> dependees = new HashSet<EventBAttribute>();
		try {
			FreeIdentifier[] freeIdentifiers = ((ISCIdentifierElement) scElement).getType(parent.getFormulaFactory()).toExpression()
					.getFreeIdentifiers();
			for (FreeIdentifier freeIdentifier : freeIdentifiers) {
				EventBAttribute attribute = parent.findAttributeByLabel(freeIdentifier.getName());
				if (attribute != null) {
					dependees.add(attribute);
				}
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return dependees;
	}
}
