package com.hifiremote.jp1;

import java.awt.Color;
import java.util.Arrays;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

public class ActivityFunctionTableModel extends JP1TableModel< Activity > implements ButtonEnabler
{
  public void set( Button btn, RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig; 
    if ( remoteConfig != null )
    {
      colorEditor = new RMColorEditor( remoteConfig.getOwner() );
      Remote remote = remoteConfig.getRemote();
      keyRenderer.setRemote( remote );
      keyEditor.setRemote( remote );
      macroEditor.setTitle( "Power Macro Editor" );
      macroEditor.setButtonEnabler( this );
      macroEditor.setRemoteConfiguration( remoteConfig );
      setData( new Activity[] { remoteConfig.getActivities().get( btn ) } );
      helpSettingBox.setModel( new DefaultComboBoxModel( helpSetting ) );
    }
  }
  
  @Override
  public void enableButtons( Button b, MacroDefinitionBox macroBox )
  {
    int limit = 15;
    if ( remoteConfig.getRemote().getAdvCodeBindFormat() == AdvancedCode.BindFormat.LONG )
      limit = 255;
    boolean canAdd = ( b != null ) && macroBox.isMoreRoom( limit );

    macroBox.add.setEnabled( canAdd && b.canAssignToPowerMacro() );
    macroBox.insert.setEnabled( canAdd && b.canAssignToPowerMacro() );
    macroBox.addShift.setEnabled( canAdd && b.canAssignShiftedToPowerMacro() );
    macroBox.insertShift.setEnabled( canAdd && b.canAssignShiftedToPowerMacro() );
    boolean xShiftEnabled = remoteConfig.getRemote().getXShiftEnabled();
    macroBox.addXShift.setEnabled( xShiftEnabled && canAdd && b.canAssignXShiftedToPowerMacro() );
    macroBox.insertXShift.setEnabled( xShiftEnabled && canAdd && b.canAssignXShiftedToPowerMacro() );
  }
  
  @Override
  public boolean isAvailable( Button b )
  {
    return  b.canAssignToPowerMacro() 
    || b.canAssignShiftedToPowerMacro() 
    || b.canAssignXShiftedToPowerMacro();
  }

  private static final String[] colNames =
  {
      "#", "Key", "Power Macro", "Audio Action", "Video Action", "Notes", "<html>Size &amp<br>Color</html>"
  };
  
  private static final String[] colPrototypeNames =
  {
      " 00 ", "Key__", "A power macro with a lot of keys_________", "Audio Action__", "Video Action__",
      "A reasonable length note", "Color_"
  };
  
  private static final Class< ? >[] colClasses =
  {
      Integer.class, Integer.class, Hex.class, String.class, String.class, Color.class
  };

  @Override
  public Class< ? > getColumnClass( int col )
  {
    return colClasses[ col ];
  }
  
  @Override
  public String getColumnName( int col )
  {
    return colNames[ col ];
  }
  @Override
  public String getColumnPrototypeName( int col )
  {
    return colPrototypeNames[ col ];
  }

  @Override
  public int getColumnCount()
  {
    int count = colNames.length - 1;
    if ( remoteConfig != null && remoteConfig.allowHighlighting() )
    {
      ++count;
    }
    return count;
  }
  
  @Override
  public boolean isCellEditable( int row, int col )
  {
    return col > 0;
  }
  
  @Override
  public boolean isColumnWidthFixed( int col )
  {
    return col != 2 && col != 5;
  }
  
  @Override
  public TableCellEditor getColumnEditor( int col )
  {
    switch ( col )
    {
      case 1:
        return keyEditor;
      case 2:
        return macroEditor;
      case 3:
      case 4:
        DefaultCellEditor editor = new DefaultCellEditor( helpSettingBox );
        editor.setClickCountToStart( RMConstants.ClickCountToStart );
        return editor;
      case 5:
        return selectAllEditor;
      case 6:
        return colorEditor;
      default:
        return null;
    }
  }
  
  @Override
  public TableCellRenderer getColumnRenderer( int col )
  {
    if ( col == 0 )
    {
      return new RowNumberRenderer();
    }
    else if ( col == 1 )
    {
      return keyRenderer;
    }
    else if ( col == 2 )
    {
      return new DefaultTableCellRenderer()
      {
        @Override
        protected void setValue( Object value )
        {
          if ( value == null )
            super.setValue( null );
          else
          {
            super.setValue( Macro.getValueString( ( Hex )value , remoteConfig ) );
          }
        }
      };
    }
    else if ( col == 6 )
    {
      return colorRenderer;
    }
    else
      return null;
  }
  
  @Override
  public Object getValueAt( int row, int column)
  {
    Activity activity = getRow( row );
    Macro macro = activity.getMacro();
    switch ( column )
    {
      case 0:
        return new Integer( row + 1 );
      case 1:
        return new Integer( macro.getKeyCode() );
      case 2:
        return macro.getData();
      case 3:
        return helpSetting[ 1 - activity.getAudioHelp() ];
      case 4:
        return helpSetting[ 1 - activity.getVideoHelp() ];
      case 5:
        return activity.getNotes();
      case 6:
        return activity.getHighlight();
      default:
        return null;
    }
  }
  
  @Override
  public void setValueAt( Object value, int row, int col )
  {
    Activity activity = getRow( row );
    Macro macro = activity.getMacro();
    if ( col == 1 )
    {
      macro.setKeyCode( ( Integer )value );
    }
    else if ( col == 2 )
    {
      macro.setData( ( Hex )value );
    }
    else if ( col == 3 )
    {
      activity.setAudioHelp( 1 - Arrays.asList( helpSetting ).indexOf( ( String )value ) );
    }
    else if ( col == 4 )
    {
      activity.setVideoHelp( 1 - Arrays.asList( helpSetting ).indexOf( ( String )value ) );
    }
    else if ( col == 5 )
    {
      activity.setNotes( ( String )value );
    }
    else if ( col == 6 )
    {
      activity.setHighlight( ( Color )value );
    }
    propertyChangeSupport.firePropertyChange( col == 5 ? "highlight" : "data", null, null );
  }
  
  private RemoteConfiguration remoteConfig = null;
  private KeyEditor keyEditor = new KeyEditor();
  private RMColorEditor colorEditor = null;
  private RMSetterEditor< Hex, MacroDefinitionBox > macroEditor = 
    new RMSetterEditor< Hex, MacroDefinitionBox >( MacroDefinitionBox.class );
  private SelectAllCellEditor selectAllEditor = new SelectAllCellEditor();
  private RMColorRenderer colorRenderer = new RMColorRenderer();
  private KeyCodeRenderer keyRenderer = new KeyCodeRenderer();
  private JComboBox helpSettingBox = new JComboBox();
  private String[] helpSetting = new String[] { "Regular", "Custom" };

}