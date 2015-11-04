package eventBRefinementSlicer.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
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
import eventBRefinementSlicer.internal.datastructures.EventBAxiom;
import eventBRefinementSlicer.internal.datastructures.EventBCondition;
import eventBRefinementSlicer.internal.datastructures.EventBConstant;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;
import eventBRefinementSlicer.internal.datastructures.EventBUnit;
import eventBRefinementSlicer.ui.jobs.EventBDependencyAnalysisJob;

/**
 * The editor in charge of selecting which parts of an EventB machine to use in
 * the slicing of refinements
 * 
 * @author Aivar Kripsaar
 *
 */

public class SelectionEditor extends EditorPart {

	private String LABEL_CHECKBOX = "";
	private String LABEL_LABEL = "Label";
	private String LABEL_CONTENT = "Content";
	private String LABEL_COMMENT = "Comment";

	private IRodinFile rodinFile;
	private IMachineRoot machineRoot;
	private EventBMachine machine;

	private EventBTreeSubcategory[] treeCategories;

	private Map<EventBElement, Integer> selectionDependencies = new HashMap<>();

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
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
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

	protected IRodinFile getRodinFileFromInput(IEditorInput input) {
		FileEditorInput editorInput = (FileEditorInput) input;
		IFile inputFile = editorInput.getFile();
		IRodinFile rodinFile = RodinCore.valueOf(inputFile);
		return rodinFile;
	}

	public IRodinFile getRodinFile() {
		if (rodinFile == null) {
			throw new IllegalStateException("Editor has not been initialized yet");
		}
		return rodinFile;
	}

	public FormulaFactory getFormulaFactory() {
		return ((IEventBRoot) machineRoot).getFormulaFactory();
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

	private void createTree(Composite parent) {
		Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tree.setLayoutData(gridData);

		String[] titles = { LABEL_CHECKBOX, LABEL_LABEL, LABEL_CONTENT, LABEL_COMMENT };

		TreeColumn column;

		for (String title : titles) {
			column = new TreeColumn(tree, SWT.NONE);
			column.setText(title);
			if (title.equals(LABEL_CHECKBOX)) {
				// column.setResizable(false);
				// column.setWidth(27);
			}
		}

		createContainerCheckedTreeViewer(tree, titles);

		for (TreeColumn oneColumn : tree.getColumns()) {
			oneColumn.pack();
		}
	}

	private void createContainerCheckedTreeViewer(Tree tree, String[] titles) {
		ContainerCheckedTreeViewer treeViewer = new ContainerCheckedTreeViewer(tree);
		treeViewer.setColumnProperties(titles);
		treeViewer.setUseHashlookup(true);

		treeViewer.setLabelProvider(new LabelProvider());

		treeViewer.addTreeListener(new ITreeViewerListener() {

			@Override
			public void treeExpanded(TreeExpansionEvent event) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						for (TreeColumn column : tree.getColumns()) {
							column.pack();
						}
					}
				});

			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						for (TreeColumn column : tree.getColumns()) {
							column.pack();
						}
					}
				});
			}
		});

		treeViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				treeViewer.update(event.getElement(), null);
				if (event.getElement() instanceof EventBTreeSubcategory) {
					return;
				}
				// TODO: Maybe put this in its own method
				EventBDependencies dependencies = machine.getDependencies();
				handleSelectionDependencies(dependencies, event);
			}

			private void handleSelectionDependencies(EventBDependencies dependencies, CheckStateChangedEvent event) {
				assert event.getElement() instanceof EventBTreeElement;
				EventBElement element = ((EventBTreeElement) event.getElement()).getOriginalElement();
				Set<EventBElement> dependees = dependencies.getDependeesForElement(element);
				Set<EventBElement> dependers = dependencies.getDependersForElement(element);
				handleSingleDependencyDirection(dependees, event);
				handleSingleDependencyDirection(dependers, event);
			}

			private void handleSingleDependencyDirection(Set<EventBElement> dependencySet, CheckStateChangedEvent event) {
				for (EventBElement dependency : dependencySet) {
					if (event.getChecked()) {
						if (!selectionDependencies.containsKey(dependency)) {
							selectionDependencies.put(dependency, 0);
						}
						selectionDependencies.put(dependency, selectionDependencies.get(dependency) + 1);
					} else {
						if (selectionDependencies.containsKey(dependency)) {
							selectionDependencies.put(dependency, selectionDependencies.get(dependency) - 1);
							if (selectionDependencies.get(dependency).intValue() <= 0) {
								selectionDependencies.remove(dependency);
							}
						}
					}
					for (Object category : treeCategories) {
						EventBTreeSubcategory treeCategory = (EventBTreeSubcategory) category;
						EventBTreeElement treeElement = treeCategory.findTreeElement(dependency);
						if (treeElement != null) {
							treeViewer.update(treeElement, null);
							break;
						}
					}
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
				if (element instanceof EventBTreeSubcategory) {
					return ((EventBTreeSubcategory) element).getChildren().length > 0;
				}
				return false;
			}

			@Override
			public Object getParent(Object element) {
				if (element instanceof EventBTreeElement) {
					return ((EventBTreeElement) element).getParent();
				}
				return null;
			}

			@Override
			public Object[] getElements(Object inputElement) {
				EventBMachine machine = (EventBMachine) inputElement;
				EventBTreeSubcategory invariants = new EventBTreeSubcategory("Invariants", machine, machine.getInvariants());
				EventBTreeSubcategory variables = new EventBTreeSubcategory("Variables", machine, machine.getVariables());
				List<EventBAxiom> axes = new ArrayList<>();
				List<EventBConstant> consts = new ArrayList<>();
				for (EventBContext context : machine.getSeenContexts()) {
					axes.addAll(context.getAxioms());
					consts.addAll(context.getConstants());
				}
				EventBTreeSubcategory axioms = new EventBTreeSubcategory("Axioms", machine, axes);
				EventBTreeSubcategory constants = new EventBTreeSubcategory("Constants", machine, consts);
				EventBTreeSubcategory[] treeChildren = { invariants, axioms, variables, constants };
				treeCategories = treeChildren;
				return treeChildren;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if ((parentElement instanceof EventBMachine)) {
					return getElements(parentElement);
				}
				if (parentElement instanceof EventBTreeSubcategory) {
					return ((EventBTreeSubcategory) parentElement).children;
				}
				return null;
			}
		});

		treeViewer.setInput(machine);

		this.treeViewer = treeViewer;
	}

	class EventBTreeSubcategory {
		final String label;
		final EventBUnit parentUnit;
		final EventBTreeElement parentElement;
		final EventBTreeElement[] children;

		public EventBTreeSubcategory(String label, EventBUnit parent, List<? extends EventBElement> children) {
			this.label = label;
			this.parentUnit = parent;
			this.parentElement = null;

			List<EventBTreeElement> treeChildren = new ArrayList<>();
			for (EventBElement originalChild : children) {
				EventBTreeElement treeChild = new EventBTreeElement(this, originalChild);
				treeChildren.add(treeChild);
			}

			this.children = treeChildren.toArray(new EventBTreeElement[treeChildren.size()]);
		}

		public EventBTreeSubcategory(String label, EventBTreeElement parent, List<? extends EventBElement> children) {
			this.label = label;
			this.parentElement = parent;
			this.parentUnit = null;

			List<EventBTreeElement> treeChildren = new ArrayList<>();
			for (EventBElement originalChild : children) {
				EventBTreeElement treeChild = new EventBTreeElement(this, originalChild);
				treeChildren.add(treeChild);
			}

			this.children = treeChildren.toArray(new EventBTreeElement[treeChildren.size()]);
		}

		public String getLabel() {
			return label;
		}

		public EventBUnit getParentUnit() {
			return parentUnit;
		}

		public EventBTreeElement getParentElement() {
			return parentElement;
		}

		public EventBTreeElement[] getChildren() {
			return children;
		}

		public EventBTreeElement findTreeElement(EventBElement originalElement) {
			for (EventBTreeElement child : children) {
				if (child.getOriginalElement().equals(originalElement)) {
					return child;
				}
			}
			return null;
		}
	}

	class EventBTreeElement {
		final EventBTreeSubcategory parent;
		final EventBElement originalElement;

		public EventBTreeElement(EventBTreeSubcategory parent, EventBElement originalElement) {
			this.parent = parent;
			this.originalElement = originalElement;
		}

		public EventBTreeSubcategory getParent() {
			return parent;
		}

		public EventBElement getOriginalElement() {
			return originalElement;
		}
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
			if (element instanceof EventBTreeSubcategory) {
				// TODO: Add color coding for categories
				return null;
			}
			if (element instanceof EventBTreeElement) {
				switch (columnIndex) {
				case 0: // Selection Column
					break;
				case 1:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN);
				case 2:
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA);
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
			if (!(element instanceof EventBTreeElement)) {
				// TODO: Add color coding for categories
				return null;
			}
			if (treeViewer.getChecked(element)) {
				return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			}
			// element = ((EventBTreeElement) element).getOriginalElement();
			// EventBElement eventBElement = (EventBElement) element;
			// if (eventBElement.isSelected()){
			// return
			// Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			// }
			if (selectionDependencies.containsKey(((EventBTreeElement) element).getOriginalElement())) {
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
			if (element instanceof EventBTreeSubcategory) {
				if (columnIndex == 1) {
					return ((EventBTreeSubcategory) element).getLabel();
				}
			}
			if (!(element instanceof EventBTreeElement)) {
				return null;
			}
			element = ((EventBTreeElement) element).getOriginalElement();
			if (!(element instanceof EventBCondition || element instanceof EventBAttribute)) {
				return null;
			}
			EventBElement eventBElement = (EventBElement) element;
			switch (columnIndex) {
			case 0: // Selection Column
				return null;
			case 1:
				return eventBElement.getLabel();
			case 2:
				if (eventBElement instanceof EventBCondition) {
					return ((EventBCondition) eventBElement).getPredicate();
				}
				return null;
			case 3:
				return eventBElement.getComment();
			default:
				return null;
			}
		}

	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		parent.setLayout(layout);
		createTree(parent);
		// new Label(parent, SWT.NONE).setText("Invariants");
		// createInvariantAndAxiomTable(parent);
		// new Label(parent, SWT.NONE).setText("Variables");
		// createVariableAndConstantTable(parent);

	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

}
