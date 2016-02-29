package eventBSliceAndMerge.internal.datastructures;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.ICarrierSet;
import org.eventb.core.ISCCarrierSet;
import org.eventb.core.ast.FormulaFactory;
import org.rodinp.core.RodinDBException;

/**
 * Internal representation of Event-B carrier sets, which are elements of Event-B contexts
 * 
 * @author Aivar Kripsaar
 *
 */

public class EventBCarrierSet extends EventBAttribute {

	public EventBCarrierSet(String label, String comment, ISCCarrierSet scCarrierSet, EventBUnit parent) {
		super(label, comment, scCarrierSet, parent);
	}

	public EventBCarrierSet(ICarrierSet carrierSet, ISCCarrierSet scCarrierSet, EventBUnit parent) throws RodinDBException {
		super(carrierSet, scCarrierSet, parent);
	}

	/**
	 * Calculates the elements populating this carrier set
	 * 
	 * @return The set of elements populating this carrier set
	 */
	private Set<EventBAttribute> calculateContents() {
		Set<EventBAttribute> contents = new HashSet<EventBAttribute>();
		assert (parent instanceof EventBContext);
		FormulaFactory formulaFactory = ((EventBContext) parent).getScContextRoot().getFormulaFactory();
		for (EventBConstant constant : ((EventBContext) parent).getConstants()) {
			try {
				if (constant.getScElement().getType(formulaFactory).toString().equals(label)) {
					contents.add(constant);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return contents;
	}

	@Override
	public ISCCarrierSet getScElement() {
		return (ISCCarrierSet) super.getScElement();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.CARRIER_SET;
		return type;
	}

	@Override
	public Set<EventBAttribute> calculateDependees() {
		return calculateContents();
	}
}
