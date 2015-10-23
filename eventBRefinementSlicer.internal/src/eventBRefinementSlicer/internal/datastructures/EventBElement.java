package eventBRefinementSlicer.internal.datastructures;


public class EventBElement {

	static final private String TYPE = "ELEMENT";
	protected EventBUnit parent = null;
	protected boolean selected = false;
	protected String label = "";
	protected String comment = "";

	public EventBElement(EventBUnit parent) {
		this.parent = parent;
	}

	public EventBElement(String label, String comment, EventBUnit parent) {
		this.label = label;
		this.comment = comment;
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

	@Override
	public String toString() {
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " ("
				+ comment + ")";
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
