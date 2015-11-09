package eventBRefinementSlicer.internal.datastructures;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.viewers.StructuredViewer;

/**
 * Common super class for EventBMachine and EventBContext
 * 
 * @author Aivar Kripsaar
 *
 */
public abstract class EventBUnit {

	protected EventBDependencies dependencies = null;

	protected Set<StructuredViewer> changeListeners = new HashSet<>();

	public void addChangeListener(StructuredViewer viewer) {
		changeListeners.add(viewer);
	}

	public void removeChangeListener(StructuredViewer viewer) {
		changeListeners.remove(viewer);
	}

	public void changedElement(EventBElement element) {
		for (StructuredViewer viewer : changeListeners) {
			viewer.update(element, null);
		}
	}

	public void setDependencies(EventBDependencies dependencies) {
		this.dependencies = dependencies;
	}

	public EventBDependencies getDependencies() {
		return dependencies;
	}
	
	public abstract EventBAttribute findAttributeByLabel(String label);
}
