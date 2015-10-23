package eventBRefinementSlicer.internal.datastructures;

/**
 * Common parent class for EventBVariable and EventBConstant
 * 
 * @author Aivar Kripsaar
 * 
 */

public class EventBAttribute extends EventBElement {
	
	private static final String TYPE = "ATTRIBUTE";
	
	public EventBAttribute(EventBUnit parent){
		super(parent);
	}
	
	public EventBAttribute(String label, String comment, EventBUnit parent){
		super(label, comment, parent);
	}
	
	@Override
	public String toString(){
		return TYPE + ": [" + (selected ? "x" : " ") + "] " + label + " (" + comment + ")"; 
	}
}
