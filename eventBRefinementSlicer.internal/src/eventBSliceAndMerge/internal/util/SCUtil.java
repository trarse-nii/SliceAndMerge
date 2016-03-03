package eventBSliceAndMerge.internal.util;

import org.eventb.core.ITraceableElement;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinDBException;

public class SCUtil {
	public static ITraceableElement findSCElement(IInternalElement element, ITraceableElement[] scElements) {
		try {
			for (ITraceableElement scElement : scElements) {
				if (scElement.getSource().getHandleIdentifier().equals(element.getHandleIdentifier())) {
					return scElement;
				}
			}
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		assert false;
		return null;
	}

}
