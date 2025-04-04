package eventBSliceAndMerge.internal.analyzers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IPOSequent;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.core.ast.Predicate;
import org.eventb.core.seqprover.transformer.ISimpleSequent;
import org.eventb.core.seqprover.transformer.SimpleSequents;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.datastructures.EventBMachine;
import eventBSliceAndMerge.internal.datastructures.EventBVariable;
import jp.ac.nii.tk_eventb_util.ast.PredicateUtil;
import jp.ac.nii.tk_eventb_util.po.INVPO;
import jp.ac.nii.tk_eventb_util.po.POUtil;
import jp.ac.nii.tk_eventb_util.z3.Z3Util;

public class POInterpolator {
	private EventBMachine machine;
	private EventBSliceSelection selection;

	public POInterpolator(EventBMachine machine, EventBSliceSelection selection) {
		this.machine = machine;
		this.selection = selection;
	}

	/**
	 * Returns variables introduced in concrete model using information of selection.
	 *
	 * @throws RodinDBException
	 * @return List of concrete variables
	 */
	private ArrayList<EventBVariable> concreteIdentifiers() throws RodinDBException {
		ArrayList<EventBVariable> result = new ArrayList<>();
		for (EventBVariable vc : machine.getVariables()) {
			boolean isInVB = false;
			for (EventBVariable vb : selection.variables) {
				if (vb.getLabel().equals(vc.getLabel())) {
					isInVB = true;
				}
			}
			if (!isInVB) {
				result.add(vc);
			}
		}
		return result;
	}

	public String complementaryPredString(String poName) throws RodinDBException, CoreException, FileNotFoundException, IOException {
		final IPOSequent poSequent = machine.getScMachineRoot().getPORoot().getSequent(poName);
		final INVPO invPO = (INVPO) POUtil.simplifiedPO(poSequent);
		FormulaFactory ff = invPO.getTypeEnvironment().getFormulaFactory();

		ArrayList<Predicate> antecedentPredicates = new ArrayList<Predicate>();
		ArrayList<Predicate> succedentPredicates = new ArrayList<Predicate>();
		Predicate succedentPredicate; // disjunction of succedentPredicates

		for (Predicate hypPredicate : invPO.getHypotheses()) {
			if (shouldBeInAntecedent(hypPredicate)) {
				antecedentPredicates.add(hypPredicate);
			} else {
				succedentPredicates.add(PredicateUtil.negation(hypPredicate, ff));
			}
		}
		for (Predicate goalPredicate : invPO.getGoals()) {
		    if (shouldBeInAntecedent(goalPredicate)) {
		        antecedentPredicates.add(PredicateUtil.negation(goalPredicate, ff));
		    } else {
		        succedentPredicates.add(goalPredicate);
		    }
		}
		succedentPredicate = PredicateUtil.disjunction(succedentPredicates, ff);

        ISimpleSequent interpSequent = SimpleSequents.make(antecedentPredicates, succedentPredicate, ff);
        return Z3Util.smt2StringOfInterpolant(interpSequent);
	}

	/**
	 * Returns whether a formula should be in the antecedent of the sequent passed to Z3.
	 * (false means the formula should be in the succedent)
	 * TODO: Handle not only VCC but also VAA
	 *
	 * @param predicate
	 * @param vccs
	 * 			Concrete variables
	 */
	private boolean shouldBeInAntecedent(Predicate predicate) {
		boolean result = false;
		ArrayList<EventBVariable> vccs = new ArrayList<EventBVariable>();

		try {
			vccs = concreteIdentifiers();
		} catch (RodinDBException e) {
			e.printStackTrace();
		}

		for (FreeIdentifier freeIdentifier: predicate.getFreeIdentifiers()) {
			for (EventBVariable vcc : vccs) {
				if (freeIdentifier.getName().equals(vcc.getLabel())) {
					result = true;
				}
			}
		}

		return result;
	}
}
