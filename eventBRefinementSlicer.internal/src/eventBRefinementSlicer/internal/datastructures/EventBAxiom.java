package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.IAxiom;
import org.rodinp.core.RodinDBException;

public class EventBAxiom extends EventBCondition {
	
	private static final String TYPE = "AXIOM";
	
	public EventBAxiom(String label, String predicate, String comment, EventBUnit parent){
		super(label, predicate, comment, parent);
	}
	
	public EventBAxiom(IAxiom axiom, EventBUnit parent) throws RodinDBException{
		super(parent);
		String label = "";
		String predicate = "";
		String comment = "";
		if (axiom.hasLabel()){
			label = axiom.getLabel();
		}
		if (axiom.hasPredicateString()){
			predicate = axiom.getPredicateString();
		}
		if (axiom.hasComment()){
			comment = axiom.getComment();
		}
		this.label = label;
		this.predicate = predicate;
		this.comment = comment;
	}
	
	@Override
	public String toString(){
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + ": " + predicate + " (" + comment + ")"; 
	}
}
