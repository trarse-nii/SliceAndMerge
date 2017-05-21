package eventBSliceAndMerge.ui.editors;

import java.util.Map;

import eventBSliceAndMerge.internal.datastructures.EventBElement;

/**
 * Container class for Event-B elements in tree
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBTreeAtomicNode extends EventBTreeNode {

	final EventBTreeCategoryNode parentCategory;
	final EventBElement originalElement;

	public EventBTreeAtomicNode(EventBTreeCategoryNode parent, EventBElement originalElement,
			Map<EventBElement, EventBTreeAtomicNode> element2TreeNode, Object outerType) {
		super(outerType);
		this.parentCategory = parent;
		this.originalElement = originalElement;
		element2TreeNode.put(originalElement, this);
	}

	public EventBTreeCategoryNode getParentCategory() {
		return parentCategory;
	}

	public EventBElement getOriginalElement() {
		return originalElement;
	}

	@Override
	public String toString() {
		return originalElement.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + outerType.hashCode();
		result = prime * result + ((originalElement == null) ? 0 : originalElement.hashCode());
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
		EventBTreeAtomicNode other = (EventBTreeAtomicNode) obj;
		if (!outerType.equals(other.outerType))
			return false;
		if (originalElement == null) {
			if (other.originalElement != null)
				return false;
		} else if (!originalElement.equals(other.originalElement))
			return false;
		return true;
	}

}
