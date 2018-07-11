/**
 * Implementation of Shinnosuke Saruwatari's method.
 * Analyze given machine's event refinements and classify them.
 * 
 * @author Tsutomu Kobayashi
 *
 */

package eventBSliceAndMerge.internal.analyzers;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.*;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Predicate;
import org.eventb.core.seqprover.transformer.*;
import org.eventb.smt.core.internal.ast.*;
import org.eventb.smt.core.internal.ast.commands.*;
import org.eventb.smt.core.internal.ast.symbols.*;
import org.eventb.smt.core.internal.translation.SMTThroughPP;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.datastructures.*;
import eventBSliceAndMerge.internal.util.*;

@SuppressWarnings("restriction")
public class RefinementAnnotator {
	private String inputFilePath = "/tmp/ra-z3-input.scm";

	private EventBMachine concreteMachine;
	private List<Predicate> invPOHypotheses;
	
	private static enum GuardRefinementType {
		LIMITED, DIVIDED, REPLACED, UNCHANGED, NOT_REFINEMENT
	}

	public RefinementAnnotator(EventBMachine concreteMachine) throws CoreException {
		this.concreteMachine = concreteMachine;
		this.invPOHypotheses = new ArrayList<Predicate>();
		IPOSequent anInvSequent = null;
		for (IPOSequent poSequent : concreteMachine.getScMachineRoot().getPORoot().getSequents()) {
			if (poSequent.getElementName().endsWith("INV")) {
				anInvSequent = poSequent;
				break;
			}
		}
		if (anInvSequent != null) {
			for (IPOPredicateSet poPredicateSet : anInvSequent.getHypotheses()) {
				invPOHypotheses.addAll(POUtil.recursivelyCollectAllPredicates(poPredicateSet, concreteMachine.getTypeEnvironment()));
			}
		}
	}
	
	public String annotateAll() throws IOException {
		StringBuilder sb = new StringBuilder();
		for (EventBEvent concreteEvent : concreteMachine.getEvents()) {
			for (EventBRefinedEvent abstractEvent : concreteEvent.getRefinedEvents()) {
				sb.append("\"" + concreteEvent.getLabel() + "\" refines \"" + abstractEvent.getLabel() + "\": ");
				sb.append(annotateEventGuardRefinement(concreteEvent, abstractEvent.getParentEvent()).toString());
				sb.append("\n");
			}
		}
		return sb.toString();
	}

    /**
     * Determine the type of refinement of guards in "concreteEvent refines abstractEvent"
     * @throws IOException 
     * @Param concreteEvent
     * @Param abstractEvent
     */
    private GuardRefinementType annotateEventGuardRefinement(EventBEvent concreteEvent, EventBEvent abstractEvent) throws IOException {
    	String z3Result;
	   	if (! (concreteMachine.getEvents().contains(concreteEvent)) && concreteEvent.getRefinedEvents().contains(abstractEvent) ) {
	 		return GuardRefinementType.NOT_REFINEMENT;
	 	}
	    writeGuardStrengtheningCheckFile(concreteEvent, abstractEvent);
	    z3Result = Z3Util.runZ3(inputFilePath);
	    if (Z3Util.isSAT(z3Result)) {
	    	if (siblingEventsExist(concreteEvent)) {
	    		return GuardRefinementType.DIVIDED;
	    	} else {
	    	    return GuardRefinementType.LIMITED;
	    	}
	    } else if (Z3Util.isUNSAT(z3Result)) {
	    	if (isGuardExactlySame(concreteEvent)) {
	    		return GuardRefinementType.UNCHANGED;
	    	} else {
	    	    return GuardRefinementType.REPLACED;
	    	}
	    } else {
	    	throw new RuntimeException("Unexpected result from Z3");
	    }
    }
    
    private void writeGuardStrengtheningCheckFile(EventBEvent concreteEvent, EventBEvent abstractEvent) throws IOException {
    	final BufferedWriter writer = new BufferedWriter(new FileWriter(inputFilePath));
    	try {
			writeSignature(writer);
			writePredicatesForGuardStrengtheningCheck(writer, concreteEvent, abstractEvent);
		} catch (RodinDBException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		writer.close();
    }
    
	private void writeSignature(BufferedWriter writer) throws RodinDBException, CoreException, IOException {
    	final IPOSequent[] sequents = concreteMachine.getScMachineRoot().getPORoot().getSequents();
    	final ITypeEnvironment typeEnvironment = concreteMachine.getScMachineRoot().getTypeEnvironment();
    	if (sequents.length == 0) {
    		throw new RuntimeException("No POs were found");
    	}
    	final IPOSequent theFirstSequent = sequents[0];
    	ArrayList<Predicate> hypotheses = new ArrayList<Predicate>();
    	for (IPOPredicateSet poPredicateSet : theFirstSequent.getHypotheses()) {
    		hypotheses.addAll(POUtil.recursivelyCollectAllPredicates(poPredicateSet, typeEnvironment));
    	}
    	Predicate goal = null;
		goal = theFirstSequent.getGoals()[0].getPredicate(typeEnvironment);
		final ISimpleSequent simpleSequent = SimpleSequents.make(hypotheses, goal, goal.getFactory());
    	final SMTSignature signature = (new SMTThroughPP()).translate("", simpleSequent).getSMTBenchmark().getSignature();

		// Sorts
		StringBuilder sb = new StringBuilder();
		for (SMTSortSymbol sort : signature.getSorts()) {
			DeclareSortCommand declareSortCommand = new DeclareSortCommand(sort);
			declareSortCommand.toString(sb);
			sb.append(" ");
		}
		writer.write(new String(sb));

		// Functions
		sb = new StringBuilder();
		for (SMTPredicateSymbol pred : signature.getPreds()) {
			DeclareFunCommand declareFunCommand = new DeclareFunCommand(pred);
			declareFunCommand.toString(sb);
			sb.append(" ");
		}
		for (SMTFunctionSymbol fun : signature.getFuns()) {
			DeclareFunCommand declareFunCommand = new DeclareFunCommand(fun);
			declareFunCommand.toString(sb);
			sb.append(" ");
		}
		sb.append("\n");
		writer.write(new String(sb));
    }

	// not GC & A & I & GA
	private void writePredicatesForGuardStrengtheningCheck(BufferedWriter writer, EventBEvent concreteEvent, EventBEvent abstractEvent) throws CoreException {
    	final ITypeEnvironment typeEnvironment = concreteMachine.getScMachineRoot().getTypeEnvironment();
		writeConjunctionPredicate(writer, collectGuards(typeEnvironment, abstractEvent), false);
		writeConjunctionPredicate(writer, collectAxioms(typeEnvironment), false);
		writeConjunctionPredicate(writer, collectInvariants(typeEnvironment), false);
		writeConjunctionPredicate(writer, collectGuards(typeEnvironment, concreteEvent), true);
		try {
			writer.write("(check-sat)\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private HashSet<Predicate> collectGuards(ITypeEnvironment typeEnvironment, EventBEvent event) {
		HashSet<Predicate> result = new HashSet<Predicate>();
		for (EventBGuard guard : event.getGuards()) {
			try {
				result.add(guard.getScElement().getPredicate(typeEnvironment));
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private HashSet<Predicate> collectAxioms(ITypeEnvironment typeEnvironment) {
		HashSet<Predicate> result = new HashSet<Predicate>();
		Stack<ISCContextRoot> scContextStack = new Stack<ISCContextRoot>();
		for (EventBContext context : concreteMachine.getSeenContexts()) {
			scContextStack.push(context.getScContextRoot());
		}
		while (!scContextStack.isEmpty()) {
			ISCContextRoot context = scContextStack.pop();
			try {
				for (ISCAxiom axiom : context.getSCAxioms()) {
					result.add(axiom.getPredicate(typeEnvironment));
				}
				for (ISCExtendsContext scExtendsContext : context.getSCExtendsClauses()) {
					scContextStack.push(scExtendsContext.getAbstractSCContext());
				}
			} catch (RodinDBException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	private HashSet<Predicate> collectInvariants(ITypeEnvironment typeEnvironment) {
		HashSet<Predicate> result = new HashSet<Predicate>();
		Stack<ISCMachineRoot> scMachineStack = new Stack<ISCMachineRoot>();
		scMachineStack.push(concreteMachine.getScMachineRoot());
		while (!scMachineStack.isEmpty()) {
			ISCMachineRoot machine = scMachineStack.pop();
			try {
				for (ISCInvariant invariant : machine.getSCInvariants()) {
					result.add(invariant.getPredicate(typeEnvironment));
				}
				for (ISCRefinesMachine scRefinesMachine : machine.getSCRefinesClauses()) {
					scMachineStack.push((ISCMachineRoot) (scRefinesMachine.getAbstractSCMachine().getRoot()));
				}
			} catch (RodinDBException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	// Translate a predicate using INV PO.
	// INV is suitable because the hypotheses include axioms, invariants, guards, and before-after predicates.
	// An alternative of SMTThroughPP.translate(predicate), which sometimes does not work.
	private String translateWithINV(Predicate predicate) {
		String result;
		final ISimpleSequent sequent = SimpleSequents.make(invPOHypotheses, predicate, invPOHypotheses.get(0).getFactory());
		final SMTBenchmark benchmark = (new SMTThroughPP()).translate("", sequent).getSMTBenchmark();
		if (benchmark != null) {
			result = "(not " + benchmark.getFormula().toString() + ")";
		} else {
			result = "true";
		}
		return result;
	}
	
	private void writeConjunctionPredicate(BufferedWriter writer, HashSet<Predicate> predicates, boolean neg) {
		StringBuilder sb = new StringBuilder();
		sb.append("(assert (and ");
		if (neg) { sb.append("(not "); }
		for (Predicate predicate : predicates) {
			sb.append(translateWithINV(predicate));
			sb.append(" ");
		}
		if (neg) { sb.append(")"); }
		sb.append("))\n");
		try {
			writer.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    private boolean siblingEventsExist(EventBEvent concreteEvent) {
    	final Set<String> abstractEventLabels = new HashSet<String>();
    	for (EventBRefinedEvent e : concreteEvent.getRefinedEvents()) {
    		abstractEventLabels.add(e.getLabel());
    	}
    	for (EventBEvent event : concreteMachine.getEvents()) {
    		final Set<String> aEventLabels = new HashSet<String>();
    		for (EventBRefinedEvent e : event.getRefinedEvents()) {
    			aEventLabels.add(e.getLabel());
    		}
    		if (event != concreteEvent && abstractEventLabels.equals(aEventLabels)) {
    			return true;
    		}
    	}
    	return false;
    }

    // Simple string comparison.
    // TODO: Implement AST level comparison.
    private boolean isGuardExactlySame(EventBEvent concreteEvent) {
    	final Set<String> concreteGuardStrings = new HashSet<String>();
    	for (EventBGuard guard : concreteEvent.getGuards()) {
    		concreteGuardStrings.add(guard.getPredicate());
    	}
    	for (EventBRefinedEvent abstractRefinedEvent : concreteEvent.getRefinedEvents()) {
    		int nAbstractGuardClauses = 0;
    		for (EventBGuard guard : abstractRefinedEvent.getParentEvent().getGuards()) {
    			boolean found = false;
    			nAbstractGuardClauses++;
    			for (String concreteGuardString : concreteGuardStrings) {
    				if (guard.getPredicate().equals(concreteGuardString)) {
    					found = true;
    				}
    			}
    			if (!found) {
    				return false;
    			}
    		}
    		if (concreteGuardStrings.size() != nAbstractGuardClauses) {
    			return false;
    		}
    	}
    	return true;
    }
}
