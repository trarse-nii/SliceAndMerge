package eventBSliceAndMerge.internal.util;

import java.util.List;
import java.util.ArrayList;

import org.eventb.core.IContextRoot;
import org.eventb.core.IExtendsContext;
import org.eventb.core.ISeesContext;
import org.rodinp.core.RodinDBException;

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
}
