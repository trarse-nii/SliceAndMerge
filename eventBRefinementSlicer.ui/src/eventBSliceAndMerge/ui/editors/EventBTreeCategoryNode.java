package eventBSliceAndMerge.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBElement.Type;

/**
 * Container class for tree subcategories (e.g. Variables, Invariants, Events).
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBTreeCategoryNode extends EventBTreeNode {

	/* Map from EventB element types to label texts */
	private static HashMap<Type, String> labelMap;
	static {
		labelMap = new HashMap<Type, String>();
		labelMap.put(Type.INVARIANT, "Invariants");
		labelMap.put(Type.VARIABLE, "Variables");
		labelMap.put(Type.EVENT, "Events");
		labelMap.put(Type.CONTEXT, "Seen Contexts");
		labelMap.put(Type.PARAMETER, "Parameters");
		labelMap.put(Type.WITNESS, "Witnesses");
		labelMap.put(Type.GUARD, "Guards");
		labelMap.put(Type.ACTION, "Actions");
		labelMap.put(Type.AXIOM, "Axioms");
		labelMap.put(Type.CONSTANT, "Constants");
		labelMap.put(Type.CARRIER_SET, "Carrier Sets");
	}

	final String label;
	final EventBTreeAtomicNode parentNode;
	final EventBTreeAtomicNode[] childrenNodes;

	public EventBTreeCategoryNode(Type type, EventBTreeAtomicNode parent, List<? extends EventBElement> children,
			Map<EventBElement, EventBTreeAtomicNode> element2TreeNode, Object outerType) {
		super(outerType);
		this.label = labelMap.get(type);
		this.parentNode = parent;
		this.childrenNodes = createChildren(children, element2TreeNode);
	}

	private EventBTreeAtomicNode[] createChildren(List<? extends EventBElement> childrenElements,
			Map<EventBElement, EventBTreeAtomicNode> element2TreeNode) {
		List<EventBTreeAtomicNode> treeChildren = new ArrayList<>();
		for (EventBElement childElement : childrenElements) {
			EventBTreeAtomicNode childNode = new EventBTreeAtomicNode(this, childElement, outerType);
			element2TreeNode.put(childElement, childNode);
			treeChildren.add(childNode);
		}
		return treeChildren.toArray(new EventBTreeAtomicNode[treeChildren.size()]);
	}

	public String getLabel() {
		return label;
	}

	public EventBTreeAtomicNode getParentElement() {
		return parentNode;
	}

	public EventBTreeAtomicNode[] getChildren() {
		return childrenNodes;
	}

	/**
	 * Finds the tree container version of the given element.
	 * 
	 * @param originalElement
	 *            Editor internal representation of element
	 * @return Tree container element of the desired element
	 */
	public EventBTreeAtomicNode findTreeElement(EventBElement originalElement) {
		for (EventBTreeAtomicNode child : childrenNodes) {
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
		result = prime * result + ((parentNode == null) ? 0 : parentNode.hashCode());
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
		EventBTreeCategoryNode other = (EventBTreeCategoryNode) obj;
		if (!outerType.equals(other.outerType))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (parentNode == null) {
			if (other.parentNode != null)
				return false;
		} else if (!parentNode.equals(other.parentNode))
			return false;
		return true;
	}

}
