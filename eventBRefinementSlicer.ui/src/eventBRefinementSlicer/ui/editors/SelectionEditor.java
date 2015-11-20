package eventBRefinementSlicer.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
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
import org.eclipse.jface.window.Window;
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
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eventb.core.IAction;
import org.eventb.core.IAssignmentElement;
import org.eventb.core.IAxiom;
import org.eventb.core.ICarrierSet;
import org.eventb.core.ICommentedElement;
import org.eventb.core.IConfigurationElement;
import org.eventb.core.IConstant;
import org.eventb.core.IContextRoot;
import org.eventb.core.IDerivedPredicateElement;
import org.eventb.core.IEvent;
import org.eventb.core.IEventBRoot;
import org.eventb.core.IExtendsContext;
import org.eventb.core.IGuard;
import org.eventb.core.IIdentifierElement;
import org.eventb.core.IInvariant;
import org.eventb.core.ILabeledElement;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.eventb.core.ast.FormulaFactory;
import org.eventb.core.basis.ContextRoot;
import org.eventb.core.basis.MachineRoot;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IInternalElementType;
import org.rodinp.core.IRefinementManager;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.IRodinProject;
import org.rodinp.core.RodinCore;
import org.rodinp.core.RodinDBException;

import eventBRefinementSlicer.internal.datastructures.EventBAction;
import eventBRefinementSlicer.internal.datastructures.EventBAxiom;
import eventBRefinementSlicer.internal.datastructures.EventBCarrierSet;
import eventBRefinementSlicer.internal.datastructures.EventBCondition;
import eventBRefinementSlicer.internal.datastructures.EventBConstant;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBDependencies;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBEvent;
import eventBRefinementSlicer.internal.datastructures.EventBGuard;
import eventBRefinementSlicer.internal.datastructures.EventBInvariant;
import eventBRefinementSlicer.internal.datastructures.EventBMachine;
import eventBRefinementSlicer.internal.datastructures.EventBTypes;
import eventBRefinementSlicer.internal.datastructures.EventBUnit;
import eventBRefinementSlicer.internal.datastructures.EventBVariable;
import eventBRefinementSlicer.ui.jobs.EventBDependencyAnalysisJob;

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
				treeViewer.setSubtreeChecked(event.getElement(), event.getChecked());
				treeViewer.update(event.getElement(), null);
				assert (event.getElement() instanceof EventBTreeElement || event.getElement() instanceof EventBTreeSubcategory);
				if (event.getElement() instanceof EventBTreeSubcategory) {
					handleChildren(((EventBTreeSubcategory) event.getElement()), event);
					return;
				}
				EventBDependencies dependencies = machine.getDependencies();
				handleSelectionDependenciesForElement((EventBTreeElement) event.getElement(), dependencies, event);
				if (((EventBTreeElement) event.getElement()).getOriginalElement() instanceof EventBEvent
						|| ((EventBTreeElement) event.getElement()).getOriginalElement() instanceof EventBContext) {
					handleEventOrContextChildren((EventBTreeElement) event.getElement(), event);
				}
			}

			private void handleChildren(EventBTreeSubcategory category, CheckStateChangedEvent event) {
				for (EventBTreeElement child : category.getChildren()) {
					treeViewer.update(child, null);
					handleSelectionDependenciesForElement(child, machine.getDependencies(), event);
					if (child.getOriginalElement() instanceof EventBEvent || child.getOriginalElement() instanceof EventBContext) {
						handleEventOrContextChildren(child, event);
					}
				}
			}

			private void handleEventOrContextChildren(EventBTreeElement eventElement, CheckStateChangedEvent event) {
				assert (eventElement.getOriginalElement() instanceof EventBEvent || eventElement.getOriginalElement() instanceof EventBContext);
				ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
				if (!contentProvider.hasChildren(eventElement)) {
					return;
				}
				for (Object childObject : contentProvider.getChildren(eventElement)) {
					assert childObject instanceof EventBTreeSubcategory;
					EventBTreeSubcategory child = (EventBTreeSubcategory) childObject;
					treeViewer.update(child, null);
					handleChildren(child, event);
				}
			}

			private void handleSelectionDependenciesForElement(EventBTreeElement element, EventBDependencies dependencies,
					CheckStateChangedEvent event) {
				EventBElement eventBElement = element.getOriginalElement();
				Set<EventBElement> dependees = dependencies.getDependeesForElement(eventBElement);
				Set<EventBElement> dependers = dependencies.getDependersForElement(eventBElement);
				handleSingleDependencyDirection(dependees, event, true);
				handleSingleDependencyDirection(dependers, event, false);
			}

			private void handleSingleDependencyDirection(Set<EventBElement> dependencySet, CheckStateChangedEvent event, boolean dependeeSet) {
				Map<EventBElement, Integer> dependencyMap;
				if (dependeeSet) {
					dependencyMap = selectionDependees;
				} else {
					dependencyMap = selectionDependers;
				}
				for (EventBElement dependency : dependencySet) {
					if (event.getChecked()) {
						if (!dependencyMap.containsKey(dependency)) {
							dependencyMap.put(dependency, 0);
						}
						dependencyMap.put(dependency, dependencyMap.get(dependency) + 1);
					} else {
						if (dependencyMap.containsKey(dependency)) {
							dependencyMap.put(dependency, dependencyMap.get(dependency) - 1);
							if (dependencyMap.get(dependency).intValue() <= 0) {
								dependencyMap.remove(dependency);
							}
						}
					}
					treeViewer.update(findTreeElement(dependency, false), null);
				}
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
							EventBTreeSubcategory guards = new EventBTreeSubcategory("Guards", parent, originalElement.getGuards());
							EventBTreeSubcategory actions = new EventBTreeSubcategory("Actions", parent, originalElement.getActions());
							EventBTreeSubcategory[] children = { guards, actions };
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

	class EventBTreeElement {
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
				try {
					String machineNameInput = "newMachine";
					boolean needNewContext = false; // TODO: get info from context selection
					if (needNewContext) {
						// TODO: Implement new input dialog
					} else {
						MachineCreationDialog inputDialog = new MachineCreationDialog(parent.getShell());
						inputDialog.setBlockOnOpen(true);
						inputDialog.open();
						if (inputDialog.getReturnCode() == Window.OK) {
							machineNameInput = inputDialog.getMachineNameInput();
							if (machineNameInput.endsWith(".bum") || machineNameInput.endsWith(".buc") || machineNameInput.endsWith(".bcm")
									|| machineNameInput.endsWith(".bcc")) {
								int end = machineNameInput.lastIndexOf(".");
								machineNameInput = machineNameInput.substring(0, end);
							}
						} else {
							return;
						}
					}
					createMachineFromSelection(machineNameInput);
				} catch (RodinDBException e1) {
					e1.printStackTrace();
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
					treeViewer.setSubtreeChecked(category, true);
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
						treeViewer.setChecked(element, true);
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

	private void createMachineFromSelection(String machineName) throws RodinDBException {
		List<Object> checkedElementsList = new ArrayList<>(Arrays.asList(treeViewer.getCheckedElements()));
		List<EventBInvariant> invariants = new ArrayList<>();
		List<EventBVariable> variables = new ArrayList<>();
		List<EventBEvent> events = new ArrayList<>();
		List<EventBGuard> guards = new ArrayList<>();
		List<EventBAction> actions = new ArrayList<>();
		List<EventBContext> contexts = new ArrayList<>();
		List<EventBContext> partialContexts = new ArrayList<>();

		for (Object checkedElement : checkedElementsList) {
			if (checkedElement instanceof EventBTreeSubcategory) {
				continue;
			}
			EventBElement element = ((EventBTreeElement) checkedElement).getOriginalElement();
			if (element instanceof EventBInvariant) {
				invariants.add((EventBInvariant) element);
			} else if (element instanceof EventBVariable) {
				variables.add((EventBVariable) element);
			} else if (element instanceof EventBGuard) {
				guards.add((EventBGuard) element);
			} else if (element instanceof EventBAction) {
				actions.add((EventBAction) element);
			} else if (element instanceof EventBEvent) {
				events.add((EventBEvent) element);
			} else if (element instanceof EventBContext) {
				// We only add completely checked Contexts (no partial
				// selections)
				if (treeViewer.getGrayed(checkedElement)) {
					partialContexts.add((EventBContext) element);
				} else {
					contexts.add((EventBContext) element);
				}
			}
		}

		RodinCore.run(new IWorkspaceRunnable() {

			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				// Get Rodin project and create new file
				IRodinProject project = rodinFile.getRodinProject();
				IRodinFile file = project.getRodinFile(machineName.concat(".bum"));
				file.create(true, null);
				file.getResource().setDerived(true, null);
				MachineRoot root = (MachineRoot) file.getRoot();
				root.setConfiguration(IConfigurationElement.DEFAULT_CONFIGURATION, monitor);

				// Add selected invariants to new machine
				for (EventBInvariant invariant : invariants) {
					addRodinElement(IInvariant.ELEMENT_TYPE, root, invariant);
				}
				// Add selected variables to new machine
				for (EventBVariable variable : variables) {
					addRodinElement(IVariable.ELEMENT_TYPE, root, variable);
				}
				// Add selected events to new machine
				for (EventBEvent event : events) {
					IEvent rodinEvent = (IEvent) addRodinElement(IEvent.ELEMENT_TYPE, root, event);
					rodinEvent.setExtended(event.isExtended(), null);
					rodinEvent.setConvergence(event.getConvergence(), null);
					// Add guards to the event
					List<EventBGuard> relevantGuards = new ArrayList<>(event.getGuards());
					relevantGuards.retainAll(guards);
					for (EventBGuard guard : relevantGuards) {
						addRodinElement(IGuard.ELEMENT_TYPE, rodinEvent, guard);
					}
					guards.removeAll(relevantGuards);
					// Add actions to the event
					List<EventBAction> relevantActions = new ArrayList<>(event.getActions());
					relevantActions.retainAll(actions);
					for (EventBAction action : relevantActions) {
						addRodinElement(IAction.ELEMENT_TYPE, rodinEvent, action);
					}
					actions.removeAll(relevantActions);
				}

				// Add refinement information from existing machine to new
				// machine
				for (IRefinesMachine refines : machineRoot.getRefinesClauses()) {
					IRefinementManager refinementManager = RodinCore.getRefinementManager();
					refinementManager.refine(refines.getAbstractMachineRoot(), root, null);
				}

				// Adds seen contexts to new machine.
				// If whole context selected, we just add it directly,
				// unless it is already included (because of refinement)
				for (EventBContext context : contexts) {
					boolean alreadyIncluded = false;
					for (ISeesContext seenContext : root.getSeesClauses()) {
						if (seenContext.getSeenContextName().equals(context.getLabel())) {
							alreadyIncluded = true;
							break;
						}
					}
					if (alreadyIncluded) {
						continue;
					}
					addRodinElement(ISeesContext.ELEMENT_TYPE, root, context);
				}

				// If only parts of the context are selected, we create a new
				// context
				for (EventBContext context : partialContexts) {
					// If the abstract machine contains the whole context, we don't do anything, because the
					// whole context has already been included through refinement
					boolean alreadyIncluded = false;
					for (ISeesContext seenContext : root.getSeesClauses()) {
						if (seenContext.getSeenContextName().equals(context.getLabel())) {
							alreadyIncluded = true;
							break;
						}
					}
					if (alreadyIncluded) {
						continue;
					}
					createSeenContext("TestName", context, root);

				}

				// Save the final result
				file.save(null, false);

				// Open new editor window for newly created machine
				IFile resource = file.getResource();
				IEditorDescriptor editorDesc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(resource.getName());
				getSite().getPage().openEditor(new FileEditorInput(resource), editorDesc.getId());
			}

			private IInternalElement addRodinElement(IInternalElementType<?> type, IInternalElement parent, EventBElement element)
					throws RodinDBException {
				IInternalElement rodinElement = parent.getInternalElement(type, element.getLabel());
				rodinElement.create(null, null);
				if (rodinElement instanceof ILabeledElement) {
					((ILabeledElement) rodinElement).setLabel(element.getLabel(), null);
				}
				if (rodinElement instanceof IIdentifierElement) {
					((IIdentifierElement) rodinElement).setIdentifierString(element.getLabel(), null);
				}
				if (rodinElement instanceof ICommentedElement) {
					if (!element.getComment().equals("")) {
						((ICommentedElement) rodinElement).setComment(element.getComment(), null);
					}
				}
				if (rodinElement instanceof IDerivedPredicateElement && element instanceof EventBCondition) {
					((IDerivedPredicateElement) rodinElement).setPredicateString(((EventBCondition) element).getPredicate(), null);
					((IDerivedPredicateElement) rodinElement).setTheorem(((EventBCondition) element).isTheorem(), null);
				}
				if (rodinElement instanceof IAssignmentElement && element instanceof EventBAction) {
					((IAssignmentElement) rodinElement).setAssignmentString(((EventBAction) element).getAssignment(), null);
				}
				if (rodinElement instanceof ISeesContext && element instanceof EventBContext) {
					((ISeesContext) rodinElement).setSeenContextName(((EventBContext) element).getLabel(), null);
				}
				return rodinElement;
			}

			private void createSeenContext(String contextName, EventBContext originalContext, MachineRoot machineRoot) throws CoreException {
				List<Object> checkedElementsList = new ArrayList<>(Arrays.asList(treeViewer.getCheckedElements()));
				List<EventBAxiom> axioms = new ArrayList<>();
				List<EventBConstant> constants = new ArrayList<>();
				List<EventBCarrierSet> carrierSets = new ArrayList<>();

				for (Object checkedElement : checkedElementsList) {
					if (checkedElement instanceof EventBTreeSubcategory) {
						continue;
					}
					EventBElement originalElement = ((EventBTreeElement) checkedElement).getOriginalElement();
					if (originalElement instanceof EventBAxiom) {
						axioms.add((EventBAxiom) originalElement);
					} else if (originalElement instanceof EventBConstant) {
						constants.add((EventBConstant) originalElement);
					} else if (originalElement instanceof EventBCarrierSet) {
						carrierSets.add((EventBCarrierSet) originalElement);
					}
				}

				// Create new file for new context
				IRodinProject project = rodinFile.getRodinProject();
				IRodinFile file = project.getRodinFile(contextName + ".buc");
				file.create(true, null);
				file.getResource().setDerived(true, null);
				ContextRoot root = (ContextRoot) file.getRoot();
				root.setConfiguration(IConfigurationElement.DEFAULT_CONFIGURATION, null);

				// Add selected axioms to context
				for (EventBAxiom axeTheAxiom : axioms) {
					addRodinElement(IAxiom.ELEMENT_TYPE, root, axeTheAxiom);
				}
				// Add selected constants to context
				for (EventBConstant constant : constants) {
					addRodinElement(IConstant.ELEMENT_TYPE, root, constant);
				}

				// Add selected carrier sets to context
				for (EventBCarrierSet carrierSet : carrierSets) {
					addRodinElement(ICarrierSet.ELEMENT_TYPE, root, carrierSet);
				}

				IExtendsContext[] extendedContextsFromOriginal = originalContext.getScContextRoot().getContextRoot().getExtendsClauses();

				IExtendsContext originalExtendedContext = null;
				// Add extension information to the new context
				for (IExtendsContext extendedContext : extendedContextsFromOriginal) {
					originalExtendedContext = extendedContext;
					IRefinementManager refinementManager = RodinCore.getRefinementManager();
					refinementManager.refine(extendedContext.getAbstractContextRoot(), root, null);
				}

				file.save(null, false);

				// We need to remove the context we extend from the derived machine's seen contexts attribute
				if (originalExtendedContext != null) {
					String extendedContextName = originalExtendedContext.getAbstractContextName();
					for (ISeesContext seenContext : machineRoot.getSeesClauses()) {
						if (seenContext.getSeenContextName().equals(extendedContextName)) {
							seenContext.delete(false, null);
						}
					}
				}
				// Add new context to derived machine
				IInternalElement rodinElement = machineRoot.getInternalElement(ISeesContext.ELEMENT_TYPE, contextName);
				rodinElement.create(null, null);
				((ISeesContext) rodinElement).setSeenContextName(contextName, null);
			}

		}, null);
	}

}
