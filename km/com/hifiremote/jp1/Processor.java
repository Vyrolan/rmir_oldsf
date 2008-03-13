package com.hifiremote.jp1;

public abstract class Processor
{
  public Processor( String name )
  {
    this( name, null, false );
  }
  
  public Processor( String name, boolean reverse )
  {
	  this( name, null, reverse );
  }

  public Processor( String name, String version )
  {
	this( name, version, false );
  }
  
  public Processor( String name, String version, boolean reverse )
  {
    this.name = name;
    this.version = version;
    this.reverse = reverse;
  }

  public void setVectorEditData( int[] opcodes, int[] addresses )
  {
    this.opCodes = opcodes;
    this.addresses = addresses;
  }


  public void setDataEditData( int min, int max )
  {
    minDataAddress = min;
    maxDataAddress = max;
  }

  public String getName(){ return name; }
  public String getVersion(){ return version; }
  public String getFullName()
  {
    if ( version == null )
      return name;
    else
      return name + '-' + version;
  }

  public String getEquivalentName()
  {
    return getFullName();
  }

  public Hex translate( Hex hex, Remote remote )
  {
    int vectorOffset = remote.getProtocolVectorOffset();
    int dataOffset = remote.getProtocolDataOffset();
    if (( vectorOffset != 0 ) || ( dataOffset != 0 ))
    {
      try
      {
        hex = ( Hex )hex.clone();
      }
      catch ( CloneNotSupportedException ex )
      {
        ex.printStackTrace( System.err );
      }
    }
    if ( vectorOffset != 0 )
      doVectorEdit( hex, vectorOffset );
    if ( dataOffset != 0 )
      doDataEdit( hex, dataOffset );
    return hex;
  }

  public Hex importCode( Hex code, String processorName )
  {
    return code;
  }

  public abstract short getInt( short[] data, int offset );
  
  public abstract void putInt( int val, short[] data, int offset );
  
  private void doVectorEdit( Hex hex, int vectorOffset )
  {
    short[] data = hex.getData();
    for ( int i = 0; i < data.length; i++ )
    {
      short opCode = data[ i ];
      for ( int j = 0; j < opCodes.length; j++ )
      {
        if ( opCode == opCodes [ j ])
        {
          int address = getInt( data, i + 1 );
          for ( int k = 0; k < addresses.length; k++ )
          {
            if ( addresses[ k ] == address )
            {
              address += vectorOffset;
              putInt( address, data, i + 1 );
              break;
            }
          }
          i += 2;
          break;
        }
      }
    }
  }

  private void doDataEdit( Hex hex, int dataOffset )
  {
    short[] data = hex.getData();

    for ( int i = 0; i < data.length - 1; i++ )
    {
      if ((( data[ i ] & Hex.ADD_OFFSET ) != 0 ) &&
          (( data[ i + 1 ] & Hex.ADD_OFFSET ) != 0 ))
      {
        int temp = getInt( data, i );
        if (( temp < minDataAddress ) || ( temp > maxDataAddress ))
          continue;
        temp += dataOffset;
        putInt( temp, data, i);
        i++;
      }
    }

    for ( int i = 0; i < data.length; i++ )
    {
      int temp = data[ i ];
      if (( temp & Hex.ADD_OFFSET ) != 0 )
      {
        temp &= 0xFF;
        temp += dataOffset;
        data[ i ] = ( short )( temp & 0xFF );
      }
    }
  }
  
  public String toString()
  {
    return getFullName();
  }

  private String name = null;
  private String version = null;
  private int[] opCodes = new int[ 0 ];
  private int[] addresses = new int[ 0 ];
  private int minDataAddress = 0x64;
  private int maxDataAddress = 0x80;
  private boolean reverse = false;
}
