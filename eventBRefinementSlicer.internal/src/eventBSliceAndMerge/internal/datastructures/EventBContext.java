package eventBSliceAndMerge.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAxiom;
import org.eventb.core.ICarrierSet;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.eventb.core.IExtendsContext;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCCarrierSet;
import org.eventb.core.ISCConstant;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ITraceableElement;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.ITypeEnvironmentBuilder;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.util.SCUtil;

/**
 * Internal representation of Event-B contexts.
 * 
 * @author Aivar Kripsaar
 *
 */
public class EventBContext extends EventBUnit {

	private List<EventBCarrierSet> carrierSets = new ArrayList<>();
	private List<EventBConstant> constants = new ArrayList<>();
	private List<EventBAxiom> axioms = new ArrayList<>();
	private ISCContextRoot scContextRoot = null;
	private List<String> extendedContextLabels = new ArrayList<>();

	/**
	 * Creates an empty context. Not recommended.
	 */
	public EventBContext() {
		// Intentionally left empty.
	}

	public EventBContext(IContextRoot contextRoot) throws RodinDBException {
		this.scContextRoot = contextRoot.getSCContextRoot();
		for (ICarrierSet originalCarrierSet : contextRoot.getCarrierSets()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalCarrierSet, scContextRoot.getSCCarrierSets());
			assert (originalSCElement instanceof ISCCarrierSet);
			EventBCarrierSet carrierSet = new EventBCarrierSet(originalCarrierSet, (ISCCarrierSet) originalSCElement, this);
			carrierSets.add(carrierSet);
		}
		for (IConstant originalConstant : contextRoot.getConstants()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalConstant, scContextRoot.getSCConstants());
			assert (originalSCElement instanceof ISCConstant);
			EventBConstant constant = new EventBConstant(originalConstant, (ISCConstant) originalSCElement, this);
			constants.add(constant);
		}
		for (IAxiom originalAxiom : contextRoot.getAxioms()) {
			ITraceableElement originalSCElement = SCUtil.findSCElement(originalAxiom, scContextRoot.getSCAxioms());
			assert (originalSCElement instanceof ISCAction);
			EventBAxiom axiom = new EventBAxiom(originalAxiom, (ISCAxiom) originalSCElement, this);
			axioms.add(axiom);
		}
		for (IExtendsContext extendedContext : contextRoot.getExtendsClauses()) {
			extendedContextLabels.add(extendedContext.getAbstractContextName());
		}
		for (EventBCarrierSet carrierSet : carrierSets) {
			carrierSet.setDependees(carrierSet.calculateDependees());
		}
		for (EventBConstant constant : constants) {
			constant.setDependees(constant.calculateDependees());
		}
		for (EventBAxiom axiom : axioms) {
			axiom.setDependees(axiom.calculateDependees());
		}
		this.label = contextRoot.getElementName();
		if (contextRoot.hasComment()) {
			this.comment = contextRoot.getComment();
		}
	}

	public EventBContext(IContextRoot contextRoot, EventBMachine parentMachine) throws RodinDBException {
		this(contextRoot);
		this.parent = parentMachine;
	}

	public List<EventBCarrierSet> getCarrierSets() {
		return carrierSets;
	}

	public void addCarrierSet(EventBCarrierSet carrierSet) {
		carrierSets.add(carrierSet);
	}

	public void addConstant(EventBConstant constant) {
		constants.add(constant);
	}

	public void addAxiom(EventBAxiom axiom) {
		axioms.add(axiom);
	}

	public List<EventBConstant> getConstants() {
		return constants;
	}

	public List<EventBAxiom> getAxioms() {
		return axioms;
	}

	public List<String> getExtendedContextLabels() {
		return extendedContextLabels;
	}

	/**
	 * Checks if this context contains the specified Event-B element
	 * 
	 * @param element
	 *            The element to check for
	 * @return True if element is contained in the context
	 */
	public boolean containsElement(EventBElement element) {
		if (constants.contains(element) || axioms.contains(element) || carrierSets.contains(element)) {
			return true;
		}
		return false;
	}

	/**
	 * Getter for statically checked Rodin-internal representation of context
	 * 
	 * @return The statically checked Rodin-internal representation of context
	 */
	public ISCContextRoot getScContextRoot() {
		return scContextRoot;
	}

	@Override
	public EventBAttribute findAttributeByLabel(String identifierName) {
		for (EventBCarrierSet carrierSet : carrierSets) {
			if (carrierSet.getLabel().equals(identifierName)) {
				return carrierSet;
			}
		}
		for (EventBConstant constant : constants) {
			if (constant.getLabel().equals(identifierName)) {
				return constant;
			}
		}
		return null;
	}

	@Override
	public ITypeEnvironmentBuilder getTypeEnvironment() throws CoreException {
		return this.scContextRoot.getTypeEnvironment();
	}

	@Override
	public FormulaFactory getFormulaFactory() {
		return scContextRoot.getFormulaFactory();
	}

	@Override
	public String getType() {
		final String type = EventBTypes.CONTEXT;
		return type;
	}

	/**
	 * Checks if this Event-B context contains any elements (i.e. axioms, constants and carrier sets).
	 * 
	 * @return True if context contains no elements
	 */
	public boolean isEmpty() {
		return (axioms.isEmpty() && constants.isEmpty() && carrierSets.isEmpty());
	}
}
