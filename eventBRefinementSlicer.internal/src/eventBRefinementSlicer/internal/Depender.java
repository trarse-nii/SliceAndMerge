package eventBRefinementSlicer.internal;

import java.util.Set;

import eventBRefinementSlicer.internal.datastructures.EventBAttribute;

public interface Depender {
	Set<EventBAttribute> getDependees();
	void setDependees(Set<EventBAttribute> dependees);
}
