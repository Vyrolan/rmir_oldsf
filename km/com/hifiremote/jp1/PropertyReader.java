package com.hifiremote.jp1;

import java.io.*;
import java.util.*;

public class PropertyReader
{
  public PropertyReader( BufferedReader reader )
  {
    this.reader = reader;
  }

  public Property nextProperty()
  {
    try
    {
      String line;
      do 
      {
        line = reader.readLine();
        if ( line == null )
          return null;
        line = line.trim();
      } while (( line.length() != 0 ) && (( line.charAt( 0 ) == '#' ) || ( line.charAt( 0 ) == '!' )));
      
      int pos = line.indexOf( '=' );
      if ( pos == -1 )
        pos = line.indexOf( ':' );
      if ( pos == -1 )
      {
        property.name = line;
        property.value = "";
        return property;
      }
      property.name = line.substring( 0, pos );
      line = line.substring( pos + 1 );
      while ( line.endsWith( "\\" ))
        line = line.substring( 0, line.length() - 1 ).trim() + reader.readLine().trim();
      property.value = decode( line );
    }
    catch ( IOException e )
    {
      e.printStackTrace( System.err );
    }
    return property;
  }

  public static String decode( String text )
  {
    StringBuffer buff = new StringBuffer( text.length());
    char[] chars = text.toCharArray();
    for ( int i = 0; i < chars.length; i++ )
    {
      char ch = chars[ i ];
      if ( ch == '\\' )
      {
        ch = chars[ ++i ];
        if ( ch == 'n' )
          buff.append( '\n' );
        else if ( ch == 't' )
          buff.append( '\t' );
        else if ( ch == 'r' )
          buff.append( '\r' );
        else if ( ch == 'u' )
        {
          String val = new String( chars, ++i, 4 );
          buff.append(( char )Integer.parseInt( val ));
          i += 4;
        }
        else 
          buff.append( ch );
      }
      else
        buff.append( ch );
    }
    return buff.toString();
  }
  
  public IniSection nextSection()
  {
    Property p = nextProperty();
    // skip empty lines
    while (( p != null ) && ( p.name.length() == 0 ))
      p = nextProperty();
    
    if ( p == null )
      return null;
    
    IniSection section = new IniSection();
    if ( p.name.charAt( 0 ) == '[' )
    {
      section.setName( p.name.substring( 1, p.name.length() - 1 ));
      p = nextProperty();
    }
    while (( p != null ) && ( p.name.length() != 0 ))
    {
      section.setProperty( p.name, p.value );
      p = nextProperty();
    }
    return section;
  }

  private BufferedReader reader = null;
  private Property property = new Property();
}
