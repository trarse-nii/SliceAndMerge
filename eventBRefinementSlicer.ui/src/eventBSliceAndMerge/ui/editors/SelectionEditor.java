package eventBSliceAndMerge.ui.editors;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eventb.core.IMachineRoot;
import org.eventb.core.IRefinesMachine;
import org.eventb.core.IVariable;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.analyzers.EventBSliceSelection;
import eventBSliceAndMerge.internal.analyzers.EventBSlicer;
import eventBSliceAndMerge.internal.datastructures.EventBAction;
import eventBSliceAndMerge.internal.datastructures.EventBAxiom;
import eventBSliceAndMerge.internal.datastructures.EventBCarrierSet;
import eventBSliceAndMerge.internal.datastructures.EventBCondition;
import eventBSliceAndMerge.internal.datastructures.EventBConstant;
import eventBSliceAndMerge.internal.datastructures.EventBContext;
import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBElement.Type;
import eventBSliceAndMerge.internal.datastructures.EventBEvent;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;
import eventBSliceAndMerge.ui.jobs.EventBDependencyAnalysisJob;
import eventBSliceAndMerge.ui.util.RodinUtil;
import eventBSliceAndMerge.ui.wizards.MachineCreationWizard;
import eventBSliceAndMerge.ui.wizards.MergeMachineWithPredecessorWizard;

/**
 * The editor in charge of selecting which parts of an Event-B machine to use in
 * the slicing of refinements
 * 
 * @author Aivar Kripsaar
 *
 */

public class SelectionEditor extends EditorPart {

	/* Labels used in the column titles of the table-like view */
	private static final String LABEL_ELEMENT = "Element";
	private static final String LABEL_CONTENT = "Content";
	private static final String LABEL_SPECIAL = "Special";
	private static final String LABEL_COMMENT = "Comment";

	/* Indexes of the columns */
	private static final int ELEMENT_COLUMN = 0;
	private static final int CONTENT_COLUMN = 1;
	private static final int SPECIAL_COLUMN = 2;
	private static final int COMMENT_COLUMN = 3;

	// Map from category label to internal representation of category.
	// Only intended for use with categories that cannot occur more then once in
	// the machine.
	// Good: Invariant, Variable, Event, Seen Context
	// Bad: Action, Guard, Witness, Parameter, Axiom, Constant, Carrier Set
	private Map<Type, EventBTreeCategoryNode> treeCategories = new HashMap<>();

	private Map<EventBElement, Integer> selectionDependees = new HashMap<>();
	private Map<EventBElement, Integer> selectionDependers = new HashMap<>();

	// Map of internal representation of element to tree-internal wrapper
	private Map<EventBElement, EventBTreeAtomicNode> elementToTreeElementMap = new HashMap<>();

	private ContainerCheckedTreeViewer treeViewer = null;

	// We require a duplicate of the checked state because of the limitations of the TreeViewer API
	private Map<Object, Boolean> selectionMap = new HashMap<>();
	
	/* Target file/machine objects */
	private IRodinFile rodinFile;
	private IMachineRoot machineRoot;
	private EventBMachine machine;

	/* ----- Non-trivial implementation of EditorPart methods ----- */

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		rodinFile = RodinUtil.getRodinFileFromInput(input);
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

	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		parent.setLayout(layout);
		createButtons(parent);
		createTree(parent);
		setPartName(machineRoot.getComponentName());
	}

	/* ----- Trivial implementation of EditorPart methods ----- */

	@Override
	public void setFocus() {
		// Intentionally left empty
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/* ----- Methods for functions via buttons ----- */

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
		newMachineButton.setToolTipText(
				"Create a new Event-B Machine based on the current selection of elements in this editor");
		newMachineButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog wizardDialog = new WizardDialog(parent.getShell(),
						new MachineCreationWizard(rodinFile.getRodinProject(), machineRoot, getSelection()));

				wizardDialog.setBlockOnOpen(true);
				wizardDialog.open();
				System.out.println();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// We add an extra container for the next button to allow a tooltip when
		// the button is disabled
		final Composite mergeButtonContainer = new Composite(buttonBar, SWT.NONE);
		mergeButtonContainer.setLayout(new FillLayout());

		// A button to merge the currently opened machine with its direct
		// ancestor (i.e. the machine this one
		// refines)
		Button mergeMachinesButton = new Button(mergeButtonContainer, SWT.PUSH);
		mergeMachinesButton.setText("Merge With Direct Predecessor");
		mergeMachinesButton.setToolTipText("Merge this machine with the one it refines, producing a new machine");
		try {
			// If current machine has no machine that it refines, we disable the
			// button and display a tooltip
			// explaining why the button is disabled
			IRefinesMachine refinesMachines[] = machineRoot.getRefinesClauses();
			if (refinesMachines.length > 0) {
				mergeMachinesButton.setEnabled(true);
				mergeButtonContainer.setToolTipText("");
			} else {
				mergeMachinesButton.setEnabled(false);
				mergeButtonContainer
						.setToolTipText("Nothing to merge with. Current machine does not refine any other machine.");
			}
		} catch (RodinDBException e) {
			e.printStackTrace();
		}
		mergeMachinesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog wizardDialog = new WizardDialog(parent.getShell(),
						new MergeMachineWithPredecessorWizard(rodinFile.getRodinProject(), machineRoot));

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
		selectAllButton.setToolTipText("Select all elements");
		selectAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				Object[] categories = ((ITreeContentProvider) treeViewer.getContentProvider()).getChildren(machine);
				for (Object category : categories) {
					setCheckedElement((EventBTreeCategoryNode)category, true);
				}
				treeViewer.refresh();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);

			}
		});

		// A button to select all elements that currently selected elements
		// depend on
		Button selectAllDependenciesButton = new Button(buttonBar, SWT.PUSH);
		selectAllDependenciesButton.setText("Select All Dependencies");
		selectAllDependenciesButton
				.setToolTipText("Select all elements that the currently selected elements depend on");
		selectAllDependenciesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				for (EventBElement dependee : selectionDependees.keySet()) {
					EventBTreeAtomicNode element = findTreeElement(dependee, true);
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

		// A button for debug mode
		if (System.getProperty("debug") != null) {
			Button debugButton = new Button(buttonBar, SWT.PUSH);
			debugButton.setText("Run test mode");
			debugButton.setToolTipText("Run test mode");
			debugButton.addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					Object[] categories = ((ITreeContentProvider) treeViewer.getContentProvider()).getChildren(machine);
					EventBTreeCategoryNode category;
					EventBTreeAtomicNode[] allVariables = null;
					for (Object c : categories) {
						category = (EventBTreeCategoryNode) c;
						if (category.getLabel().equals("Variables")) {
							allVariables = category.getChildren();
							break;
						}
					}
					// Generate all sub-refinements by specifying one variable
					LinkedList<EventBElement> selection = new LinkedList<EventBElement>();
					StringBuffer message = new StringBuffer("Created sub-refinements each with variable\n");
					DecimalFormat format = new DecimalFormat("000");
					int count = 0;
					for (EventBTreeAtomicNode var : allVariables) {
						EventBElement elem = var.originalElement;
						IMachineRoot precedingMachineRoot = RodinUtil.getPrecedingMachineRoot(machineRoot);
						boolean skip = false;
						try {
							for (IVariable v : precedingMachineRoot.getVariables()) {
								if (v.getIdentifierString().equals(elem.getLabel())) {
									skip = true;
									break;
								}
							}
						} catch (RodinDBException e1) {
							e1.printStackTrace();
						}
						if (skip) {
							continue;
						}
						selection.add(elem);
						selection.addAll(getDependees(elem));
						try {
							EventBSlicer.createMachineFromSelection(
									machineRoot.getRodinFile().getBareName() + "-slice" + format.format(count) + "-"
											+ elem.getLabel(),
									new EventBSliceSelection(selection), rodinFile.getRodinProject(), machineRoot);
							message.append(elem.getLabel());
							message.append(", ");
						} catch (RodinDBException e1) {
							e1.printStackTrace();
						}
						selection.clear();
						count++;
					}
					message.delete(message.length() - 2, message.length());
					MessageDialog.open(MessageDialog.INFORMATION, treeViewer.getControl().getShell(), "Test Mode",
							message.toString(), SWT.NONE);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
			});

		}

	}

	/* ----- Methods for manipulating on the tree view UI ----- */

	/**
	 * Creates a tree UI element, which is the main body of the selection editor
	 * 
	 * @param parent
	 *            The parent of the tree
	 */
	private void createTree(Composite parent) {
		// We create a tree that allows for multiple selections and including
		// checkbox behavior
		Tree tree = new Tree(parent,
				SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL);
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

		// A workaround to allow better highlighting of elements when hovering
		// or selecting them
		// Without this, the highlight colors set by the Tree Viewer are
		// removed, simply replaced by the
		// standard selection & highlight colors of the OS
		tree.addListener(SWT.EraseItem, new Listener() {
			@Override
			public void handleEvent(Event event) {
				GC gc = event.gc;
				TreeItem item = (TreeItem) event.item;
				int width = tree.getClientArea().x + tree.getClientArea().width - event.x;
				// Sets background and foreground color to the ones set by the
				// Tree Viewer
				gc.setBackground(item.getBackground(event.index));
				gc.setForeground(item.getForeground(event.index));
				gc.fillRectangle(event.x, event.y, width, event.height);
			}
		});

		packColumns();
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

		// Any time the tree is either expanded or collapsed, the columns must
		// once again be packed
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

		// Any time an element is checked or unchecked, we use a method to take
		// care of additional
		// highlighting and dependency related changes
		treeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				EventBTreeNode node = (EventBTreeNode) event.getElement();
				// If the element is part of a context, we wish to disable
				// selection, because only whole
				// contexts should be selectable
				// We have to hack this in by undoing the checking of the
				// checkbox manually
				if (event.getElement() instanceof EventBTreeAtomicNode) {
					EventBElement element = ((EventBTreeAtomicNode) event.getElement()).getOriginalElement();
					if ((element instanceof EventBAxiom || element instanceof EventBConstant
							|| element instanceof EventBCarrierSet)) {
						treeViewer.setSubtreeChecked(event.getElement(), !event.getChecked());
						// Correct checked state of parents
						correctParentsChecked(node);
						return;
					}
				} else if (event.getElement() instanceof EventBTreeCategoryNode) {
					EventBTreeCategoryNode treeCategory = (EventBTreeCategoryNode) event.getElement();
					if (treeCategory.getParentElement() != null
							&& treeCategory.getParentElement().getOriginalElement() instanceof EventBContext) {
						treeViewer.setSubtreeChecked(event.getElement(), !event.getChecked());
						// Correct checked state of parents
						correctParentsChecked(node);
						return;
					}
				}
				updateSelectionDependenciesForSubtree((EventBTreeNode)event.getElement(), event.getChecked());
				correctParentsChecked(node);
			}
		});

		treeViewer.setContentProvider(new TreeContentProvider());
		treeViewer.setInput(machine);
		this.treeViewer = treeViewer;
	}

	/**
	 * Adjusts size of columns to fit visible content. Also resizes last column
	 * to fill remainder of space.
	 */
	private void packColumns() {
		Tree tree = treeViewer.getTree();
		int columnsWidth = 0;
		for (TreeColumn column : tree.getColumns()) {
			column.pack();
		}
		// After packing all columns, we manually change the size of the last
		// column
		for (TreeColumn column : tree.getColumns()) {
			columnsWidth += column.getWidth();
		}
		TreeColumn lastColumn = tree.getColumn(tree.getColumnCount() - 1);
		columnsWidth -= lastColumn.getWidth();

		Rectangle area = tree.getClientArea();
		int width = area.width;

		// We set the width of the last column to be the width of the tree area
		// minus the width of every other
		// column added up, filling up the rest of the area
		if (lastColumn.getWidth() < width - columnsWidth) {
			lastColumn.setWidth(width - columnsWidth);
		}
	}

	/* ----- Methods for managing selection considering dependencies ----- */

	/**
	 * Sets the checked status for the given element and its children. Also
	 * updates the dependency-related highlighting as necessary.
	 * 
	 * @param element
	 *            The element being checked or unchecked
	 * @param checked
	 *            Desired checked state
	 */
	private void setCheckedElement(EventBTreeNode element, boolean checked) {
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
	 * Updates the local dependency maps to account for the change of the
	 * checked state of an element
	 * 
	 * @param element
	 *            Element that has had its checked state changed
	 * @param checked
	 *            New checked status of element
	 */
	private void updateSelectionDependenciesForElement(EventBTreeNode element, boolean checked) {
		if (element instanceof EventBTreeAtomicNode) {
			for (EventBElement dependee : getDependees(((EventBTreeAtomicNode) element).originalElement)) {
				updateSelectionDependency(dependee, true, checked);
			}
			for (EventBElement depender : getDependers(((EventBTreeAtomicNode) element).originalElement)) {
				updateSelectionDependency(depender, false, checked);
			}
		}
		treeViewer.update(element, null);
	}

	/**
	 * Updates the local dependency maps to account for the change of the
	 * checked state of an element and its subtree
	 * 
	 * @param element
	 *            Element that has had its checked state changed
	 * @param checked
	 *            New checked status of element
	 */
	private void updateSelectionDependenciesForSubtree(EventBTreeNode element, boolean checked) {
		if (!(checked ^ selectionMap.getOrDefault(element, false))) {
			// If checked state of this element doesn't change, nothing else
			// needs to be done.
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
			treeViewer.setChecked(child, checked);
			updateSelectionDependenciesForSubtree((EventBTreeNode)child, checked);
		}
	}

	/**
	 * Gets all children of given parent element and sets their checked status
	 * as desired.
	 * 
	 * @param parent
	 *            Parent element of children we need to change
	 * @param checked
	 *            Desired checked state for children of parent
	 */
	private void setChildrenChecked(EventBTreeNode parent, boolean checked) {
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (!contentProvider.hasChildren(parent)) {
			return;
		}
		for (Object child : contentProvider.getChildren(parent)) {
			// Update a child only if its checked status changes
			if (checked ^ selectionMap.getOrDefault(child, false)) {
				setCheckedElement((EventBTreeNode)child, checked);
			}
		}
	}

	/**
	 * Corrects the parents's checked (selection) state based on child's changed
	 * checked status
	 * 
	 * @param element
	 *            The element which has had its checked status changed.
	 */
	private void correctParentsChecked(EventBTreeNode element) {
		EventBTreeNode parent = null;
		if (element instanceof EventBTreeAtomicNode) {
			EventBTreeAtomicNode treeElement = (EventBTreeAtomicNode) element;
			parent = treeElement.getParentCategory();
			
		} else {
			assert element instanceof EventBTreeCategoryNode;
			EventBTreeCategoryNode treeCategory = (EventBTreeCategoryNode) element;
			parent = treeCategory.getParentElement();
		}
		if (parent == null) {
			return;
		}
		correctElementChecked(parent);
	}

	/**
	 * Checks element's current checked state based on checked states of
	 * children and corrects if necessary.
	 * 
	 * @param element
	 *            Tree element which needs to be checked and corrected
	 */
	private void correctElementChecked(EventBTreeNode element) {
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
	 * Updates maps counting number of dependers and dependees that are
	 * currently selected (checked) and causes update so that dependencies are
	 * properly highlighted.
	 * 
	 * @param dependecy
	 *            Element for which we update dependency counts.
	 * @param dependee
	 *            Boolean to signify whether the element is being depended on,
	 *            or if it is depending on another element itself.
	 * @param increase
	 *            True if dependency count needs to be increased (when another
	 *            dependency partner of the given element has been selected).
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
	 * Updates an element as well as all of its parents in the editor's tree
	 * viewer.
	 * 
	 * @param element
	 *            Element to update
	 */
	private void updateElement(EventBTreeNode element) {
		if (element == null) {
			return;
		}
		treeViewer.update(element, null);
		if (element instanceof EventBTreeAtomicNode) {
			EventBTreeAtomicNode treeElement = (EventBTreeAtomicNode) element;
			if (treeElement.getParentCategory() != null) {
				updateElement(treeElement.getParentCategory());
			}
		}
		if (element instanceof EventBTreeCategoryNode) {
			EventBTreeCategoryNode treeCategory = (EventBTreeCategoryNode) element;
			if (treeCategory.getParentElement() != null) {
				updateElement(treeCategory.getParentElement());
			}
		}
	}

	/*
	 * ----- Methods for transformation between UI representations and internal
	 * representations -----
	 */

	/**
	 * Get internal representations from the selected UI elements
	 * 
	 * @return
	 */
	private EventBSliceSelection getSelection() {
		Object[] selectedElements = treeViewer.getCheckedElements();
		LinkedList<EventBElement> originalElements = new LinkedList<>();
		for (Object checkedElement : selectedElements) {
			if (checkedElement instanceof EventBTreeCategoryNode) {
				continue;
			}
			originalElements.add(((EventBTreeAtomicNode) checkedElement).getOriginalElement());
		}
		return new EventBSliceSelection(originalElements);
	}

	/**
	 * Finds the tree-internal container for a given editor-internal Event-B
	 * element
	 * 
	 * @param element
	 *            The Event-B element for which the tree-internal container is
	 *            desired
	 * @param expand
	 *            True if tree in editor should be expanded to have the element
	 *            be visible
	 * @return Tree-internal container element for given Event-B element
	 */
	private EventBTreeAtomicNode findTreeElement(EventBElement element, boolean expand) {
		EventBTreeAtomicNode treeElement = null;
		if (!expand && elementToTreeElementMap.containsKey(element)) {
			return elementToTreeElementMap.get(element);
		}

		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		Type type = element.getType();
		EventBTreeCategoryNode category = treeCategories.get(type);
		// TODO: by this logic only findable the first element when multiple
		// elements exist for a category?

		if (type == Type.INVARIANT || type == Type.VARIABLE || type == Type.CONTEXT || type == Type.EVENT) {
			treeViewer.expandToLevel(category, 1);
			return elementToTreeElementMap.get(element);
		}

		if (type == Type.CARRIER_SET) {
			for (EventBTreeAtomicNode treeContext : category.getChildren()) {
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
					// We pick out the correct subcategory for the element we
					// are searching for
					assert child instanceof EventBTreeCategoryNode;
					EventBTreeCategoryNode subcategory = (EventBTreeCategoryNode) child;
					String label = "";
					if (element.getType().equals(Type.CONSTANT)) {
						label = "Constants";
					} else if (element.getType().equals(Type.AXIOM)) {
						label = "Axioms";
					} else if (element.getType().equals(Type.CARRIER_SET)) {
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

		if (type == Type.ACTION) {
			for (EventBTreeAtomicNode treeEvent : category.getChildren()) {
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
					// We pick out the correct subcategory for the searched
					// element
					assert child instanceof EventBTreeCategoryNode;
					EventBTreeCategoryNode subcategory = (EventBTreeCategoryNode) child;
					String label = "";
					if (element.getType().equals(Type.GUARD)) {
						label = "Guards";
					} else if (element.getType().equals(Type.ACTION)) {
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

		return treeElement;
	}

	/* ----- Auxiliary methods ----- */

	/**
	 * Fetches all elements a given element depends on
	 * 
	 * @param element
	 * @return
	 */
	private Set<EventBElement> getDependees(EventBElement element) {
		return machine.getDependencies().getDependeesForElement(element);
	}

	/**
	 * Fetches all elements that depend on a given element
	 * 
	 * @param element
	 * @return
	 */
	private Set<EventBElement> getDependers(EventBElement element) {
		return machine.getDependencies().getDependersForElement(element);
	}

	/* ----- Internal classes ----- */

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
			if (element instanceof EventBTreeCategoryNode) {
				// TODO: Add color coding for categories
				return null;
			}
			if (element instanceof EventBTreeAtomicNode) {
				if (treeViewer.getChecked(element) || checkElementForDependencies(element)) {
					// If the element is being highlighted, we give the text a
					// different color for easier
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

		/**
		 * Checks the given element to see if it or its children are depended on
		 * by any other element.
		 * 
		 * @param element
		 *            The elements for which dependency needs to be checked.
		 * @return True if the given element or at least one of its children is
		 *         being depended on by another element
		 */
		private Boolean checkElementForDependencies(Object element) {
			if (element instanceof EventBTreeAtomicNode) {
				EventBTreeAtomicNode treeElement = (EventBTreeAtomicNode) element;
				if (selectionDependees.containsKey(treeElement.getOriginalElement())
						&& !treeViewer.getChecked(treeElement)) {
					// Element itself depends on another element
					return true;
				}
			}
			ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
			if (!contentProvider.hasChildren(element)) {
				// Element has no children, thus can't have any children that
				// depend on any other element
				return false;
			}
			for (Object child : contentProvider.getChildren(element)) {
				// If even one child returns true, then we should return true;
				if (checkElementForDependencies(child)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (!(element instanceof EventBTreeAtomicNode || element instanceof EventBTreeCategoryNode)) {
				return null;
			}
			if (treeViewer == null) {
				return null;
			}
			if (checkElementForDependencies(element)) {
				// If the element or one of its children is depended upon by a
				// currently selected element
				return Display.getDefault().getSystemColor(SWT.COLOR_RED);
			}
			if (treeViewer.getChecked(element)) {
				// If the element is selected (checked)
				return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			}

			if (element instanceof EventBTreeCategoryNode) {
				// We don't deal with further cases for categories
				return null;
			}

			if (selectionDependers.containsKey(((EventBTreeAtomicNode) element).getOriginalElement())) {
				// If the element depends on a currently selected element
				return Display.getDefault().getSystemColor(SWT.COLOR_CYAN);
			}
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// Intentionally returns null
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (element instanceof EventBTreeCategoryNode) {
				// Categories only need their labels displayed
				if (columnIndex == ELEMENT_COLUMN) {
					return ((EventBTreeCategoryNode) element).getLabel();
				}
			}
			if (!(element instanceof EventBTreeAtomicNode)) {
				return null;
			}
			element = ((EventBTreeAtomicNode) element).getOriginalElement();
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

	/**
	 * 
	 * @author Aivar Kripsaar
	 *
	 */
	class TreeContentProvider implements ITreeContentProvider {

		private EventBTreeCategoryNode[] treeRootCategories;
		private Map<EventBEvent, EventBTreeCategoryNode[]> eventSubcategories = new HashMap<>();
		private Map<EventBContext, EventBTreeCategoryNode[]> contextSubcategories = new HashMap<>();

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
			if (element instanceof EventBTreeCategoryNode) {
				return ((EventBTreeCategoryNode) element).getChildren().length > 0;
			}
			if (element instanceof EventBTreeAtomicNode) {
				if (((EventBTreeAtomicNode) element).getOriginalElement() instanceof EventBEvent) {
					EventBEvent event = (EventBEvent) ((EventBTreeAtomicNode) element).getOriginalElement();
					return !(event.isEmpty());
				}
				if (((EventBTreeAtomicNode) element).getOriginalElement() instanceof EventBContext) {
					EventBContext context = (EventBContext) ((EventBTreeAtomicNode) element).getOriginalElement();
					return !(context.isEmpty());
				}
			}
			return false;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof EventBTreeAtomicNode) {
				return ((EventBTreeAtomicNode) element).getParentCategory();
			}
			return null;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			EventBMachine machine = (EventBMachine) inputElement;
			if (treeRootCategories == null) {
				EventBTreeCategoryNode[] children = new EventBTreeCategoryNode[4];
				children[0] = addCategory(Type.INVARIANT, machine.getInvariants(), elementToTreeElementMap);
				children[1] = addCategory(Type.VARIABLE, machine.getVariables(), elementToTreeElementMap);
				children[2] = addCategory(Type.EVENT, machine.getEvents(), elementToTreeElementMap);
				children[3] = addCategory(Type.CONTEXT, machine.getSeenContexts(), elementToTreeElementMap);
				treeRootCategories = children;
			}
			return treeRootCategories;
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if ((parentElement instanceof EventBMachine)) {
				return getElements(parentElement);
			}
			if (parentElement instanceof EventBTreeCategoryNode) {
				return ((EventBTreeCategoryNode) parentElement).getChildren();
			}
			if (parentElement instanceof EventBTreeAtomicNode) {
				if (!(((EventBTreeAtomicNode) parentElement).getOriginalElement() instanceof EventBEvent
						|| ((EventBTreeAtomicNode) parentElement).getOriginalElement() instanceof EventBContext)) {
					return null;
				}
				if (((EventBTreeAtomicNode) parentElement).getOriginalElement() instanceof EventBEvent) {
					EventBEvent originalElement = (EventBEvent) ((EventBTreeAtomicNode) parentElement)
							.getOriginalElement();
					EventBTreeAtomicNode parent = (EventBTreeAtomicNode) parentElement;
					if (!eventSubcategories.containsKey(originalElement)) {
						EventBTreeCategoryNode[] children = new EventBTreeCategoryNode[4];
						children[0] = addCategory(Type.PARAMETER, parent, originalElement.getParameters(),
								elementToTreeElementMap);
						children[1] = addCategory(Type.WITNESS, parent, originalElement.getWitnesses(),
								elementToTreeElementMap);
						children[2] = addCategory(Type.GUARD, parent, originalElement.getGuards(),
								elementToTreeElementMap);
						children[3] = addCategory(Type.ACTION, parent, originalElement.getActions(),
								elementToTreeElementMap);
						eventSubcategories.put(originalElement, children);
					}
					return eventSubcategories.get(originalElement);
				}
				if (((EventBTreeAtomicNode) parentElement).getOriginalElement() instanceof EventBContext) {
					EventBContext originalElement = (EventBContext) ((EventBTreeAtomicNode) parentElement)
							.getOriginalElement();
					EventBTreeAtomicNode parent = (EventBTreeAtomicNode) parentElement;
					if (!contextSubcategories.containsKey(originalElement)) {
						EventBTreeCategoryNode[] children = new EventBTreeCategoryNode[3];
						children[0] = addCategory(Type.AXIOM, parent, originalElement.getAxioms(),
								elementToTreeElementMap);
						children[1] = addCategory(Type.CONSTANT, parent, originalElement.getConstants(),
								elementToTreeElementMap);
						children[2] = addCategory(Type.CARRIER_SET, parent, originalElement.getCarrierSets(),
								elementToTreeElementMap);
						contextSubcategories.put(originalElement, children);
					}
					return contextSubcategories.get(originalElement);
				}
			}
			return null;
		}

		private EventBTreeCategoryNode addCategory(Type type, List<? extends EventBElement> children,
				Map<EventBElement, EventBTreeAtomicNode> elementToTreeElementMap) {
			EventBTreeCategoryNode category = new EventBTreeCategoryNode(type, null, children, elementToTreeElementMap,
					this);
			treeCategories.put(type, category);
			return category;
		}

		private EventBTreeCategoryNode addCategory(Type type, EventBTreeAtomicNode parent,
				List<? extends EventBElement> children, Map<EventBElement, EventBTreeAtomicNode> elementToTreeElementMap) {
			EventBTreeCategoryNode category = new EventBTreeCategoryNode(type, parent, children, elementToTreeElementMap,
					this);
			treeCategories.put(type, category);
			return category;
		}

	}

}
