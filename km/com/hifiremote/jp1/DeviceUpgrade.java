package com.hifiremote.jp1;

import java.util.*;
import java.io.*;
import java.text.*;
import javax.swing.*;
import java.awt.*;

public class DeviceUpgrade
{
  public DeviceUpgrade()
  {
    devTypeAliasName = deviceTypeAliasNames[ 0 ];
    initFunctions();
  }

  public void reset( Remote[] remotes, ProtocolManager protocolManager )
  {
    description = null;
    setupCode = 0;

    // remove all currently assigned functions
    remote.clearButtonAssignments();

    if ( remote == null )
      remote = remotes[ 0 ];
    devTypeAliasName = deviceTypeAliasNames[ 0 ];

    DeviceParameter[] devParms = protocol.getDeviceParameters();
    for ( int i = 0; i < devParms.length; i++ )
      devParms[ i ].setValue( null );

    Vector names = protocolManager.getNames();
    Protocol tentative = null;
    for ( Enumeration e = names.elements(); e.hasMoreElements(); )
    {
      String protocolName = ( String )e.nextElement();
      Protocol p = protocolManager.findProtocolForRemote( remote, protocolName );
      if ( p != null )
      {
        protocol = p;
        break;
      }
    }

    notes = null;
    file = null;

    functions.clear();
    initFunctions();

    extFunctions.clear();
  }

  private void initFunctions()
  {
    for ( int i = 0; i < defaultFunctionNames.length; i++ )
      functions.add( new Function( defaultFunctionNames[ i ]));
  }

  public void setDescription( String text )
  {
    description = text;
  }

  public String getDescription()
  {
    return description;
  }

  public void setSetupCode( int setupCode )
  {
    this.setupCode = setupCode;
  }

  public int getSetupCode()
  {
    return setupCode;
  }

  public void setRemote( Remote newRemote )
  {
    if (( remote != null ) && ( remote != newRemote ))
    {
      Button[] buttons = remote.getUpgradeButtons();
      Button[] newButtons = newRemote.getUpgradeButtons();
      for ( int i = 0; i < buttons.length; i++ )
      {
        Button b = buttons[ i ];
        Function f = b.getFunction();
        Function sf = b.getShiftedFunction();
        if (( f != null ) || ( sf != null ))
        {
          if ( f != null )
            b.setFunction( null );
          if ( sf != null )
            b.setShiftedFunction( null );

          Button newB = newRemote.findByStandardName( b );
          if ( newB != null )
          {
            if ( f != null )
              newB.setFunction( f );
            if ( sf != null )
              newB.setShiftedFunction( sf );
          }
        }
      }
    }
    remote = newRemote;
  }

  public Remote getRemote()
  {
    return remote;
  }

  public void setDeviceTypeAliasName( String name )
  {
    if ( name != null )
    {
      if ( remote.getDeviceTypeByAliasName( name ) != null )
      {
        devTypeAliasName = name;
        return;
      }
      System.err.println( "Unable to find device type with alias name " + name );
    }
    devTypeAliasName = deviceTypeAliasNames[ 0 ];
  }

  public String getDeviceTypeAliasName()
  {
    return devTypeAliasName;
  }

  public DeviceType getDeviceType()
  {
    return remote.getDeviceTypeByAliasName( devTypeAliasName );
  }

  public void setProtocol( Protocol protocol )
  {
    this.protocol = protocol;
  }

  public Protocol getProtocol()
  {
    return protocol;
  }

  public void setNotes( String notes )
  {
    this.notes = notes;
  }

  public String getNotes()
  {
    return notes;
  }

  public Vector getFunctions()
  {
    return functions;
  }

  public Function getFunction( String name )
  {
    Function rc = getFunction( name, functions );
    if ( rc == null )
      rc =  getFunction( name, extFunctions );
    return rc;
  }

  public Function getFunction( String name, Vector funcs )
  {
    Function rc = null;
    for ( Enumeration e = funcs.elements(); e.hasMoreElements(); )
    {
      Function func = ( Function )e.nextElement();
      if ( func.getName().equals( name ))
      {
        rc = func;
        break;
      }
    }
    return rc;
  }

  public Vector getExternalFunctions()
  {
    return extFunctions;
  }

  public File getFile(){ return file; }

  private int findDigitMapIndex()
  {
    Button[] buttons = remote.getUpgradeButtons();
    int[] digitMaps = remote.getDigitMaps();
    if (( digitMaps != null ) && ( protocol.getDefaultCmd().length() == 1 ))
    {
      for ( int i = 0; i < digitMaps.length; i++ )
      {
        int mapNum = digitMaps[ i ];
        int[] codes = DigitMaps.data[ mapNum ];
        int rc = -1;
        for ( int j = 0; ; j++ )
        {
          Function f = buttons[ j ].getFunction();
          if (( f != null ) && !f.isExternal())
            if (( f.getHex().getData()[ 0 ] & 0xFF ) == codes[ j ])
              rc = i + 1;
            else
              break;
          if ( j == 9 )
          {
            return rc;
          }
        }
      }
    }
    return -1;
  }

  public String getUpgradeText()
  {
    StringBuffer buff = new StringBuffer( 400 );
    buff.append( "Upgrade code 0 = " );
    DeviceType devType = remote.getDeviceTypeByAliasName( devTypeAliasName );
    byte[] id = protocol.getID().getData();
    int temp = devType.getNumber() * 0x1000 +
               ( id[ 0 ] & 1 ) * 0x0800 +
               setupCode - remote.getDeviceCodeOffset();

    byte[] deviceCode = new byte[2];
    deviceCode[ 0 ] = ( byte )(temp >> 8 );
    deviceCode[ 1 ] = ( byte )temp;

    buff.append( Hex.toString( deviceCode ));
    buff.append( " (" );
    buff.append( devTypeAliasName );
    buff.append( '/' );
    DecimalFormat df = new DecimalFormat( "0000" );
    buff.append( df.format( setupCode ));
    buff.append( ")\n " );
    buff.append( Hex.toString( id[ 1 ]));

    int digitMapIndex = -1;

    if ( !remote.getOmitDigitMapByte())
    {
      buff.append( ' ' );
      digitMapIndex = findDigitMapIndex();
      if ( digitMapIndex == -1 )
        buff.append( "00" );
      else
      {
        byte[] array = new byte[ 1 ];
        array[ 0 ] = ( byte )digitMapIndex;
        buff.append( Hex.toString( array ));
      }
    }

    ButtonMap map = devType.getButtonMap();
    if ( map != null )
    {
      buff.append( ' ' );
      buff.append( Hex.toString( map.toBitMap( digitMapIndex != -1 )));
    }

    buff.append( ' ' );
    buff.append( protocol.getFixedData().toString());

    if ( map != null )
    {
      byte[] data = map.toCommandList( digitMapIndex != -1 );
      if (( data != null ) && ( data.length != 0 ))
      {
        buff.append( "\n " );
        buff.append( Hex.toString( data, 16 ));
      }
    }

    Button[] buttons = remote.getUpgradeButtons();
    boolean hasKeyMoves = false;
    int startingButton = 0;
    int i;
    for ( i = 0; i < buttons.length; i++ )
    {
      Button b = buttons[ i ];
      Function f = b.getFunction();
      Function sf = b.getShiftedFunction();
      Function xf = b.getXShiftedFunction();
      if ((( f != null ) && (( map == null ) || !map.isPresent( b ) || f.isExternal())) ||
          (( sf != null ) && ( sf.getHex() != null )) || (( xf != null) && ( xf.getHex() != null )))
      {
        hasKeyMoves = true;
        break;
      }
    }
    if ( hasKeyMoves )
    {
      deviceCode[ 0 ] = ( byte )( deviceCode[ 0 ] & 0xF7 );
      buff.append( "\nKeyMoves" );
      for ( ; i < buttons.length; i++ )
      {
        Button button = buttons[ i ];
        byte[] keyMoves = button.getKeyMoves( deviceCode, devType, remote );
        if (( keyMoves != null ) && keyMoves.length > 0 )
        {
          buff.append( "\n " );
          buff.append( Hex.toString( keyMoves ));
        }
      }
    }

    buff.append( "\nEND" );

    return buff.toString();
  }

  public void store()
    throws IOException
  {
    store( file );
  }

  public static String valueArrayToString( Value[] parms )
  {
    StringBuffer buff = new StringBuffer( 200 );
    for ( int i = 0; i < parms.length; i++ )
    {
      if ( i > 0 )
        buff.append( ' ' );
      buff.append( parms[ i ].getUserValue());
    }
    return buff.toString();
  }

  public Value[] stringToValueArray( String str )
  {
    StringTokenizer st = new StringTokenizer( str );
    Value[] parms = new Value[ st.countTokens()];
    for ( int i = 0; i < parms.length; i++ )
    {
      String token = st.nextToken();
      Integer val = null;
      if ( !token.equals( "null" ))
      {
        if ( token.equals( "true" ))
          val = new Integer( 1 );
        else if ( token.equals( "false" ))
          val = new Integer( 0 );
        else
          val = new Integer( token );
      }
      parms[ i ] = new Value( val, null );
    }
    return parms;
  }

  public static void print( PrintWriter out, String name, String value )
  {
    out.print( name );
    out.print( '=' );
    
    if ( value != null )
    {
      boolean escapeSpace = true;
      for ( int i = 0; i < value.length(); i++ )
      {
        char ch = value.charAt( i );
        if ( ch == ' ' )
        {
          if ( escapeSpace )
            out.print( "\\ " );
          else
            out.print( ch );
        }
        else
        {
          escapeSpace = false;
          switch ( ch )
          {
            case '\\':
              out.print( "\\\\" );
              break;
            case '\t':
              out.print( "\\t" );
              break;
            case '\n':
              out.print( "\\n" );
              break;
            case '\r':
              out.print( "\\r" );
              break;
            case '#':
              out.print( "\\#" );
              break;
            case '!':
              out.print( "\\!" );
              break;
            case '=':
              out.print( "\\=" );
              break;
            case ':':
              out.print( "\\:" );
              break;
            default:
              out.print( ch );
              break;
          }
        }
      }
    }
    out.println();
  }

  public void store( File file )
    throws IOException
  {
    this.file = file;
    PrintWriter out = new PrintWriter( new FileWriter( file ));

    if ( description != null )
      print( out, "Description", description );
    print( out, "Remote.name", remote.getName());
    print( out, "Remote.signature", remote.getSignature());
    print( out, "DeviceType", devTypeAliasName );
    DeviceType devType = remote.getDeviceTypeByAliasName( devTypeAliasName );
    print( out, "DeviceIndex", Integer.toHexString( devType.getNumber()));
    print( out, "SetupCode", Integer.toString( setupCode ));
    protocol.store( out );
    if ( notes != null )
      print( out, "Notes", notes );
    int i = 0;
    for ( Enumeration e = functions.elements(); e.hasMoreElements(); i++ )
    {
      Function func = ( Function )e.nextElement();
      func.store( out, "Function." + i );
    }
    i = 0;
    for ( Enumeration e = extFunctions.elements(); e.hasMoreElements(); i++ )
    {
      ExternalFunction func = ( ExternalFunction )e.nextElement();
      func.store( out, "ExtFunction." + i );
    }
    Button[] buttons = remote.getUpgradeButtons();
    for ( i = 0; i < buttons.length; i++ )
    {
      Button b = buttons[ i ];
      Function f = b.getFunction();

      String fstr;
      if ( f == null )
        fstr = "null";
      else
        fstr = f.getName();

      Function sf = b.getShiftedFunction();
      String sstr;
      if ( sf == null )
        sstr = "null";
      else
        sstr = sf.getName();

      Function xf = b.getXShiftedFunction();
      String xstr;
      if ( xf == null )
        xstr = null;
      else
        xstr = xf.getName();
      if (( f != null ) || ( sf != null ) || ( xf != null ))
      {
        print( out, "Button." + Integer.toHexString( b.getKeyCode()),
                           fstr + '|' + sstr + '|' + xstr );
      }

    }
    out.flush();
    out.close();
  }

  public void load( File file, Remote[] remotes,
                    ProtocolManager protocolManager )
    throws Exception
  {
    this.file = file;
    Properties props = new Properties();
    FileInputStream in = new FileInputStream( file );
    props.load( in );
    in.close();

    String str = props.getProperty( "Description" );
    if ( str != null )
      description = str;
    str = props.getProperty( "Remote.name" );
    String sig = props.getProperty( "Remote.signature" );
    int index = Arrays.binarySearch( remotes, str );
    if ( index < 0 )
    {
      // build a list of similar remote names, and ask the user to pick a match.
      Vector similarRemotes = new Vector();
      for ( int i = 0; i < remotes.length; i++ )
      {
        if ( remotes[ i ].getName().indexOf( str ) != -1 )
          similarRemotes.add( remotes[ i ]);
      }

      Object[] simRemotes = null;
      if ( similarRemotes.size() > 0 )
        simRemotes = similarRemotes.toArray();
      else
        simRemotes = remotes;

      String message = "Could not find an exact match for the remote \"" + str + "\".  Choose one from the list below:";

      Object rc = ( Remote )JOptionPane.showInputDialog( null,
                                                         message,
                                                         "Upgrade Load Error",
                                                         JOptionPane.ERROR_MESSAGE,
                                                         null,
                                                         simRemotes,
                                                         simRemotes[ 0 ]);
      if ( rc == null )
        return;
      else
        remote = ( Remote )rc;
    }
    else
      remote = remotes[ index ];
    remote.load();
    index = -1;
    str = props.getProperty( "DeviceIndex" );
    if ( str != null )
      index = Integer.parseInt( str, 16 );
    setDeviceTypeAliasName( props.getProperty( "DeviceType" ) );
    setupCode = Integer.parseInt( props.getProperty( "SetupCode" ));

    Hex pid = new Hex( props.getProperty( "Protocol", "0200" ));
    String name = props.getProperty( "Protocol.name", "" );
    String variantName = props.getProperty( "Protocol.variantName", "" );

    if ( name.equals( "Manual Settings" ))
    {
      protocol = new ManualProtocol( name, pid, props );
      protocolManager.add( protocol );
    }
    else
    {
      protocol = protocolManager.findNearestProtocol( name, pid, variantName );

      if ( protocol == null )
      {
        JOptionPane.showMessageDialog( null,
                                       "No protocol found with name=\"" + name +
                                       "\", ID=" + pid.toString() +
                                       ", and variantName=\"" + variantName + "\"",
                                       "File Load Error", JOptionPane.ERROR_MESSAGE );
        return;
      }
    }

    str = props.getProperty( "ProtocolParms" );
    if (( str != null ) && ( str.length() != 0 ))
      protocol.setDeviceParms( stringToValueArray( str ));

    notes = props.getProperty( "Notes" );

    functions.clear();
    int i = 0;
    while ( true )
    {
      Function f = new Function();
      f.load( props, "Function." + i );
      if ( f.isEmpty())
      {
        break;
      }
      functions.add( f );
      i++;
    }

    extFunctions.clear();
    i = 0;
    while ( true )
    {
      ExternalFunction f = new ExternalFunction();
      f.load( props, "ExtFunction." + i, remote );
      if ( f.isEmpty())
      {
        break;
      }
      extFunctions.add( f );
      i++;
    }

    Button[] buttons = remote.getUpgradeButtons();
    for ( i = 0; i < buttons.length; i++ )
    {
      Button b = buttons[ i ];
      str = props.getProperty( "Button." + Integer.toHexString( b.getKeyCode()));
      if ( str == null )
      {
        continue;
      }
      StringTokenizer st = new StringTokenizer( str, "|" );
      str = st.nextToken();
      Function func = null;
      if ( !str.equals( "null" ))
      {
        func = getFunction( str );
        b.setFunction( func );
      }
      str = st.nextToken();
      if ( !str.equals( "null" ))
      {
        func = getFunction( str );
        b.setShiftedFunction( func );
      }
      if ( st.hasMoreTokens())
      {
        str = st.nextToken();
        if ( !str.equals( "null" ))
        {
          func = getFunction( str );
          b.setXShiftedFunction( func );
        }
      }
    }
  }

  private String getNextField( StringTokenizer st, String delim )
  {
    String rc = null;
    if ( st.hasMoreTokens())
    {
      rc = st.nextToken();
      if ( rc.equals( delim ))
        rc = null;
      else if ( st.hasMoreTokens())
        st.nextToken(); // skip delim
    }
    return rc;
  }

  public void importFile( File file, Remote[] remotes,
                    ProtocolManager protocolManager )
    throws Exception
  {
    System.err.println( "DeviceUpgrade.importFile()" );
    BufferedReader in = new BufferedReader( new FileReader( file ));

    String line = in.readLine(); // line 1
    String token = line.substring( 0, 5 );
    if ( !token.equals( "Name:" ))
    {
      System.err.println( "The file \"" + file + "\" is not a valid KM upgrade file!" );
      // Bad file!
      return;
    }
    String delim = line.substring( 5, 6 );
    StringTokenizer st = new StringTokenizer( line, delim );
    st.nextToken();
    description = st.nextToken();

    String protocolLine = in.readLine(); // line 3
    String manualLine = in.readLine(); // line 4

    line = in.readLine(); // line 5
    st = new StringTokenizer( line, delim );
    st.nextToken();
    token = st.nextToken();
    setupCode = Integer.parseInt( token );
    token = st.nextToken();
    String str = token.substring( 5 );

    int index = Arrays.binarySearch( remotes, str );
    if ( index < 0 )
    {
      // build a list of similar remote names, and ask the user to pick a match.
      // First check if there is a slash in the name;
      String[] subNames = new String[ 0 ];
      System.err.println( "Searching for slashes in " + str );
      int slash = str.indexOf( '/' );
      System.err.println( "slash=" + slash );
      if ( slash != -1 )
      {
        System.err.println( "Got a multi-model remote to import: " + str );
        int count = 2;
        while (( slash = str.indexOf( '/', slash + 1 )) != -1 )
          count++;
        subNames = new String[ count ];
        StringTokenizer nameTokenizer = new StringTokenizer( str, " /" );
        for ( int i = 0; i < count; i++ )
        {
          subNames[ i ] = nameTokenizer.nextToken();
          System.err.println( "Added subName " + subNames[ i ]);
        }
      }
      else
      {
        subNames = new String[ 1 ];
        StringTokenizer nameTokenizer = new StringTokenizer( str );
        subNames[ 0 ] = nameTokenizer.nextToken();
      }
      Vector similarRemotes = new Vector();
      for ( int i = 0; i < remotes.length; i++ )
      {
        for ( int j = 0; j < subNames.length; j++ )
        {
          if ( remotes[ i ].getName().indexOf( subNames[ j ]) != -1 )
          {
            similarRemotes.add( remotes[ i ]);
            break;
          }
        }
      }

      Remote[] simRemotes = new Remote[ 0 ];
      if ( similarRemotes.size() > 0 )
        simRemotes = ( Remote[] )similarRemotes.toArray( simRemotes );
      else
        simRemotes = remotes;

      if ( simRemotes.length == 1 )
        remote = simRemotes[ 0 ];
      else
      {
        String message = "Could not find an exact match for the remote \"" + str + "\".  Choose one from the list below:";

        Object rc = ( Remote )JOptionPane.showInputDialog( null,
                                                           message,
                                                           "Upgrade Load Error",
                                                           JOptionPane.ERROR_MESSAGE,
                                                           null,
                                                           simRemotes,
                                                           simRemotes[ 0 ]);
        if ( rc == null )
          return;
        else
          remote = ( Remote )rc;
      }
    }
    else
      remote = remotes[ index ];

    in.readLine(); // skip line 5
    line = in.readLine(); // line 6
    Hex pid = null;
    int equals = line.indexOf( '=' );
    if (( equals != -1 ) && line.substring( 0, equals + 1 ).equalsIgnoreCase( "Upgrade Code 0 =" ))
    {
      byte[] id = new byte[ 2 ];
      int temp = Integer.parseInt( line.substring( 17, 19 ), 16 );
      if (( temp & 8 ) != 0 )
        id[ 0 ] = 1;

      line = in.readLine(); // line 7
      temp = Integer.parseInt( line.substring( 0, 2 ), 16 );
      id[ 1 ] = ( byte )temp;
      pid = new Hex( id );
      System.err.println( "Imported protocol id is " + pid );
    }
    else
      in.readLine(); // line 7

    remote.load();
    token = st.nextToken();
    str = token.substring( 5 );

    if ( remote.getDeviceTypeByAliasName( str ) == null )
    {
      String rc = null;
      String msg = "Remote \"" + remote.getName() + "\" does not support the device type " +
      str + ".  Please select one of the supported device types below to use instead.\n";
      while ( rc == null )
      {
        rc = ( String )JOptionPane.showInputDialog( null,
                                                    msg,
                                                    "Unsupported Device Type",
                                                    JOptionPane.ERROR_MESSAGE,
                                                    null,
                                                    remote.getDeviceTypeAliasNames(),
                                                    null );
      }
      str = rc;
    }
    setDeviceTypeAliasName( str );

    String buttonStyle = st.nextToken();
    st = new StringTokenizer( protocolLine, delim, true );
    st.nextToken(); // skip header
    st.nextToken(); // skip delim
    String protocolName = st.nextToken();  // protocol name
    st.nextToken(); // skip delim

    if ( protocolName.equals( "Manual Settings" ))
    {
      System.err.println( "protocolName=" + protocolName );
      StringTokenizer manual = new StringTokenizer( manualLine, delim );
      manual.nextToken(); // skip header
      pid = new Hex( manual.nextToken()); // pid
      System.err.println( "pid=" + pid );
      int byte2 = Integer.parseInt( manual.nextToken().substring( 0, 1 ));
      System.err.println( "byte2=" +  byte2 );
      String signalStyle = manual.nextToken();
      System.err.println( "SignalStyle=" + signalStyle );
      String bitsStr = manual.nextToken();
      int devBits = Integer.parseInt( bitsStr.substring( 0, 1 ), 16);
      int cmdBits = Integer.parseInt( bitsStr.substring( 1 ), 16 );
      System.err.println( "devBits=" + devBits + " and cmdBits=" + cmdBits );

      Vector values = new Vector();

      str = st.nextToken(); // Device 1
      if ( !str.equals( delim ))
      {
        st.nextToken(); // skip delim
        values.add( new Integer( str ));
      }

      str = st.nextToken(); // Device 2
      if ( !str.equals( delim ))
      {
        st.nextToken(); // skip delim
        values.add( new Integer( str ));
      }

      str = st.nextToken(); // Device 3
      if ( !str.equals( delim ))
      {
        st.nextToken(); // skip delim
        values.add( new Integer( str ));
      }

      str = st.nextToken(); // Raw Fixed Data
      if ( str.equals( delim ))
        str = "";
      byte[] rawHex = Hex.parseHex( str );

      protocol = new ManualProtocol( protocolName, pid, byte2, signalStyle, devBits, values, rawHex, cmdBits );
      protocolManager.add( protocol );
    }
    else
    {
//    protocol = protocolManager.findProtocolForRemote( remote, protocolName );
      protocol = protocolManager.findNearestProtocol( protocolName, pid, null );

      if ( protocol == null )
      {
        protocol = protocolManager.findProtocolByOldName( remote, protocolName );
  
        if ( protocol == null )
        {
          JOptionPane.showMessageDialog( null,
                                         "No protocol found with name=\"" + protocolName +
                                         "\" for remote \"" + remote.getName(),
                                         "Import Failure", JOptionPane.ERROR_MESSAGE );
          return;
        }
      }
  
      DeviceParameter[] devParms = protocol.getDeviceParameters();
      for ( int i = 0; i < devParms.length; i++ )
      {
        // Skip over Flag parms because KM didn't have these.
        if ( devParms[ i ].getClass() == FlagDeviceParm.class )
          continue;
  
        token = st.nextToken();
        Object val = null;
        if ( token.equals( delim ))
          val = null;
        else
        {
          st.nextToken(); // skip delim
          if ( token.equals( "true" ))
            val = new Integer( 1 );
          else if ( token.equals( "false" ))
            val = new Integer( 0 );
          else
            val = new Integer( token );
        }
        devParms[ i ].setValue( val );
      }
    }

    for ( int i = 8; i < 35; i++ )
      in.readLine();

    // compute cmdIndex
    boolean useOBC = false;
    boolean useEFC = false;
    if ( buttonStyle.equals( "OBC" ))
    {
      useOBC = true;
    }
    else if ( buttonStyle.equals( "EFC" ))
      useEFC = true;

    int obcIndex = -1;
    CmdParameter[] cmdParms = protocol.getCommandParameters();
    for ( obcIndex = 0; obcIndex < cmdParms.length; obcIndex++ )
    {
      if ( cmdParms[ obcIndex ].getName().equals( "OBC" ))
        break;
    }

    functions.clear();

    Vector unassigned = new Vector();
    Vector usedFunctions = new Vector();
    for ( int i = 0; i < 128; i++ )
    {
      line = in.readLine();
      st = new StringTokenizer( line, delim, true );
      token = getNextField( st, delim ); // get the name (field 1)
      if (( token != null ) && ( token.length() == 5 ) &&
          token.startsWith( "num " ) && Character.isDigit( token.charAt( 4 )))
        token = token.substring( 4 );

      Function f = getFunction( token, usedFunctions );
      if ( f == null )
      {
        f = new Function();
        f.setName( token );
      }

      token = getNextField( st, delim );  // get the function code (field 2)
      if ( token != null )
      {
        Hex hex = protocol.getDefaultCmd();
        if ( useOBC )
          protocol.setValueAt( obcIndex, hex, new Integer( token ));
        else if ( useEFC )
          protocol.efc2hex( new EFC( token ), hex );

        token = getNextField( st, delim ); // get byte2 (field 3)
        if ( token != null )
          protocol.setValueAt( obcIndex - 1, hex, new Integer( token ));

        f.setHex( hex );
      }
      else
      {
        token = getNextField( st, delim ); // skip field 3
      }
      String actualName = getNextField( st, delim ); // get assigned button name (field 4)

      if (( actualName != null ) && actualName.length() == 0 )
        actualName = null;
      String buttonName = null;
      if ( actualName != null )
        buttonName = genericButtonNames[ i ];
      Button b = null;
      if ( buttonName != null )
        b = remote.findByStandardName( new Button( buttonName, null, ( byte )0 ));

      token = getNextField( st, delim );  // get normal function (field 5)
      if (( buttonName != null ) && ( token != null ) &&
           Character.isDigit( token.charAt( 0 )) &&
           Character.isDigit( token.charAt( 1 )) &&
           ( token.charAt( 2 ) == ' ' ) &&
           ( token.charAt( 3 ) == '-' ) &&
           ( token.charAt( 4 ) == ' ' ))
      {
        String name = token.substring( 5 );
        if (( name.length() == 5 ) && name.startsWith( "num " ) &&
              Character.isDigit( name.charAt( 4 )))
          name = name.substring( 4 );

        Function func = null;
        if (( f.getName() != null ) && f.getName().equals( name ))
          func = f;
        else
          func = getFunction( name, functions );
        if ( func == null )
        {
          func = new Function();
          func.setName( name );
          usedFunctions.add( func );
        }

        if ( b == null )
        {
          Vector temp = new Vector( 2 );
          temp.add( name );
          temp.add( buttonName );
          unassigned.add( temp );
        }
        else
          b.setFunction( func );
      }

      token = getNextField( st, delim );  // get notes (field 6)
      if ( token != null )
        f.setNotes( token );

      if ( !f.isEmpty())
        functions.add( f );

      // skip to field 13
      for ( int j = 7; j <= 13; j++ )
        token = getNextField( st, delim );

      if ( token != null )
      {
        String name = token.substring( 5 );
        if (( name.length() == 5 ) && name.startsWith( "num " ) &&
              Character.isDigit( token.charAt( 4 )))
          name = name.substring( 4 );
        Function func = getFunction( name, functions );
        if ( func == null )
        {
          func = new Function();
          func.setName( name );
          usedFunctions.add( func );
        }
        if ( b == null )
        {
          Vector temp = new Vector( 2 );
          temp.add( name );
          temp.add( "shift-" + buttonName );
          unassigned.add( temp );
        }
        else
          b.setShiftedFunction( func );
      }
    }

    while (( line = in.readLine()) != null )
    {
      line = in.readLine();
      st = new StringTokenizer( line, delim );
      token = getNextField( st, delim );
      if ( token != null )
      {
        if ( token.equals( "Line Notes:" ) || token.equals( "Notes:" ))
        {
          StringBuffer buff = new StringBuffer();
          boolean first = true;
          while (( line = in.readLine()) != null )
          {
            st = new StringTokenizer( line, delim );
            if ( st.hasMoreTokens())
            {
              token = st.nextToken();
              if ( token.startsWith( "EOF Marker" ))
                break;
              if ( first )
                first = false;
              else
                buff.append( "\n" );
              buff.append( token );
            }
            else
              buff.append( "\n" );
          }
          notes = buff.toString().trim();
          if ( protocol.getClass() == ManualProtocol.class )
            protocol.importUpgradeCode( remote, notes );
          
        }
      }
    }
    if ( !unassigned.isEmpty())
    {
      String message = "Some of the functions defined in the imported device upgrade " +
                       "were assigned to buttons that could not be matched by name. " +
                       "The functions and the corresponding button names are listed below." +
                       "\n\nUse the Button or Layout panel to assign those functions properly.";

      JFrame frame = new JFrame( "Import Failure" );
      Container container = frame.getContentPane();

      JTextArea text = new JTextArea( message );
      text.setEditable( false );
      text.setLineWrap( true );
      text.setWrapStyleWord( true );
      text.setBackground( container.getBackground() );
      container.add( text, BorderLayout.NORTH );
      Vector titles = new Vector();
      titles.add( "Function name" );
      titles.add( "Button name" );
      JTable table = new JTable( unassigned, titles );
      container.add( new JScrollPane( table ), BorderLayout.CENTER );
      frame.pack();
      frame.show();
    }
  }

  public static final String[] getDeviceTypeAliasNames()
  {
    return deviceTypeAliasNames;
  }

  public void autoAssignFunctions()
  {
    autoAssignFunctions( functions );
    autoAssignFunctions( extFunctions );
  }

  private void autoAssignFunctions( Vector funcs )
  {
    Button[] buttons = remote.getUpgradeButtons();
    for ( Enumeration e = funcs.elements(); e.hasMoreElements(); )
    {
      Function func = ( Function )e.nextElement();
      if ( func.getHex() != null )
      {
        for ( int i = 0; i < buttons.length; i++ )
        {
          Button b = buttons[ i ];
          if ( b.getFunction() == null )
          {
            if ( b.getName().equalsIgnoreCase( func.getName()) ||
                 b.getStandardName().equalsIgnoreCase( func.getName()))
            {
              b.setFunction( func );
              break;
            }
          }
        }
      }
    }
  }

  private String description = null;
  private int setupCode = 0;
  private Remote remote = null;
  private String devTypeAliasName = null;
  private Protocol protocol = null;
  private String notes = null;
  private Vector functions = new Vector();
  private Vector extFunctions = new Vector();
  private File file = null;

  private static final String[] deviceTypeAliasNames =
  {
    "Cable", "TV", "VCR", "CD", "Tuner", "DVD", "SAT", "Tape", "Laserdisc",
    "DAT", "Home Auto", "Misc Audio", "Phono", "Video Acc", "Amp", "PVR", "OEM Mode"
  };

  private static final String[] defaultFunctionNames =
  {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "vol up", "vol down", "mute",
    "channel up", "channel down",
    "power", "enter", "tv/vcr",
    "last (prev ch)", "menu", "program guide", "up arrow", "down arrow",
    "left arrow", "right arrow", "select", "sleep", "pip on/off", "display",
    "pip swap", "pip move", "play", "pause", "rewind", "fast fwd", "stop",
    "record", "exit", "surround", "input toggle", "+100", "fav/scan",
    "device button", "next track", "prev track", "shift-left", "shift-right",
    "pip freeze", "slow", "eject", "slow+", "slow-", "X2", "center", "rear"
  };

  private final static String[] genericButtonNames =
  {
    "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
    "vol up", "vol down", "mute",
    "channel up", "channel down",
    "power", "enter", "tv/vcr", "prev ch", "menu", "guide",
    "up arrow", "down arrow", "left arrow", "right arrow", "select",
    "sleep", "pip on/off", "display", "pip swap", "pip move",
    "play", "pause", "rewind", "fast fwd", "stop", "record",
    "exit", "surround", "input", "+100", "fav/scan",
    "device button", "next track", "prev track", "shift-left", "shift-right",
    "pip freeze", "slow", "eject", "slow+", "slow-", "x2", "center", "rear",
    "phantom1", "phantom2", "phantom3", "phantom4", "phantom5", "phantom6",
    "phantom7", "phantom8", "phantom9", "phantom10",
    "setup", "light", "theater",
    "macro1", "macro2", "macro3", "macro4",
    "learn1", "learn2", "learn3", "learn4",
    "button85", "button86", "button87", "button88", "button89", "button90",
    "button91", "button92", "button93", "button94", "button95", "button96",
    "button97", "button98", "button99", "button100", "button101", "button102",
    "button103", "button104", "button105", "button106", "button107", "button108",
    "button109", "button110", "button112", "button113", "button114", "button115",
    "button116", "button117", "button118", "button119", "button120", "button121",
    "button122", "button123", "button124", "button125", "button126", "button127",
    "button128", "button129", "button130", "button131", "button132", "button133",
    "button134", "button135", "button136"
  };
}
