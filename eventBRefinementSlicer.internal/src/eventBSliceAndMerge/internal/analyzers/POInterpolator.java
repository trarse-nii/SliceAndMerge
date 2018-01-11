package eventBSliceAndMerge.internal.analyzers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eventb.core.IPOPredicate;
import org.eventb.core.IPOPredicateSet;
import org.eventb.core.IPOSequent;
import org.eventb.core.ast.ITypeEnvironment;
import org.eventb.core.ast.Predicate;
import org.eventb.core.seqprover.transformer.ISimpleSequent;
import org.eventb.core.seqprover.transformer.SimpleSequents;
import org.eventb.smt.core.internal.ast.SMTBenchmark;
import org.eventb.smt.core.internal.ast.SMTFormula;
import org.eventb.smt.core.internal.ast.SMTSignature;
import org.eventb.smt.core.internal.ast.commands.DeclareFunCommand;
import org.eventb.smt.core.internal.ast.commands.DeclareSortCommand;
import org.eventb.smt.core.internal.ast.symbols.SMTFunctionSymbol;
import org.eventb.smt.core.internal.ast.symbols.SMTPredicateSymbol;
import org.eventb.smt.core.internal.ast.symbols.SMTSortSymbol;
import org.eventb.smt.core.internal.translation.SMTThroughPP;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.datastructures.EventBMachine;
import eventBSliceAndMerge.internal.datastructures.EventBVariable;

@SuppressWarnings("restriction")
public class POInterpolator {
	private EventBMachine machine;
	private ITypeEnvironment typeEnvironment;
	private EventBSliceSelection selection;
	
	private String inputFilePath = "/tmp/z3-input.scm";
	private String outputFilePath = "/tmp/z3-output.scm";
	private String z3Path = "/usr/local/bin/z3";
	private String goshPath = "/usr/bin/gosh";
	private String identifierScmPath = "/usr/local/lib/refinement-interpolator/identifiers.scm";
	private String smtToEventBScmPath = "/usr/local/lib/refinement-interpolator/smt-to-event-b.scm";

	public POInterpolator(EventBMachine machine,
			ITypeEnvironment typeEnvironment, EventBSliceSelection selection) {
		this.machine = machine;
		this.typeEnvironment = typeEnvironment;
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

	/**
	 * Create a file of an SMT-formatted PO.
	 * 
	 * @param poName
	 *            Name of the PO
	 * @throws RodinDBException
	 * @throws CoreException
	 * @throws IOException
	 */
	public void createSMTInputFile(String poName) throws RodinDBException, CoreException, IOException {
		IPOSequent sequent = machine.getScMachineRoot().getPORoot().getSequent(poName);
		IPOPredicate[] goals = sequent.getGoals();
		assert(goals.length == 1);
		Predicate goal = goals[0].getPredicate(typeEnvironment);

		System.out.println("Creating input file for " + poName);
		ArrayList<Predicate> hypotheses = new ArrayList<Predicate>();
		try {
			for (IPOPredicateSet poPredicateSet : sequent.getHypotheses()) {
				hypotheses.addAll(recursivelyCollectAllPredicates(poPredicateSet));
			}
		} catch (RodinDBException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		final ISimpleSequent simpleSequent = SimpleSequents.make(hypotheses, goal, goal.getFactory());
		final SMTBenchmark benchmark = (new SMTThroughPP()).translate(poName, simpleSequent).getSMTBenchmark();
		final SMTSignature signature = benchmark.getSignature();
		final BufferedWriter writer = new BufferedWriter(new FileWriter(inputFilePath));;
		
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
		}
		writer.write(new String(sb));
		
		ArrayList<SMTFormula> antecedentFormulas = new ArrayList<>();
		ArrayList<SMTFormula> succedentFormulas = new ArrayList<>();

		// Goal
		if (shouldBeInAntecedent(benchmark.getFormula(), concreteIdentifiers())) {
			antecedentFormulas.add(benchmark.getFormula());
		} else {
			succedentFormulas.add(benchmark.getFormula());
		}

		// Hypotheses
		for (SMTFormula assumption : benchmark.getAssumptions()) {
			if (shouldBeInAntecedent(assumption, concreteIdentifiers())) {
				antecedentFormulas.add(assumption);
			} else {
				succedentFormulas.add(assumption);
			}
		}

		writer.write("(compute-interpolant (and ");
		for (SMTFormula antecedentFormula : antecedentFormulas) {
			writer.write(antecedentFormula.toString());
		}
		writer.write(") (and ");
		for (SMTFormula succedentFormula : succedentFormulas) {
			writer.write(succedentFormula.toString());
		}
		writer.write("))");
		
		writer.close();
	} 
	
	/**
	 * Create a file of the SMT-formatted interpolant (the output of Z3).
	 * 
	 * @param output
	 *            The output (on standard output) from Z3
	 * @throws IOException
	 */
	public void createSMTOutputFile (String output) throws IOException {
		final BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
		
		writer.write(output);
		
		writer.close();
	}
	
	/**
	 * Collect all the predicates (goal and hypotheses) of the PO (predicates written in a BPR file).
	 * 
	 * @param poPredicateSet
	 * @return List of all predicates in the PO
	 * @throws RodinDBException
	 * @throws CoreException
	 */
	private ArrayList<Predicate> recursivelyCollectAllPredicates(IPOPredicateSet poPredicateSet) throws RodinDBException, CoreException {
		ArrayList<Predicate> preds = new ArrayList<Predicate>();
		if (poPredicateSet != null) {
			for (IPOPredicate poPred : poPredicateSet.getPredicates()) {
				preds.add(poPred.getPredicate(typeEnvironment));
			}
			preds.addAll(recursivelyCollectAllPredicates(poPredicateSet.getParentPredicateSet()));
		}
		return preds;
	}

	/**
	 * Returns whether a formula should be in the antecedent of the sequent passed to Z3.
	 * (false means the formula should be in the succedent)
	 * TODO: Handle not only VCC but also VAA 
	 * 
	 * @param formula
	 * @param vccs
	 * 			Concrete variables
	 */
	private boolean shouldBeInAntecedent(SMTFormula formula, ArrayList<EventBVariable> vccs) {
		boolean result = false;
		
		try {
			for (String freeIdentifierLabel : freeIdentifiers(formula)) {
				for (EventBVariable vcc : vccs) {
					if (freeIdentifierLabel.equals(vcc.getLabel())) {
						result = true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}
	
	/**
	 * TODO: Implement in Java
	 * 
	 * @param formula
	 * @return The list of free identifiers in the formula
	 * @throws IOException
	 */
	private ArrayList<String> freeIdentifiers(SMTFormula formula) throws IOException {
		ArrayList<String> result = new ArrayList<>();
		String[] cmd = {
			goshPath,
			identifierScmPath,
			formula.toString()
		};
		Process pr = Runtime.getRuntime().exec(cmd);
		BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			result.add(line);
		}
		in.close();
		return result;
	}
	
	/**
	 * Runs Z3 with the converted PO as the input
	 * @return The output from Z3
	 * @throws IOException
	 */
	public String runZ3() throws IOException {
		String result = "";
		Process pr = Runtime.getRuntime().exec(z3Path + " -smt2 " + inputFilePath);
		BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			result = result + "\n" + line;
		}
		in.close();
		return result;
	}
	
	/**
	 * TODO: Implement in Java
	 * 
	 * @return The interpolant in Event-B notation.
	 * @throws IOException
	 */
	public String eventBInterpolant() throws IOException {
		String result = "";
		String[] cmd = {
			goshPath,
			smtToEventBScmPath,
			inputFilePath,
			outputFilePath
		};
		Process pr = Runtime.getRuntime().exec(cmd);
		BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line;
		while ((line = in.readLine()) != null) {
			result = result + "\n" + line;
		}
		in.close();
		return result;
	}
}
