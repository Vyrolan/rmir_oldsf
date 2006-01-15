package com.hifiremote.jp1;

public class RC5_5xComboTranslator
  extends Translate
{
  private final static int TYPE = 0;
  private final static int OBC = 1;
  private final static int RC5_DEVICE = 2;
  private final static int RC5X_DEVICE = 3;
  private final static int RC5X_SUB_DEVICE = 4;

  public RC5_5xComboTranslator( String[] textParms )
  {
    super( textParms );
  }

  public Integer getValue( Value value )
  {
    if ( value == null )
      return null;
    return ( Integer )value.getValue();
  }
    
  public void in( Value[] parms, Hex hexData, DeviceParameter[] devParms, int onlyIndex )
  {
    int type;
    Integer I = getValue( parms[ TYPE ]);
    int i;
    if ( I != null )
    {
      type = I.intValue();
      insert( hexData, 15, 1, type );
    }
    else
      type = extract( hexData, 15, 1 );

    if ( type == 0 ) // RC-5
    {
      insert( hexData, 0, 1, 0 );
      insert( hexData, 2, 1, 0 );
      insert( hexData, 14, 1, 0 );

      I = getValue( parms[ RC5_DEVICE ]);
      if ( I != null )
      {
        i = I.intValue();
        insert( hexData, 3, 5, complement( i, 5 ));
      }
      I = getValue( parms[ OBC ]);
      if ( I != null )
      {
        int obc = I.intValue();
        i = obc & 0x003F;
        insert( hexData, 8, 6, complement( i, 6 ));
        i = obc >> 6;
        insert( hexData, 1, 1, i );
      }
    }
    else // RC-5x
    {
      insert( hexData, 12, 1, 0 );
      I = getValue( parms[ RC5X_DEVICE ]);
      if ( I != null )
      {
        i = I.intValue();
        insert( hexData, 13, 2, i );
      }
      I = getValue( parms[ RC5X_SUB_DEVICE ]);
      if ( I != null )
      {
        i = I.intValue();
        insert( hexData, 0, 6, complement( i, 6 ));
      }
      I = getValue( parms[ OBC ]);
      if ( I != null )
      {
        i = I.intValue();
        insert( hexData, 6, 6, complement( i, 6 ));
      }
    }
  }

  public void out( Hex hexData, Value[] parms, DeviceParameter[] devParms )
  {
    int type = extract( hexData, 15, 1 );
    parms[ TYPE ] = new Value( type );
    if ( type == 0 ) // RC-5
    {
      parms[ RC5_DEVICE ] = new Value( complement( extract( hexData, 3, 5 ), 5 ));
      int obc = complement( extract( hexData, 8, 6 ), 6 );
      if ( extract( hexData, 1, 1 ) == 1 )
        obc += 64;
      parms[ OBC ] = new Value( obc );
      parms[ RC5X_DEVICE ] = new Value( null );
      parms[ RC5X_SUB_DEVICE ] = new Value( null );
    }
    else // RC-5X
    {
      parms[ RC5_DEVICE ] = new Value( null );
      int device = extract( hexData, 13, 2 );
      parms[ RC5X_DEVICE ] = new Value( device );
      int subDevice = complement( extract( hexData, 0, 6 ), 6 );
      int flag = (( Integer )devParms[ ( device * 2 ) + 1 ].getValueOrDefault()).intValue();
      if ( flag != 0 )
        subDevice += 64;      
      parms[ RC5X_SUB_DEVICE ] = new Value( subDevice ); 
      parms[ OBC ] = new Value( complement( extract( hexData, 6, 6 ), 6 ));
    }
  }
}
