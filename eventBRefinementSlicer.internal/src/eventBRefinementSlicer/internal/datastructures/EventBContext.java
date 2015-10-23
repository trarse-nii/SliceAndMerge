package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.List;

import org.eventb.core.IAxiom;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.rodinp.core.RodinDBException;

public class EventBContext extends EventBUnit {
	private List<EventBConstant> constants = new ArrayList<>();
	private List<EventBAxiom> axioms = new ArrayList<>();
	
	public EventBContext(IContextRoot contextRoot) throws RodinDBException{
		for (IConstant originalConstant : contextRoot.getConstants()){
			EventBConstant constant = new EventBConstant(originalConstant, this);
			constants.add(constant);
		}
		for (IAxiom originalAxiom : contextRoot.getAxioms()){
			EventBAxiom axiom = new EventBAxiom(originalAxiom, this);
			axioms.add(axiom);
		}
	}
	
	public List<EventBConstant> getConstants(){
		return constants;
	}
	
	public List<EventBAxiom> getAxioms() {
		return axioms;
	}
}
