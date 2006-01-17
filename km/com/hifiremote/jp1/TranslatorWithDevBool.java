package com.hifiremote.jp1;

public class TranslatorWithDevBool
  extends Translator
{
  private int devIndex = 0;

  public TranslatorWithDevBool( String[] textParms )
  {
    super( new String[0] );
    int parmIndex = 0;
    for ( int i = 0; i < textParms.length; i ++ )
    {
      String text = textParms[ i ];
      if ( text.equalsIgnoreCase( "lsb" ))
        lsb = true;
      else if ( text.equalsIgnoreCase( "comp" ))
        comp = true;
      else
      {
        int val = Integer.parseInt( text );
        switch ( parmIndex )
        {
          case IndexIndex:
            devIndex = val;
            break;
          case IndexIndex + 1:
            index = val;
            break;
          case BitsIndex + 1:
            bits = val;
            break;
          case BitOffsetIndex + 1:
            bitOffset = val;
            break;
          case LsbOffsetIndex + 1:
            lsbOffset = val;
            break;
          case AdjustOffset + 1:
          {
            adjust = val;
            break;
          }
          default:
            break;
        }
        parmIndex++;
      }
    }
  }

  public void out( Hex hexData, Value[] parms, DeviceParameter[] devParms )
  {
    super.out( hexData, parms, devParms );
    Integer v = ( Integer )parms[ index ].getValue();
    Integer i = ( Integer )devParms[ devIndex ].getValueOrDefault();
    int val = ( i.intValue() << bits ) + v.intValue();
    parms[ index ] = new Value( new Integer( val ));
  }
}


