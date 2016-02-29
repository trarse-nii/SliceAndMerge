package eventBSliceAndMerge.internal.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eventb.core.IContextRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ISCContextRoot;
import org.eventb.core.ISCMachineRoot;
import org.eventb.core.ITraceableElement;
import org.eventb.core.basis.EventBRoot;
import org.eventb.internal.core.sc.MachineStaticChecker;
import org.eventb.internal.core.sc.ContextStaticChecker;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.RodinDBException;;

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
	
	public static ISCMachineRoot makeStaticCheckedMachine(IMachineRoot machineRoot) {
		IInternalElement internalElementRoot = machineRoot.getRoot();
		assert (internalElementRoot instanceof EventBRoot);
		ISCMachineRoot scMachineRoot = ((EventBRoot) internalElementRoot).getSCMachineRoot();
		try {
			new MachineStaticChecker().run(machineRoot.getRodinFile().getResource(), scMachineRoot.getRodinFile().getResource(), new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		assert(scMachineRoot.getRodinFile().getResource().exists());
		return scMachineRoot;
	}

	public static ISCContextRoot makeStaticCheckedContext(IContextRoot contextRoot) {
		IInternalElement internalElementRoot = contextRoot.getRoot();
		assert (internalElementRoot instanceof EventBRoot);
		ISCContextRoot scContextRoot = ((EventBRoot) internalElementRoot).getSCContextRoot();
		try {
			new ContextStaticChecker().run(contextRoot.getRodinFile().getResource(), scContextRoot.getRodinFile().getResource(), new NullProgressMonitor());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		assert(scContextRoot.getRodinFile().getResource().exists());
		return scContextRoot;
	}
}
