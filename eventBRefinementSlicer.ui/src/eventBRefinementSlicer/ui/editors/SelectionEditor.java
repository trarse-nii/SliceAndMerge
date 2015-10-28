package eventBRefinementSlicer.ui.editors;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.IContextRoot;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.ast.FormulaFactory;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.datastructures.EventBAttribute;
import eventBRefinementSlicer.internal.datastructures.EventBCondition;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBInvariant;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;
import eventBRefinementSlicer.internal.datastructures.EventBVariable;
import eventBRefinementSlicer.ui.jobs.EventBDependencyAnalysisJob;
public class SelectionEditor extends EditorPart {

	private String LABEL_TYPE = "Type";
	private String LABEL_CHECKBOX = "";
	private String LABEL_LABEL = "Label";
	private String LABEL_CONTENT = "Content";
	private String LABEL_COMMENT = "Comment";
	
	private SelectionManager selectionManager;

	private IRodinFile rodinFile;
	private IMachineRoot machineRoot;
	private EventBMachine machine;
	
	private Map<EventBElement, Integer> selectionDependencies = new HashMap<>();

	private CheckboxTableViewer attributeCheckboxTableViewer = null;
	private CheckboxTableViewer conditionCheckboxTableViewer = null;
	private ContainerCheckedTreeViewer treeViewer = null;

	public SelectionEditor() {
		// TODO Auto-generated constructor stub
	}
	

	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
		selectionManager = SelectionManager.getInstance();
		rodinFile = getRodinFileFromInput(input);
		IInternalElement internalElementRoot = rodinFile.getRoot();
		assert (internalElementRoot instanceof IContextRoot) || (internalElementRoot instanceof IMachineRoot);
		machineRoot = (IMachineRoot) internalElementRoot;
		try {
			machine = new EventBMachine(machineRoot);
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		EventBDependencyAnalysisJob.doEventBDependencyAnalysis(machine);
	}
	
	protected IRodinFile getRodinFileFromInput(IEditorInput input){
		FileEditorInput editorInput = (FileEditorInput) input;
		IFile inputFile = editorInput.getFile();
		IRodinFile rodinFile = RodinCore.valueOf(inputFile);
		return rodinFile;
	}
	
	public IRodinFile getRodinFile(){
		if(rodinFile == null){
			throw new IllegalStateException("Editor has not been initialized yet");
		}
		return rodinFile;
	}
	
	public FormulaFactory getFormulaFactory(){
		return ((IEventBRoot)machineRoot).getFormulaFactory();
	}

	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	private void createInvariantAndAxiomTable(Composite parent){
		Table table = createTable(parent);
		String [] titles = {
				LABEL_CHECKBOX,
				LABEL_TYPE,
				LABEL_LABEL,
				LABEL_CONTENT,
				LABEL_COMMENT
		};
		
		TableColumn column;
		
		column = new TableColumn(table, SWT.NONE, 0);
		column.setText(titles[0]);
		column.setResizable(false);
		column.setWidth(27);
		
		column = new TableColumn(table, SWT.NONE, 1);
		column.setText(titles[1]);
		
		column = new TableColumn(table, SWT.NONE, 2);
		column.setText(titles[2]);
		
		column = new TableColumn(table, SWT.NONE, 3);
		column.setText(titles[3]);
		
		column = new TableColumn(table, SWT.NONE, 4);
		column.setText(titles[4]);

		createCheckboxTableViewerForInvariantsAndAxioms(table, titles);
		
		for (TableColumn oneColumn : table.getColumns()){
			if (oneColumn.getText().equals(LABEL_CHECKBOX)){
				continue;
			}
			oneColumn.pack();
		}
	}
	
	private void createVariableAndConstantTable(Composite parent){
		Table table = createTable(parent);
		String [] titles = {
			LABEL_CHECKBOX,
			LABEL_TYPE,
			LABEL_LABEL,
			LABEL_COMMENT
		};
		
		TableColumn column;
		
		column = new TableColumn(table, SWT.NONE, 0);
		column.setText(titles[0]);
		column.setResizable(false);
		column.setWidth(27);
		
		column = new TableColumn(table, SWT.NONE, 1);
		column.setText(titles[1]);
		
		column = new TableColumn(table, SWT.NONE, 2);
		column.setText(titles[2]);
		
		column = new TableColumn(table, SWT.NONE, 3);
		column.setText(titles[3]);
		
		createCheckboxTableViewerForVariablesAndConstants(table, titles);
		
		for (TableColumn oneColumn : table.getColumns()){
			if (oneColumn.getText().equals(LABEL_CHECKBOX)){
				continue;
			}
			oneColumn.pack();
		}
	}
	
	private void createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK | SWT.V_SCROLL
				| SWT.H_SCROLL);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		tree.setLayoutData(gridData);

		String[] titles = {
				LABEL_CHECKBOX,
				LABEL_LABEL,
				LABEL_CONTENT,
				LABEL_COMMENT
 };

		TreeColumn column;

		for (String title : titles) {
			column = new TreeColumn(tree, SWT.NONE);
			column.setText(title);
			if (title.equals(LABEL_CHECKBOX)) {
				column.setResizable(false);
				// column.setWidth(27);
			}
		}

		createContainerCheckedTreeViewer(tree, titles);

		for (TreeColumn oneColumn : tree.getColumns()) {
			// if (oneColumn.getText().equals(LABEL_CHECKBOX)) {
			// continue;
			// }
			oneColumn.pack();
		}
	}

	private void createContainerCheckedTreeViewer(Tree tree, String[] titles) {
		ContainerCheckedTreeViewer treeViewer = new ContainerCheckedTreeViewer(tree);
		treeViewer.setColumnProperties(titles);
		treeViewer.setUseHashlookup(true);

		treeViewer.setLabelProvider(new LabelProvider());

		treeViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getElement() instanceof EventBElement) {
					EventBElement element = (EventBElement) event.getElement();
					element.setSelected(event.getChecked());
					treeViewer.update(element, null);
					// TODO: Add dependency stuff.
				}
			}
		});

		treeViewer.setContentProvider(new ITreeContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// TODO Auto-generated method stub

			}

			@Override
			public void dispose() {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean hasChildren(Object element) {
				if (element instanceof EventBMachine) {
					EventBMachine machine = (EventBMachine) element;
					if (machine.getInvariants().isEmpty() && machine.getVariables().isEmpty()) {
						return false;
					}
					return true;
				}
				return false;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof EventBElement) {
					return ((EventBElement) element).getParent();
				}
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				return getChildren(inputElement);
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if ((parentElement instanceof EventBMachine)) {
					EventBMachine machine = (EventBMachine) parentElement;
					List<EventBElement> children = new ArrayList<>();
					children.addAll(machine.getInvariants());
					children.addAll(machine.getVariables());
					return children.toArray();
				}
				return null;
			}
		});

		treeViewer.setInput(machine);
	}

	private Table createTable(Composite parent){
		Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.getVerticalBar().setVisible(true);
		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, true);
		table.setLayoutData(gridData);
		
		return table;
	}
	
	private void createCheckboxTableViewerForInvariantsAndAxioms(Table table, String[] columnNames){
		conditionCheckboxTableViewer = createCheckboxTableViewer(table, columnNames);
		
		conditionCheckboxTableViewer.setContentProvider(new IStructuredContentProvider(){

			@Override
			public void dispose() {
				// Nothing to dispose of
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if(oldInput != null){
					((EventBMachine)oldInput).removeChangeListener((CheckboxTableViewer)viewer);
				}
				if (newInput != null){
					((EventBMachine)newInput).addChangeListener((CheckboxTableViewer)viewer);
				}
				
			}

			@Override
			public Object[] getElements(Object inputElement) {
				EventBMachine machine = (EventBMachine) inputElement;
				List<EventBCondition> conditions = new ArrayList<>();
				conditions.addAll(machine.getInvariants());
				for(EventBContext context : machine.getSeenContexts()){
					conditions.addAll(context.getAxioms());
				}
				return conditions.toArray();
			}
			
		});

		conditionCheckboxTableViewer.setInput(machine);
	}
	
	private void createCheckboxTableViewerForVariablesAndConstants(Table table, String[] columnNames){
		attributeCheckboxTableViewer = createCheckboxTableViewer(table, columnNames);
		
		attributeCheckboxTableViewer.setContentProvider(new IStructuredContentProvider(){

			@Override
			public void dispose() {
				// Nothing to dispose of
			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
				if(oldInput != null){
					((EventBMachine)oldInput).removeChangeListener((CheckboxTableViewer)viewer);
				}
				if (newInput != null){
					((EventBMachine)newInput).addChangeListener((CheckboxTableViewer)viewer);
				}
				
			}

			@Override
			public Object[] getElements(Object inputElement) {
				EventBMachine machine = (EventBMachine) inputElement;
				List<EventBAttribute> attributes = new ArrayList<>();
				attributes.addAll(machine.getVariables());
				for (EventBContext context : machine.getSeenContexts()){
					attributes.addAll(context.getConstants());
				}
				
				return attributes.toArray();
			}
			
		});
		

		attributeCheckboxTableViewer.setInput(machine);
		
	}
	
	private CheckboxTableViewer createCheckboxTableViewer(Table table, String[] columnNames){
		CheckboxTableViewer checkboxTableViewer = new CheckboxTableViewer(table);
		checkboxTableViewer.setColumnProperties(columnNames);
		checkboxTableViewer.setUseHashlookup(true);
		
		
		checkboxTableViewer.setLabelProvider(new LabelProvider());
		
		checkboxTableViewer.addCheckStateListener(new ICheckStateListener() {
			
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				EventBElement element = (EventBElement) event.getElement();
				element.setSelected(event.getChecked());
				selectionManager.setSelection(element, event.getChecked());
				checkboxTableViewer.update(event.getElement(), null);
				EventBDependencies dependencies = element.getParent().getDependencies();
				for (EventBElement dependee : dependencies.getDependeesForElement(element)) {
					if(event.getChecked()){
						if (!selectionDependencies.containsKey(dependee)){
							selectionDependencies.put(dependee, 0);
						}
						selectionDependencies.put(dependee, selectionDependencies.get(dependee) + 1);
					} else {
						if (selectionDependencies.containsKey(dependee)) {
							selectionDependencies.put(dependee, selectionDependencies.get(dependee) - 1);
							if (selectionDependencies.get(dependee).intValue() <= 0) {
								selectionDependencies.remove(dependee);
							}
						}
					}
					if (dependee instanceof EventBAttribute) {
						attributeCheckboxTableViewer.update(dependee, null);
					} else {
						conditionCheckboxTableViewer.update(dependee, null);
					}
				}
				for (EventBElement depender : dependencies.getDependersForElement(element)) {
					if (event.getChecked()) {
						if (!selectionDependencies.containsKey(depender)) {
							selectionDependencies.put(depender, 0);
						}
						selectionDependencies.put(depender, selectionDependencies.get(depender) + 1);
					} else {
						if (selectionDependencies.containsKey(depender)) {
							selectionDependencies.put(depender, selectionDependencies.get(depender) - 1);
							if (selectionDependencies.get(depender).intValue() <= 0) {
								selectionDependencies.remove(depender);
							}
						}
					}
					if (depender instanceof EventBAttribute) {
						attributeCheckboxTableViewer.update(depender, null);
					} else {
						conditionCheckboxTableViewer.update(depender, null);
					}
				}
			}
		});
		
		return checkboxTableViewer;
	} 
	
	class LabelProvider implements ITableLabelProvider, ITableColorProvider, ITableFontProvider {

		@Override
		public void addListener(ILabelProviderListener listener) {
			// Intentionally left empty
		}

		@Override
		public void dispose() {
			// Intentionally left empty
		}

		@Override
		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener listener) {
			// Intentionally left empty
		}

		@Override
		public Font getFont(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof EventBCondition){
				switch (columnIndex) {
				case 0: // Selection Column
					break;
				case 1:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
				case 2:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN);
				case 3:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA);
				case 4:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
				default:
					break;
				}
			}
			if (element instanceof EventBAttribute){
				switch (columnIndex) {
				case 0: // Selection Column
					break;
				case 1:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
				case 2:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN);
				case 3:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
				default:
					break;
				}
			}

			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			EventBElement eventBElement = (EventBElement) element;
			if (eventBElement.isSelected()){
				return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			}
			if (selectionDependencies.containsKey(eventBElement)) {
				return Display.getDefault().getSystemColor(SWT.COLOR_RED);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (!(element instanceof EventBCondition || element instanceof EventBAttribute)){
				return null;
			} else if (element instanceof EventBCondition){
				EventBCondition condition = (EventBCondition) element;
				switch (columnIndex) {
				case 0: // Selection Column
					return null;
				case 1:
					if (element instanceof EventBInvariant){
						return "Invariant";
					} else {
						return "Axiom";
					}
				case 2:
					return condition.getLabel();
				case 3:
					return condition.getPredicate();
				case 4:
					return condition.getComment();
				default:
					return null;
				}
			} else {
				EventBAttribute attribute = (EventBAttribute) element;
				switch (columnIndex) {
				case 0: // Selection Column
					return null;
				case 1:
					if (attribute instanceof EventBVariable){
						return "Variable";
					} else {
						return "Constant";
					}
				case 2:
					return attribute.getLabel();
				case 3:
					return attribute.getComment();
				default:
					return null;
				}
			}
		}
		
	}
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		parent.setLayout(layout);
		createTree(parent);
		new Label(parent, SWT.NONE).setText("Invariants");
		createInvariantAndAxiomTable(parent);
		new Label(parent, SWT.NONE).setText("Variables");
		createVariableAndConstantTable(parent);
		
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
