/*
 * BoundTable.java
 *
 * Created on July 24, 2007, 5:30 PM
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.totsp.gwittir.client.ui.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ScrollListener;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.Widget;

import com.totsp.gwittir.client.beans.Bindable;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Introspector;
import com.totsp.gwittir.client.beans.Property;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.Button;
import com.totsp.gwittir.client.ui.Label;
import com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory;
import com.totsp.gwittir.client.util.ListSorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;


//
// TODO Going backwards in single select +insert  is wrong.
//

/**
 * This is an option-rich table for use with objects implementing the Bindable interfaces.
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet" Cooper</a>
 * @see com.totsp.gwittir.client.beans.Bindable
 */
public class BoundTable extends AbstractBoundWidget implements HasChunks {
    /**
     * A placholder for no mask options (0)
     */
    public static final int NONE_MASK = 0;

    /**
     * Renderer the table inside a scroll panel.
     *
     * <p>If the table has a DataProvider, it will use the "Google Reader"
     * get-next-chunk-on-max-scroll operation.</p>
     */
    public static final int SCROLL_MASK = 1;

    /**
     * Renderers a heading row on the table using the labels on the Column objects.
     */
    public static final int HEADER_MASK = 2;

    /**
     * Lets the user have multiple rows in the "selected" state at a time.
     */
    public static final int MULTIROWSELECT_MASK = 4;

    /**
     * Turns off row selection and styling.
     */
    public static final int NO_SELECT_ROW_MASK = 8;

    /**
     * Turns off selected column stying.
     */
    public static final int NO_SELECT_COL_MASK = 16;

    /**
     * Turns off cell selection stying.
     */
    public static final int NO_SELECT_CELL_MASK = 32;

    /**
     * Tells the table to render a spacing row in between bound rows.
     */
    public static final int SPACER_ROW_MASK = 64;

    /**
     * If this table has a DataProvider AND it is not scrolling, this supresses the
     * first, previous, next and last buttons at the bottom of the table.
     */
    public static final int NO_NAV_ROW_MASK = 128;

    /**
     * Enables sorting on the table when a header row is clicked.
     *
     * If this table has a DataProvider, it must be a SortableDataProvider for this to work.
     */
    public static final int SORT_MASK = 256;

    /**
     * Enables the click in widget insertion. Note: This will use the default widget type
     * for the model object from the BoundWidgetTypeFactory
     * @see com.totsp.gwittir.client.ui.util.BoundWidgetTypeFactory
     */
    public static final int INSERT_WIDGET_MASK = 512;
    private static final String DEFAULT_STYLE = "default";
    private static final String NAV_STYLE = "nav";
    private Binding topBinding;
    private BoundWidgetTypeFactory typeFactory;
    private Collection value;
    private DataProvider provider;
    private FlexTable table;
    private HashMap clickListeners = new HashMap() /*<SourcesClickEvents, ClickListener>*/;
    private HashMap focusListeners = new HashMap() /*<HasFocus, FocusListener>*/;
    private HashMap selectedRowStyles /*<Integer, String>*/;
    private ScrollPanel scroll;
    private String selectedCellLastStyle;
    private String selectedColLastStyle = BoundTable.DEFAULT_STYLE;
    private String selectedRowLastStyle = BoundTable.DEFAULT_STYLE;
    private Widget base;
    private boolean[] ascending;
    private Column[] columns;
    private boolean inChunk = false;
    private int currentChunk = -1;
    private int lastScrollPosition;
    private int masks;
    private int numberOfChunks;
    private int selectedCellRowLastIndex = -1;
    private int selectedColLastIndex = -1;
    private int selectedRowLastIndex = -1;

    /** Creates a new instance of BoundTable */
    public BoundTable() {
        super();
        this.init(0);
    }

    /**
     * Creates a new instance of Bound table with the indicated options value.
     * @param masks int value containing the sum of the *_MASK options for the table.
     */
    public BoundTable(int masks) {
        super();
        this.init(masks);
    }

    /**
     * Creates a new instance of Bound table with the indicated options value.
     * @param masks int value containing the sum of the *_MASK options for the table.
     */
    public BoundTable(int masks, BoundWidgetTypeFactory typeFactory) {
        super();
        this.typeFactory = typeFactory;
        this.init(masks);
    }

    /**
     * Creates a new instance of a table using a Collection as a data set.
     * @param masks int value containing the sum of the *_MASK options for the table.
     * @param cols The Column objects for the table.
     * @param value A collection containing Bindable objects to render in the table.
     */
    public BoundTable(int masks, Column[] cols, Collection value) {
        super();
        this.columns = cols;
        this.value = value;
        this.init(masks);
    }

    /**
     * Creates a new instance of a table using a Collection as a data set.
     * @param masks int value containing the sum of the *_MASK options for the table.
     * @param cols The Column objects for the table.
     * @param value A collection containing Bindable objects to render in the table.
     */
    public BoundTable(int masks, BoundWidgetTypeFactory typeFactory,
        Column[] cols, Collection value) {
        super();
        this.columns = cols;
        this.value = value;
        this.typeFactory = typeFactory;
        this.init(masks);
    }

    /**
     * Creates a new instance of a table using a Collection as a data set.
     * @param masks int value containing the sum of the *_MASK options for the table.
     * @param cols The Column objects for the table.
     * @param value A collection containing Bindable objects to render in the table.
     */
    public BoundTable(int masks, Column[] cols) {
        super();
        this.columns = cols;
        this.init(masks);
    }

    /**
     * Creates a new instance of a table using a Collection as a data set.
     * @param masks int value containing the sum of the *_MASK options for the table.
     * @param cols The Column objects for the table.
     * @param value A collection containing Bindable objects to render in the table.
     */
    public BoundTable(int masks, BoundWidgetTypeFactory typeFactory,
        Column[] cols) {
        super();
        this.columns = cols;
        this.typeFactory = typeFactory;
        this.init(masks);
    }

    /**
     * Creates a new instance of BoundTable
     * @param masks int value containing the sum of the *_MASK options for the table.
     * @param cols The Column objects for the table.
     * @param provider Instance of DataProvider to get chunked data from.
     */
    public BoundTable(int masks, Column[] cols, DataProvider provider) {
        super();
        this.columns = cols;
        this.provider = provider;
        this.init(masks);
    }

    /**
     * Creates a new instance of BoundTable
     * @param masks int value containing the sum of the *_MASK options for the table.
     * @param cols The Column objects for the table.
     * @param provider Instance of DataProvider to get chunked data from.
     */
    public BoundTable(int masks, BoundWidgetTypeFactory typeFactory,
        Column[] cols, DataProvider provider) {
        super();
        this.columns = cols;
        this.provider = provider;
        this.typeFactory = typeFactory;
        this.init(masks);
    }

    /**
     * Adds a new Bindable object to the table.
     * @param o An object of type Bindable.
     */
    public void add(Bindable o) {
        if(this.value.add(o)) {
            this.addRow(o);
        }
    }

    /**
     * Adds a colleciton of Bindables to the table
     * @param c A collection containing Bindable objects.
     */
    public void add(Collection c) {
        for(Iterator it = c.iterator(); it.hasNext();)
            this.add((Bindable) it.next());
    }

    private void addRow(Bindable o) {
        int row = table.getRowCount();
        
        if((
                    (((masks & BoundTable.HEADER_MASK) > 0) && (row >= 2))
                    || (((masks & BoundTable.HEADER_MASK) == 0) && (row >= 1))
                ) && ((masks & BoundTable.SPACER_ROW_MASK) > 0)) {
            table.setWidget(row, 0, new Label(""));
            table.getFlexCellFormatter()
                 .setColSpan(row, 0, this.getColumns().length );
            table.getRowFormatter().setStyleName(row, "spacer");
            row++;
        }

        Binding bindingRow = new Binding();
        topBinding.getChildren().add(bindingRow);

        for(int col = 0; col < getColumns().length; col++) {
            Widget widget = (Widget) createCellWidget(bindingRow,
                    getColumns()[col], o);
            table.setWidget(row, col , widget);

            if(widget instanceof HasFocus) {
                addSelectedFocusListener((HasFocus) widget,
                    topBinding.getChildren().size() - 1, col );
            }

            if(widget instanceof SourcesClickEvents) {
                addSelectedClickListener((SourcesClickEvents) widget,
                    topBinding.getChildren().size() - 1, col );
            }
        }

        if(this.isAttached()) {
            bindingRow.setLeft();
            bindingRow.bind();
        }
    }

    private void addSelectedClickListener(final SourcesClickEvents widget,
        final int objectNumber, final int col) {
        ClickListener l = new ClickListener() {
                public void onClick(Widget sender) {
                    int row = calculateObjectToRowOffset(objectNumber);
                    //GWT.log( "Click row: "+ row + " object: "+objectNumber, null);
                    handleSelect(true, row, col);
                }
            };

        widget.addClickListener(l);
        clickListeners.put(widget, l);
    }

    private void addSelectedFocusListener(final HasFocus widget,
        final int objectNumber, final int col) {
        FocusListener l = new FocusListener() {
                public void onLostFocus(Widget sender) {
                }

                public void onFocus(Widget sender) {
                    int row = calculateObjectToRowOffset(objectNumber);
                    GWT.log( "Focus row: "+ row + " object: "+objectNumber +" col: "+ col, null);
                    GWT.log( "SelectedRowLastIndex "+selectedRowLastIndex, null );
                    handleSelect(row != selectedRowLastIndex, row, col);
                }
            };

        widget.addFocusListener(l);
        focusListeners.put(widget, l);
    }

    public void addStyleName(String style) {
        this.base.addStyleName(style);
    }

    public void addTableListener(TableListener listener) {
        this.table.addTableListener(listener);
    }

    private int calculateObjectToRowOffset(int row) {
        if((masks & BoundTable.SPACER_ROW_MASK) > 0) {
            row += row;
        }

        if((masks & BoundTable.HEADER_MASK) > 0) {
            row++;
        }
        GWT.log( "Row before: "+ row, null);
        if((masks & BoundTable.INSERT_WIDGET_MASK) > 0) {
            //if( (masks & BoundTable.MULTIROWSELECT_MASK) > 0){
             //   row += this.selectedRowsBeforeObjectRow(row);
            //} else {
                row += this.selectedRowsBeforeRow( row );
            //}
        }
        GWT.log( "Row after "+ row, null);
        return row;
    }

    private Integer calculateRowToObjectOffset(Integer rowNumber) {
        int row = rowNumber.intValue();

        if((masks & BoundTable.HEADER_MASK) > 0) {
            row -= 1;
        }

        if((masks & BoundTable.SPACER_ROW_MASK) > 0) {
            row -= (row / 2);
        }
        //GWT.log( "Selected rows before row "+row+" "+ this.selectedRowsBeforeRow( row ), null );
        if((masks & BoundTable.INSERT_WIDGET_MASK) > 0 && (masks & BoundTable.MULTIROWSELECT_MASK) > 0 ) {
            GWT.log( "At"+ row+ " Removing: "+this.selectedRowsBeforeRow(row), null );
            row -= this.selectedRowsBeforeRow(row);
        }
        GWT.log( "Returning object instance index: "+row, null);
        return new Integer(row);
    }

    /**
     * Clears the table and cleans up all bindings and listeners.
     */
    public void clear() {
        this.topBinding.unbind();
        this.topBinding.getChildren().clear();

        for(Iterator it = this.focusListeners.entrySet().iterator();
                it.hasNext();) {
            Entry entry = (Entry) it.next();
            ((HasFocus) entry.getKey()).removeFocusListener((FocusListener) entry
                .getValue());
        }

        for(Iterator it = this.clickListeners.entrySet().iterator();
                it.hasNext();) {
            Entry entry = (Entry) it.next();
            ((SourcesClickEvents) entry.getKey()).removeClickListener((ClickListener) entry
                .getValue());
        }

        this.table.clear();

        while(table.getRowCount() > 0) {
            this.table.removeRow(0);
        }

        if(this.selectedRowStyles != null) {
            this.selectedRowStyles.clear();
        }

        this.clearSelectedCol();
        this.selectedCellLastStyle = BoundTable.DEFAULT_STYLE;
        this.selectedColLastIndex = -1;
        this.selectedColLastStyle = BoundTable.DEFAULT_STYLE;
        this.selectedRowLastIndex = -1;
        this.selectedRowLastStyle = BoundTable.DEFAULT_STYLE;
    }

    private void clearSelectedCol() {
        //GWT.log( "Clear selected col: "+ this.selectedColLastIndex, null);
        //GWT.log( this.selectedColLastStyle, null );
        if(this.selectedColLastIndex != -1) {
            this.getColumnFormatter()
                .setStyleName(this.selectedColLastIndex,
                this.selectedColLastStyle);
        }
    }

    private void clearSelectedRows() {
        List old = this.getSelected();

        if((this.masks & BoundTable.MULTIROWSELECT_MASK) > 0) {
            for(Iterator it = this.selectedRowStyles.entrySet().iterator();
                    it.hasNext();) {
                Entry entry = (Entry) it.next();
                int row = ((Integer) entry.getKey()).intValue();
                this.table.getRowFormatter()
                          .setStyleName(row, (String) entry.getValue());
            }

            this.selectedRowStyles.clear();
        } else if((this.masks & BoundTable.NO_SELECT_ROW_MASK) == 0) {
            if(this.selectedRowLastIndex != -1) {
                this.getRowFormatter()
                    .setStyleName(this.selectedRowLastIndex,
                    this.selectedRowLastStyle);
                this.selectedRowLastStyle = BoundTable.DEFAULT_STYLE;
            }
        }
        this.selectedRowLastIndex = -1;
        this.selectedCellRowLastIndex = -1;

        this.changes.firePropertyChange("selected", old, this.getSelected());
    }

    private BoundWidget createCellWidget(Binding rowBinding, Column col,
        Bindable target) {
        final BoundWidget widget;
        Binding binding = null;

        if(col.getCellProvider() != null) {
            widget = col.getCellProvider().get();
        } else {
            final Property p = Introspector.INSTANCE.getDescriptor(target)
                                                    .getProperty(col
                    .getPropertyName());
            widget = this.typeFactory.getWidgetProvider(col.getPropertyName(),
                    p.getType()).get();

            // TODO Figure out some way to make this read only.
        }

        binding = new Binding(widget, "value", col.getValidator(),
                col.getFeedback(), target, col.getPropertyName(), null, null);
        widget.setModel(target);

        if(binding != null) {
            rowBinding.getChildren().add(binding);
        }

        return widget;
    }

    private Widget createNavWidget() {
        Grid p = new Grid(1, 5);
        p.setStyleName(BoundTable.NAV_STYLE);

        Button b = new Button("<<",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        first();
                    }
                });
        b.setStyleName(BoundTable.NAV_STYLE);

        if(this.getCurrentChunk() == 0) {
            b.setEnabled(false);
        }

        p.setWidget(0, 0, b);
        b = new Button("<",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        previous();
                    }
                });
        b.setStyleName(BoundTable.NAV_STYLE);

        if(this.getCurrentChunk() == 0) {
            b.setEnabled(false);
        }

        p.setWidget(0, 1, b);

        b = new Button(">",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        next();
                    }
                });
        b.setStyleName(BoundTable.NAV_STYLE);

        if(this.getCurrentChunk() == (this.getNumberOfChunks() - 1)) {
            b.setEnabled(false);
        }

        Label l = new Label((this.getCurrentChunk() + 1) + " / "
                + this.getNumberOfChunks());
        p.setWidget(0, 2, l);
        p.getCellFormatter()
         .setHorizontalAlignment(0, 2, HasHorizontalAlignment.ALIGN_CENTER);

        p.setWidget(0, 3, b);
        b = new Button(">>",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        last();
                    }
                });
        b.setStyleName(BoundTable.NAV_STYLE);

        if(this.getCurrentChunk() == (this.getNumberOfChunks() - 1)) {
            b.setEnabled(false);
        }

        p.setWidget(0, 4, b);

        return p;
    }

    /**
     * Causes the table to go to the first chunk of data, if a data provider is used.
     */
    public void first() {
        this.currentChunk = 0;
        this.provider.getChunk(this, this.getCurrentChunk());
        this.inChunk = true;
    }

    /**
     * Returns the Binding object used by this table.
     * @return The Binding object for this table.
     */
    public Binding getBinding() {
        return this.topBinding;
    }

    public int getCellCount(int row) {
        return this.table.getCellCount(row);
    }

    public HTMLTable.CellFormatter getCellFormatter() {
        return this.table.getCellFormatter();
    }

    public int getCellPadding() {
        return this.table.getCellPadding();
    }

    public int getCellSpacing() {
        return this.table.getCellSpacing();
    }

    public HTMLTable.ColumnFormatter getColumnFormatter() {
        return this.table.getColumnFormatter();
    }

    /**
     * Returns the Columns used in this table.
     * @return Column[] used for rendering this table.
     */
    public Column[] getColumns() {
        return columns;
    }

    /**
     * Returns the current fetched chunk from the data provider.
     * @return int index of the current chunk.
     */
    public int getCurrentChunk() {
        return currentChunk;
    }

    public FlexTable.FlexCellFormatter getFlexCellFormatter() {
        return this.table.getFlexCellFormatter();
    }

    public String getHTML(int row, int column) {
        return this.table.getHTML(row, column);
    }

    /**
     * Returns the number of available chunks (passed in from the DataProvider)
     * @return int number of chunks available from the DataProvider
     */
    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public int getRowCount() {
        return this.table.getRowCount();
    }

    public HTMLTable.RowFormatter getRowFormatter() {
        return this.table.getRowFormatter();
    }

    /**
     * Returns a List containing the current selected row objects.
     * @return List of Bindables from the selected rows.
     */
    public List getSelected() {
        ArrayList selected = new ArrayList();
        HashSet realIndexes = new HashSet();

        if(this.selectedRowStyles != null) {
            for(Iterator it = this.selectedRowStyles.keySet().iterator();
                    it.hasNext();) {
                realIndexes.add(calculateRowToObjectOffset((Integer) it.next()));
            }
        } else if(this.selectedRowLastIndex != -1) {
            realIndexes.add(calculateRowToObjectOffset(
                    new Integer(this.selectedRowLastIndex)));
        }

        int i = 0;

        for(Iterator it = this.topBinding.getChildren().iterator();
                it.hasNext(); i++) {
            if(realIndexes.contains(new Integer(i))) {
                selected.add(((Binding) ((Binding) it.next()).getChildren()
                                         .get(0)).getRight().object);
            } else {
                it.next();
            }
        }

        return selected;
    }

    public String getStyleName() {
        return this.base.getStyleName();
    }

    public String getTitle() {
        return this.table.getTitle();
    }

    public Object getValue() {
        return value;
    }

    public BoundWidget getWidget(int row, int col) {
        return (BoundWidget) this.table.getWidget(row, col);
    }

    private void handleSelect(boolean toggleRow, int row, int col) {
        int calcRow = row;
        GWT.log( "Toggle row "+ toggleRow, null );
        GWT.log( " ON "+row+", "+col, new RuntimeException() );
        if((this.masks & BoundTable.INSERT_WIDGET_MASK) > 0) {
            if((
                        (this.selectedRowStyles == null)
                        && this.selectedRowLastIndex != -1
                        && (this.selectedRowLastIndex == (row - 1))
                    )
                    || (
                        (this.selectedRowStyles != null)
                        && this.selectedRowStyles.containsKey(
                            new Integer(row - 1))
                    )) { 
                return;
            }

            calcRow = row - this.selectedRowsBeforeRow(row);
        } /*
        String selectedRows = " Selected rows: ";
        for(Iterator it = this.selectedRowStyles.keySet().iterator(); it.hasNext(); ){
        selectedRows += it.next() + " ";
        }
        //GWT.log( selectedRows, null );
        //GWT.log( "Raw Row: "+ row, null );
        //GWT.log( "Selected before row: "+ this.selectedRowsBeforeRow( row ), null );
        //GWT.log( "Calc row: "+ calcRow, null);
        */
        if((
                    ((masks & BoundTable.SPACER_ROW_MASK) == 0)
                    && (
                        (
                            ((masks & BoundTable.HEADER_MASK) > 0)
                            && (calcRow > 0)
                        ) || ((masks & BoundTable.HEADER_MASK) == 0)
                    )
                )
                || (
                    ((masks & BoundTable.HEADER_MASK) > 0)
                    & ((calcRow % 2) != 0)
                )
                || (
                    ((masks & BoundTable.HEADER_MASK) == 0)
                    && ((calcRow % 2) != 1)
                )) {
            GWT.log( "Inside" , null); 
            if(toggleRow &&
                    (
                    ((masks & BoundTable.MULTIROWSELECT_MASK) == 0 && row != this.selectedCellRowLastIndex)) ||
                      (masks & BoundTable.MULTIROWSELECT_MASK) > 0 && toggleRow ){
             
            //if( toggleRow || (masks & BoundTable.MULTIROWSELECT_MASK) == 0){
                row = setSelectedRow(row);
            }

            
            setSelectedCell(row , col);
           
            
            setSelectedCol(col);
            
            this.selectedCellRowLastIndex = row ;
        }
    }

    private void init(int masksValue) {
        this.topBinding = new Binding();
        this.masks = masksValue;
        this.typeFactory = (this.typeFactory == null)
            ? new BoundWidgetTypeFactory() : this.typeFactory;

        if((this.masks & BoundTable.SORT_MASK) > 0) {
            this.ascending = new boolean[this.columns.length];
        }

        if((this.masks & BoundTable.MULTIROWSELECT_MASK) > 0) {
            this.selectedRowStyles = new HashMap();
        }

        this.table = new FlexTable();
        this.table.setCellPadding(0);
        this.table.setCellSpacing(0);

        if((masks & BoundTable.SCROLL_MASK) > 0) {
            this.scroll = new ScrollPanel();
            this.scroll.setWidget(table);
            super.initWidget(scroll);

            if(this.provider != null) {
                scroll.addScrollListener(new ScrollListener() {
                        public void onScroll(Widget widget, int scrollLeft,
                            int scrollTop) {
                            //GWT.log("" + inChunk, null);
                            if((inChunk == false)
                                    && (
                                        scrollTop >= (
                                            table.getOffsetHeight()
                                            - scroll.getOffsetHeight()
                                        )
                                    )) {
                                lastScrollPosition = scrollTop - 1;
                                next();
                            }
                        }
                    });
            }
        } else {
            super.initWidget(table);
        }

        this.table.setCellSpacing(0);
        table.addTableListener(new TableListener() {
                public void onCellClicked(SourcesTableEvents sender, int row,
                    int cell) {
                    //GWT.log("-----Table Listener " + row + ", " + cell, null);
                    handleSelect(true, row, cell);

                    if(((masks & BoundTable.SORT_MASK) > 0)
                            && ((masks & BoundTable.HEADER_MASK) > 0)
                            && (row == 0)) {
                        sortColumn(cell);
                    }
                }
            });
        this.base = (
                (this.scroll == null) ? (Widget) this.table : (Widget) this.scroll
            );
        this.value = (this.value == null) ? new ArrayList() : this.value;
        this.columns = (this.columns == null) ? new Column[0] : this.columns;
        this.setStyleName("gwittir-BoundTable");

        if((this.provider != null) && (this.getCurrentChunk() == -1)) {
            this.provider.init(this);
            this.inChunk = true;
        }
    }

    /**
     * Method called by the DataProvider to initialize the first chunk and pass
     * in the to total number of chunks available.
     * @param c Data for Chunk index 0
     * @param numberOfChunks The total number of available chunks of data.
     */
    public void init(Collection c, int numberOfChunks) {
        //GWT.log("Chunk init", null);
        this.numberOfChunks = numberOfChunks;
        this.currentChunk = 0;
        this.setValue(c);
        this.inChunk = false;
    }

    private void insertNestedWidget(int row) {
        GWT.log( "Inserting nested for row "+row, null);
        Integer realIndex = this.calculateRowToObjectOffset(new Integer(row));
        GWT.log( "RealIndex: "+ realIndex, null );
        int i = 0;
        Bindable o = null;

        for(Iterator it = this.topBinding.getChildren().iterator();
                it.hasNext(); i++) {
            if(realIndex.intValue() == i) {
                o = ((Binding) ((Binding) it.next()).getChildren().get(0))
                    .getRight().object;

                break;
            } else {
                it.next();
            }
        }

        BoundWidget widget = this.typeFactory.getWidgetProvider(Introspector.INSTANCE
                .resolveClass(o)).get();
        widget.setModel(o);
        this.table.insertRow(row + 1);
        this.table.setWidget(row + 1, 0, (Widget) widget);
        this.table.getFlexCellFormatter()
                  .setColSpan(row + 1, 0, this.columns.length + 1);
        this.table.getCellFormatter().setStyleName( row +1, 0, "expanded");
        this.modifySelectedIndexes(row, +1);
    }

    /**
     * Causes the table to render the last chunk of data.
     */
    public void last() {
        if((this.numberOfChunks - 1) >= 0) {
            this.currentChunk = this.numberOfChunks - 1;
            this.provider.getChunk(this, currentChunk);
            this.inChunk = true;
        }
    }

    private void modifySelectedIndexes(int fromRow, int modifier) {
        //GWT.log( "Modifying indexes from: "+ fromRow + " with "+ modifier, null);
        if(this.selectedRowLastIndex > fromRow) {
            this.selectedRowLastIndex += modifier;
        }
        if(this.selectedCellRowLastIndex > fromRow ){
            this.selectedCellRowLastIndex += modifier;
        }
        if(this.selectedRowStyles == null) {
            return;
        }

        HashMap newSelectedRowStyles = new HashMap();

        for(Iterator it = this.selectedRowStyles.entrySet().iterator();
                it.hasNext();) {
            Entry entry = (Entry) it.next();
            Integer entryRow = (Integer) entry.getKey();

            if(entryRow.intValue() > fromRow) {
                newSelectedRowStyles.put(new Integer(entryRow.intValue()
                        + modifier), entry.getValue());
            } else {
                newSelectedRowStyles.put(entryRow, entry.getValue());
            }
        }

        this.selectedRowStyles = newSelectedRowStyles;

        /*String selectedRows = " Selected rows: ";
        for(Iterator it = this.selectedRowStyles.keySet().iterator(); it.hasNext(); ){
            selectedRows += it.next() + " ";
        }
        //GWT.log( selectedRows, null ); */
    }

    /**
     * Causes the table to render the next chunk of data.
     */
    public void next() {
        if((this.currentChunk + 1) < this.numberOfChunks) {
            this.provider.getChunk(this, ++currentChunk);
            this.inChunk = true;
        }

        //GWT.log("Next fired", null);
    }

    protected void onAttach() {
        super.onAttach();
        //GWT.log("onAttach", null);
        this.renderAll();
    }

    protected void onDetach() {
        super.onDetach();
        this.clear();
    }

    /**
     * Causes teh table to render the previous chunk of data.
     */
    public void previous() {
        if((this.getCurrentChunk() - 1) >= 0) {
            this.provider.getChunk(this, --currentChunk);
            inChunk = true;
        }
    }

    private void removeNestedWidget(int row) {
        //GWT.log( "Removing nested for: "+ row, null);
        this.modifySelectedIndexes(row, -1);
        this.table.removeRow(row + 1);
    }

    private void renderAll() {
        this.clear();
        if((this.masks & BoundTable.HEADER_MASK) > 0) {
            for(int i = 0; i < this.columns.length; i++) {
                this.table.setWidget(0, i ,
                    new Label(this.columns[i].getLabel()));
                this.table.getCellFormatter()
                          .setStyleName(0, i , "header");
            }
        }
        for(Iterator it = this.value.iterator(); it.hasNext();) {
            this.addRow((Bindable) it.next());
        }

        if((this.provider != null)
                && ((this.masks & BoundTable.SCROLL_MASK) == 0)
                && ((this.masks & BoundTable.NO_NAV_ROW_MASK) == 0)) {
            int row = this.table.getRowCount();
            this.table.setWidget(row, 0, this.createNavWidget());
            this.table.getFlexCellFormatter()
                      .setColSpan(row, 0, this.columns.length );
            table.getCellFormatter()
                 .setHorizontalAlignment(row, 0,
                HasHorizontalAlignment.ALIGN_CENTER);
        }
    }

    /*private int xselectedRowsBeforeObjectRow(int row) {
        if(this.selectedRowStyles == null) {
            //GWT.log( "===="+row+"==="+this.selectedRowLastIndex,null );
            return (
                (this.selectedRowLastIndex == -1)
                || (this.selectedRowLastIndex > row)
            ) ? 0 : 1;
        }

        int count = 0;

        for(Iterator it = this.selectedRowStyles.keySet().iterator();
                it.hasNext();) {
            if(((Integer) it.next()).intValue() < row) {
                count++;
                row += 1;
            }
        }

        return count;
    }*/

    private int selectedRowsBeforeRow(int row) {
        GWT.log( "=======Selected rows before "+row, null);
        GWT.log( "=======lastRow "+this.selectedRowLastIndex, null );
        if(this.selectedRowStyles == null) {
            GWT.log( "========" +  ((
                (this.selectedRowLastIndex == -1)
                || (this.selectedRowLastIndex >= row)
            ) ? 0 : 1), null);
            return //((this.masks & BoundTable.HEADER_MASK ) == 0  )||
                    (
                (this.selectedRowLastIndex == -1)
                || (this.selectedRowLastIndex >= row)
            ) ? 0 : 1;
            
        }

        int count = 0;

        for(Iterator it = this.selectedRowStyles.keySet().iterator();
                it.hasNext();) {
            if(((Integer) it.next()).intValue() < row) {
                count++;
            }
        }

        return count;
    }

    public void setBorderWidth(int width) {
        this.table.setBorderWidth(width);
    }

    public void setCellPadding(int padding) {
        this.table.setCellPadding(padding);
    }

    public void setCellSpacing(int spacing) {
        this.table.setCellSpacing(spacing);
    }

    /**
     * Called by the DataProvider to pass in a requested chunk of data.
     * THIS METHOD MUST BE CALLED ASYNCRONOUSLY.
     * @param c The next requested chunk of Bindable objects.
     */
    public void setChunk(Collection c) {
        if(!this.inChunk) {
            throw new RuntimeException(
                "This method MUST becalled asyncronously!");
        }

        if((masks & this.SCROLL_MASK) > 0) {
            this.add(c);
        } else {
            this.setValue(c);
        }

        if(((masks & this.SCROLL_MASK) > 0)
                && (
                    this.scroll.getScrollPosition() >= this.lastScrollPosition
                )) {
            this.scroll.setScrollPosition(this.lastScrollPosition);
        }

        this.inChunk = false;
    }

    /**
     * Sets Column[] object for use on the table.
     * Note, this will foce a re-init of the table.
     * @param columns Column[] to use to render the table.
     */
    public void setColumns(Column[] columns) {
        this.columns = columns;
        this.renderAll();
    }

    public void setDataProvider(DataProvider provider) {
        this.provider = provider;
        this.provider.init(this);
        this.inChunk = true;
    }

    public void setHeight(String height) {
        this.base.setHeight(height);
    }

    public void setPixelSize(int width, int height) {
        this.table.setPixelSize(width, height);
    }

    /**
     * Sets the indicated items in the list to "selected" state.
     * @param selected A List of Bindables to set as the Selected value.
     */
    public void setSelected(List selected) {
        int i = 0;
        this.clearSelectedRows();

        for(Iterator it = this.topBinding.getChildren().iterator();
                it.hasNext(); i++) {
            Bindable b = ((Binding) ((Binding) it.next()).getChildren().get(0))
                .getRight().object;

            if(selected.contains(b)) {
                this.setSelectedRow(calculateObjectToRowOffset(i));
            }
        }
    }

    private void setSelectedCell(int row, int col) {
        if((((this.masks & BoundTable.HEADER_MASK) > 0) && (row == 0))
                || ((this.masks & BoundTable.NO_SELECT_CELL_MASK) > 0)) {
            return;
        }

        if((this.selectedColLastIndex != -1)
                && (this.selectedCellRowLastIndex != -1)) {
            //GWT.log( "Unsetting "+ this.selectedCellRowLastIndex+", "+ this.selectedColLastIndex, null);
            /*if( (masks & BoundTable.MULTIROWSELECT_MASK + BoundTable.INSERT_WIDGET_MASK) > 0 &&
                    row < this.selectedCellRowLastIndex ){
                this.selectedCellRowLastIndex--;
            } */
            this.getCellFormatter()
                .setStyleName(this.selectedCellRowLastIndex,
                this.selectedColLastIndex, this.selectedCellLastStyle);
        }

        //GWT.log("Getting style for " + row + ", " + col, null);
        this.selectedCellLastStyle = table.getCellFormatter()
                                          .getStyleName(row, col);

        if((this.selectedCellLastStyle == null)
                || (this.selectedCellLastStyle.length() == 0)) {
            this.selectedCellLastStyle = BoundTable.DEFAULT_STYLE;
        }

        table.getCellFormatter().setStyleName(row, col, "selected");
    }

    private void setSelectedCol(int col) {
        clearSelectedCol();
        this.selectedColLastIndex = col;

        if((this.masks & BoundTable.NO_SELECT_COL_MASK) == 0) {
            this.selectedColLastStyle = table.getColumnFormatter()
                                             .getStyleName(col);

            if((this.selectedColLastStyle == null)
                    || (this.selectedColLastStyle.length() == 0)) {
                this.selectedColLastStyle = BoundTable.DEFAULT_STYLE;
            }

            table.getColumnFormatter().setStyleName(col, "selected");
        }
    }

    private int setSelectedRow(int row) {
        //GWT.log ("selectRow() " + row, null);
        //GWT.log("Last: " + this.selectedRowLastIndex, null);
        if(((this.masks & BoundTable.HEADER_MASK) > 0) && (row == 0)) {
            return row;
        }

        List old = this.getSelected();

        //GWT.log( "Selecting row: "+ row, null);
        if((this.masks & BoundTable.MULTIROWSELECT_MASK) > 0) {
            if(this.selectedRowStyles.containsKey(new Integer(row))) {
                //Handle Widget remove on Multirow
                this.getRowFormatter()
                    .setStyleName(row,
                    (String) this.selectedRowStyles.remove(new Integer(row)));

                if((this.masks & BoundTable.INSERT_WIDGET_MASK) > 0) {
                    this.removeNestedWidget(row);
                }
            } else {
                String lastStyle = table.getRowFormatter().getStyleName(row);
                lastStyle = ((lastStyle == null) || (lastStyle.length() == 0))
                    ? BoundTable.DEFAULT_STYLE : lastStyle;
                this.selectedRowStyles.put(new Integer(row), lastStyle);
                this.getRowFormatter().setStyleName(row, "selected");

                if((this.masks & BoundTable.INSERT_WIDGET_MASK) > 0) {
                    this.insertNestedWidget(row);
                }
            }

            //GWT.log( "Number of selected rows:  "+ this.selectedRowStyles.size(), null);
        } else if((this.masks & BoundTable.NO_SELECT_ROW_MASK) == 0) {
            if(this.selectedRowLastIndex != -1) {
                //GWT.log("Resetting " + this.selectedRowLastIndex + " to "
                //    + this.selectedRowLastStyle, null);
                this.getRowFormatter()
                    .setStyleName(this.selectedRowLastIndex,
                    this.selectedRowLastStyle);

                if((this.masks & BoundTable.INSERT_WIDGET_MASK) > 0) {
                    this.removeNestedWidget(this.selectedRowLastIndex);
                    if( this.selectedRowLastIndex < row ){
                        row--;
                    }
                }
            }

            String currentStyle = table.getRowFormatter().getStyleName(row);

            if((currentStyle == null) || !currentStyle.equals("selected")) {
                this.selectedRowLastStyle = currentStyle;
            }

            if((this.selectedRowLastStyle == null)
                    || (this.selectedRowLastStyle.length() == 0)) {
                this.selectedRowLastStyle = BoundTable.DEFAULT_STYLE;
            }

            //GWT.log("Last style for row: " + this.selectedRowLastStyle, null);

            table.getRowFormatter().setStyleName(row, "selected");

            if((this.masks & BoundTable.INSERT_WIDGET_MASK) > 0) {
                //GWT.log("Inserting for row: "+ row, null);
                this.insertNestedWidget(row);
            }
            
        }

        this.selectedRowLastIndex = (this.selectedRowLastIndex == row) ? (-1)
                                                                       : row;
        this.changes.firePropertyChange("selected", old, this.getSelected());
        return row;
    }

    public void setSize(String width, String height) {
        this.base.setSize(width, height);
    }

    public void setStyleName(String style) {
        this.base.setStyleName(style);
    }

    public void setValue(Object value) {
        Collection old = this.value;
        this.value = (Collection) value;
        this.changes.firePropertyChange("value", old, this.value);

        if(this.isAttached()) {
            this.renderAll();
        }
    }

    public void setWidth(String width) {
        this.base.setWidth(width);
    }

    /**
     * Sorts the table based on the value of the property in the specified column
     * index.
     *
     * If using a SortableDataProvider, this will throw a runtime exception if the
     * column denoted by the index is not a supported sortable column.
     * @param index index of the column to sort the table on.
     */
    public void sortColumn(int index) {
        ascending[index] = !ascending[index];

        if(this.provider == null) {
            ArrayList sort = new ArrayList();
            sort.addAll(value);

            try {
                ListSorter.sortOnProperty(sort,
                    columns[index].getPropertyName(), ascending[index]);
            } catch(Exception e) {
                //GWT.log("Exception during sort", e);
            }

            value.clear();

            for(Iterator it = sort.iterator(); it.hasNext();) {
                value.add(it.next());
            }

            setValue(value);
        } else if(this.provider instanceof SortableDataProvider) {
            SortableDataProvider sdp = (SortableDataProvider) this.provider;
            boolean canSort = false;
            String[] sortableProperties = sdp.getSortableProperties();

            for(int i = 0; (i < sortableProperties.length) && !canSort; i++) {
                if(sortableProperties[i].equals(
                            this.columns[index].getPropertyName())) {
                    canSort = true;
                }
            }

            if(!canSort) {
                throw new RuntimeException(
                    "That is not a sortable field from this data provider.");
            }

            sdp.sortOnProperty(this, this.columns[index].getPropertyName(),
                this.ascending[index]);
        }
    }
}