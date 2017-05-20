package eventBSliceAndMerge.internal.datastructures;

import org.eventb.core.ICommentedElement;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinDBException;

/**
 * Base class for custom internal representation of Event-B elements, more
 * specific classes inherit from this one. Meant to simplify access to various
 * information contained within elements.
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBElement {

	public enum Type {
		ACTION, ATTRIBUTE, AXIOM, CARRIER_SET, CONDITION, CONSTANT, CONTEXT, ELEMENT, EVENT, GUARD, INVARIANT, MACHINE, UNIT, VARIABLE, REFINED_EVENT, PARAMETER, WITNESS
	}

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

	public EventBElement(IInternalElement internalElement, IInternalElement scElement, EventBUnit parent)
			throws RodinDBException {
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

	/**
	 * Returns label describing this element (usually its name)
	 * 
	 * @return The string label of this element
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Returns label describing this element (usually its name) including the
	 * parent label (expected to be overridden by specific subclasses)
	 * 
	 * @return
	 */
	public String getLabelFullPath() {
		return label;
	}

	/**
	 * Getter for optional comment describing this element
	 * 
	 * @return String comment descibing this element
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * Getter for statically checked Rodin-internal representation of this
	 * element
	 * 
	 * @return The statically checked Rodin-internal representation of this
	 *         element
	 */
	public IInternalElement getScElement() {
		return scElement;
	}

	/**
	 * Getter for string representation of this element's type
	 * 
	 * @return String representation of this element's type
	 */
	public Type getType() {
		return Type.ELEMENT;
	}

	@Override
	public String toString() {
		return getType() + ": " + label + " (" + comment + ")";
	}

	/**
	 * Returns array of this element's two string descriptors - the label and
	 * the comment
	 * 
	 * @return Array containing this elements string descriptors (e.g. label and
	 *         comment)
	 */
	public Object[] toArray() {
		Object[] array = { label, comment };
		return array;
	}

	/**
	 * Getter for this element's parent unit (machine or context)
	 * 
	 * @return The parent of this element
	 */
	public EventBUnit getParent() {
		return parent;
	}

	public static boolean isLeafElement(EventBElement element) {
		return element instanceof EventBAction || element instanceof EventBAttribute
				|| element instanceof EventBCondition || element instanceof EventBCarrierSet;
	}

}
