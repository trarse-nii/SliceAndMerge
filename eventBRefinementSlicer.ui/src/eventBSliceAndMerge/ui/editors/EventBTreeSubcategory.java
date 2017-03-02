package eventBSliceAndMerge.ui.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBUnit;

/**
 * Container class for tree subcategories (e.g. Variables, Invariants, Events).
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBTreeSubcategory extends EventBTreeNode {

	final String label;
	final EventBUnit parentUnit;
	final EventBTreeElement parentElement;
	final EventBTreeElement[] children;

	public EventBTreeSubcategory(String label, EventBUnit parent, List<? extends EventBElement> children,
			Map<EventBElement, EventBTreeElement> elementToTreeElementMap, Object outerType) {
		super(outerType);
		this.label = label;
		this.parentUnit = parent;
		this.parentElement = null;
		this.children = createChildren(children, elementToTreeElementMap);
	}

	public EventBTreeSubcategory(String label, EventBTreeElement parent, List<? extends EventBElement> children,
			Map<EventBElement, EventBTreeElement> elementToTreeElementMap, Object outerType) {
		super(outerType);
		this.label = label;
		this.parentElement = parent;
		this.parentUnit = null;
		this.children = createChildren(children, elementToTreeElementMap);
	}
	
	private EventBTreeElement[] createChildren(List<? extends EventBElement> children, Map<EventBElement, EventBTreeElement> elementToTreeElementMap){
		List<EventBTreeElement> treeChildren = new ArrayList<>();
		for (EventBElement originalChild : children) {
			EventBTreeElement treeChild = new EventBTreeElement(this, originalChild, outerType);
			elementToTreeElementMap.put(originalChild, treeChild);
			treeChildren.add(treeChild);
		}
		return treeChildren.toArray(new EventBTreeElement[treeChildren.size()]);
	}

	public String getLabel() {
		return label;
	}

	public EventBUnit getParentUnit() {
		return parentUnit;
	}

	public EventBTreeElement getParentElement() {
		return parentElement;
	}

	public EventBTreeElement[] getChildren() {
		return children;
	}

	/**
	 * Finds the tree container version of the given element.
	 * 
	 * @param originalElement
	 *            Editor internal representation of element
	 * @return Tree container element of the desired element
	 */
	public EventBTreeElement findTreeElement(EventBElement originalElement) {
		for (EventBTreeElement child : children) {
			if (child.getOriginalElement().equals(originalElement)) {
				return child;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + outerType.hashCode();
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((parentElement == null) ? 0 : parentElement.hashCode());
		result = prime * result + ((parentUnit == null) ? 0 : parentUnit.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventBTreeSubcategory other = (EventBTreeSubcategory) obj;
		if (!outerType.equals(other.outerType))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (parentElement == null) {
			if (other.parentElement != null)
				return false;
		} else if (!parentElement.equals(other.parentElement))
			return false;
		if (parentUnit == null) {
			if (other.parentUnit != null)
				return false;
		} else if (!parentUnit.equals(other.parentUnit))
			return false;
		return true;
	}

}
