package com.hifiremote.jp1;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;

public class ButtonPanel
  extends KMPanel
  implements ActionListener
{
  public ButtonPanel( DeviceUpgrade devUpgrade )
  {
    super( "Buttons", devUpgrade );
    setLayout( new BorderLayout());

    table = new JTableX();
    model = new ButtonTableModel( devUpgrade );
    table.setModel( model );
    table.setRowSelectionAllowed( false );
    table.setColumnSelectionAllowed( false );
    table.setCellSelectionEnabled( true );
    table.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
    table.setSurrendersFocusOnKeystroke( true );
    table.getInputMap().put( KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0), "delete");
    table.setAutoResizeMode( JTable.AUTO_RESIZE_LAST_COLUMN );
    table.setDefaultEditor( Function.class, popupEditor );

    deleteAction = new AbstractAction( "Remove" ) 
    {
      public void actionPerformed(ActionEvent e) 
      {
        int[] cols = table.getSelectedColumns();
        int[] rows = table.getSelectedRows();
        for ( int c = 0; c < cols.length; c++ )
          for ( int r = 0; r < rows.length; r++ )
            setFunctionAt( null, rows[ r ], cols[ c ] );
      }
    };
    deleteAction.setEnabled( false );
    table.getActionMap().put( "delete", deleteAction );

    renderer = new FunctionRenderer( deviceUpgrade );
    table.setDefaultRenderer( Button.class, renderer );
    table.getTableHeader().setReorderingAllowed( false );

    ListSelectionListener lsl = new ListSelectionListener() 
    {
      public void valueChanged( ListSelectionEvent e ) 
      {
        //Ignore extra messages.
        if ( e.getValueIsAdjusting())
          return;

        selectionChanged();
      }
    };
    table.getSelectionModel().addListSelectionListener( lsl );
    table.getColumnModel().getSelectionModel().addListSelectionListener( lsl );

    TransferHandler th = new TransferHandler()
    {
      public boolean canImport( JComponent comp, DataFlavor[] flavors )
      {
        for ( int i = 0; i < flavors.length; i++ )
        {
          if ( flavors[ i ] == LocalObjectTransferable.getFlavor())
          {
            return true;
          }
        }
        return false;
      }

      public boolean importData( JComponent c, Transferable t )
      {
        boolean rc = false;
        JTable table = ( JTable )c;
        int row = table.getSelectedRow();
        int col = table.getSelectedColumn();
        if ( col != 0 )
        {
          try
          {
            Function f = ( Function )t.getTransferData( LocalObjectTransferable.getFlavor());
            setFunctionAt( f, row, col );
          }
          catch ( Exception e )
          {
            rc = false;
            System.err.println( "ButtonPanel.importData() caught an exception!" );
            e.printStackTrace( System.err );
          }
        }

        return rc;
      }
    };
    table.setTransferHandler( th );
    try
    {
      table.getDropTarget().addDropTargetListener( new DropTargetAdapter()
      {
        public void dragOver( DropTargetDragEvent dte )
        {
          int col = table.getSelectedColumn();
          int row = table.getSelectedRow();
          if ( canAssign( row, col ))
            dte.acceptDrag( dte.getDropAction());
          else
            dte.rejectDrag();
        }

        public void drop( DropTargetDropEvent dte )
        {
          ;
        }
      });
    }
    catch ( Exception x )
    {
      x.printStackTrace( System.err );
    }

    add( new JScrollPane( table ), BorderLayout.CENTER );

    JPanel panel = new JPanel( new BorderLayout());
    JLabel label = new JLabel( "Available Functions:" );
    label.setBorder( BorderFactory.createEmptyBorder( 2, 2, 3, 2 ));
    panel.add( label, BorderLayout.NORTH );
    add( panel, BorderLayout.EAST );

    JPanel outerPanel = new JPanel( new BorderLayout());
    functionPanel = new JPanel( new GridLayout( 0, 3 ));
    outerPanel.add( functionPanel, BorderLayout.NORTH );
    panel.add( new JScrollPane( outerPanel ), BorderLayout.CENTER );

    panel = new JPanel();
    autoAssign = new JButton( "Auto assign" );
    autoAssign.setToolTipText( "Assign functions to buttons of the same name that don't have a functon." );
    autoAssign.addActionListener( this );
    panel.add( autoAssign );

    JButton button = new JButton( deleteAction );
    button.setToolTipText( "Remove the assigned function from the button." );
    panel.add( button );

    add( panel, BorderLayout.SOUTH );
  }

  private void selectionChanged()
  {
    boolean enableDelete = false;
    int[] rows = table.getSelectedRows();
    int[] cols = table.getSelectedColumns();
    for ( int r = 0; ( r < rows.length ) && !enableDelete ; r++ )
    {
      int row = rows[ r ];
      Button b = ( Button )model.getValueAt( row, 0 );
      for ( int c = 0; ( c < cols.length ) && !enableDelete ; c++ )
      {
        int col = cols[ c ];
        if ( col > 0 )
        {
          Function f = null;
          if ( col == 1 )
            f = deviceUpgrade.getFunction( b, Button.NORMAL_STATE );
          else if ( col == 2 )
            f = deviceUpgrade.getFunction( b, Button.SHIFTED_STATE );
          else if ( col == 3 )
            f = deviceUpgrade.getFunction( b, Button.XSHIFTED_STATE );
          if ( f != null )
            enableDelete = true;
        }
      }
    }
    deleteAction.setEnabled( enableDelete );
  }

  public void update()
  {
    model.setDeviceUpgrade( deviceUpgrade );
    renderer.setDeviceUpgrade( deviceUpgrade );
    
    if ( deviceUpgrade != null )
    {
      setButtons( deviceUpgrade.getRemote().getUpgradeButtons());
      setFunctions();
    }
  }

  private void setButtons( Button[] buttons )
  {
    this.buttons = buttons;
    model.setButtons();
  }

  private void addFunction( Function f )
  {
    if (( f == null ) ||
        (( f.getHex() != null ) && ( f.getName() != null ) && (f.getName().length() > 0 )))
    {
      FunctionLabel l;
      if ( f == null )
        l = new FunctionLabel( null );
      else
        l = f.getLabel();
      l.addMouseListener( doubleClickListener );
      functionPanel.add( l );

      popupEditor.addObject( f );
    }
  }

  private void setFunctions()
  {
    popupEditor.removeAll();

    functionPanel.removeAll();

    for ( Function function : deviceUpgrade.getFunctions())
      addFunction( function );
 
    for ( ExternalFunction function : deviceUpgrade.getExternalFunctions())
      addFunction( function );

    functionPanel.doLayout();
  }

  // From interface ActionListener
  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == autoAssign )
    {
      deviceUpgrade.autoAssignFunctions();
      model.setButtons();
    }
    deviceUpgrade.checkSize();
  }

  private boolean canAssign( int row, int col )
  {
    return model.isCellEditable( row, col );
  }

  private void setFunctionAt( Function function, int row, int col )
  {
    int[] rows = null;
    int[] cols = null;
    if ( table.isCellSelected( row, col ))
    {
      rows = table.getSelectedRows();
      cols = table.getSelectedColumns();
    }
    else
    {
      rows = new int[ 1 ];
      rows[ 0 ] = row;
      cols = new int[ 1 ];
      cols[ 0 ] = col;
    }

    int firstRow = row;
    for ( int r = 0; r < rows.length; r++ )
    {
      row = rows[ r ];
      if ( r == 0 )
        firstRow = row;
      for ( int c = 0; c < cols.length; c++ )
      {
        col = cols[ c ];
          
        if (( col > 0 ) && ( row != -1 ))
        {
          model.setValueAt( function, row, col );
        }
      }
      model.fireTableRowsUpdated( firstRow, row );
    }
    selectionChanged();
  }

  class DoubleClickListener
    extends MouseAdapter
  {
    public void mouseClicked( MouseEvent e )
    {
      FunctionLabel label = ( FunctionLabel )e.getSource();
      if ( e.getClickCount() == 2 )
      {
        int col = table.getSelectedColumn();
        int row = table.getSelectedRow();
        if ( canAssign( row, col ))
        {
          setFunctionAt( label.getFunction(), row, col );
          deviceUpgrade.checkSize();
        }
      }
    }
  }

  public void setFont( Font aFont )
  {
    super.setFont( aFont );
    if (( aFont == null ) || ( table == null ))
      return;
    table.setRowHeight( aFont.getSize() + 2 );
  }

  private JTableX table = null;
  private ButtonTableModel model = null;
  private JPanel functionPanel = null;
  // private JPopupMenu popup = null;
  private int mouseRow = 0;
  private int mouseCol = 0;
  private DoubleClickListener doubleClickListener = new DoubleClickListener();
  private JButton autoAssign = null;
  private AbstractAction deleteAction = null;
  private Button[] buttons = null;
  private PopupEditor popupEditor = new PopupEditor();
  private FunctionRenderer renderer = null;
}

