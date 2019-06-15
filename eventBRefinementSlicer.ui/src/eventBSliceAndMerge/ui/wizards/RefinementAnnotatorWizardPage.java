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
 * Wizard page used for annotating event refinement. 
 * 
 * @author Tsutomu Kobayashi
 *
 */

public class RefinementAnnotatorWizardPage extends WizardPage {

	private String labelMessage = "Annotate event refinement";
	private String runButtonLabel = "run";

	private Button runButton;
	private Text outputText;

	public RefinementAnnotatorWizardPage(String title, String description) {
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

		runButton = new Button(composite, SWT.PUSH);
		runButton.setText(runButtonLabel);
		runButton.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		runButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RefinementAnnotatorWizard wizard = (RefinementAnnotatorWizard) getWizard();
				wizard.analyze();
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

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			runButton.setFocus();
		}
	}

	public boolean finish() {
		return true;
	}
	
	public void setResult(String result) {
		outputText.setText(result);
	}
}
