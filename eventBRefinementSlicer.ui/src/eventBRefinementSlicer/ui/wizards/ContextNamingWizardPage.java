package eventBRefinementSlicer.ui.wizards;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eventBRefinementSlicer.internal.datastructures.EventBAxiom;
import eventBRefinementSlicer.internal.datastructures.EventBCarrierSet;
import eventBRefinementSlicer.internal.datastructures.EventBConstant;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBTypes;
import eventBRefinementSlicer.ui.editors.SelectionEditor.EventBTreeElement;

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

	private EventBContext originalContext;
	private Object[] selectedElements;

	private Text contextInputText;

	private TableViewer previewTableViewer;

	public ContextNamingWizardPage(EventBContext originalContext, Object[] selectedElements) {
		super(title);

		this.originalContext = originalContext;
		this.selectedElements = selectedElements;

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

		label = new Label(composite, SWT.WRAP);
		label.setText("Preview:");
		layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		layoutData.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
		label.setLayoutData(layoutData);
		label.setFont(parent.getFont());

		EventBContext newContext = generateNewContext();

		PreviewTableViewerFactory previewTableFactory = PreviewTableViewerFactory.getInstance();
		previewTableViewer = previewTableFactory.createTableViewer(composite, newContext);

		setControl(composite);
		setPageComplete(true);
	}

	protected int getInputTextStyle() {
		return SWT.SINGLE | SWT.BORDER;
	}

	private EventBContext generateNewContext() {

		EventBContext newContext = new EventBContext();
		for (Object object : selectedElements) {
			if (!(object instanceof EventBTreeElement)) {
				continue;
			}
			EventBElement element = ((EventBTreeElement) object).getOriginalElement();
			if (!originalContext.containsElement(element)) {
				continue;
			}
			switch (element.getType()) {
			case EventBTypes.CARRIER_SET:
				newContext.addCarrierSet((EventBCarrierSet) element);
				break;
			case EventBTypes.AXIOM:
				newContext.addAxiom((EventBAxiom) element);
				break;
			case EventBTypes.CONSTANT:
				newContext.addConstant((EventBConstant) element);
				break;
			default:
				// Intentionally left empty.
				break;
			}
		}

		return newContext;
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
