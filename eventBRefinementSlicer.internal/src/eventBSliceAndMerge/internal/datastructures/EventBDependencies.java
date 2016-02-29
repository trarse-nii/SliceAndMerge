package eventBSliceAndMerge.internal.datastructures;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class containing information about an element's dependencies. Dependencies are tracked in two maps in order
 * to keep track of which other elements a certain element is dependent on, and which other elements depend on
 * certain elements.
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBDependencies {
	private Map<EventBElement, Set<EventBElement>> dependencyMap = new HashMap<>();
	private Map<EventBElement, Set<EventBElement>> reverseDependencyMap = new HashMap<>();

	public void setDependencyMap(Map<EventBElement, Set<EventBElement>> dependencyMap) {
		this.dependencyMap = dependencyMap;
	}

	public void setReverseDependencyMap(Map<EventBElement, Set<EventBElement>> reverseDependencyMap) {
		this.reverseDependencyMap = reverseDependencyMap;
	}

	/**
	 * Adds a new dependency
	 * 
	 * @param depender
	 *            The element depending
	 * @param dependee
	 *            The element being depended on
	 */
	public void addDependency(EventBElement depender, EventBElement dependee) {
		if (!dependencyMap.containsKey(depender)) {
			dependencyMap.put(depender, new HashSet<>());
		}
		dependencyMap.get(depender).add(dependee);
		if (!reverseDependencyMap.containsKey(dependee)) {
			reverseDependencyMap.put(dependee, new HashSet<>());
		}
		reverseDependencyMap.get(dependee).add(depender);
	}

	/**
	 * Removes a dependency
	 * 
	 * @param depender
	 *            The element depending
	 * @param dependee
	 *            The element being depended on
	 */
	public void removeDependency(EventBElement depender, EventBElement dependee) {
		if (dependencyMap.containsKey(depender)) {
			dependencyMap.get(depender).remove(dependee);
			if (dependencyMap.get(depender).isEmpty()) {
				dependencyMap.remove(depender);
			}
		}
		if (reverseDependencyMap.containsKey(dependee)) {
			reverseDependencyMap.get(dependee).remove(depender);
			if (reverseDependencyMap.get(dependee).isEmpty()) {
				reverseDependencyMap.remove(dependee);
			}
		}
	}

	/**
	 * Checks if one given element (depender) is dependent on the other given element (dependee)
	 * 
	 * @param depender
	 *            The element potentially depending on the other element
	 * @param dependee
	 *            The element potentially being depended upon
	 * @return True if the depender depends on the dependee
	 */
	public boolean isDependent(EventBElement depender, EventBElement dependee) {
		if (!dependencyMap.containsKey(depender)) {
			return false;
		}
		return dependencyMap.get(depender).contains(dependee);
	}

	/**
	 * Fetches all elements a given element depends on
	 * 
	 * @param element
	 *            The element for which we want all elements it depends on
	 * @return Set of all elements the given element is dependent on
	 */
	public Set<EventBElement> getDependeesForElement(EventBElement element) {
		if (!dependencyMap.containsKey(element)) {
			return Collections.emptySet();
		}
		return dependencyMap.get(element);
	}

	/**
	 * Fetches all elements that depend on a given element
	 * 
	 * @param element
	 *            The element for which we want all elements that depend on it
	 * @return Set of all elements that depend on the given element
	 */
	public Set<EventBElement> getDependersForElement(EventBElement element) {
		if (!reverseDependencyMap.containsKey(element)) {
			return Collections.emptySet();
		}
		return reverseDependencyMap.get(element);
	}
}
