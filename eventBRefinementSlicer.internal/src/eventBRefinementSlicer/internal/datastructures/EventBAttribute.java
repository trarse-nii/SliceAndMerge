package eventBRefinementSlicer.internal.datastructures;

import java.util.Set;
import java.util.HashSet;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IIdentifierElement;
import org.eventb.core.ISCIdentifierElement;
import org.eventb.core.ast.FreeIdentifier;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.Depender;

/**
 * Common parent class for EventBVariable and EventBConstant
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
	protected String getType() {
		final String type = "ATTRIBUTE";
		return type;
	}

	public Set<EventBAttribute> getDependees() {
		return dependees;
	}

	public void setDependees(Set<EventBAttribute> dependees) {
		this.dependees = dependees;
	}

	public Set<EventBAttribute> calculateDependees() {
		Set<EventBAttribute> dependees = new HashSet<EventBAttribute>();
		try {
			FreeIdentifier[] freeIdentifiers = ((ISCIdentifierElement) scElement).getType(parent.getFormulaFactory()).toExpression().getFreeIdentifiers();
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
