package eventBRefinementSlicer.ui.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eventBRefinementSlicer.internal.datastructures.EventBContext;

/**
 * 
 * @author Aivar Kripsaar
 *
 */
public class ContextNamingWizardPage extends WizardPage {

	private static String title = "Create Sub-Refinement";
	private static String description = "Enter a name for newly derived context";
	private String labelMessage = "Enter name ";
	private String contextNameInput = "newContext";

	EventBContext originalContext;

	private Text contextInputText;

	public ContextNamingWizardPage(EventBContext originalContext) {
		super(title);

		this.originalContext = originalContext;

		labelMessage += "(original context: " + originalContext.getLabel() + "):";

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

		contextInputText = new Text(composite, getInputTextStyle());
		contextInputText.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		setControl(composite);
		setPageComplete(true);
	}

	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER;
	}

	public String getContextNameInput() {
		if (contextNameInput.endsWith(".bum") || contextNameInput.endsWith(".buc") || contextNameInput.endsWith(".bcm")
				|| contextNameInput.endsWith(".bcc")) {
			int end = contextNameInput.lastIndexOf(".");
			contextNameInput = contextNameInput.substring(0, end);
		}
		return contextNameInput;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			contextInputText.setFocus();
			contextInputText.setText(contextNameInput);
			contextInputText.selectAll();
		}
	}

	public boolean finish() {
		contextNameInput = contextInputText.getText();
		return true;
	}

}
