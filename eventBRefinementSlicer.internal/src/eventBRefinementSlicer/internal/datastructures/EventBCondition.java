package eventBRefinementSlicer.internal.datastructures;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IDerivedPredicateElement;
import org.eventb.core.ILabeledElement;
import org.eventb.core.ISCPredicateElement;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Predicate;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.Depender;

/**
 * Common parent class for EventBInvariant, EventBAxiom, and EventBGuard
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBCondition extends EventBElement implements Depender {

	protected String predicate = "";
	protected boolean isTheorem = false;
	protected Set<EventBAttribute> dependees = new HashSet<>();

	public EventBCondition(EventBUnit parent) {
		super(parent);
	}

	public EventBCondition(String label, String predicate, ISCPredicateElement scPredicateElement, String comment, EventBUnit parent) {
		super(label, comment, scPredicateElement, parent);
		this.predicate = predicate;
	}

	public EventBCondition(IDerivedPredicateElement predicateElement, ISCPredicateElement scPredicateElement, EventBUnit parent)
			throws RodinDBException {
		super(predicateElement, scPredicateElement, parent);
		if (predicateElement instanceof ILabeledElement && ((ILabeledElement) predicateElement).hasLabel()) {
			this.label = ((ILabeledElement) predicateElement).getLabel();
		}
		if (predicateElement.hasPredicateString()) {
			this.predicate = predicateElement.getPredicateString();
		}
		this.isTheorem = predicateElement.isTheorem();
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public boolean isTheorem() {
		return isTheorem;
	}

	@Override
	public ISCPredicateElement getScElement() {
		return (ISCPredicateElement) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.CONDITION;
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + ": " + predicate + " (" + comment + ")";
	}

	@Override
	public Object[] toArray() {
		Object[] array = { getLabel(), getPredicate(), getComment() };
		return array;
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
		Predicate pred;
		try {
			ITypeEnvironment typeEnvironment = parent.getTypeEnvironment();
			pred = getScElement().getPredicate(typeEnvironment);
			for (FreeIdentifier freeIdentifier : pred.getFreeIdentifiers()) {
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
