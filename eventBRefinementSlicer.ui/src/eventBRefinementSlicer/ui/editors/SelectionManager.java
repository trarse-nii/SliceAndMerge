package eventBRefinementSlicer.ui.editors;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eventBRefinementSlicer.internal.datastructures.EventBElement;

public class SelectionManager {
	
	private final static SelectionManager INSTANCE = new SelectionManager();
	private Map<EventBElement, Boolean> selectionMap = new HashMap<>();
	private Set<EventBElement> selectedElements = new HashSet<>();
	
	
	protected SelectionManager(){
		// Intentionally left empty
	}
	
	public static SelectionManager getInstance(){
		return INSTANCE;
	}
	
	public boolean isElementSelected(EventBElement element){
		if (!selectionMap.containsKey(element)){
			selectionMap.put(element, Boolean.FALSE);
			return false;
		}
		return selectionMap.get(element).booleanValue();
	}
	
	public void setSelection(EventBElement element, boolean selection){
		selectionMap.put(element, Boolean.valueOf(selection));
		if (selection){
			selectedElements.add(element);
		} else {
			selectedElements.remove(element);
		}
	}
	
	public void addSelection(EventBElement element){
		setSelection(element, true);
	}
	
	public void removeSelection(EventBElement element){
		setSelection(element, false);
	}
	
	public void clearAllSelections(){
		selectionMap.clear();
	}
	
	public Set<EventBElement> getSelectedElements(){
		return selectedElements;
	}
}
