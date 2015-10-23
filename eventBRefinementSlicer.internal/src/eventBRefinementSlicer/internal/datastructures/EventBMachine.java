package eventBRefinementSlicer.internal.datastructures;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eventb.core.IContextRoot;
import org.eventb.core.IInvariant;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.rodinp.core.RodinDBException;

public class EventBMachine extends EventBUnit {
	
	private List<EventBInvariant> invariants = new ArrayList<>();
	private List<EventBVariable> variables = new ArrayList<>();
	private Set<EventBContext> seenContexts = new HashSet<>();

	public EventBMachine(IMachineRoot machineRoot) throws RodinDBException{
		for (IInvariant originalInvariant : machineRoot.getInvariants()){
			EventBInvariant invariant = new EventBInvariant(originalInvariant, this);
			invariants.add(invariant);
		}
		for (IVariable originalVariable : machineRoot.getVariables()){
			EventBVariable variable = new EventBVariable(originalVariable, this);
			variables.add(variable);
		}
		for (ISeesContext seenContext : machineRoot.getSeesClauses()){
			IContextRoot contextRoot = seenContext.getSeenContextRoot();
			EventBContext context = new EventBContext(contextRoot);
			seenContexts.add(context);
		}
	}
	
	public List<EventBInvariant> getInvariants(){
		return invariants;
	}
	
	public List<EventBVariable> getVariables(){
		return variables;
	}
	
	public Set<EventBContext> getSeenContexts() {
		return seenContexts;
	}
	
	public void addSeenContext(EventBContext context){
		seenContexts.add(context);
	}
	
	public void removeSeenContext(EventBContext context){
		seenContexts.remove(context);
	}
}
