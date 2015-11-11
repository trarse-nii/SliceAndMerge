package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IAxiom;
import org.eventb.core.ICarrierSet;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCCarrierSet;
import org.eventb.core.ISCConstant;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ITraceableElement;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.ITypeEnvironmentBuilder;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.util.SCUtil;

public class EventBContext extends EventBUnit {

	private List<EventBCarrierSet> carrierSets = new ArrayList<>();
	private List<EventBConstant> constants = new ArrayList<>();
	private List<EventBAxiom> axioms = new ArrayList<>();
	private ISCContextRoot scContextRoot = null;

	public EventBContext(IContextRoot contextRoot) throws RodinDBException {
		this.scContextRoot = SCUtil.makeStaticCheckedContext(contextRoot);
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
		for (EventBCarrierSet carrierSet : carrierSets) {
			carrierSet.setDependees(carrierSet.calculateDependees());
		}
		for (EventBConstant constant : constants) {
			constant.setDependees(constant.calculateDependees());
		}
		for (EventBAxiom axiom : axioms) {
			axiom.setDependees(axiom.calculateDependees());
		}
	}

	public List<EventBCarrierSet> getCarrierSets() {
		return carrierSets;
	}

	public List<EventBConstant> getConstants() {
		return constants;
	}

	public List<EventBAxiom> getAxioms() {
		return axioms;
	}

	public ISCContextRoot getScContextRoot() {
		return scContextRoot;
	}
	
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
	
	public ITypeEnvironmentBuilder getTypeEnvironment() throws CoreException {
		return this.scContextRoot.getTypeEnvironment();
	}

	public FormulaFactory getFormulaFactory() {
		return scContextRoot.getFormulaFactory();
	}
}
