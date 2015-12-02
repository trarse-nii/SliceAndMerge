package eventBRefinementSlicer.ui.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableFontProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import eventBRefinementSlicer.internal.datastructures.EventBAction;
import eventBRefinementSlicer.internal.datastructures.EventBCondition;
import eventBRefinementSlicer.internal.datastructures.EventBContext;
import eventBRefinementSlicer.internal.datastructures.EventBElement;
import eventBRefinementSlicer.internal.datastructures.EventBEvent;
import eventBRefinementSlicer.internal.datastructures.EventBTypes;

/**
 * 
 * @author Aivar Kripsaar
 *
 */
public class PreviewTableViewerFactory {

	private static final String LABEL_ELEMENT = "Element";
	private static final String LABEL_CONTENT = "Content";
	private static final String LABEL_SPECIAL = "Special";
	private static final String LABEL_COMMENT = "Comment";

	private static final int ELEMENT_COLUMN = 0;
	private static final int CONTENT_COLUMN = 1;
	private static final int SPECIAL_COLUMN = 2;
	private static final int COMMENT_COLUMN = 3;

	private static String[] titles = { LABEL_ELEMENT, LABEL_CONTENT, LABEL_SPECIAL, LABEL_COMMENT };

	private static final PreviewTableViewerFactory instance = new PreviewTableViewerFactory();

	private PreviewTableViewerFactory() {
		// Intentionally left empty
	}

	public static PreviewTableViewerFactory getInstance() {
		return instance;
	}

	private Table createTable(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.HIDE_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(gridData);

		TableColumn column;

		for (String title : titles) {
			column = new TableColumn(table, SWT.NONE);
			column.setText(title);
			column.setResizable(false);
		}

		packColumns(table);

		// table.addControlListener(new ControlAdapter() {
		// @Override
		// public void controlResized(ControlEvent e) {
		// packColumns(table);
		// }
		// });

		return table;
	}

	public TableViewer createTableViewer(Composite parent, Object input) {

		Table table = createTable(parent);

		TableViewer tableViewer = new TableViewer(table);
		tableViewer.setColumnProperties(titles);
		tableViewer.setUseHashlookup(true);

		tableViewer.setLabelProvider(new LabelProvider());

		tableViewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// Intentionally left empty
			}

			@Override
			public void dispose() {
				// Intentionally left empty
			}

			@Override
			public Object[] getElements(Object inputElement) {
				assert inputElement instanceof EventBElement;
				EventBElement element = (EventBElement) inputElement;
				switch (element.getType()) {
				case EventBTypes.CONTEXT:
					EventBContext context = (EventBContext) element;
					List<EventBElement> contextElements = new ArrayList<>();
					contextElements.addAll(context.getCarrierSets());
					contextElements.addAll(context.getAxioms());
					contextElements.addAll(context.getConstants());
					return contextElements.toArray();
					// TODO: Add more cases according to need.
				default:
					break;
				}
				return null;
			}
		});

		tableViewer.setInput(input);

		packColumns(table);

		return tableViewer;
	}

	private void packColumns(Table table) {
		int columnsWidth = 0;
		for (TableColumn column : table.getColumns()) {
			column.pack();
		}
		for (TableColumn column : table.getColumns()) {
			columnsWidth += column.getWidth();
		}
		TableColumn lastColumn = table.getColumn(table.getColumnCount() - 1);
		columnsWidth -= lastColumn.getWidth();

		Rectangle area = table.getClientArea();
		int width = area.width;

		if (lastColumn.getWidth() < width - columnsWidth) {
			lastColumn.setWidth(width - columnsWidth);
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
			// Intentionally left empty
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

			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			// Intentionally left empty
			return null;
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			// Intentionally left empty
			return null;
		}

		@Override
		public String getColumnText(Object element, int columnIndex) {

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

}
