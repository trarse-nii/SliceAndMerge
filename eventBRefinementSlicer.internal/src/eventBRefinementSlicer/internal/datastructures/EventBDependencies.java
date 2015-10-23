package eventBRefinementSlicer.internal.datastructures;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventBDependencies {
	private Map<EventBElement, Set<EventBElement>> dependencyMap = new HashMap<>();
	private Map<EventBElement, Set<EventBElement>> reverseDependencyMap = new HashMap<>();

	public void setDependencyMap(
			Map<EventBElement, Set<EventBElement>> dependencyMap) {
		this.dependencyMap = dependencyMap;
	}

	public void setReverseDependencyMap(
			Map<EventBElement, Set<EventBElement>> reverseDependencyMap) {
		this.reverseDependencyMap = reverseDependencyMap;
	}

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

	public void removeDependency(EventBElement depender,
			EventBElement dependee) {
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

	public boolean isDependent(EventBElement depender, EventBElement dependee) {
		if (!dependencyMap.containsKey(depender)) {
			return false;
		}
		return dependencyMap.get(depender).contains(dependee);
	}

	public Set<EventBElement> getDependeesForElement(EventBElement element) {
		if (!dependencyMap.containsKey(element)) {
			return Collections.emptySet();
		}
		return dependencyMap.get(element);
	}

	public Set<EventBElement> getDependersForElement(EventBElement element) {
		if (!reverseDependencyMap.containsKey(element)) {
			return Collections.emptySet();
		}
		return reverseDependencyMap.get(element);
	}
}
