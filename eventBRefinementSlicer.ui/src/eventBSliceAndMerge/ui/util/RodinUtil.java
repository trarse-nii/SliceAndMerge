package eventBSliceAndMerge.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesMachine;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

/**
 * Utility class to manipulate on Event-B files using the Rodin platform
 * 
 * @author Fuyuki Ishikawa
 *
 */
public class RodinUtil {

	/**
	 * Gets the internal representation of the Rodin File from the editor input
	 * 
	 * @param input
	 *            The editor's input
	 * @return Internal representation of the Rodin File
	 */
	public static IRodinFile getRodinFileFromInput(IEditorInput input) {
		FileEditorInput editorInput = (FileEditorInput) input;
		IFile inputFile = editorInput.getFile();
		IRodinFile rodinFile = RodinCore.valueOf(inputFile);
		return rodinFile;
	}

	/**
	 * Get the preceding machine (the machine refined by the specified machine)
	 * 
	 * @param machineRoot
	 * @return
	 */
	public static IMachineRoot getPrecedingMachineRoot(IMachineRoot machineRoot) {
		// There's only one refines clause per machine by definition. We can't
		// access it directly because Rodin is silly like that.
		IMachineRoot ret = null;
		try {
			for (IRefinesMachine refinesMachine : machineRoot.getRefinesClauses()) {
				ret = refinesMachine.getAbstractMachineRoot();
			}
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		return ret;
	}

}
