package eventBSliceAndMerge.internal.util;

import java.util.List;
import java.util.ArrayList;

import org.eventb.core.IAction;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IExtendsContext;
import org.eventb.core.ISeesContext;
import org.eventb.core.ast.Assignment;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.ast.FreeIdentifier;
import org.eventb.internal.core.lexer.Scanner;
import org.eventb.internal.core.parser.GenParser;
import org.eventb.internal.core.parser.ParseResult;
import org.rodinp.core.RodinDBException;

@SuppressWarnings("restriction")
public class MachineUtil {
	/*
	 * Returns whether context1 extends* context0
	 * */
	public static boolean isContextExtendedAs(ISeesContext context0, ISeesContext context1) {
		boolean result = false;
		List<IExtendsContext> extended = new ArrayList<>();
		try {
			extended = extendedContexts(context1.getSeenContextRoot());
			for (IExtendsContext context : extended) {
				if (context.getAbstractContextName().equals(context0.getSeenContextName())) {
					result = true;
					break;
				}
			}
		} catch (RodinDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	private static List<IExtendsContext> extendedContexts(IContextRoot context) {
		List<IExtendsContext> result = new ArrayList<IExtendsContext>();
		List<IContextRoot> directlyExtendedContexts = new ArrayList<IContextRoot>();
		try {
			for (IExtendsContext c : context.getExtendsClauses()) {
				result.add(c);
				directlyExtendedContexts.add(c.getAbstractContextRoot());
			}
		} catch (RodinDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (! directlyExtendedContexts.isEmpty()) {
			for (IContextRoot c : directlyExtendedContexts) {
				result.addAll(extendedContexts(c));
			}
		}
		return result;
	}

	public static List<FreeIdentifier> assignedIdentifiers(IAction action) {
		List<FreeIdentifier> result = new ArrayList<FreeIdentifier>();
		FormulaFactory ff = ((IEventBRoot) action.getRoot()).getFormulaFactory();
		ParseResult parseResult = new ParseResult(ff, action);
		Scanner scanner = null;
		try {
			scanner = new Scanner(action.getAssignmentString(), parseResult, ff.getGrammar());
		} catch (RodinDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assert(scanner != null);
		GenParser parser = new GenParser(Assignment.class, scanner, parseResult, false);
		parser.parse();
		FreeIdentifier[] fis = parser.getResult().getParsedAssignment().getAssignedIdentifiers();
		for (FreeIdentifier fi : fis) {
			result.add(fi);
		}
		return result;
	}
}
