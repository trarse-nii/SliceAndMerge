package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.ICommentedElement;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinDBException;

/**
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBElement {

	protected EventBUnit parent = null;
	protected String label = "";
	protected String comment = "";
	protected IInternalElement scElement = null;

	public EventBElement(EventBUnit parent) {
		this.parent = parent;
	}

	public EventBElement(String label, String comment, IInternalElement scElement, EventBUnit parent) {
		this.label = label;
		this.comment = comment;
		this.scElement = scElement;
		this.parent = parent;
	}

	public EventBElement(IInternalElement internalElement, IInternalElement scElement, EventBUnit parent) throws RodinDBException {
		this.parent = parent;
		this.scElement = scElement;
		if (internalElement instanceof ICommentedElement && ((ICommentedElement) internalElement).hasComment()) {
			this.comment = ((ICommentedElement) internalElement).getComment();
		}
	}

	public EventBElement(IInternalElement scElement, EventBUnit parent) throws RodinDBException {
		this.parent = parent;
		this.scElement = scElement;
	}

	public String getLabel() {
		return label;
	}

	public String getComment() {
		return comment;
	}

	public IInternalElement getScElement() {
		return scElement;
	}

	public String getType() {
		final String type = EventBTypes.ELEMENT;
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + " (" + comment + ")";
	}

	public Object[] toArray() {
		Object[] array = { label, comment };
		return array;
	}

	public void hasChanged() {
		parent.changedElement(this);
	}

	public EventBUnit getParent() {
		return parent;
	}

}
