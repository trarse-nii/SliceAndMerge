package eventBRefinementSlicer.internal.datastructures;

import org.eventb.core.EventBAttributes;
import org.eventb.core.IConstant;
import org.eventb.core.ISCConstant;
import org.rodinp.core.RodinDBException;

public class EventBConstant extends EventBAttribute {
	
	private static final String TYPE = "CONSTANT";
	
	public EventBConstant(String label, String comment, ISCConstant scConstant, EventBUnit parent){
		super(label, comment, scConstant, parent);
	}
	
	public EventBConstant(IConstant constant, ISCConstant scConstant, EventBUnit parent) throws RodinDBException{
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
		this.scElement = scConstant;
	}
	
	@Override
	public ISCConstant getScElement() {
		return (ISCConstant) super.getScElement();
	}
	
	@Override
	public String toString(){
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")"; 
	}
}
