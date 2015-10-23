package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.EventBAttributes;
import org.eventb.core.IConstant;
import org.rodinp.core.RodinDBException;

public class EventBConstant extends EventBAttribute {
	
	private static final String TYPE = "CONSTANT";
	
	public EventBConstant(String label, String comment, EventBUnit parent){
		super(label, comment, parent);
	}
	
	public EventBConstant(IConstant constant, EventBUnit parent) throws RodinDBException{
		super(parent);
		String label = "";
		String comment = "";
		if (constant.hasAttribute(EventBAttributes.IDENTIFIER_ATTRIBUTE)){
			label = constant.getAttributeValue(EventBAttributes.IDENTIFIER_ATTRIBUTE);
		}
		if (constant.hasComment()){
			comment = constant.getComment();
		}
		this.label = label;
		this.comment = comment;
	}
	
	@Override
	public String toString(){
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")"; 
	}
}
