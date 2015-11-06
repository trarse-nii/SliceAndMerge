package eventBRefinementSlicer.internal.datastructures;

import org.rodinp.core.IInternalElement;

/**
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBElement {

	protected EventBUnit parent = null;
	protected boolean selected = false;
	protected String label = "";
	protected String comment = "";
	protected IInternalElement scElement;

	public EventBElement(EventBUnit parent) {
		this.parent = parent;
	}

	public EventBElement(String label, String comment, IInternalElement scElement, EventBUnit parent) {
		this.label = label;
		this.comment = comment;
		this.scElement = scElement;
		this.parent = parent;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
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

	protected String getType() {
		final String type = "ELEMENT";
		return type;
	}

	@Override
	public String toString() {
		return getType() + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")";
	}

	public Object[] toArray() {
		Object[] array = { selected, label, comment };
		return array;
	}

	public void hasChanged() {
		parent.changedElement(this);
	}

	public EventBUnit getParent() {
		return parent;
	}

}
