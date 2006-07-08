/**
 * A sorter for TableModels. The sorter has a model (conforming to TableModel)
 * and itself implements TableModel. TableSorter does not store or copy
 * the data in the TableModel, instead it maintains an array of
 * integers which it keeps the same size as the number of rows in its
 * model. When the model changes it notifies the sorter that something
 * has changed eg. "rowsAdded" so that its internal array of integers
 * can be reallocated. As requests are made of the sorter (like
 * getValueAt(row, col) it redirects them to its model via the mapping
 * array. That way the TableSorter appears to hold another copy of the table
 * with the rows in a different order. The sorting algorthm used is stable
 * which means that it does not move around rows when its comparison
 * function returns 0 to denote that they are equivalent.
 *
 * @version 1.5 12/17/97
 * @author Philip Milne
 */

package com.hifiremote.jp1;

import java.util.*;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelEvent;

// Imports for picking up mouse events from the JTable.

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.text.DecimalFormat;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

public class TableSorter< E > extends TableMap< E >
{
    int             indexes[];
    Vector< Integer >          sortingColumns = new Vector< Integer >();
    boolean         ascending = true;
    int compares;
    DecimalFormat df = new DecimalFormat( "000" );

    public TableSorter() {
        indexes = new int[0]; // for consistency
    }

    public TableSorter( JP1TableModel< E > model ) {
        setModel( model );
    }

    public void setModel( JP1TableModel< E > model ) {
        super.setModel( model );
        reallocateIndexes();
    }

    @SuppressWarnings("unchecked")
    public int compareRowsByColumn(int row1, int row2, int column) {
        Class type = model.getColumnClass(column);
        TableModel data = model;

        // Check for nulls.

        Object o1 = data.getValueAt(row1, column);
        Object o2 = data.getValueAt(row2, column);

        // If both values are null, return 0.
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) { // Define null less than everything.
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        /*
         * We copy all returned values from the getValue call in case
         * an optimised model is reusing one object to return many
         * values.  The Number subclasses in the JDK are immutable and
         * so will not be used in this way but other subclasses of
         * Number might want to do this to save space and avoid
         * unnecessary heap allocation.
         */

        if ( o1 instanceof Comparable )
        {
          Comparable c1 = ( Comparable )o1;
          Comparable c2 = ( Comparable )o2;
          return c1.compareTo( c2 );
        }
        else
        {
          String s1 = o1.toString();
          String s2 = o2.toString();
          return s1.compareTo(s2);
        }
    }

    public int compare(int row1, int row2) {
        compares++;
        for (int level = 0; level < sortingColumns.size(); level++) {
            Integer column = (Integer)sortingColumns.elementAt(level);
            int result = compareRowsByColumn(row1, row2, column.intValue());
            if (result != 0) {
                return ascending ? result : -result;
            }
        }
        return 0;
    }

    public void reallocateIndexes() {
        int rowCount = model.getRowCount();

        // Set up a new array of indexes with the right number of elements
        // for the new data model.
        indexes = new int[rowCount];

        // Initialise with the identity mapping.
        for (int row = 0; row < rowCount; row++) {
            indexes[row] = row;
        }
    }

    private int convertModelRowToIndex( int row )
    {
      int i = 0;
      for ( ; i < indexes.length; i++ )
      {
        if ( indexes[ i ] == row )
          break;
      }
      return i;
    }

    public void tableChanged(TableModelEvent e)
    {
      //System.out.println("Sorter: tableChanged");
      int firstRow = e.getFirstRow();
      int firstIndex = 0;
      if ( firstRow != -1 )
        firstIndex = convertModelRowToIndex( firstRow );
      else
        firstIndex = -1;
      int lastRow = e.getLastRow();
      int lastIndex = 0;
      if ( lastRow != -1 )
      {
        if ( lastRow == firstRow )
          lastIndex = firstIndex;
        else
          lastIndex = convertModelRowToIndex( lastRow );
      }
      else
        lastIndex = -1;

      if (( firstIndex == -1 ) || ( lastIndex == -1 ))
        reallocateIndexes();

      TableModelEvent newEvent = new TableModelEvent(( TableModel )e.getSource(),
                                                      firstIndex, lastIndex,
                                                      e.getColumn(),
                                                      e.getType());
      super.tableChanged( e );
    }

    public void checkModel()
    {
      if (indexes.length != model.getRowCount())
      {
        System.err.println("Sorter not informed of a change in model.");
        reallocateIndexes();
      }
    }

    public void sort(Object sender) {
        checkModel();

        compares = 0;
        // n2sort();
        // qsort(0, indexes.length-1);
        shuttlesort((int[])indexes.clone(), indexes, 0, indexes.length);
        //System.out.println("Compares: "+compares);
    }

    public void n2sort() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = i+1; j < getRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) == -1) {
                    swap(i, j);
                }
            }
        }
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
    public void shuttlesort(int from[], int to[], int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high)/2;
        shuttlesort(to, from, low, middle);
        shuttlesort(to, from, middle, high);

        int p = low;
        int q = middle;

        /* This is an optional short-cut; at each recursive call,
        check to see if the elements in this subset are already
        ordered.  If so, no further comparisons are needed; the
        sub-array can just be copied.  The array must be copied rather
        than assigned otherwise sister calls in the recursion might
        get out of sinc.  When the number of elements is three they
        are partitioned so that the first set, [low, mid), has one
        element and and the second, [mid, high), has two. We skip the
        optimisation when the number of elements is three or less as
        the first compare in the normal merge will produce the same
        sequence of steps. This optimisation seems to be worthwhile
        for partially ordered lists but some analysis is needed to
        find out how the performance drops to Nlog(N) as the initial
        order diminishes - it may drop very quickly.  */

        if (high - low >= 4 && compare(from[middle-1], from[middle]) <= 0) {
            for (int i = low; i < high; i++) {
                to[i] = from[i];
            }
            return;
        }

        // A normal merge.

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            }
            else {
                to[i] = from[q++];
            }
        }
    }

    public void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".

    public Object getValueAt(int aRow, int aColumn) {
        checkModel();
        return model.getValueAt(indexes[aRow], aColumn);
    }

    public void setValueAt(Object aValue, int aRow, int aColumn) {
        checkModel();
        model.setValueAt(aValue, indexes[aRow], aColumn);
    }

    public void sortByColumn(int column) {
        sortByColumn(column, true);
    }

    public void sortByColumn(int column, boolean ascending) {
        this.ascending = ascending;
        sortingColumns.removeAllElements();
        sortingColumns.addElement(new Integer( column ));
        sort(this);
        super.tableChanged(new TableModelEvent(this));
    }

    public int convertRowIndexToModel( int aRow )
    {
      checkModel();
      return indexes[ aRow ];
    }

    // There is no-where else to put this.
    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    public void addMouseListenerToHeaderInTable(JTable table) {
        final TableSorter sorter = this;
        final JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        MouseAdapter listMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TableColumnModel columnModel = tableView.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = tableView.convertColumnIndexToModel(viewColumn);
                if (e.getClickCount() == 1 && column != -1) {
                    //System.out.println("Sorting ...");
                    int shiftPressed = e.getModifiers()&InputEvent.SHIFT_MASK;
                    boolean ascending = (shiftPressed == 0);
                    sorter.sortByColumn(column, ascending);
                }
            }
        };
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }

    public E getRow( int row )
    {
      return model.getRow( indexes[ row ]);
    }

    public void addRow( E object )
    {
      model.addRow( object );
      int[] saved = indexes;
      indexes = new int[ saved.length + 1 ];
      int i;
      for ( i = 0; i < saved.length; i++ )
        indexes[ i ] = saved[ i ];
      indexes[ i ] = i;
    }

    public void insertRow( int row, E object )
    {
      model.insertRow( indexes[ row ], object );
      int mappedRow = indexes[ row ];
      int[] saved = indexes;
      indexes = new int[ indexes.length + 1 ];
      int temp;
      for ( int i = 0; i < indexes.length; i++ )
      {
        if ( i < row )
        {
          temp = saved[ i ];
        }
        else if ( i == row )
        {
          temp = saved[ i ];
        }
        else // temp > row
        {
          temp = saved[ i - 1 ];
        }

        if (( i != row ) && ( temp >= mappedRow ))
        {
          temp++;
        }

        indexes[ i ] = temp;
      }
    }

    public void removeRow( int row )
    {
      model.removeRow( indexes[ row ]);
      int modelRow = indexes[ row ];
      int[] saved = indexes;
      indexes = new int[ saved.length - 1 ];
      int temp;
      for ( int i = 0; i < indexes.length; i++ )
      {
        if ( i < row )
        {
          temp = saved[ i ];
        }
        else
        {
          temp = saved[ i + 1 ];
        }

        if ( temp > modelRow )
        {
          temp--;
        }

        indexes[ i ] = temp;
      }
   }

   public void moveRow( int from, int to )
   {
     E o = getRow( from );
     if ( from < to )
       to++;
     if ( to >= indexes.length )
       addRow( o );
     else
       insertRow( to, o );
     if ( from > to )
       from++;
     removeRow( from );
   }
}
