package com.hifiremote.jp1;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.text.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import info.clearthought.layout.*;

public class ManualSettingsDialog
  extends JDialog
  implements ActionListener, PropertyChangeListener, DocumentListener
{
  public ManualSettingsDialog( JDialog owner, ManualProtocol protocol )
  {
    super( owner, "Manual Settings", true );
    createGui( owner, protocol );
  }
  public ManualSettingsDialog( JFrame owner, ManualProtocol protocol )
  {
    super( owner, "Manual Settings", true );
    createGui( owner, protocol );
  }
  
  private void createGui( Component owner, ManualProtocol protocol )
  {
    setLocationRelativeTo( owner );
    Container contentPane = getContentPane();

    this.protocol = protocol;
    System.err.println( "protocol=" + protocol );

    {
      System.err.println( "Copying device parameters" );
      DeviceParameter[] parms = protocol.getDeviceParameters();
      if ( parms != null )
      {
        Translate[] xlators = protocol.getDeviceTranslators();
        for ( int i = 0; i < parms.length; i++ )
        {
          deviceParms.add( parms[ i ]);
          deviceTranslators.add( xlators[ i ]);
        }
      }
    }

    double b = 5;        // space between rows and around border
    double c = 10;       // space between columns
    double f = TableLayout.FILL;
    double pr = TableLayout.PREFERRED;
    double size[][] =
    {
      { b, pr, c, pr, b },                        // cols
      { b, pr, b, pr, b, f, b, f, b, pr, b, f }         // rows
    };
    TableLayout tl = new TableLayout( size );
    JPanel mainPanel = new JPanel( tl );
    contentPane.add( mainPanel, BorderLayout.CENTER );

    JLabel label = new JLabel( "Name:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 1" );
    name = new JTextField( protocol.getName());
    name.setEditable( false );
    name.getDocument().addDocumentListener( this );
    mainPanel.add( name, "3, 1" );

    label = new JLabel( "Protocol ID:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 3" );

    pid = new JFormattedTextField( new HexFormat( 2, 2 ));
    pid.addPropertyChangeListener( "value", this );
    mainPanel.add( pid, "3, 3" );

    // Protocol Code Table
    JPanel codePanel = new JPanel( new BorderLayout());
    JPanel codeTablePanel = new JPanel( new BorderLayout());
    codePanel.add( codeTablePanel, BorderLayout.NORTH );
    mainPanel.add( codePanel, "1, 5, 3, 5" );
    codeTablePanel.setBorder( BorderFactory.createTitledBorder( "Protocol code" ));
    TableModel codeTableModel = new TableModel();
    JTableX codeTable = new JTableX( codeTableModel );
    DefaultTableCellRenderer r = ( DefaultTableCellRenderer )codeTable.getDefaultRenderer( String.class );
    r.setHorizontalAlignment( SwingConstants.CENTER );
    codeTable.setDefaultEditor( Hex.class, new HexEditor());
    codeTablePanel.add( codeTable.getTableHeader(), BorderLayout.NORTH );
    codeTablePanel.add( codeTable, BorderLayout.CENTER );

    JLabel l = new JLabel( colNames [ 0 ]);
    l.setBorder( BorderFactory.createEmptyBorder( 0, 4, 0, 4 ));

    TableColumnModel columnModel = codeTable.getColumnModel();
    TableColumn column = columnModel.getColumn( 0 );
    int width = l.getPreferredSize().width;

    procs = ProcessorManager.getProcessors();
    for ( int i = 0; i < procs.length; i++ )
    {
      l.setText( procs[ i ].getFullName());
      width =  Math.max( width, l.getPreferredSize().width );
    }
    for ( int i = 0; i < procs.length; i++ )
    {
      column.setMinWidth( width );
      column.setMaxWidth( width );
      column.setPreferredWidth( width );
    }
    codeTable.doLayout();
    
    // Device Parameter Table
    deviceModel = new ParameterTableModel( deviceParms, deviceTranslators );

    deviceTable = new JTableX( deviceModel );
    SpinnerCellEditor editor = new SpinnerCellEditor( 0, 8, 1 );
    deviceTable.setDefaultEditor( Integer.class, editor );
    JScrollPane scrollPane = new JScrollPane( deviceTable );
    Box box = Box.createVerticalBox();
    box.setBorder( BorderFactory.createTitledBorder( "Device Parameters" ));
    box.add( scrollPane );
    mainPanel.add( box, "1, 7, 3, 7" );
    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ));
    addDevice = new JButton( "Add" );
    addDevice.addActionListener( this );
    buttonPanel.add( addDevice );
    editDevice = new JButton( "Edit" );
    editDevice.addActionListener( this );
    buttonPanel.add( editDevice );
    deleteDevice = new JButton( "Delete" );
    deleteDevice.addActionListener( this );
    buttonPanel.add( deleteDevice );
    box.add( buttonPanel );
    Dimension d = deviceTable.getPreferredScrollableViewportSize();
    d.height = deviceTable.getRowHeight() * 5;
    deviceTable.setPreferredScrollableViewportSize( d );

    label = new JLabel( "Raw Fixed Data:", SwingConstants.RIGHT );
    mainPanel.add( label, "1, 9" );
    rawHexData = new JTextField();
    mainPanel.add( rawHexData, "3, 9" );

    {
      System.err.println( "Copying comand parameters" );
      CmdParameter[] parms = protocol.getCommandParameters();
      if ( parms != null )
      {
        Translate[] xlators = protocol.getCmdTranslators();
        for ( int i = 0; i < parms.length; i++ )
        {
          cmdParms.add( parms[ i ]);
          cmdTranslators.add( xlators[ i ]);
        }
      }
    }

    commandModel = new ParameterTableModel( cmdParms, cmdTranslators );

    commandTable = new JTableX( commandModel );
    commandTable.setDefaultEditor( Integer.class, editor );
    scrollPane = new JScrollPane( commandTable );
    box = Box.createVerticalBox();
    box.setBorder( BorderFactory.createTitledBorder( "Command Parameters" ));
    box.add( scrollPane );
    mainPanel.add( box, "1, 11, 3, 11" );
    buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ));
    buttonPanel.add( new JButton( "Add" ));
    buttonPanel.add( new JButton( "Edit" ));
    buttonPanel.add( new JButton( "Delete" ));
    box.add( buttonPanel );
    d = commandTable.getPreferredScrollableViewportSize();
    d.height = commandTable.getRowHeight() * 5;
    commandTable.setPreferredScrollableViewportSize( d );

    buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ));

    view = new JButton( "View Ini" );
    view.setToolTipText( "View the protocols.ini entry for this protocol." );
    view.addActionListener( this );
    view.setEnabled( false );
    buttonPanel.add( view );

    buttonPanel.add( Box.createHorizontalGlue());

    ok = new JButton( "OK" );
    ok.addActionListener( this );
    buttonPanel.add( ok );

    cancel = new JButton( "Cancel" );
    cancel.addActionListener( this );
    buttonPanel.add( cancel );

    contentPane.add( buttonPanel, BorderLayout.SOUTH );

    Hex id = protocol.getID();
    pid.setValue( id );
    rawHexData.setText( protocol.getFixedData( new Value[ 0 ]).toString());

    pack();
    Rectangle rect = getBounds();
    int x = rect.x - rect.width / 2;
    int y = rect.y - rect.height / 2;
    setLocation( x, y );
  }

  private MaskFormatter createMaskFormatter( String mask )
  {
    MaskFormatter f = null;
    try
    {
      f = new MaskFormatter( "HH HH" );
    }
    catch (ParseException e)
    {
      e.printStackTrace( System.err );
    }
    return f;
  }

  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == addDevice )
    {
      String name =
        ( String )JOptionPane.showInputDialog( this,
                                               "Please enter a name for the device parameter.",
                                               "Parameter Name",
                                               JOptionPane.QUESTION_MESSAGE );
      if ( name == null )
        return;
      String type = ( String )JOptionPane.showInputDialog( this,
                                                        "How will the parameter \"" + name + "\" be presented to the user?",
                                                        "Device Parameter Type",
                                                        JOptionPane.QUESTION_MESSAGE,
                                                        null,
                                                        typeChoices,
                                                        typeChoices[ 0 ]);
      if ( type == null )
        return;
      int bits = 0;
      if ( type.equals( typeChoices[ 0 ]))
      {
        bits = 8;
        NumberDeviceParm parm = new NumberDeviceParm( name,
                                                      new DirectDefaultValue( new Integer( 0 )),
                                                      10,
                                                      bits );
        deviceParms.add( parm );

      }
      else if ( type.equals( typeChoices[ 1 ]))
      {
        bits = 8;
        JTextArea textArea = new JTextArea( 8, 20 );
        new TextPopupMenu( textArea );
        Box box = Box.createVerticalBox();
        box.add( new JLabel( "Enter the choices for paramter \"" + name + ",\" one on each line." ));
        box.add( new JScrollPane( textArea ));
        int temp = JOptionPane.showConfirmDialog( this, box, "Drop-down list choices", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE );
        if ( temp == JOptionPane.CANCEL_OPTION )
          return;
        StringTokenizer st = new StringTokenizer( textArea.getText(), "\n" );
        String[] choices = new String[ st.countTokens()];
        int i = 0;
        while ( st.hasMoreTokens())
        {
          choices[ i++ ] = st.nextToken().trim();
        }
        ChoiceDeviceParm parm = new ChoiceDeviceParm( name,
                                                    new DirectDefaultValue( new Integer( 0 )),
                                                    choices );
        deviceParms.add( parm );
      }
      else // Check box
      {
        bits = 1;
        FlagDeviceParm parm = new FlagDeviceParm( name,
                                                  new DirectDefaultValue( new Integer( 0 )));
        deviceParms.add( parm );

      }

      int index = deviceTranslators.size();
      Translator xlator = new Translator( false, false, index, bits, index * 8 );
      deviceTranslators.add( xlator );
      int newRow = deviceParms.size() - 1;
      deviceModel.fireTableRowsInserted( newRow, newRow );
    }
    else if ( source == editDevice )
    {
    }
    else if ( source == deleteDevice )
    {
      int row = deviceTable.getSelectedRow();
      if (( row < 0 ) || ( row >= deviceParms.size()))
        return;
      deviceParms.remove( row );
      deviceTranslators.remove( row );
      deviceModel.fireTableRowsDeleted( row, row );
    }
    else if ( source == deleteCommand )
    {
      int row = commandTable.getSelectedRow();
      if (( row < 0 ) || ( row >= deviceParms.size()))
        return;
      deviceParms.remove( row );
      deviceTranslators.remove( row );
      deviceModel.fireTableRowsDeleted( row, row );
    }
    else if ( source == view )
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter( sw );
      try
      {
        pw.println( "[" + protocol.getName() + "]" );
        pw.println( "PID=" + protocol.getID() );
        protocol.store( new PropertyWriter( pw ));
      }
      catch ( Exception ex )
      {
        ex.printStackTrace( System.err );
      }
      JTextArea ta = new JTextArea( sw.toString(), 10, 70 );
      new TextPopupMenu( ta );
      ta.setEditable( false );
//      ta.setLineWrap( true );
//      ta.setWrapStyleWord( true );
      JOptionPane.showMessageDialog( this, new JScrollPane( ta ), "Protocol.ini entry text", JOptionPane.PLAIN_MESSAGE );
    }
    else if ( source == ok )
    {
      userAction = JOptionPane.OK_OPTION;
      setVisible( false );
      dispose();
    }
    else if ( source == cancel )
    {
      userAction = JOptionPane.CANCEL_OPTION;
      setVisible( false );
      dispose();
    }
  }

  public ManualProtocol getProtocol()
  {
    if ( userAction != JOptionPane.OK_OPTION )
      return null;

    protocol.setDeviceParms( deviceParms );
    protocol.setDeviceTranslators( deviceTranslators );
    protocol.setCommandParms( cmdParms );
    protocol.setCommandTranslators( cmdTranslators );
    protocol.setRawHex( new Hex( rawHexData.getText()));

    return protocol;
  }

  // PropertyChangeListener methods
  public void propertyChange( PropertyChangeEvent e )
  {
    Object source = e.getSource();
    if ( source == pid )
    {
      Hex id = ( Hex )pid.getValue();
      System.err.println( "propertyChange:pid is " + id );
      boolean flag = ( id != null );
      ok.setEnabled( flag );
      view.setEnabled( flag );
      protocol.setID( id );
    }
  }

  // DocumentListener methods
  public void documentChanged( DocumentEvent e )
  {
    String text = name.getText();
    protocol.setName( text );
  }
  public void changedUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }
  public void insertUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }
  public void removeUpdate( DocumentEvent e )
  {
    documentChanged( e );
  }

  public class TableModel
    extends AbstractTableModel
  {
    public int getRowCount(){ return procs.length; }
    public int getColumnCount(){ return colNames.length; }
    public String getColumnName( int col ){ return colNames[ col ]; }
    public Class getColumnClass( int col ){ return classes[ col ]; }
    public boolean isCellEditable( int row, int col ){ return ( col == 1 ); }
    public Object getValueAt( int row, int col )
    {
      if ( col == 0 )
        return procs[ row ];
      else
        return protocol.getCode( procs[ row ]);
    }
    public void setValueAt( Object value, int row, int col )
    {
      if ( col == 1 )
      {
        protocol.setCode(( Hex )value, procs[ row ] );
        fireTableRowsUpdated( row, row );
      }
    }
  }

  private void importProtocolCode( String string )
  {
    StringTokenizer st = new StringTokenizer( string, "\n" );
    String text = null;
    String processor = null;
    while( st.hasMoreTokens())
    {
      while ( st.hasMoreTokens())
      {
        text = st.nextToken().toUpperCase();
        System.err.println( "got '" + text );
        if ( text.startsWith( "UPGRADE PROTOCOL 0 =" ))
        {
          StringTokenizer st2 = new StringTokenizer( text, "()=" );
          st2.nextToken(); // discard everything before the =
          String pidStr = st2.nextToken().trim();
          System.err.println( "Imported pid is " + pidStr );
          processor = st2.nextToken().trim();
          System.err.println( "processorName is " + processor );
          if ( processor.startsWith( "S3C8" ))
            processor = "S3C80";
          if ( st2.hasMoreTokens())
          {
            String importedName = st2.nextToken().trim();
            System.err.println( "importedName is " + importedName );
          }
          break;
        }
      }
      if ( st.hasMoreTokens())
      {
        text = st.nextToken(); // 1st line of code
        while ( st.hasMoreTokens())
        {
          String temp = st.nextToken();
          if ( temp.equals( "End" ))
            break;
          text = text + ' ' + temp;
        }
        System.err.println( "getting processor with name " + processor );
        Processor p = ProcessorManager.getProcessor( processor );
        if ( p != null )
          processor = p.getFullName();
        System.err.println( "Adding code for processor " + processor );
        System.err.println( "Code is "  + text );
        protocol.setCode( new Hex( text ), p );
        /*
        for ( int i = 0; i < procNames.length; i++ )
        {
          if ( procNames[ i ].equals( processor ))
            tableModel.fireTableRowsUpdated( i, i );
        }
        */
      }
    }
  }

  private ManualProtocol protocol = null;

  private java.util.List< DeviceParameter > deviceParms = new ArrayList< DeviceParameter >();
  private java.util.List< Translate > deviceTranslators = new ArrayList< Translate >();
  private java.util.List< CmdParameter > cmdParms = new ArrayList< CmdParameter >();
  private java.util.List< Translate > cmdTranslators = new ArrayList< Translate >();

  private ParameterTableModel deviceModel = null;
  private JTableX deviceTable = null;
  private ParameterTableModel commandModel = null;
  private JTableX commandTable = null;

  private JTextField name = null;

  private JFormattedTextField pid = null;

  private JTextField rawHexData = null;

  private JTextArea protocolCode = null;

  private JButton addDevice = null;
  private JButton editDevice = null;
  private JButton deleteDevice = null;

  private JButton addCommand = null;
  private JButton editCommand = null;
  private JButton deleteCommand = null;

  private JButton view = null;
  private JButton ok = null;
  private JButton cancel = null;
  private int userAction = JOptionPane.CANCEL_OPTION;
  private final static Object[] typeChoices = { "Numeric entry", "Drop-down list", "Check-box" };
  private final static Object[] bitChoices = { "8", "7", "6", "5", "4", "3", "2", "1" };
  private final static Object[] orderChoices =
  {
    "MSB (Most Significant Bit First)",
    "LSB (Least Significant Bit First)"
  };
  private final static String[] colNames = { "Processor", "Protocol Code" };
  private final static Class[] classes = { Processor.class, Hex.class };
  private static Processor[] procs = new Processor[ 0 ];


}
