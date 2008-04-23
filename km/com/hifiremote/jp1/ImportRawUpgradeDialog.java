package com.hifiremote.jp1;

import info.clearthought.layout.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.text.ParseException;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

public class ImportRawUpgradeDialog
  extends JDialog
  implements ActionListener, DocumentListener, ItemListener
{
  public ImportRawUpgradeDialog( JFrame owner, DeviceUpgrade deviceUpgrade )
  {
    super( owner, "Import Raw Upgrade", true );
    this.deviceUpgrade = deviceUpgrade;
    createGui( owner );
  }

  public ImportRawUpgradeDialog( JDialog owner, DeviceUpgrade deviceUpgrade )
  {
    super( owner, "Import Raw Upgrade", true );
    this.deviceUpgrade = deviceUpgrade;
    createGui( owner );
  }

  private void createGui( Component owner )
  {
    setLocationRelativeTo( owner );
    Container contentPane = getContentPane();

    double b = 5;        // space between rows and around border
    double c = 10;       // space between columns
    double f = TableLayout.FILL;
    double pr = TableLayout.PREFERRED;
    double size[][] =
    {
      { b, pr, c, pr, b },                         // cols
      { b, pr, b, pr, b, pr, b, pr, pr, b, pr, pr, b, pr, b }  // rows
    };
    TableLayout tl = new TableLayout( size );
    JPanel mainPanel = new JPanel( tl );
    contentPane.add( mainPanel, BorderLayout.CENTER );

    JLabel label = new JLabel( "Remote:" );
    mainPanel.add( label, "1, 1" );

    Remote[] remotes = RemoteManager.getRemoteManager().getRemotes();
    remoteList = new JComboBox( remotes );
    label.setLabelFor( remoteList );
    remoteList.setSelectedItem( deviceUpgrade.getRemote());
    remoteList.addActionListener( this );
    mainPanel.add( remoteList, "3, 1" );

    label = new JLabel( "Device Type:" );
    mainPanel.add( label, "1, 3" );

    String[] aliasNames = deviceUpgrade.getRemote().getDeviceTypeAliasNames();
    String alias = deviceUpgrade.getDeviceTypeAliasName();

    deviceTypeList = new JComboBox( aliasNames );
    label.setLabelFor( deviceTypeList );
    deviceTypeList.setMaximumRowCount( aliasNames.length );
    deviceTypeList.setSelectedItem( alias );
    mainPanel.add( deviceTypeList, "3, 3" );

    protocolGreaterThanFF = new JCheckBox( "Protocol > FF" );
    protocolGreaterThanFF.addItemListener( this );
    mainPanel.add( protocolGreaterThanFF, "3, 5" );
    protocolGreaterThanFF.setVisible( !deviceUpgrade.getRemote().usesTwoBytePID());

    label = new JLabel( "Upgrade Code:" );
    mainPanel.add( label, "1, 7, 3, 7" );
    upgradeCode = new JTextArea( 10, 40 );
    upgradeCode.getDocument().addDocumentListener( this );
    new TextPopupMenu( upgradeCode );
    label.setLabelFor( upgradeCode );
    mainPanel.add( new JScrollPane( upgradeCode ), "1, 8, 3, 8" );

    protocolLabel = new JLabel( "Protocol Code:" );
    mainPanel.add( protocolLabel, "1, 10, 3, 10" );
    protocolCode = new JTextArea( 10, 40 );
    protocolCode.getDocument().addDocumentListener( this );
    new TextPopupMenu( protocolCode );
    protocolLabel.setLabelFor( protocolCode );
    mainPanel.add( new JScrollPane( protocolCode ), "1, 11, 3, 11" );

    JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ));

    ok = new JButton( "OK" );
    ok.setEnabled( false );
    ok.setMnemonic( KeyEvent.VK_O );
    ok.addActionListener( this );
    buttonPanel.add( ok );

    cancel = new JButton( "Cancel" );
    cancel.setMnemonic( KeyEvent.VK_C );
    cancel.addActionListener( this );
    buttonPanel.add( cancel );

    mainPanel.add( buttonPanel, "1, 13, 3, 13" );

    pack();
    Rectangle rect = getBounds();
    int x = rect.x - rect.width / 2;
    int y = rect.y - rect.height / 2;
    setLocation( x, y );
  }

  public void actionPerformed( ActionEvent e )
  {
    Object source = e.getSource();
    if ( source == remoteList )
    {
      try
      {
        Remote remote = ( Remote )remoteList.getSelectedItem();
        remote.load();
        String[] aliasNames = remote.getDeviceTypeAliasNames();
        String alias = deviceUpgrade.getDeviceTypeAliasName();
        deviceTypeList.setModel( new DefaultComboBoxModel( aliasNames ));
        deviceTypeList.setMaximumRowCount( aliasNames.length );
        deviceTypeList.setSelectedItem( alias );
        protocolGreaterThanFF.setVisible( !remote.usesTwoBytePID());
        validateInput();
      }
      catch ( Exception ex )
      {
        ex.printStackTrace( System.err );
      }
    }
    else if ( source == ok )
    {
      userAction = JOptionPane.OK_OPTION;
      setVisible( false );
      try 
      {
        deviceUpgrade.importRawUpgrade( uCode,
                                        ( Remote )remoteList.getSelectedItem(),
                                        ( String )deviceTypeList.getSelectedItem(),
                                        pid, pCode );
      }
      catch ( ParseException pe )
      {
        JOptionPane.showMessageDialog( this, pe.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE );
      }
      dispose();
    }
    else if ( source == cancel )
    {
      userAction = JOptionPane.CANCEL_OPTION;
      setVisible( false );
      dispose();
    }
  }

  private void validateInput()
  {
    if ( uCode == null )
    {
      ok.setEnabled( false );
      protocolCode.setEnabled( false );
      protocolLabel.setText( "Protocol Code:" );
      protocolLabel.setEnabled( false );
      return;
    }

    Remote remote = ( Remote )remoteList.getSelectedItem();
    if ( remote.usesTwoBytePID())
    {
      pid = new Hex( uCode, 0, 2 );
    }
    else
    {
      short[] temp = new short[ 2 ];
      temp[ 0 ] = ( short )( protocolGreaterThanFF.isSelected() ? 1 : 0 );
      temp[ 1 ] = uCode.getData()[ 0 ];
      pid = new Hex( temp );
    }

    Protocol p = ProtocolManager.getProtocolManager().findProtocolForRemote( remote, pid );
    if ( p != null )
    {
      protocolLabel.setText( "Protocol Code:" );
      ok.setEnabled( true );
      return;
    }
    else
    {
      protocolLabel.setText( "Protocol Code for Unknown PID " + pid + " ** Required **" );
      protocolLabel.setEnabled( true );
      protocolCode.setEnabled( true );
    }

    if ( pCode == null )
    {
      ok.setEnabled( false );
      return;
    }

    ok.setEnabled( true );
  }

  // DocumentListener methods
  public void documentChanged( DocumentEvent e )
  {
    Document doc = e.getDocument();
    if ( doc == upgradeCode.getDocument())
    {
      String text = upgradeCode.getText().trim();
      if (( text == null ) || ( text.length() == 0 ))
      {
        uCode = null;
      }
      else
      {
        try
        {
          uCode = new Hex( text );
        }
        catch ( Exception ex )
        {
          uCode = null;
        }
      }
    }
    else if ( doc == protocolCode.getDocument())
    {
      String text = protocolCode.getText().trim();
      if (( text == null ) || ( text.length() == 0 ))
      {
        pCode = null;
      }
      else
        try
        {
          pCode = new Hex( text );
          if ( pCode.length() < 3 )
            pCode = null;
        }
        catch ( Exception ex )
        {
          pCode = null;
        }
    }
    validateInput();
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

  // ItemListener methods
  public void itemStateChanged( ItemEvent e )
  {
    validateInput();
  }

  private DeviceUpgrade deviceUpgrade = null;

  private JComboBox remoteList = null;
  private JComboBox deviceTypeList = null;
  private JCheckBox protocolGreaterThanFF = null;
  private JTextArea upgradeCode = null;
  private JLabel protocolLabel = null;
  private JTextArea protocolCode = null;
  private Hex uCode = null;
  private Hex pid = null;
  private Hex pCode = null;

  private JButton ok = null;
  private JButton cancel = null;
  private int userAction = JOptionPane.CANCEL_OPTION;
}
