package eventBRefinementSlicer.internal.datastructures;

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
	public String getType() {
		final String type = EventBTypes.UNIT;
		return type;
	}

	public abstract EventBAttribute findAttributeByLabel(String label);

	public abstract ITypeEnvironmentBuilder getTypeEnvironment() throws CoreException;

	public abstract FormulaFactory getFormulaFactory();
}
