package eventBSliceAndMerge.ui.editors;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
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
import org.eventb.core.ISeesContext;
import org.eventb.core.IVariable;
import org.rodinp.core.IInternalElement;
import org.rodinp.core.IRodinFile;
import org.rodinp.core.RodinDBException;

import eventBSliceAndMerge.internal.analyzers.EventBSliceSelection;
import eventBSliceAndMerge.internal.analyzers.EventBSlicer;
import eventBSliceAndMerge.internal.datastructures.EventBAction;
import eventBSliceAndMerge.internal.datastructures.EventBAttribute;
import eventBSliceAndMerge.internal.datastructures.EventBCondition;
import eventBSliceAndMerge.internal.datastructures.EventBContext;
import eventBSliceAndMerge.internal.datastructures.EventBElement;
import eventBSliceAndMerge.internal.datastructures.EventBElement.Type;
import eventBSliceAndMerge.internal.datastructures.EventBEvent;
import eventBSliceAndMerge.internal.datastructures.EventBMachine;
import eventBSliceAndMerge.internal.datastructures.EventBVariable;
import eventBSliceAndMerge.ui.jobs.EventBDependencyAnalysisJob;
import eventBSliceAndMerge.ui.util.RodinUtil;
import eventBSliceAndMerge.ui.wizards.MachineCreationWizard;
import eventBSliceAndMerge.ui.wizards.MergeMachineWithPredecessorWizard;
import eventBSliceAndMerge.ui.wizards.POInterpolationWizard;
import eventBSliceAndMerge.ui.wizards.RefinementAnnotatorWizard;

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
	private static final String LABEL_STATUS = "Status";
	private static final String LABEL_SPECIAL = "Note";
	private static final String LABEL_COMMENT = "Comment";

	/* Indexes of the columns */
	private static final int ELEMENT_COLUMN = 0;
	private static final int CONTENT_COLUMN = 1;
	private static final int STATUS_COLUMN = 2;
	private static final int SPECIAL_COLUMN = 3;
	private static final int COMMENT_COLUMN = 4;

	private ContainerCheckedTreeViewer treeViewer = null;

	// Map from category label to internal representation of category.
	// Only intended for use with categories that cannot occur more then once in
	// the machine.
	// Good: Invariant, Variable, Event, Seen Context
	// Bad: Action, Guard, Witness, Parameter, Axiom, Constant, Carrier Set
	private Map<Type, EventBTreeCategoryNode> treeCategories = new HashMap<>();

	private Map<EventBElement, Set<EventBElement>> selectionDependees = new HashMap<>();
	private Map<EventBElement, Set<EventBElement>> selectionDependers = new HashMap<>();

	// Map of internal representation of element to tree-internal wrapper
	private Map<EventBElement, EventBTreeAtomicNode> element2TreeNode = new HashMap<>();

	// Status of nodes
	private HashSet<EventBTreeNode> userChecked = new HashSet<>();
	private HashSet<EventBTreeNode> autoChecked = new HashSet<>();
	private HashSet<EventBTreeNode> alwaysChecked = new HashSet<>();
	private HashSet<EventBTreeNode> noCost = new HashSet<>();

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
				HashSet<EventBTreeNode> nodes = new HashSet<>();
				for (Object category : categories) {
					nodes.add((EventBTreeNode) category);
				}
				setChecked(nodes, true, true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);

			}
		});

		// A button to deselect all elements of the machine opened in the editor
		Button deselectAllButton = new Button(buttonBar, SWT.PUSH);
		deselectAllButton.setText("Reset");
		deselectAllButton.setToolTipText("Reset the selection");
		deselectAllButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				reset();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);

			}
		});

		// A button to select all necessary/preferable elements
		Button selectAllDependenciesButton = new Button(buttonBar, SWT.PUSH);
		selectAllDependenciesButton.setText("Auto Select");
		selectAllDependenciesButton.setToolTipText(
				"Select all elements that the selected elements depend on and that depend only on the selected elements");
		selectAllDependenciesButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				autoSelect(true, parent.getShell());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// A button to select all predicates written with selected variables
		Button selectSliceButton = new Button(buttonBar, SWT.PUSH);
		selectSliceButton.setText("Select Slice");
		selectSliceButton.setToolTipText(
				"Select all predicates written with selected variables");
		selectSliceButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				selectSlice(parent.getShell());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);
			}
		});

		// A button to generate SMT format of a PO in the machine
		Button generateSMTPOButton = new Button(buttonBar, SWT.PUSH);
		generateSMTPOButton.setText("Interpolator");
		generateSMTPOButton.setToolTipText("Generate a PO's interpolation written with selected variables");
		generateSMTPOButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog wizardDialog = new WizardDialog(parent.getShell(),
						new POInterpolationWizard(machine, getSelection()));
				
				wizardDialog.setBlockOnOpen(true);
				wizardDialog.open();
				System.out.println();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				widgetSelected(e);

			}
		});
		
		// A button for event refinement annotation
		Button refinementAnalysisButton = new Button(buttonBar, SWT.PUSH);
		refinementAnalysisButton.setText("Ref Analysis");
		refinementAnalysisButton.setToolTipText("Analyze refinement of events of this machine");
		refinementAnalysisButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				WizardDialog wizardDialog = new WizardDialog(parent.getShell(),
						new RefinementAnnotatorWizard(machine));
				
				wizardDialog.setBlockOnOpen(true);
				wizardDialog.open();
				System.out.println();
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
					StringBuffer message = new StringBuffer("Created sub-refinements each with variable\n");
					DecimalFormat format = new DecimalFormat("000");
					int count = 0;
					for (EventBTreeAtomicNode var : allVariables) {
						if (alwaysChecked.contains(var)) {
							continue;
						}
						setChecked(var, true, true);
						autoSelect(false, parent.getShell());
						try {
							EventBSlicer.createMachineFromSelection(
									machineRoot.getRodinFile().getBareName() + "-slice" + format.format(count) + "-"
											+ var.originalElement.getLabel(),
									getSelection(), rodinFile.getRodinProject(), machineRoot);
							message.append(var.originalElement.getLabel());
							message.append(", ");
						} catch (RodinDBException e1) {
							e1.printStackTrace();
						}
						reset();
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

		String[] titles = { LABEL_ELEMENT, LABEL_CONTENT, LABEL_STATUS, LABEL_SPECIAL, LABEL_COMMENT };
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
		this.treeViewer = new ContainerCheckedTreeViewer(tree);
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
				setChecked((EventBTreeNode) event.getElement(), event.getChecked(), false);
			}
		});

		treeViewer.setContentProvider(new TreeContentProvider(machine));
		treeViewer.setInput(machine);

		initialCheck();
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
	 * Initial check set
	 */
	private void initialCheck() {
		if (alwaysChecked.isEmpty()) {
			// Variables contained both of the abstract machine and the concrete machine.
			// Also, contexts seen by the concrete machine.
			ITreeContentProvider provider = (ITreeContentProvider) treeViewer.getContentProvider();
			HashSet<String> variables = new HashSet<>();
			HashSet<String> contexts = new HashSet<>();
			try {
				IMachineRoot abstractRoot = RodinUtil.getPrecedingMachineRoot(machineRoot);
				if (abstractRoot != null) {
					for (IVariable var : abstractRoot.getVariables()) {
						variables.add(var.getIdentifierString());
					}
					for (ISeesContext context : abstractRoot.getSeesClauses()) {
						contexts.add(context.getSeenContextName());
					}
				}
			} catch (RodinDBException e) {
				e.printStackTrace();
			}
			for (EventBElement elem : element2TreeNode.keySet()) {
				if (elem.getType() == Type.VARIABLE) {
					if (variables.contains(elem.getLabel())) {
						alwaysChecked.add(element2TreeNode.get(elem));
					}
				} else if (elem.getType() == Type.CONTEXT) {
					EventBTreeNode node = element2TreeNode.get(elem);
					alwaysChecked.add(node);
					for (Object obj1 : provider.getChildren(node)) {
						alwaysChecked.add((EventBTreeNode) obj1);
						if (provider.hasChildren(obj1)) {
							for (Object obj2 : provider.getChildren(obj1)) {
								alwaysChecked.add((EventBTreeNode) obj2);
							}
						}
					}
				}
			}
		}
		for (EventBTreeNode node : alwaysChecked) {
			if (node instanceof EventBTreeAtomicNode) {
				expandToShow(((EventBTreeAtomicNode) node).originalElement);
			}
			treeViewer.setChecked(node, true);
			treeViewer.update(node, null);
		}

		updateNoCostElements();
	}

	/**
	 * Reset into the initial check
	 */
	private void reset() {
		for (Object obj : treeViewer.getCheckedElements()) {
			treeViewer.setChecked(obj, false);
		}
		selectionDependees.clear();
		selectionDependers.clear();
		userChecked.clear();
		autoChecked.clear();
		treeViewer.refresh();
		initialCheck();
	}

	/**
	 * Automatically select elements that the currently selected elements depend
	 * on and those that depend only on the currently selected elements
	 * 
	 * @param preview
	 *            Whether to show a preview to ask the user to do the selection
	 * @param shell
	 *            Shell used to show the preview dialog
	 * @return Whether some elements are additionally selected or not
	 */
	private boolean autoSelect(boolean preview, Shell shell) {
		HashSet<EventBTreeNode> toBeAdded;
		boolean ret = false;

		// May repeat until conversion (should be twice otherwise bug?)
		for (int i = 0; i < 5; i++) {
			toBeAdded = new HashSet<>();
			// Select all necessary elements that the selected elements
			// depend on
			for (EventBElement dependee : selectionDependees.keySet()) {
				EventBTreeNode node = element2TreeNode.get(dependee);
				if (!treeViewer.getChecked(node)) {
					toBeAdded.add(node);
				}
			}
			// Also select all no-cost elements that depend only on the
			// selected elements (variables)
			toBeAdded.addAll(noCost);

			if (toBeAdded.isEmpty()) {
				if (i == 0 && preview) {
					MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
					box.setText("Auto Select Error");
					box.setMessage("No elements to automatically select for the current selection");
					box.open();
				}
				break;
			}
			if (preview) {
				MessageBox box = new MessageBox(shell, SWT.OK | SWT.CANCEL);
				box.setText("Auto Select");
				StringBuffer buf = new StringBuffer();
				if (i == 0) {
					buf.append("Select the following elements.\n");
				} else {
					buf.append("Auto select again for the result (selecting the following elements)?\n");
				}
				for (EventBTreeNode node : toBeAdded) {
					buf.append(node.toString());
					buf.append('\n');
				}
				box.setMessage(buf.toString());
				int res = box.open();
				if (res != SWT.OK) {
					break;
				}
			}
			ret = true;
			setChecked(toBeAdded, true, true);
		}
		return ret;
	}

	/**
	 * Automatically select predicates written with selected variables
	 * 
	 * @param shell
	 *            Shell used to show the preview dialog
	 * @return Whether some elements are additionally selected or not
	 */
	private boolean selectSlice(Shell shell) {
		HashSet<EventBTreeNode> toBeAdded;
		HashSet<EventBVariable> selectedVariables;
		HashSet<EventBTreeNode> checkedNodes;

		toBeAdded = new HashSet<>();
		selectedVariables = new HashSet<>();
		checkedNodes = new HashSet<>();
		
		checkedNodes.addAll(autoChecked);
		checkedNodes.addAll(userChecked);
		checkedNodes.addAll(alwaysChecked);

		// Find user-checked variables
		for (EventBTreeNode node : checkedNodes) {
			if (node instanceof EventBTreeAtomicNode) {
				EventBElement element = ((EventBTreeAtomicNode) node).getOriginalElement();
				if (element instanceof EventBVariable) {
					selectedVariables.add((EventBVariable) element);
				}
			}
		}

		Object[] categories = ((ITreeContentProvider) treeViewer.getContentProvider()).getChildren(machine);
		HashSet<EventBTreeNode> nodes = new HashSet<>();
		for (EventBTreeCategoryNode category : (EventBTreeCategoryNode [])categories) {
			String label = category.getLabel();
			if (label.equals(EventBTreeCategoryNode.LABELMAP.get(Type.INVARIANT))) {
				for (EventBTreeAtomicNode node : category.getChildren()) {
					if (isSpecifiableWithVariables(node.getOriginalElement(), selectedVariables)) {
						toBeAdded.add(node);
					}
				}
			} else if (label.equals(EventBTreeCategoryNode.LABELMAP.get(Type.EVENT))) {
				for (EventBTreeAtomicNode node : category.getChildren()) {
					HashSet<EventBElement> elements = new HashSet<>();
					elements.addAll(((EventBEvent) node.getOriginalElement()).getGuards());
					elements.addAll(((EventBEvent) node.getOriginalElement()).getWitnesses());
					elements.addAll(((EventBEvent) node.getOriginalElement()).getActions());
					for (EventBElement element : elements) {
						if (isSpecifiableWithVariables(element, selectedVariables)) {
							toBeAdded.add(element2TreeNode.get(element));
						}
					}
				}
			}
				
			nodes.add((EventBTreeNode) category);
		}

		if (toBeAdded.isEmpty()) {
			MessageBox box = new MessageBox(shell, SWT.ICON_ERROR);
			box.setText("Select Slice Error");
			box.setMessage("No elements to automatically select for the current selection");
			box.open();
			return false;
		}
		setChecked(toBeAdded, true, true);

		return true;
	}
	
	private boolean isSpecifiableWithVariables(EventBElement element, HashSet<EventBVariable> variables) {
		assert (element instanceof EventBCondition || element instanceof EventBAction);

		Set<EventBAttribute> attributes = new HashSet<EventBAttribute>();
		boolean result = true;

		if (element instanceof EventBCondition) {
			attributes = ((EventBCondition) element).getDependees();
		} else if (element instanceof EventBAction) {
			attributes = ((EventBAction) element).getDependees();
		}

		for (EventBAttribute attribute : attributes) {
			if (attribute instanceof EventBVariable
				&& (! variables.contains(attribute))) {
				result = false;
			}
		}
		return result;
	}

	/**
	 * Automated action to check/uncheck element or reaction to check/uncheck
	 * action just done by the user. If you call this method many times for
	 * multiple nodes, you should rather use another setChecked that accepts a
	 * set of nodes.
	 * 
	 * @param node
	 *            Target Node
	 * @param checked
	 *            The status to be made (auto case) or the status just made
	 *            (user case)
	 * @param auto
	 *            Whether this call is for automated action to make the status
	 *            or reaction to the status already made by the user
	 */
	private void setChecked(EventBTreeNode node, boolean checked, boolean auto) {
		HashSet<EventBTreeNode> nodes = new HashSet<>();
		nodes.add(node);
		setChecked(nodes, checked, auto);
	}

	/**
	 * Automated action to check/uncheck element or reaction to check/uncheck
	 * action just done by the user
	 * 
	 * @param nodes
	 *            Target Nodes
	 * @param checked
	 *            The status to be made (auto case) or the status just made
	 *            (user case)
	 * @param auto
	 *            Whether this call is for automated action to make the status
	 *            or reaction to the status already made by the user
	 */
	private void setChecked(Set<EventBTreeNode> nodes, boolean checked, boolean auto) {
		// If there are elements in contexts (e.g., constants), include the
		// whole context
		HashSet<EventBTreeNode> newnodes = new HashSet<>(nodes);
		for (EventBTreeNode node : nodes) {
			if (node instanceof EventBTreeAtomicNode) {
				EventBTreeAtomicNode anode = (EventBTreeAtomicNode) node;
				EventBElement element = anode.originalElement;
				if (EventBElement.isContextELement(element)) {
					newnodes.remove(node);
					newnodes.add(anode.getParentCategory().getParentElement());
				}
			}
		}

		for (EventBTreeNode node : newnodes) {
			setCheckedSub(node, checked, auto);
		}

		// Update information on elements that can be selected with no cost
		updateNoCostElements();

		// Adjust highlighting on parent elements
		correctParentsHighlighted();
	}

	/**
	 * Implementation of the recursive procedure for the setChecked method
	 * 
	 * @param node
	 *            Target Node
	 * @param checked
	 *            The status to be made (auto case) or the status just made
	 *            (user case)
	 * @param auto
	 *            Whether this call is for automated action to make the status
	 *            or reaction to the status already made by the user
	 */
	private void setCheckedSub(EventBTreeNode node, boolean checked, boolean auto) {
		// First dispose the check/uncheck if necessary - in the case of
		// reaction to the user only by undoing it

		// Do not uncheck an always-check element
		if (alwaysChecked.contains(node) && !checked) {
			treeViewer.setChecked(node, true);
			return;
		}

		// Start the update of the node
		if (auto) {
			// First expand (otherwise not checked)
			if (checked && node instanceof EventBTreeAtomicNode) {
				expandToShow(((EventBTreeAtomicNode) node).originalElement);
			}
			treeViewer.setChecked(node, checked);
		}

		// Update the status
		if (node instanceof EventBTreeAtomicNode) {
			if (checked) {
				if (auto) {
					if (!userChecked.contains(node)) {
						autoChecked.add(node);
					}
				} else if (!autoChecked.contains(node)) {
					userChecked.add(node);
				}
			} else {
				autoChecked.remove(node);
				userChecked.remove(node);
			}
		}

		// Update the dependency information and the presentation of each
		// dependee/dependant
		if (node instanceof EventBTreeAtomicNode) {
			EventBElement targetElement = ((EventBTreeAtomicNode) node).originalElement;
			for (EventBElement dependee : getDependees(targetElement)) {
				updateSelectionDependency(targetElement, dependee, true, checked);
			}
			for (EventBElement depender : getDependers(targetElement)) {
				updateSelectionDependency(targetElement, depender, false, checked);
			}
		}

		// Update the presentation that reflects the changes made above
		treeViewer.update(node, null);

		// Recursively update the descendants
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (contentProvider.hasChildren(node)) {
			for (Object child : contentProvider.getChildren(node)) {
				setCheckedSub((EventBTreeNode) child, checked, auto);
			}
		}
	}

	/**
	 * Recursively correct highlight of parent nodes depending on their
	 * descendants
	 * 
	 */
	private void correctParentsHighlighted() {
		Object[] categories = ((ITreeContentProvider) treeViewer.getContentProvider()).getChildren(machine);
		for (Object category : categories) {
			correctParentsHighlightedSub((EventBTreeNode) category);
		}
	}

	/**
	 * Recursively correct highlight of parent nodes depending on their
	 * descendants
	 * 
	 * @param node
	 * @return 0: unchecked, 1: grayed, 2: checked - plus 3 if noCost
	 */
	private int correctParentsHighlightedSub(EventBTreeNode node) {
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
		if (!contentProvider.hasChildren(node)) {
			if (treeViewer.getChecked(node)) {
				if (noCost.contains(node)) {
					return 5;
				} else {
					return 2;
				}
			} else {
				if (noCost.contains(node)) {
					return 3;
				} else {
					return 0;
				}
			}
		}
		boolean hasUnchecked = false;
		boolean hasPartiallyChecked = false;
		boolean hasChecked = false;
		boolean hasCost = false;
		for (Object child : contentProvider.getChildren(node)) {
			int result = correctParentsHighlightedSub((EventBTreeNode) child);
			if (result < 3) {
				hasCost = true;
			}
			switch (result % 3) {
			case 0:
				hasUnchecked = true;
			case 1:
				hasPartiallyChecked = true;
			default:
				hasChecked = true;
			}
		}
		int ret;
		if (!hasUnchecked && !hasPartiallyChecked) {
			ret = 2;
			treeViewer.setChecked(node, true);
		} else if (!hasPartiallyChecked && !hasChecked) {
			ret = 0;
			treeViewer.setChecked(node, false);
		} else {
			ret = 1;
			treeViewer.setGrayed(node, true);
		}
		if (!hasCost) {
			ret = ret + 3;
			noCost.add(node);
		} else {
			noCost.remove(node);
		}
		treeViewer.update(node, null);
		return ret;
	}

	/**
	 * Updates dependers and dependees of currently selected elements
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
	private void updateSelectionDependency(EventBElement targetElement, EventBElement dependecy, boolean dependee,
			boolean increase) {
		Map<EventBElement, Set<EventBElement>> dependencyMap;
		if (dependee) {
			dependencyMap = selectionDependees;
		} else {
			dependencyMap = selectionDependers;
		}
		Set<EventBElement> elements = dependencyMap.get(dependecy);
		if (elements == null) {
			elements = new HashSet<>();
			dependencyMap.put(dependecy, elements);
		}
		if (increase) {
			elements.add(targetElement);
		} else {
			elements.remove(targetElement);
			if (elements.isEmpty()) {
				dependencyMap.remove(dependecy);
			}
		}
		expandToShow(dependecy);
		treeViewer.update(element2TreeNode.get(dependecy), null);
	}

	private void updateNoCostElements() {
		HashSet<EventBTreeNode> oldNoCost = new HashSet<>(noCost);
		noCost.clear();
		for (EventBElement depender : selectionDependers.keySet()) {
			EventBTreeAtomicNode node = element2TreeNode.get(depender);
			boolean keep = true;
			for (EventBElement dependee : getDependees(depender)) {
				if (!getSelection().variables.contains(dependee)) {
					keep = false;
					break;
				}
			}
			if (keep) {
				noCost.add(node);
				treeViewer.update(node, null);
			}
		}
		for (EventBTreeNode node : oldNoCost) {
			if (!noCost.contains(node)) {
				treeViewer.update(node, null);
			}
		}
		for (Object node : treeViewer.getCheckedElements()) {
			noCost.remove(node);
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
		LinkedList<EventBElement> originalElements = new LinkedList<>();
		for (Object node : treeViewer.getCheckedElements()) {
			if (node instanceof EventBTreeCategoryNode) {
				continue;
			}
			originalElements.add(((EventBTreeAtomicNode) node).getOriginalElement());
		}
		return new EventBSliceSelection(originalElements);
	}

	/**
	 * Expand to show the element
	 * 
	 * @param element
	 *            The Event-B element for which the tree-internal container is
	 *            desired
	 */
	private void expandToShow(EventBElement element) {
		Type type = element.getType();
		EventBTreeCategoryNode category = treeCategories.get(type);
		ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();

		// Elements at depth 1
		if (type == Type.INVARIANT || type == Type.VARIABLE || type == Type.CONTEXT || type == Type.EVENT) {
			treeViewer.expandToLevel(category, 1);
			return;
		}

		// Elements at depth 2
		if (type == Type.ACTION || type == Type.GUARD || type == Type.PARAMETER || type == Type.WITNESS) {
			category = treeCategories.get(Type.EVENT);
		} else if (type == Type.AXIOM || type == Type.CARRIER_SET || type == Type.CONSTANT) {
			category = treeCategories.get(Type.CONTEXT);
		} else {
			throw new RuntimeException("Unexpected Execution");
		}

		treeViewer.expandToLevel(category, 1);
		for (EventBTreeAtomicNode node : category.getChildren()) {
			if (node.originalElement instanceof EventBEvent) {
				EventBEvent event = (EventBEvent) node.getOriginalElement();
				if (!event.containsElement(element)) {
					continue;
				}
			} else if (node.originalElement instanceof EventBContext) {
				EventBContext context = (EventBContext) node.getOriginalElement();
				if (!context.containsElement(element)) {
					continue;
				}
			} else {
				throw new RuntimeException("Unexpected Execution");
			}
			treeViewer.expandToLevel(node, 1);
			Object[] children = contentProvider.getChildren(node);
			for (Object child : children) {
				EventBTreeCategoryNode subcategory = (EventBTreeCategoryNode) child;
				if (subcategory.getLabel().equals(EventBTreeCategoryNode.LABELMAP.get(element.getType()))) {
					treeViewer.expandToLevel(subcategory, 1);
					packColumns();
				}
			}
		}
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
			if (element instanceof EventBTreeCategoryNode || element instanceof EventBTreeAtomicNode) {
				if (alwaysChecked.contains(element)) {
					switch (columnIndex) {
					case ELEMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
					case STATUS_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
					case CONTENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_MAGENTA);
					case SPECIAL_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
					case COMMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_GREEN);
					default:
						break;
					}
				} else if (treeViewer.getChecked(element) || checkElementForDependencies(element) != null) {
					// If the element is being highlighted, we give the text a
					// different color for easier
					// readability.
					switch (columnIndex) {
					case ELEMENT_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_CYAN);
					case STATUS_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
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
					case STATUS_COLUMN:
						return Display.getDefault().getSystemColor(SWT.COLOR_BLUE);
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
		 * @return elements by which the given element or at least one of its
		 *         children is being depended on - or null if such elements do
		 *         not exist
		 */
		private Set<EventBElement> checkElementForDependencies(Object element) {
			if (element instanceof EventBTreeAtomicNode) {
				EventBTreeAtomicNode treeElement = (EventBTreeAtomicNode) element;
				if (selectionDependees.containsKey(treeElement.getOriginalElement())
						&& !treeViewer.getChecked(treeElement)) {
					// Element itself depends on another element
					return selectionDependees.get(treeElement.getOriginalElement());
				}
			}
			ITreeContentProvider contentProvider = (ITreeContentProvider) treeViewer.getContentProvider();
			if (!contentProvider.hasChildren(element)) {
				// Element has no children, thus can't have any children that
				// depend on any other element
				return null;
			}
			for (Object child : contentProvider.getChildren(element)) {
				// If even one child returns true, then we should return true;
				if (checkElementForDependencies(child) != null) {
					return new HashSet<EventBElement>();
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			if (!(element instanceof EventBTreeAtomicNode || element instanceof EventBTreeCategoryNode)) {
				return null;
			}
			if (treeViewer.getChecked(element)) {
				// If the element is selected (checked)
				return Display.getDefault().getSystemColor(SWT.COLOR_LIST_SELECTION);
			}
			if (checkElementForDependencies(element) != null) {
				// If the element or one of its children is depended upon by a
				// currently selected element
				return Display.getDefault().getSystemColor(SWT.COLOR_RED);
			}
			if (noCost.contains(element)) {
				return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
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
			Object originalElement = ((EventBTreeAtomicNode) element).getOriginalElement();
			if (!(originalElement instanceof EventBElement)) {
				return null;
			}
			EventBElement eventBElement = (EventBElement) originalElement;
			switch (columnIndex) {
			case ELEMENT_COLUMN:
				return eventBElement.getLabel();
			case STATUS_COLUMN:
				if (alwaysChecked.contains(element)) {
					return "Auto-Selected (Fixed)";
				} else if (userChecked.contains(element)) {
					return "User-Selected";
				} else if (autoChecked.contains(element)) {
					return "Auto-Selected";
				} else if (checkElementForDependencies(element) != null
						&& !checkElementForDependencies(element).isEmpty()) {
					LinkedList<String> tmp = new LinkedList<>();
					for (EventBElement e : checkElementForDependencies(element)) {
						tmp.add(e.getLabelFullPath());
					}
					return "Necessary for " + tmp.toString();
				} else if (noCost.contains(element)) {
					return "Should accompnay Selected Variables";
				}
				return null;
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

		public TreeContentProvider(EventBMachine machine) {
			init(getElements(machine));
		}

		private void init(Object[] elements) {
			for (Object e : elements) {
				if (hasChildren(e)) {
					init(getChildren(e));
				}
			}
		}

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
				children[0] = addCategoryTopLevel(Type.INVARIANT, machine.getInvariants());
				children[1] = addCategoryTopLevel(Type.VARIABLE, machine.getVariables());
				children[2] = addCategoryTopLevel(Type.EVENT, machine.getEvents());
				children[3] = addCategoryTopLevel(Type.CONTEXT, machine.getSeenContexts());
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
						ArrayList<EventBTreeCategoryNode> children = new ArrayList<>();
						if (!originalElement.getParameters().isEmpty()) {
							children.add(
									addCategoryInnerLevel(Type.PARAMETER, parent, originalElement.getParameters()));
						}
						if (!originalElement.getWitnesses().isEmpty()) {
							children.add(addCategoryInnerLevel(Type.WITNESS, parent, originalElement.getWitnesses()));
						}
						if (!originalElement.getGuards().isEmpty()) {
							children.add(addCategoryInnerLevel(Type.GUARD, parent, originalElement.getGuards()));
						}
						if (!originalElement.getActions().isEmpty()) {
							children.add(addCategoryInnerLevel(Type.ACTION, parent, originalElement.getActions()));
						}
						EventBTreeCategoryNode[] nodes = new EventBTreeCategoryNode[children.size()];
						nodes = children.toArray(nodes);
						eventSubcategories.put(originalElement, nodes);
					}
					return eventSubcategories.get(originalElement);
				}
				if (((EventBTreeAtomicNode) parentElement).getOriginalElement() instanceof EventBContext) {
					EventBContext originalElement = (EventBContext) ((EventBTreeAtomicNode) parentElement)
							.getOriginalElement();
					EventBTreeAtomicNode parent = (EventBTreeAtomicNode) parentElement;
					if (!contextSubcategories.containsKey(originalElement)) {
						ArrayList<EventBTreeCategoryNode> children = new ArrayList<>();
						if (!originalElement.getAxioms().isEmpty()) {
							children.add(addCategoryInnerLevel(Type.AXIOM, parent, originalElement.getAxioms()));
						}
						if (!originalElement.getConstants().isEmpty()) {
							children.add(addCategoryInnerLevel(Type.CONSTANT, parent, originalElement.getConstants()));
						}
						if (!originalElement.getCarrierSets().isEmpty()) {
							children.add(
									addCategoryInnerLevel(Type.CARRIER_SET, parent, originalElement.getCarrierSets()));
						}
						EventBTreeCategoryNode[] nodes = new EventBTreeCategoryNode[children.size()];
						nodes = children.toArray(nodes);
						contextSubcategories.put(originalElement, nodes);
					}
					return contextSubcategories.get(originalElement);
				}
			}
			return null;
		}

		private EventBTreeCategoryNode addCategoryTopLevel(Type type, List<? extends EventBElement> children) {
			EventBTreeCategoryNode category = new EventBTreeCategoryNode(type, null, children, element2TreeNode, this);
			treeCategories.put(type, category);
			return category;
		}

		private EventBTreeCategoryNode addCategoryInnerLevel(Type type, EventBTreeAtomicNode parent,
				List<? extends EventBElement> children) {
			EventBTreeCategoryNode category = new EventBTreeCategoryNode(type, parent, children, element2TreeNode,
					this);
			treeCategories.put(type, category);
			return category;
		}

	}

}
