package com.hifiremote.jp1;

import java.util.Properties;
import java.io.PrintWriter;

public class Function
{
  public Function(){}

  public Function( String name, Hex hex, String notes )
  {
    this.name = name;
    this.hex = hex;
    this.notes = notes;
  }

  public Function( String name )
  {
    this.name = name;
  }

  public boolean isExternal(){ return false; }

  public boolean isEmpty()
  {
    return ( name == null ) && ( hex == null ) && ( notes == null );
  }

  public void store( Properties props, String prefix )
  {
    if ( isEmpty())
      props.setProperty( prefix + ".name", "" );

    if ( name != null )
      props.setProperty( prefix + ".name", name );
    if ( hex != null )
      props.setProperty( prefix + ".hex", hex.toString());
    if ( notes != null )
      props.setProperty( prefix + ".notes", notes );
  }

  public void store( PropertyWriter out, String prefix )
  {
    if ( isEmpty())
      out.print( prefix + ".name", "" );

    if ( name != null )
      out.print( prefix + ".name", name );
    if ( hex != null )
      out.print( prefix + ".hex", hex.toString());
    if ( notes != null )
      out.print( prefix + ".notes", notes );
  }

  public void load( Properties props, String prefix )
  {
    String str = props.getProperty( prefix + ".name" );
    if ( str != null )
      setName( str );
    str = props.getProperty( prefix + ".hex" );
    if ( str != null )
      setHex( new Hex( str ));
    str = props.getProperty( prefix + ".notes" );
    if ( str != null )
      setNotes( str );
  }

  public Function setName( String name )
  {
    this.name = name;
    if ( label != null )
      label.setText( name );
    if ( item != null )
      item.setText( name );
    return this;
  }

  public Function setNotes( String notes )
  {
    this.notes = notes;
    if ( item != null )
      item.setToolTipText( notes );
    if ( label != null )
      label.setToolTipText( notes );
    return this;
  }

  public Function setHex( Hex hex )
  {
    this.hex = hex;
    return this;
  }

  public String toString()
  {
    return name;
  }
  public String getName(){ return name; }
  public String getNotes(){ return notes; }
  public Hex getHex(){ return hex; }

  public FunctionLabel getLabel()
  {
    if ( label == null )
    {
      label = new FunctionLabel( this );
      if ( assigned())
        label.showAssigned();
    }
    return label;
  }

  public FunctionItem getItem()
  {
    if ( item == null )
      item = new FunctionItem( this );
    return item;
  }

  public void addReference()
  {
    ++refCount;
    if ( label != null )
      label.showAssigned();
  }

  public void removeReference()
  {
    if ( refCount > 0 )
      --refCount;
    if (( refCount == 0 ) && ( label != null ))
      label.showUnassigned();
  }

  public boolean assigned()
  {
    return ( refCount > 0 );
  }

  public boolean[] getUseDefault()
  {
    return useDefault;
  }

  protected String name = null;
  protected String notes = null;
  protected Hex hex = null;
  private FunctionLabel label = null;
  private FunctionItem item = null;
  private int refCount = 0;
  private boolean[] useDefault = null;
}
