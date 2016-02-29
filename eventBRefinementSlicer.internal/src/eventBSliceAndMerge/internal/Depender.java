package eventBSliceAndMerge.internal;

import java.util.Set;

import eventBSliceAndMerge.internal.datastructures.EventBAttribute;

/**
 * Interface for element types that can depend on other elements
 * 
 * @author Aivar Kripsaar
 *
 */
public interface Depender {

	/**
	 * Getter for elements this element depends on.
	 * 
	 * @return Set of elements this element depends on
	 */
	Set<EventBAttribute> getDependees();

	/**
	 * Setter for elements this element depends on.
	 * 
	 * @param dependees
	 *            Set of elements this element depends on
	 */
	void setDependees(Set<EventBAttribute> dependees);

	/**
	 * Calculates the elements this element depends on.
	 * 
	 * @return Set of elements this element depends on
	 */
	public Set<EventBAttribute> calculateDependees();
}
