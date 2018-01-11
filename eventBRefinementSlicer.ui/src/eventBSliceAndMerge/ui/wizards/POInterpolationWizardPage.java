package eventBSliceAndMerge.ui.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;

/**
 * 
 * Wizard page used for getting the name of the target PO.
 * 
 * @author Tsutomu Kobayashi
 *
 */

public class POInterpolationWizardPage extends WizardPage {

	private String labelMessage = "Enter PO name:";
	private String poNameInput = "evt/inv1/INV";
	private String generateButtonLabel = "Generate";

	private Text poNameInputText;
	private Button generateButton;
	private Text outputText;

	public POInterpolationWizardPage(String title, String description) {
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

		poNameInputText = new Text(composite, getInputTextStyle());
		poNameInputText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		
		generateButton = new Button(composite, SWT.PUSH);
		generateButton.setText(generateButtonLabel);
		generateButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		generateButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				POInterpolationWizard wizard = (POInterpolationWizard) getWizard();
				wizard.generate(poNameInputText.getText());
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
		
		outputText = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		outputText.setLayoutData(new GridData(GridData.FILL_BOTH));

		setControl(composite);
		setPageComplete(true);
	}

	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER;
	}

	/**
	 * Getter of page result. Also removes superfluous extension if provided by the user.
	 * 
	 * @return Name of the target PO
	 */
	public String getPONameInput() {
		return poNameInput;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			poNameInputText.setFocus();
			poNameInputText.setText(poNameInput);
			poNameInputText.selectAll();
		}
	}

	public boolean finish() {
		poNameInput = poNameInputText.getText();
		return true;
	}
	
	public void setResult(String result) {
		outputText.setText(result);
	}
}
