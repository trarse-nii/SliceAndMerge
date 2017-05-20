package eventBSliceAndMerge.internal.datastructures;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.ITypeEnvironmentBuilder;

/**
 * Common super class for EventBMachine and EventBContext
 * 
 * @author Aivar Kripsaar
 *
 */
public abstract class EventBUnit extends EventBElement {

	public EventBUnit() {
		super(null);
	}

	protected EventBDependencies dependencies = null;

	public void setDependencies(EventBDependencies dependencies) {
		this.dependencies = dependencies;
	}

	public EventBDependencies getDependencies() {
		return dependencies;
	}

	@Override
	public Type getType() {
		return Type.UNIT;
	}

	/**
	 * Finds an attribute element with the given label contained within this unit (machine or context)
	 * 
	 * @param label
	 *            The label of the attribute element that needs to be found
	 * @return The attribute element that was found
	 */
	public abstract EventBAttribute findAttributeByLabel(String label);

	/**
	 * Gets the type environment of the unit (i.e. machine or context). Needed for certain operations
	 * involving statically checked elements.
	 * 
	 * @return This unit's type environment
	 * @throws CoreException
	 */
	public abstract ITypeEnvironmentBuilder getTypeEnvironment() throws CoreException;

	/**
	 * Gets the formula factory of the unit (i.e. machine or context). Needed for certain operations involving
	 * statically checked elements.
	 * 
	 * @return This unit's formula factory
	 */
	public abstract FormulaFactory getFormulaFactory();
}
