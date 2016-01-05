package eventBRefinementSlicer.ui.editors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
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

import eventBRefinementSlicer.internal.datastructures.EventBAction;
import eventBRefinementSlicer.internal.datastructures.EventBCondition;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBEvent;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;
import eventBRefinementSlicer.internal.datastructures.EventBTypes;
import eventBRefinementSlicer.internal.datastructures.EventBUnit;
import eventBRefinementSlicer.ui.jobs.EventBDependencyAnalysisJob;
import eventBRefinementSlicer.ui.wizards.MachineCreationWizard;

/**
 * The editor in charge of selecting which parts of an EventB machine to use in the slicing of refinements
 * 
 * @author Aivar Kripsaar
 *
 */

public class SelectionEditor extends EditorPart {

	private static final String LABEL_ELEMENT = "Element";
	private static final String LABEL_CONTENT = "Content";
	private static final String LABEL_SPECIAL = "Special";
	private static final String LABEL_COMMENT = "Comment";

	private static final int ELEMENT_COLUMN = 0;
	private static final int CONTENT_COLUMN = 1;
	private static final int SPECIAL_COLUMN = 2;
	private static final int COMMENT_COLUMN = 3;

	private IRodinFile rodinFile;
	private IMachineRoot machineRoot;
	private EventBMachine machine;

	// TODO: Replace with something more reasonable. Or get rid of it altogether.
	private Map<String, EventBTreeSubcategory> treeCategories = new HashMap<>();

	private Map<EventBElement, Integer> selectionDependees = new HashMap<>();
	private Map<EventBElement, Integer> selectionDependers = new HashMap<>();

	private Map<EventBElement, EventBTreeElement> elementToTreeElementMap = new HashMap<>();

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
		} catch (CoreException e) {
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

		String[] titles = { LABEL_ELEMENT, LABEL_CONTENT, LABEL_SPECIAL, LABEL_COMMENT };

		TreeColumn column;

		for (String title : titles) {
			column = new TreeColumn(tree, SWT.NONE);
			column.setText(title);
			column.setResizable(false);
		}

		createContainerCheckedTreeViewer(tree, titles);

		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				packColumns();
			}
		});

		tree.addListener(SWT.EraseItem, new Listener() {

			@Override
			public void handleEvent(Event event) {
				GC gc = event.gc;
				TreeItem item = (TreeItem) event.item;
				int width = tree.getClientArea().x + tree.getClientArea().width - event.x;
				if (event.index == tree.getColumnCount() - 1 || tree.getColumnCount() == 0) {
					if (width > 0) {
						Region region = new Region();
						gc.getClipping(region);
						region.add(event.x, event.y, width, event.height);
						gc.setClipping(region);
						region.dispose();
					}
				}
				gc.setBackground(item.getBackground(event.index));
				gc.setForeground(item.getForeground(event.index));
				gc.fillRectangle(event.x, event.y, width, event.height);
			}
		});

		packColumns();
	}

	private void packColumns() {
		Tree tree = treeViewer.getTree();
		int columnsWidth = 0;
		for (TreeColumn column : tree.getColumns()) {
			column.pack();
		}
		for (TreeColumn column : tree.getColumns()) {
			columnsWidth += column.getWidth();
		}
		TreeColumn lastColumn = tree.getColumn(tree.getColumnCount() - 1);
		columnsWidth -= lastColumn.getWidth();

		Rectangle area = tree.getClientArea();
		int width = area.width;

		if (lastColumn.getWidth() < width - columnsWidth) {
			lastColumn.setWidth(width - columnsWidth);
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
						packColumns();
					}
				});

			}

			@Override
			public void treeCollapsed(TreeExpansionEvent event) {
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						packColumns();
					}
				});
			}
		});

		treeViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				setCheckedElement(event.getElement(), event.getChecked());
			}

		});

		treeViewer.setContentProvider(new ITreeContentProvider() {

			private EventBTreeSubcategory[] treeRootCategories;
			private Map<EventBEvent, EventBTreeSubcategory[]> eventSubcategories = new HashMap<>();
			private Map<EventBContext, EventBTreeSubcategory[]> contextSubcategories = new HashMap<>();

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
				if (element instanceof EventBTreeElement) {
					if (((EventBTreeElement) element).getOriginalElement() instanceof EventBEvent) {
						EventBEvent event = (EventBEvent) ((EventBTreeElement) element).getOriginalElement();
						return !(event.isEmpty());
					}
					if (((EventBTreeElement) element).getOriginalElement() instanceof EventBContext) {
						EventBContext context = (EventBContext) ((EventBTreeElement) element).getOriginalElement();
						return !(context.isEmpty());
					}
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
				if (treeRootCategories == null) {
					EventBTreeSubcategory invariants = new EventBTreeSubcategory("Invariants", machine, machine.getInvariants());
					EventBTreeSubcategory variables = new EventBTreeSubcategory("Variables", machine, machine.getVariables());
					EventBTreeSubcategory events = new EventBTreeSubcategory("Events", machine, machine.getEvents());
					EventBTreeSubcategory contexts = new EventBTreeSubcategory("Seen Contexts", machine, machine.getSeenContexts());
					EventBTreeSubcategory[] treeChildren = { invariants, variables, events, contexts };
					addCategories(treeChildren);
					treeRootCategories = treeChildren;
				}

				return treeRootCategories;
			}

			@Override
			public Object[] getChildren(Object parentElement) {
				if ((parentElement instanceof EventBMachine)) {
					return getElements(parentElement);
				}
				if (parentElement instanceof EventBTreeSubcategory) {
					return ((EventBTreeSubcategory) parentElement).children;
				}
				if (parentElement instanceof EventBTreeElement) {
					if (!(((EventBTreeElement) parentElement).getOriginalElement() instanceof EventBEvent || ((EventBTreeElement) parentElement)
							.getOriginalElement() instanceof EventBContext)) {
						return null;
					}
					if (((EventBTreeElement) parentElement).getOriginalElement() instanceof EventBEvent) {
						EventBEvent originalElement = (EventBEvent) ((EventBTreeElement) parentElement).getOriginalElement();
						EventBTreeElement parent = (EventBTreeElement) parentElement;
						if (!eventSubcategories.containsKey(originalElement)) {
							EventBTreeSubcategory parameters = new EventBTreeSubcategory("Parameters", parent, originalElement.getParameters());
							EventBTreeSubcategory guards = new EventBTreeSubcategory("Guards", parent, originalElement.getGuards());
							EventBTreeSubcategory actions = new EventBTreeSubcategory("Actions", parent, originalElement.getActions());
							EventBTreeSubcategory[] children = { parameters, guards, actions };
							addCategories(children);
							eventSubcategories.put(originalElement, children);
						}
						return eventSubcategories.get(originalElement);
					}
					if (((EventBTreeElement) parentElement).getOriginalElement() instanceof EventBContext) {
						EventBContext originalElement = (EventBContext) ((EventBTreeElement) parentElement).getOriginalElement();
						EventBTreeElement parent = (EventBTreeElement) parentElement;
						if (!contextSubcategories.containsKey(originalElement)) {
							EventBTreeSubcategory axioms = new EventBTreeSubcategory("Axioms", parent, originalElement.getAxioms());
							EventBTreeSubcategory constants = new EventBTreeSubcategory("Constants", parent, originalElement.getConstants());
							EventBTreeSubcategory carrierSets = new EventBTreeSubcategory("Carrier Sets", parent, originalElement.getCarrierSets());
							EventBTreeSubcategory[] children = { axioms, constants, carrierSets };
							addCategories(children);
							contextSubcategories.put(originalElement, children);
						}
						return contextSubcategories.get(originalElement);
					}
				}
				return null;
			}
		});

		treeViewer.setInput(machine);

		this.treeViewer = treeViewer;
	}

	private void setCheckedElement(Object element, boolean checked) {
		treeViewer.setSubtreeChecked(element, checked);
		updateElement(element);

		//

		// Get dependency information and add it to the local maps
		if (element instanceof EventBTreeElement) {
			EventBDependencies dependencies = machine.getDependencies();
			Set<EventBElement> dependees = dependencies.getDependeesForElement(((EventBTreeElement) element).getOriginalElement());
			for (EventBElement dependee : dependees) {
				updateSelectionDependency(dependee, true, checked);
			}
			Set<EventBElement> dependers = dependencies.getDependersForElement(((EventBTreeElement) element).getOriginalElement());
			for (EventBElement depender : dependers) {
				updateSelectionDependency(depender, false, checked);
			}
		}

		// Update selected children and update their dependencies
		handleChildren(element, checked);

	}

	private void handleChildren(Object parent, boolean checked) {
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (!contentProvider.hasChildren(parent)) {
			return;
		}
		for (Object child : contentProvider.getChildren(parent)) {
			setCheckedElement(child, checked);
		}
	}

	private void updateSelectionDependency(EventBElement dependecy, boolean dependee, boolean increase) {
		Map<EventBElement, Integer> dependencyMap;
		if (dependee) {
			dependencyMap = selectionDependees;
		} else {
			dependencyMap = selectionDependers;
		}
		int count = dependencyMap.containsKey(dependecy) ? dependencyMap.get(dependecy) : 0;
		count += increase ? 1 : -1;
		if (count > 0) {
			dependencyMap.put(dependecy, count);
		} else {
			dependencyMap.remove(dependecy);
		}
		updateElement(findTreeElement(dependecy, false));
	}

	/*
	 * We update both the element and all of its parents, just in case.
	 */
	private void updateElement(Object element) {
		if (element == null) {
			return;
		}
		treeViewer.update(element, null);
		if (element instanceof EventBTreeElement) {
			EventBTreeElement treeElement = (EventBTreeElement) element;
			if (treeElement.getParent() != null) {
				updateElement(treeElement.getParent());
			}
		}
		if (element instanceof EventBTreeSubcategory) {
			EventBTreeSubcategory treeCategory = (EventBTreeSubcategory) element;
			if (treeCategory.getParentElement() != null) {
				updateElement(treeCategory.getParentElement());
			}
		}
	}

	public class EventBTreeSubcategory {
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

		@Override
		public String toString() {
			return label;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			result = prime * result + ((parentElement == null) ? 0 : parentElement.hashCode());
			result = prime * result + ((parentUnit == null) ? 0 : parentUnit.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EventBTreeSubcategory other = (EventBTreeSubcategory) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (label == null) {
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			if (parentElement == null) {
				if (other.parentElement != null)
					return false;
			} else if (!parentElement.equals(other.parentElement))
				return false;
			if (parentUnit == null) {
				if (other.parentUnit != null)
					return false;
			} else if (!parentUnit.equals(other.parentUnit))
				return false;
			return true;
		}

		private SelectionEditor getOuterType() {
			return SelectionEditor.this;
		}

	}

	public class EventBTreeElement {
		final EventBTreeSubcategory parent;
		final EventBElement originalElement;

		public EventBTreeElement(EventBTreeSubcategory parent, EventBElement originalElement) {
			this.parent = parent;
			this.originalElement = originalElement;
			elementToTreeElementMap.put(originalElement, this);
		}

		public EventBTreeSubcategory getParent() {
			return parent;
		}

		public EventBElement getOriginalElement() {
			return originalElement;
		}

		@Override
		public String toString() {
			return originalElement.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((originalElement == null) ? 0 : originalElement.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			EventBTreeElement other = (EventBTreeElement) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (originalElement == null) {
				if (other.originalElement != null)
					return false;
			} else if (!originalElement.equals(other.originalElement))
				return false;
			return true;
		}

		private SelectionEditor getOuterType() {
			return SelectionEditor.this;
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
				if (treeViewer.getChecked(element) || selectionDependees.containsKey(((EventBTreeElement) element).getOriginalElement())) {
					switch (columnIndex) {
					case ELEMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_CYAN);
					case CONTENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA);
					case SPECIAL_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
					case COMMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
					default:
						break;
					}
				} else {
					switch (columnIndex) {
					case ELEMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_DARK_CYAN);
					case CONTENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA);
					case SPECIAL_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
					case COMMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
					default:
						break;
					}
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
			if (selectionDependees.containsKey(((EventBTreeElement) element).getOriginalElement())) {
				return Display.getDefault().getSystemColor(SWT.COLOR_RED);
			}
			if (selectionDependers.containsKey(((EventBTreeElement) element).getOriginalElement())) {
				return Display.getDefault().getSystemColor(SWT.COLOR_CYAN);
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
				if (columnIndex == ELEMENT_COLUMN) {
					return ((EventBTreeSubcategory) element).getLabel();
				}
			}
			if (!(element instanceof EventBTreeElement)) {
				return null;
			}
			element = ((EventBTreeElement) element).getOriginalElement();
			if (!(element instanceof EventBElement)) {
				return null;
			}
			EventBElement eventBElement = (EventBElement) element;
			switch (columnIndex) {
			case ELEMENT_COLUMN:
				return eventBElement.getLabel();
			case CONTENT_COLUMN:
				if (eventBElement instanceof EventBCondition) {
					return ((EventBCondition) eventBElement).getPredicate();
				} else if (eventBElement instanceof EventBAction) {
					return ((EventBAction) eventBElement).getAssignment();
				}
				return null;
			case SPECIAL_COLUMN:
				if (eventBElement instanceof EventBCondition) {
					if (((EventBCondition) eventBElement).isTheorem()) {
						return "theorem";
					} else {
						return "not theorem";
					}
				}
				if (eventBElement instanceof EventBEvent) {
					EventBEvent event = (EventBEvent) eventBElement;
					String res = "";
					if (event.isExtended()) {
						res = res + "extended";
					} else {
						res = res + "not extended";
					}
					switch (event.getConvergence()) {
					case ORDINARY:
						res = res + ", ordinary";
						break;
					case CONVERGENT:
						res = res + ", convergent";
						break;
					case ANTICIPATED:
						res = res + ", anticipated";
						break;
					default:
						break;
					}
					return res;
				}
				return null;
			case COMMENT_COLUMN:
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
		createButtons(parent);
		createTree(parent);
		setPartName(machineRoot.getComponentName());

	}

	private void createButtons(Composite parent) {
		Composite buttonBar = new Composite(parent, SWT.NONE);
		buttonBar.setLayout(new RowLayout());
		Button newMachineButton = new Button(buttonBar, SWT.PUSH);
		newMachineButton.setText("Create Sub-Refinement");
		newMachineButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				// TODO: Add functionality
				boolean needNewContext = false; // TODO: get info from context selection
				if (needNewContext) {
					// TODO: Implement new input dialog
				} else {
					WizardDialog wizardDialog = new WizardDialog(parent.getShell(), new MachineCreationWizard(rodinFile.getRodinProject(),
							machineRoot, treeViewer.getCheckedElements(), treeViewer.getGrayedElements()));

					wizardDialog.setBlockOnOpen(true);
					wizardDialog.open();
					System.out.println();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		Button selectAllButton = new Button(buttonBar, SWT.PUSH);
		selectAllButton.setText("Select All");
		selectAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Object[] categories = ((ITreeContentProvider) treeViewer.getContentProvider()).getChildren(machine);
				for (Object category : categories) {
					setCheckedElement(category, true);
				}
				treeViewer.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);

			}
		});

		Button selectAllDependenciesButton = new Button(buttonBar, SWT.PUSH);
		selectAllDependenciesButton.setText("Select All Dependencies");
		selectAllDependenciesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (EventBElement dependee : selectionDependees.keySet()) {
					EventBTreeElement element = findTreeElement(dependee, true);
					if (element != null) {
						setCheckedElement(element, true);
					}
				}
				treeViewer.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}

	private EventBTreeElement findTreeElement(EventBElement element, boolean expand) {
		EventBTreeElement treeElement = null;
		if (!expand && elementToTreeElementMap.containsKey(element)) {
			return elementToTreeElementMap.get(element);
		}

		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();

		switch (element.getType()) {
		case EventBTypes.INVARIANT:
			treeViewer.expandToLevel(treeCategories.get("Invariants"), 1);
			treeElement = elementToTreeElementMap.get(element);
			return treeElement;
		case EventBTypes.VARIABLE:
			treeViewer.expandToLevel(treeCategories.get("Variables"), 1);
			return elementToTreeElementMap.get(element);
		case EventBTypes.CONTEXT: {
			EventBTreeSubcategory treeContexts = treeCategories.get("Seen Contexts");
			treeViewer.expandToLevel(treeContexts, 1);
			return elementToTreeElementMap.get(element);
		}
		case EventBTypes.CONSTANT:
		case EventBTypes.AXIOM:
		case EventBTypes.CARRIER_SET: {
			EventBTreeSubcategory treeContexts = treeCategories.get("Seen Contexts");
			for (EventBTreeElement treeContext : treeContexts.getChildren()) {
				assert treeContext.getOriginalElement() instanceof EventBContext;
				EventBContext context = (EventBContext) treeContext.getOriginalElement();
				if (!context.containsElement(element)) {
					continue;
				}
				if (expand) {
					treeViewer.expandToLevel(treeContext, 1);
				}
				Object[] childrenOfContext = contentProvider.getChildren(treeContext);
				for (Object child : childrenOfContext) {
					assert child instanceof EventBTreeSubcategory;
					EventBTreeSubcategory subcategory = (EventBTreeSubcategory) child;
					String label = "";
					if (element.getType().equals(EventBTypes.CONSTANT)) {
						label = "Constants";
					} else if (element.getType().equals(EventBTypes.AXIOM)) {
						label = "Axioms";
					} else if (element.getType().equals(EventBTypes.CARRIER_SET)) {
						label = "Carrier Sets";
					}
					if (subcategory.getLabel().equals(label)) {
						if (expand) {
							treeViewer.expandToLevel(subcategory, 1);
							packColumns();
						}
						return elementToTreeElementMap.get(element);
					}
				}
			}
		}
		case EventBTypes.EVENT: {
			EventBTreeSubcategory treeEvents = treeCategories.get("Events");
			treeViewer.expandToLevel(treeEvents, 1);
			return elementToTreeElementMap.get(element);
		}
		case EventBTypes.GUARD:
		case EventBTypes.ACTION: {
			EventBTreeSubcategory treeEvents = treeCategories.get("Events");
			for (EventBTreeElement treeEvent : treeEvents.getChildren()) {
				assert treeEvent.getOriginalElement() instanceof EventBEvent;
				EventBEvent event = (EventBEvent) treeEvent.getOriginalElement();
				if (!event.containsElement(element)) {
					continue;
				}
				if (expand) {
					treeViewer.expandToLevel(treeEvent, 1);
				}
				Object[] childrenofContext = contentProvider.getChildren(treeEvent);
				for (Object child : childrenofContext) {
					assert child instanceof EventBTreeSubcategory;
					EventBTreeSubcategory subcategory = (EventBTreeSubcategory) child;
					String label = "";
					if (element.getType().equals(EventBTypes.GUARD)) {
						label = "Guards";
					} else if (element.getType().equals(EventBTypes.ACTION)) {
						label = "Actions";
					}
					if (subcategory.getLabel().equals(label)) {
						if (expand) {
							treeViewer.expandToLevel(subcategory, 1);
							packColumns();
						}
						return elementToTreeElementMap.get(element);
					}
				}
			}
		}
		default:
			break;
		}

		return treeElement;
	}

	private void addCategories(EventBTreeSubcategory[] categories) {
		for (EventBTreeSubcategory category : categories) {
			treeCategories.put(category.getLabel(), category);
		}
	}
}
