package com.hifiremote.jp1;

import java.io.*;

public abstract class Parameter
{
  public Parameter( String name )
  {
    this( name, null );
  }

  public Parameter( String name, DefaultValue defaultValue )
  {
    setName( name );
    this.defaultValue = defaultValue;
  }

  public String getName(){ return name; }
  public void setName( String aName )
  {
    int semi = aName.indexOf( ';' );
    if ( semi == -1 )
      name = aName;
    else
    {
      name = aName.substring( 0, semi );
      displayName = aName.substring( semi + 1 );
    }
  }
  
  public String getDisplayName()
  {
    if ( displayName != null )
      return displayName;
    return name;
  }

  public abstract String getDescription();

  public DefaultValue getDefaultValue()
  {
    return defaultValue;
  }
  public void setDefault( DefaultValue value ){ defaultValue = value; }
  public void setDefault( int value )
  {
    setDefault( new DirectDefaultValue( new Integer( value ) ) );
  }

  public abstract Object getValue();

  public Object getValueOrDefault()
  {
    Object rc = getValue();
    if ( rc != null )
      return rc;
    if ( defaultValue != null )
      return defaultValue.value();
    else
      return null;
  }
  public abstract void setValue( Object value );

  protected String name;
  protected String displayName = null;
  protected DefaultValue defaultValue;
}
