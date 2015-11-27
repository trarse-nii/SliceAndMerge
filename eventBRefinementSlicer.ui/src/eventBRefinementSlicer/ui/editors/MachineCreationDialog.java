package eventBRefinementSlicer.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class MachineCreationDialog extends Dialog {

	private String title = "Create Sub-Refinement";
	private String labelMessage = "Enter a name for the new derived sub-refinement machine";
	private String machineNameInput = "newMachine";

	private List<String> contextNameInputs = new ArrayList<>();

	private Text machineInputText;
	private Button okButton;

	public MachineCreationDialog(Shell parentShell) {
		super(parentShell);
		// TODO Auto-generated constructor stub
	}

	public MachineCreationDialog(IShellProvider parentShell) {
		super(parentShell);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.OK_ID) {
			machineNameInput = machineInputText.getText();
		}
		super.buttonPressed(buttonId);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		machineInputText.setFocus();
		machineInputText.setText(machineNameInput);
		machineInputText.selectAll();

	};

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(labelMessage);
		GridData layoutData = new GridData(GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL | GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		layoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(layoutData);
		label.setFont(parent.getFont());

		machineInputText = new Text(composite, getInputTextStyle());
		machineInputText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		applyDialogFont(composite);

		return composite;

	}

	// Swiped from JFace's InputDialog
	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER;
	}

	public String getMachineNameInput() {
		if (machineNameInput.endsWith(".bum") || machineNameInput.endsWith(".buc") || machineNameInput.endsWith(".bcm")
				|| machineNameInput.endsWith(".bcc")) {
			int end = machineNameInput.lastIndexOf(".");
			machineNameInput = machineNameInput.substring(0, end);
		}
		return machineNameInput;
	}

}
