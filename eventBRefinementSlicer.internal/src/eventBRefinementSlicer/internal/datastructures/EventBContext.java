package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IAxiom;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.eventb.core.ISCAction;
import org.eventb.core.ISCAxiom;
import org.eventb.core.ISCConstant;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ITraceableElement;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.util.SCUtil;

public class EventBContext extends EventBUnit {

	private List<EventBConstant> constants = new ArrayList<>();
	private List<EventBAxiom> axioms = new ArrayList<>();
	private ISCContextRoot scContextRoot = null;

	public EventBContext(IContextRoot contextRoot) throws RodinDBException {
		ISCContextRoot scContextRoot = SCUtil.makeStaticCheckedContext(contextRoot);
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
		this.scContextRoot = scContextRoot;
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
		for (EventBConstant constant : constants) {
			if (constant.getLabel().equals(identifierName)) {
				return constant;
			}
		}
		return null;
	}
}
