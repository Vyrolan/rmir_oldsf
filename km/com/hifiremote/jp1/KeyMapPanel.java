package com.hifiremote.jp1;

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.print.*;
import java.util.*;

public class KeyMapPanel
  extends KMPanel implements ActionListener, Printable
{
  public KeyMapPanel( DeviceUpgrade devUpgrade )
  {
    super( devUpgrade );
    setLayout( new BorderLayout());

    String fontName = "Tahoma";
    
    titleLabel = new JLabel();
    titleLabel.setFont( new Font( fontName, Font.BOLD, 14 ));
    titleLabel.setBackground( Color.WHITE );
    titleLabel.setAlignmentX( 0.5f );

    subtitleLabel = new JLabel();
    subtitleLabel.setFont( new Font( fontName, Font.BOLD, 10 ));
    subtitleLabel.setBackground( Color.WHITE );
    subtitleLabel.setAlignmentX( 0.5f );
                                
    box = Box.createVerticalBox();
    box.setBorder( 
      BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ),
                                          BorderFactory.createLineBorder( Color.BLACK )));
    box.add( titleLabel );
    box.add( subtitleLabel );

    add( new JScrollPane( box ), BorderLayout.CENTER );

    TableModel dataModel = new AbstractTableModel() 
    {
      public int getColumnCount()
      {
        Remote r = deviceUpgrade.getRemote();
        if (( r != null ) && r.getXShiftEnabled())
          return 4;
        else
          return 3;
      }
      public int getRowCount() { return list.size();}
      public Object getValueAt(int row, int col)
      { 
        Button b = ( Button )list.elementAt( row );
        switch ( col )
        {
          case 0:
            return "  " + b.getName();
          case 1:
            return getFuncName( b.getFunction());
          case 2:
            return getFuncName( b.getShiftedFunction());
          case 3:
            return getFuncName( b.getXShiftedFunction());
          default:
            return null;
        }
      }

      private String getFuncName( Function f )
      {
        if ( f == null )
          return null;
        String s = f.getName();
        if ( s == null )
          return null;
        return "  " + s;
      }

      public String getColumnName( int column ) 
      {
        return headers[column];
      }
      public Class getColumnClass( int col ) 
      {
        return String.class;
      }
      public boolean isCellEditable(int row, int col){ return false; } 
    };

    table = new JTable( dataModel );
    table.setAlignmentX( 0.5f );
    table.setFont( new Font( fontName, Font.PLAIN, 10 ));
    table.setRowHeight( 12 );

    DefaultTableCellRenderer renderer = ( DefaultTableCellRenderer )table.getDefaultRenderer( String.class );

    header = table.getTableHeader();
    header.setReorderingAllowed( false );
    header.setResizingAllowed( false );
    header.setFont( new Font( fontName, Font.BOLD, 8 ));

    Box tableBox = box.createVerticalBox();
//    box.setBorder( BorderFactory.createLineBorder( Color.BLACK ));
    tableBox.add( header );
    tableBox.add( table );

    box.add( tableBox );

    print = new JButton( "Print" );
    print.addActionListener( this );

    JPanel panel = new JPanel();
    panel.add( print );
    add( panel, BorderLayout.SOUTH );
  }

  private void getAssignedButtons()
  {
    list.clear();
    Button[] buttons = deviceUpgrade.getRemote().getUpgradeButtons();

    for ( int i = 0; i < buttons.length; i++ )
    {
      Button b = buttons[ i ];
      if (( b.getFunction() != null ) ||
          ( b.getShiftedFunction() != null ) ||
          ( b.getXShiftedFunction() != null ))
        list.add( b );
    }
  }

  public void paint( Graphics g )
  {
    Graphics2D g2 = ( Graphics2D )g;
    g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON );

    super.paint( g2 );
  }

  public void update()
  {
    String text = deviceUpgrade.getDescription();
    if (( text == null ) || text.length() == 0 )
      text = " ";
    titleLabel.setText( text );
    text = deviceUpgrade.getRemote().getName() + " (" + 
           deviceUpgrade.getDeviceTypeAliasName() + '/' + 
           df.format( deviceUpgrade.getSetupCode()) + " - " +
           deviceUpgrade.getProtocol().getName() + ')';
    subtitleLabel.setText( text );
    getAssignedButtons();
    Remote r = deviceUpgrade.getRemote();
    headers[ 2 ] = r.getShiftLabel() + " Function";
    headers[ 3 ] = r.getXShiftLabel() + " Function";
    (( AbstractTableModel )table.getModel()).fireTableStructureChanged();

    // adjust column widths
//    JLabel l = new JLabel( "Shifted Function" );
//    l.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 4 ));
//    int width = l.getPreferredSize().width;
//
//    TableColumnModel columnModel = table.getColumnModel();
//    TableColumn column;
//
//    int cols = table.getModel().getColumnCount();
//    for ( int i = 0; i < cols; i++ )
//    {
//      column = columnModel.getColumn( i );
//      column.setMaxWidth( width );
//    }
//    table.doLayout();
  }                  
    
  public void actionPerformed( ActionEvent e )
  {
    PrinterJob pj = PrinterJob.getPrinterJob();
    pj.setPrintable( this );
    pj.printDialog();
    try
    {
      pj.print();
    }
    catch ( Exception printException )
    {
      printException.printStackTrace( System.err );
    }
  }  

  public int print(Graphics g, PageFormat pageFormat, int pageIndex)
   throws PrinterException
  {
    Graphics2D  g2 = (Graphics2D) g;
    g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                         RenderingHints.VALUE_ANTIALIAS_ON );

    g2.setColor( Color.black );
  
    if ( pageIndex >= 1 )
    {
      return NO_SUCH_PAGE;
    }
  
    g2.translate( pageFormat.getImageableX(), pageFormat.getImageableY());
    Dimension d = box.getSize();
    d.width = ( int )pageFormat.getImageableWidth();
    box.setSize( d );
    box.validate();
    box.paint( g2 );
  
//    tableView.paint( g2 );
//    g2.translate( 0f, pageIndex*pageHeightForTable);
//    g2.translate( 0f, - headerHeightOnPage );
//    g2.setClip(0, 0,(int) Math.ceil(tableWidthOnPage),
//                           (int)Math.ceil(headerHeightOnPage));
//    tableHeader.paint( g2 );//paint header at top
//    tableHeader.setFont( savedHeaderFont );
//  
//    tableView.setFont( savedTableFont );
      return Printable.PAGE_EXISTS;
  }

  private static DecimalFormat df = new DecimalFormat( "0000" );
  private Box box = null;
  private JLabel titleLabel = null;
  private JLabel subtitleLabel = null;
  private JTable table = null;
  private JTableHeader header = null;
  private JButton print = null;
  private Vector list = new Vector();
  private static String[] headers =
  { 
    "Button", "Normal Function", "Shifted Function", "XShifted Function" };
  }