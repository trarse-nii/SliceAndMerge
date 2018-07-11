package eventBSliceAndMerge.internal.util;

import java.util.ArrayList;


import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IPOPredicate;
import org.eventb.core.IPOPredicateSet;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Predicate;
import org.rodinp.core.RodinDBException;

public class POUtil {
	/**
	 * Collect all the predicates (goal and hypotheses) of the PO (predicates written in a BPR file).
	 * 
	 * @param poPredicateSet
	 * @return List of all predicates in the PO
	 * @throws RodinDBException
	 * @throws CoreException
	 */
	public static ArrayList<Predicate> recursivelyCollectAllPredicates(IPOPredicateSet poPredicateSet, ITypeEnvironment typeEnvironment) {
		ArrayList<Predicate> preds = new ArrayList<Predicate>();
		if (poPredicateSet != null) {
			try {
				for (IPOPredicate poPred : poPredicateSet.getPredicates()) {
					preds.add(poPred.getPredicate(typeEnvironment));
				}
				preds.addAll(recursivelyCollectAllPredicates(poPredicateSet.getParentPredicateSet(), typeEnvironment));
			} catch (RodinDBException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return preds;
	}
}