package eventBRefinementSlicer.ui.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * 
 * @author Aivar Kripsaar
 *
 */

public class MachineNamingWizardPage extends WizardPage {

	private static String title = "Create Sub-Refinement";
	private static String description = "Enter a name for the new derived sub-refinement machine";
	private String labelMessage = "Enter name:";
	private String machineNameInput = "newMachine";

	private Text machineInputText;

	public MachineNamingWizardPage() {
		super(title);
		setTitle(title);
		setDescription(description);

	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		Label label = new Label(composite, SWT.WRAP);
		label.setText(labelMessage);
		GridData layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		layoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(layoutData);
		label.setFont(parent.getFont());

		machineInputText = new Text(composite, getInputTextStyle());
		machineInputText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		setControl(composite);
		setPageComplete(true);

	}

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

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			machineInputText.setFocus();
			machineInputText.setText(machineNameInput);
			machineInputText.selectAll();
		}
	}

	public boolean finish() {
		machineNameInput = machineInputText.getText();
		return true;
	}

}
