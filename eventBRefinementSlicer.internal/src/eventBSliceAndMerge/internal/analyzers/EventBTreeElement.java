package eventBSliceAndMerge.internal.analyzers;

import eventBSliceAndMerge.internal.datastructures.EventBElement;

/**
 * Container class for Event-B elements in tree
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBTreeElement {

	final EventBTreeSubcategory parent;
	final EventBElement originalElement;
	final Object outerType;
	
	public EventBTreeElement(EventBTreeSubcategory parent, EventBElement originalElement, Object outerType) {
		this.parent = parent;
		this.originalElement = originalElement;
		this.outerType = outerType;
	}

	public EventBTreeSubcategory getParent() {
		return parent;
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
		EventBTreeElement other = (EventBTreeElement) obj;
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
