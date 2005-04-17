package com.hifiremote.jp1;

import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

public abstract class HexParmEditorNode
  extends ProtocolEditorNode
{
  public HexParmEditorNode()
  {
     super( "Parameter", true );
     add( new TranslatorEditorNode());
  }

  public ProtocolEditorNode createChild()
  {
    return new TranslatorEditorNode();
  }

  public void print( PrintWriter pw )
  {
    pw.print( getName() + ':' );
    if ( type == NUMBER )
      pw.print( bits );
    else if ( type == CHOICE )
    {
      for ( Enumeration e = choices.elements(); e.hasMoreElements(); )
      {
        pw.print( e.nextElement());
        if ( e.hasMoreElements())
          pw.print( "|" );
      }
    }
    else if ( type == FLAG )
      pw.print( "bool" );
    if ( defaultValue != -1 )
      pw.print( "=" + defaultValue );
  }

  public boolean canDelete(){ return true; }

  private int bits = 8;
  public int getBits()
  {
    if ( type == NUMBER )
      return bits;
    else if ( type == FLAG )
      return 1;
    else // Choice
    {
      int numChoices = choices.size();
      int rc = 0;
      while ( numChoices > 0 )
      {
        numChoices /= 2;
        rc++;
      }
      if ( rc == 0 )
        rc = 1;
      return rc;
    }
  }
  public void setBits( int newBits ){ bits = newBits; }

  private int type = NUMBER;
  public void setType( int newType ){ type = newType; }
  public int getType(){ return type; }

  private Vector choices = new Vector();
  private int defaultValue = -1;

  public final static int NUMBER = 0;
  public final static int CHOICE = 1;
  public final static int FLAG = 2;
}
