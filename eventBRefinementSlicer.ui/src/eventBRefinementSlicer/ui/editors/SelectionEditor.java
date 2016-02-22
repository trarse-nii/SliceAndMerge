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
import org.eclipse.swt.layout.FillLayout;
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
import org.eventb.core.IEventBRoot;
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.ast.FormulaFactory;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
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
import eventBRefinementSlicer.internal.datastructures.EventBMachine;
import eventBRefinementSlicer.internal.datastructures.EventBTypes;
import eventBRefinementSlicer.internal.datastructures.EventBUnit;
import eventBRefinementSlicer.ui.jobs.EventBDependencyAnalysisJob;
import eventBRefinementSlicer.ui.wizards.MachineCreationWizard;
import eventBRefinementSlicer.ui.wizards.MergeMachineWithPredecessorWizard;

/**
 * The editor in charge of selecting which parts of an Event-B machine to use in the slicing of refinements
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

	// We require a duplicate of the checked state because of the limitations of the TreeViewer API
	private Map<Object, Boolean> selectionMap = new HashMap<>();

	// TODO: Replace with something more reasonable. Or get rid of it altogether.
	private Map<String, EventBTreeSubcategory> treeCategories = new HashMap<>();

	private Map<EventBElement, Integer> selectionDependees = new HashMap<>();
	private Map<EventBElement, Integer> selectionDependers = new HashMap<>();

	private Map<EventBElement, EventBTreeElement> elementToTreeElementMap = new HashMap<>();

	private ContainerCheckedTreeViewer treeViewer = null;

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		rodinFile = getRodinFileFromInput(input);
		IInternalElement internalElementRoot = rodinFile.getRoot();
		assert (internalElementRoot instanceof IMachineRoot);
		machineRoot = (IMachineRoot) internalElementRoot;
		try {
			machine = new EventBMachine(machineRoot);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		EventBDependencyAnalysisJob.doEventBDependencyAnalysis(machine);
	}

	/**
	 * Gets the internal representation of the Rodin File from the editor input
	 * 
	 * @param input
	 *            The editor's input
	 * @return Internal representation of the Rodin File
	 */
	protected IRodinFile getRodinFileFromInput(IEditorInput input) {
		FileEditorInput editorInput = (FileEditorInput) input;
		IFile inputFile = editorInput.getFile();
		IRodinFile rodinFile = RodinCore.valueOf(inputFile);
		return rodinFile;
	}

	/**
	 * Gets the internal Rodin File
	 * 
	 * @return Internal representation of the Rodin File
	 */
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
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * Creates a tree UI element, which is the main body of the selection editor
	 * 
	 * @param parent
	 *            The parent of the tree
	 */
	private void createTree(Composite parent) {
		// We create a tree that allows for multiple selections and including checkbox behavior
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

		// We resize the columns every time the tree component changes size
		tree.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				packColumns();
			}
		});

		// A workaround to allow better highlighting of elements when hovering or selecting them
		// Without this, the highlight colors set by the Tree Viewer are removed, simply replaced by the
		// standard selection & highlight colors of the OS
		tree.addListener(SWT.EraseItem, new Listener() {

			@Override
			public void handleEvent(Event event) {
				GC gc = event.gc;
				TreeItem item = (TreeItem) event.item;
				int width = tree.getClientArea().x + tree.getClientArea().width - event.x;
				// Sets background and foreground color to the ones set by the Tree Viewer
				gc.setBackground(item.getBackground(event.index));
				gc.setForeground(item.getForeground(event.index));
				gc.fillRectangle(event.x, event.y, width, event.height);
			}
		});

		packColumns();
	}

	/**
	 * Adjusts size of columns to fit visible content. Also resizes last column to fill remainder of space.
	 */
	private void packColumns() {
		Tree tree = treeViewer.getTree();
		int columnsWidth = 0;
		for (TreeColumn column : tree.getColumns()) {
			column.pack();
		}
		// After packing all columns, we manually change the size of the last column
		for (TreeColumn column : tree.getColumns()) {
			columnsWidth += column.getWidth();
		}
		TreeColumn lastColumn = tree.getColumn(tree.getColumnCount() - 1);
		columnsWidth -= lastColumn.getWidth();

		Rectangle area = tree.getClientArea();
		int width = area.width;

		// We set the width of the last column to be the width of the tree area minus the width of every other
		// column added up, filling up the rest of the area
		if (lastColumn.getWidth() < width - columnsWidth) {
			lastColumn.setWidth(width - columnsWidth);
		}
	}

	/**
	 * Creates a tree viewer element with elements that can be checked off
	 * 
	 * @param tree
	 *            Parent tree element
	 * @param titles
	 *            Titles for the columns
	 */
	private void createContainerCheckedTreeViewer(Tree tree, String[] titles) {
		ContainerCheckedTreeViewer treeViewer = new ContainerCheckedTreeViewer(tree);
		treeViewer.setColumnProperties(titles);
		treeViewer.setUseHashlookup(true);

		treeViewer.setLabelProvider(new LabelProvider());

		// Any time the tree is either expanded or collapsed, the columns must once again be packed
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

		// Any time an element is checked or unchecked, we use a method to take care of additional
		// highlighting and dependency related changes
		treeViewer.addCheckStateListener(new ICheckStateListener() {

			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				// If the element is part of a context, we wish to disable selection, because only whole
				// contexts should be selectable
				// We have to hack this in by undoing the checking of the checkbox manually
				if (event.getElement() instanceof EventBTreeElement) {
					EventBElement element = ((EventBTreeElement) event.getElement()).getOriginalElement();
					if ((element instanceof EventBAxiom || element instanceof EventBConstant || element instanceof EventBCarrierSet)) {
						treeViewer.setSubtreeChecked(event.getElement(), !event.getChecked());
						// Correct checked state of parents
						correctParentsChecked(event.getElement());
						return;
					}
				} else if (event.getElement() instanceof EventBTreeSubcategory) {
					EventBTreeSubcategory treeCategory = (EventBTreeSubcategory) event.getElement();
					if (treeCategory.getParentElement() != null && treeCategory.getParentElement().getOriginalElement() instanceof EventBContext) {
						treeViewer.setSubtreeChecked(event.getElement(), !event.getChecked());
						// Correct checked state of parents
						correctParentsChecked(event.getElement());
						return;
					}
				}
				updateSelectionDependenciesForSubtree(event.getElement(), event.getChecked());
				correctParentsChecked(event.getElement());
			}

		});

		treeViewer.setContentProvider(new ITreeContentProvider() {

			private EventBTreeSubcategory[] treeRootCategories;
			private Map<EventBEvent, EventBTreeSubcategory[]> eventSubcategories = new HashMap<>();
			private Map<EventBContext, EventBTreeSubcategory[]> contextSubcategories = new HashMap<>();

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

			}

			@Override
			public void dispose() {

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
							EventBTreeSubcategory witnesses = new EventBTreeSubcategory("Witnesses", parent, originalElement.getWitnesses());
							EventBTreeSubcategory guards = new EventBTreeSubcategory("Guards", parent, originalElement.getGuards());
							EventBTreeSubcategory actions = new EventBTreeSubcategory("Actions", parent, originalElement.getActions());
							EventBTreeSubcategory[] children = { parameters, witnesses, guards, actions };
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

	/**
	 * Sets the checked status for the given element and its children. Also updates the dependency-related
	 * highlighting as necessary.
	 * 
	 * @param element
	 *            The element being checked or unchecked
	 * @param checked
	 *            Desired checked state
	 */
	private void setCheckedElement(Object element, boolean checked) {
		treeViewer.setSubtreeChecked(element, checked);
		updateElement(element);

		// Get dependency information and add it to the local maps
		updateSelectionDependenciesForElement(element, checked);

		// Update the selection map
		selectionMap.put(element, checked);

		// Update children of selected element and update their dependencies
		setChildrenChecked(element, checked);

		// Correct checked state of parents
		correctParentsChecked(element);

	}

	/**
	 * Updates the local dependency maps to account for the change of the checked state of an element
	 * 
	 * @param element
	 *            Element that has had its checked state changed
	 * @param checked
	 *            New checked status of element
	 */
	private void updateSelectionDependenciesForElement(Object element, boolean checked) {
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
		treeViewer.update(element, null);
	}

	/**
	 * Updates the local dependency maps to account for the change of the checked state of an element and its
	 * subtree
	 * 
	 * @param element
	 *            Element that has had its checked state changed
	 * @param checked
	 *            New checked status of element
	 */
	private void updateSelectionDependenciesForSubtree(Object element, boolean checked) {
		if (!(checked ^ selectionMap.getOrDefault(element, false))) {
			// If checked state of this element doesn't change, nothing else needs to be done.
			treeViewer.update(element, null);
			return;
		}
		selectionMap.put(element, checked);
		updateSelectionDependenciesForElement(element, checked);
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (!contentProvider.hasChildren(element)) {
			return;
		}
		for (Object child : contentProvider.getChildren(element)) {
			updateSelectionDependenciesForSubtree(child, checked);
		}
	}

	/**
	 * Gets all children of given parent element and sets their checked status as desired.
	 * 
	 * @param parent
	 *            Parent element of children we need to change
	 * @param checked
	 *            Desired checked state for children of parent
	 */
	private void setChildrenChecked(Object parent, boolean checked) {
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (!contentProvider.hasChildren(parent)) {
			return;
		}
		for (Object child : contentProvider.getChildren(parent)) {
			if (checked ^ selectionMap.getOrDefault(child, false)) {
				// We only update a child if its checked status changes
				setCheckedElement(child, checked);
			}
		}
	}

	/**
	 * Corrects the parents's checked (selection) state based on child's changed checked status
	 * 
	 * @param element
	 *            The element which has had its checked status changed.
	 */
	private void correctParentsChecked(Object element) {
		if (element instanceof EventBTreeElement) {
			EventBTreeElement treeElement = (EventBTreeElement) element;
			EventBTreeSubcategory parent = treeElement.getParent();
			if (parent == null) {
				return;
			}
			correctElementChecked(parent);
		} else if (element instanceof EventBTreeSubcategory) {
			EventBTreeSubcategory treeCategory = (EventBTreeSubcategory) element;
			EventBTreeElement parent = treeCategory.getParentElement();
			if (parent == null) {
				return;
			}
			correctElementChecked(parent);
		}
	}

	/**
	 * Checks element's current checked state based on checked states of children and corrects if necessary.
	 * 
	 * @param element
	 *            Tree element which needs to be checked and corrected
	 */
	private void correctElementChecked(Object element) {
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (!contentProvider.hasChildren(element)) {
			return;
		}
		Boolean isChecked = true;
		Boolean isPartiallyChecked = false;
		for (Object child : contentProvider.getChildren(element)) {
			if (treeViewer.getChecked(child) || treeViewer.getGrayed(child)) {
				isPartiallyChecked = true;
			}
			if (!treeViewer.getChecked(child) || treeViewer.getGrayed(child)) {
				isChecked = false;
			}
		}
		if (isChecked) {
			treeViewer.setChecked(element, true);
			treeViewer.setGrayed(element, false);
			treeViewer.update(element, null);
			selectionMap.put(element, true);
			correctParentsChecked(element);
		} else if (isPartiallyChecked) {
			treeViewer.setGrayChecked(element, true);
			treeViewer.update(element, null);
			selectionMap.put(element, true);
			correctParentsChecked(element);
		} else {
			treeViewer.setChecked(element, false);
			treeViewer.setGrayed(element, false);
			treeViewer.update(element, null);
			selectionMap.put(element, false);
			correctParentsChecked(element);
		}
	}

	/**
	 * Updates maps counting number of dependers and dependees that are currently selected (checked) and
	 * causes update so that dependencies are properly highlighted.
	 * 
	 * @param dependecy
	 *            Element for which we update dependency counts.
	 * @param dependee
	 *            Boolean to signify whether the element is being depended on, or if it is depending on
	 *            another element itself.
	 * @param increase
	 *            True if dependency count needs to be increased (when another dependency partner of the given
	 *            element has been selected).
	 */
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
		// Element needs to be updated so that its highlighting is correct.
		updateElement(findTreeElement(dependecy, false));
	}

	/**
	 * Updates an element as well as all of its parents in the editor's tree viewer.
	 * 
	 * @param element
	 *            Element to update
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

	/**
	 * Container class for tree subcategories (e.g. Variables, Invariants, Events).
	 * 
	 * @author Aivar Kripsaar
	 *
	 */
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

		/**
		 * Finds the tree container version of the given element.
		 * 
		 * @param originalElement
		 *            Editor internal representation of element
		 * @return Tree container element of the desired element
		 */
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

	/**
	 * Container class for Event-B elements in tree
	 * 
	 * @author Aivar Kripsaar
	 *
	 */
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

	/**
	 * Label provider for table viewer, implementing the necessary interfaces.
	 * 
	 * @author Aivar Kripsaar
	 *
	 */
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
					// If the element is being highlighted, we give the text a different color for easier
					// readability.
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
			if (!(element instanceof EventBTreeElement || element instanceof EventBTreeSubcategory)) {
				return null;
			}
			if (treeViewer == null) {
				return null;
			}
			if (element instanceof EventBTreeSubcategory) {
				// TODO: Add color coding for categories
				if (treeViewer.getChecked(element) && !treeViewer.getGrayed(element)) {
					// If all elements are selected, we color the category with the color for selected
					// elements
					return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
				}
				for (EventBTreeElement child : ((EventBTreeSubcategory) element).getChildren()) {
					if (selectionDependees.containsKey(child.getOriginalElement()) && !treeViewer.getChecked(child)) {
						// If even a single unselected child of the category is a dependency, we mark the
						// category red
						return Display.getDefault().getSystemColor(SWT.COLOR_RED);
					}
				}
				return null;
			}
			if (treeViewer.getChecked(element)) {
				// If the element is selected (checked)
				return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			}
			if (selectionDependees.containsKey(((EventBTreeElement) element).getOriginalElement())) {
				// If the element is depended upon by a currently selected element
				return Display.getDefault().getSystemColor(SWT.COLOR_RED);
			}
			if (selectionDependers.containsKey(((EventBTreeElement) element).getOriginalElement())) {
				// If the element depends on a currently selected element
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
				// Categories only need their labels displayed
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

	/**
	 * Creates buttons for editor
	 * 
	 * @param parent
	 *            The parent editor
	 */
	private void createButtons(Composite parent) {
		Composite buttonBar = new Composite(parent, SWT.NONE);
		buttonBar.setLayout(new RowLayout());
		// Button for creating a new sub-refinement from the selected elements
		Button newMachineButton = new Button(buttonBar, SWT.PUSH);
		newMachineButton.setText("Create Sub-Refinement");
		newMachineButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog wizardDialog = new WizardDialog(parent.getShell(), new MachineCreationWizard(rodinFile.getRodinProject(), machineRoot,
						treeViewer.getCheckedElements()));

				wizardDialog.setBlockOnOpen(true);
				wizardDialog.open();
				System.out.println();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// We add an extra container for the next button to allow a tooltip when the button is disabled
		final Composite mergeButtonContainer = new Composite(buttonBar, SWT.NONE);
		mergeButtonContainer.setLayout(new FillLayout());

		// A button to merge the currently opened machine with its direct ancestor (i.e. the machine this one
		// refines)
		Button mergeMachinesButton = new Button(mergeButtonContainer, SWT.PUSH);
		mergeMachinesButton.setText("Merge With Direct Predecessor");
		try {
			// If current machine has no machine that it refines, we disable the button and display a tooltip
			// explaining why the button is disabled
			IRefinesMachine refinesMachines[] = machineRoot.getRefinesClauses();
			if (refinesMachines.length > 0) {
				mergeMachinesButton.setEnabled(true);
				mergeButtonContainer.setToolTipText("");
			} else {
				mergeMachinesButton.setEnabled(false);
				mergeButtonContainer.setToolTipText("Nothing to merge with. Current machine does not refine any other machine.");
			}
		} catch (RodinDBException e) {
			// TODO: handle exception
		}

		mergeMachinesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog wizardDialog = new WizardDialog(parent.getShell(), new MergeMachineWithPredecessorWizard(rodinFile.getRodinProject(),
						machineRoot));

				wizardDialog.setBlockOnOpen(true);
				wizardDialog.open();
				System.out.println();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// A button to select all elements of the machine opened in the editor
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

		// A button to select all elements that currently selected elements depend on
		Button selectAllDependenciesButton = new Button(buttonBar, SWT.PUSH);
		selectAllDependenciesButton.setText("Select All Dependencies");
		selectAllDependenciesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (EventBElement dependee : selectionDependees.keySet()) {
					EventBTreeElement element = findTreeElement(dependee, true);
					if (element != null && !treeViewer.getChecked(element)) {
						if (!(element.getOriginalElement() instanceof EventBContext)
								&& element.getOriginalElement().getParent() instanceof EventBContext) {
							setCheckedElement(findTreeElement(element.getOriginalElement().getParent(), false), true);
						} else {
							setCheckedElement(element, true);
						}
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

	/**
	 * Finds the tree-internal container for a given editor-internal Event-B element
	 * 
	 * @param element
	 *            The Event-B element for which the tree-internal container is desired
	 * @param expand
	 *            True if tree in editor should be expanded to have the element be visible
	 * @return Tree-internal container element for given Event-B element
	 */
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
					// We pick out the correct subcategory for the element we are searching for
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
				Object[] childrenofEvent = contentProvider.getChildren(treeEvent);
				for (Object child : childrenofEvent) {
					// We pick out the correct subcategory for the searched element
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

	/**
	 * Add categories to internal label to category map
	 * 
	 * @param categories
	 *            Array of categories to add
	 */
	private void addCategories(EventBTreeSubcategory[] categories) {
		// TODO: Need to rework this stuff
		for (EventBTreeSubcategory category : categories) {
			treeCategories.put(category.getLabel(), category);
		}
	}
}
