package com.hifiremote.jp1;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import com.hifiremote.jp1.AssemblerOpCode.AddressMode;
import com.hifiremote.jp1.AssemblerOpCode.OpArg;
import com.hifiremote.jp1.AssemblerOpCode.Token;
import com.hifiremote.jp1.assembler.CommonData;
import com.hifiremote.jp1.assembler.S3C80data;

public class AssemblerTableModel extends JP1TableModel< AssemblerItem >
{
  private Hex hex = null;
  private List< AssemblerItem > itemList = new ArrayList< AssemblerItem >();
  private int burstUnit = 0;
  private int pfCount = 0;
  private int pdCount = 0;
  private short[] data = null;
  private Integer[] pf = new Integer[ 5 ];
  
  private static final String[] colNames =
  {
      "Addr", "Code", "Label", "Op", "Op Args", "Comments"
  };
  
  private static final String[] colPrototypeNames =
  {
      "0000", "00 00 00 00", "XMITIR_", "AAAAA", "DCBUF+1, DCBUF+2_", "Carrier OFF: 99.999 uSec"
  };
  
  public AssemblerTableModel()
  {
    setData( itemList );
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
    return 6;
  }
  
  @Override
  public boolean isColumnWidthFixed( int col )
  {
    return col <= 1;
  }
  
  @Override
  public boolean isCellEditable( int row, int col )
  {
    return ( col > 0 );
  }
  
  @Override
  public TableCellEditor getColumnEditor( int col )
  {
    return selectAllEditor;
  }
  
  @Override
  public TableCellRenderer getColumnRenderer( int col )
  {
    return assemblerCellRenderer;
  }
  
  public ManualSettingsDialog dialog = null;
  
  @Override
  public Object getValueAt( int row, int column )
  {
    AssemblerItem item = getRow( row );
    switch ( column )
    {
      case 0:
        if ( item.getAddress() == 0 )
          return "";
        String hexStr = Integer.toHexString( item.getAddress() );
        hexStr = "0000".substring( hexStr.length() ) + hexStr;
        return hexStr.toUpperCase();
      case 1:
        return item.getHex();
      case 2:
        return item.getLabel();
      case 3:
        return item.getOperation();
      case 4:
        return item.getArgumentText();
      case 5:
        int n = item.getErrorCode();
        return n > 0 ? AssemblerItem.getError( n ) : item.getComments();
      default:
        return null;
    }
  }
  
  @Override
  public void setValueAt( Object value, int row, int column )
  {
    if ( row == itemList.size() - 1 )
    {
      itemList.add( new AssemblerItem() );
    }
    AssemblerItem item = getRow( row );
    String text = ( String )value;
    switch ( column )
    {
      case 1:
        item.setHex( new Hex( text ) );
        return;
      case 2:
        item.setLabel( text );
        return;
      case 3:
        item.setOperation( text );
        return;
      case 4:
        item.setArgumentText( text );
        return;
      case 5:
        item.setComments( text );
        return;
      default:
        return;
    }
  }

  public Hex getHex()
  {
    return hex;
  }
  
  public void setHex( Hex hex )
  {
    this.hex = hex;
  }

  public static class DisasmState
  {
    public boolean useFunctionConstants = true;
    public boolean useRegisterConstants = true;
    public List< Integer > absUsed = new ArrayList< Integer >();
    public List< Integer > zeroUsed = new ArrayList< Integer >();
    public List< Integer > relUsed = new ArrayList< Integer >();
    public boolean toRC = false;
    public boolean toW = false;
  }
  
  public Hex assemble( Processor processor )
  {
    pfCount = 0;
    pdCount = 0;
    int addr = processor.getRAMAddress();
    
    // Get start address and check S3C80 versus S3F80
    for ( AssemblerItem item : itemList )
    {
      if ( item.getOperation().equals( "ORG" ) )
      {
        for ( Token t : OpArg.getArgs( item.getArgumentText(), null, null ) ) addr = t.value;
      }
    }
    if ( processor instanceof S3C80Processor )
    {
      processor = ProcessorManager.getProcessor( ( addr & 0xC000  ) == 0xC000 ? "S3F80" : "S3C80" );
    }
    dialog.setProcessor( processor, addr );

    LinkedHashMap< String, String > asmLabels = processor.getAsmLabels();
    
    // Locate all labels
    for ( AssemblerItem item : itemList )
    {
      if ( !item.getLabel().isEmpty() )
      {
        String lbl = item.getLabel().trim().toUpperCase();
        String txt = null;
        if ( lbl.endsWith( ":" )) lbl = lbl.substring( 0, lbl.length() - 1 );
        if ( item.getOperation().equals( "EQU" ) )
        {
          txt = item.getArgumentText().toUpperCase();
          AssemblerOpCode opCode = new AssemblerOpCode();
          opCode.setName( "EQU" );
          opCode.setLength( 0 );
          item.setOpCode( opCode );
        }
        else
        {
          // Set dummy value where final value not yet known
          txt = "FFFFH";
        }
        asmLabels.put( lbl, txt );
      }
    }
    
    // Main assembly loop handles all items not involving relative addresses
    int length = 0;
    for ( AssemblerItem item : itemList )
    {
      String op = item.getOperation();
      if ( op.isEmpty() )
      {
        continue;
      }
      else if ( Arrays.asList( "DB", "DW", "ORG" ).contains( op ) )
      {
        item.setErrorCode( 0 );
        OpArg args = OpArg.getArgs( item.getArgumentText(), null, asmLabels );
        AssemblerOpCode opCode = new AssemblerOpCode();
        opCode.setName( op );
        opCode.setLength( 0 );
        AddressMode mode = opCode.getMode();
        mode.length = op.equals( "ORG" ) ? 0 : op.equals( "DB" ) ? args.size() : 2 * args.size();
        for ( int i = 0; i < args.size() - 1; i++ ) mode.outline += "%X, ";
        if ( args.size() > 0 ) mode.outline += "%X";
        if ( !args.outline.equals( mode.outline ) || op.equals( "ORG" ) && args.size() > 1 ) item.setErrorCode( 2 );
        Hex hx = new Hex( mode.length );
        
        for ( Token t : args )
        {
          if ( t.value < 0 || t.value > ( op.equals( "DB" ) ? 0xFF : 0xFFFF ) ) item.setErrorCode( 5 );
        }
        
        int n = 0;
        if ( item.getErrorCode() == 0 ) for ( Token t : args )
        {
          if ( op.equals( "ORG" ) )
          {
            addr = t.value;
          }
          else if ( op.equals( "DB" ) )
          {
            hx.set( ( short )( int )t.value, n++ );
          }
          else // "DW"
          {
            hx.put( t.value, n );
            n += 2;
          }
        }
        item.setHex( hx );
        item.setOpCode( opCode );
        if ( !op.equals( "ORG" ) ) item.setAddress( addr );
      }
      else if ( !op.equals( "EQU" ) )
      {
        item.setAddress( addr );
        item.assemble( processor, asmLabels, false );
        // Replace dummy label values with final ones
        if ( !item.getLabel().isEmpty() )
        {
          String txt = item.getLabel();
          if ( txt.endsWith( ":" )) txt = txt.substring( 0, txt.length() - 1 );
          asmLabels.put( txt, Integer.toString( addr ) );
        }
      }
      addr += item.getLength();
      length += item.getLength();
    }
    
    // Assemble those items that do involve relative addresses
    boolean valid = true;
    Hex hexOut = new Hex( length );
    int n = 0;
    for ( AssemblerItem item : itemList )
    {
      if ( item.getOpCode() == null ) continue;
      if ( item.getOpCode().getMode().relMap != 0 )
      {
        item.assemble( processor, asmLabels, true );
      }
      valid = valid && ( item.getErrorCode() == 0 );
      if ( valid && item.getHex() != null ) 
      {
        hexOut.put( item.getHex(), n );
        String op = item.getOperation();
        if ( n == processor.getStartOffset() && ( op.equals( "JR" ) || op.equals( "BRA" ) ) )
        {
          pdCount = item.getHex().getData()[ 1 ] + processor.getStartOffset() - 3;
        }
      }
      n += item.getLength();
    }
    if ( valid )
    {
      if ( pdCount > 0 )
      {
        pfCount = 1;
        for ( ; pfCount < pdCount && pfCount < processor.getZeroSizes().get( "PF0" ) && hexOut.getData()[ pfCount + 4 ] >> 7 == 1; pfCount++ );
        pdCount -= pfCount;
      }
      data = hexOut.getData();
      return hexOut;
    }
    else
    { 
      pdCount = 0;
      return null;
    }
  }

  public void disassemble( Hex hexD, Processor processor )
  {
    itemList.clear();
    this.hex = ( hexD == null ) ? new Hex( 0 ) : new Hex( hexD );
    List< Integer > labelAddresses = new ArrayList< Integer >();
    Arrays.fill( dialog.getBasicValues(), ( short )0 );
    Arrays.fill( dialog.getPfValues(), null );
    Arrays.fill( dialog.getPdValues(), null );
    pfCount = 0;
    pdCount = 0;
    int addr = processor.getRAMAddress(); 
    dialog.setDataStyle( processor.getDataStyle() );
    dialog.setProcessor( processor, addr );

    if ( hex != null && hex.length() > 0 )
    {
           
      if ( processor instanceof S3C80Processor 
          && ( ( S3C80Processor )processor ).testCode( hex ) == S3C80Processor.CodeType.NEW )
      {
        addr = S3C80Processor.newRAMAddress;  // S3C8+ code
        processor = ProcessorManager.getProcessor( "S3F80" );
        dialog.setProcessor( processor, addr );
      }
      DisasmState state = new DisasmState();
      state.useFunctionConstants = dialog.useFunctionConstants.isSelected();
      state.useRegisterConstants = dialog.useRegisterConstants.isSelected();
      state.toRC = dialog.rcButton.isSelected();
      state.toW = dialog.wButton.isSelected();
      dialog.setAbsUsed( state.absUsed );
      dialog.setZeroUsed( state.zeroUsed );
      
      // Add ORG statement
      AssemblerItem item = new AssemblerItem();
      item.setOperation( "ORG" );
      AssemblerOpCode opCode = new AssemblerOpCode();
      opCode.setName( "ORG" );
      opCode.setLength( 0 );
      opCode.getMode().length = 0;
      item.setOpCode( opCode );
      String format = processor.getAddressModes().get( "EQU4" ).format;
      item.setArgumentText( String.format( format, addr ) );
      itemList.add( item );

      dbOut( 0, processor.getStartOffset(), addr, 0, processor );
      Hex pHex = hex.subHex( processor.getStartOffset() );
      short[] data = pHex.getData();
      addr += processor.getStartOffset();

      // Find addresses that need labelling, from all relative addressing modes
      int index = 0;
      while ( index < pHex.length() )
      {
        AssemblerOpCode oc  = processor.getOpCode( pHex.subHex( index ) );
        AddressMode mode = oc.getMode();

        for ( int i = 0; ( mode.relMap >> i ) != 0; i++ )
        {
          if ( ( ( mode.relMap >> i ) & 1 ) == 1 )
          {
            int n = index + oc.getLength() + mode.nibbleBytes + i;
            if ( n < data.length )
            {
              int newAddr = addr + data[ n ] + n + 1 - ( data[ n ] > 0x7F ? 0x100 : 0 );
              if ( !labelAddresses.contains( newAddr ) )
              {
                labelAddresses.add( newAddr );
              }
            }
          }
        }

        if ( index == 0 && ( oc.getName().equals( "JR" ) || oc.getName().equals( "BRA" ) ) )
        {
          index += data[ 1 ];
        }
        index += oc.getLength() + mode.length;
      }
      Collections.sort( labelAddresses );
      LinkedHashMap< Integer, String > labels = new LinkedHashMap< Integer, String >();
      for ( int i = 0; i < labelAddresses.size(); i++ )
      {
        labels.put( labelAddresses.get( i ), "L" + i );
      }
      
      // Disassemble
      index = 0;
      while ( index < pHex.length() )
      {
        item = new AssemblerItem( addr + index, pHex.subHex( index ) );
        int opLength = item.disassemble( processor, labels, state );
        
        if ( opLength == 0 )
        {
          dbOut( index, hex.length(), addr, processor.getStartOffset(), processor );
          break;
        }

        itemList.add( item );
      
        if ( index == 0 && ( item.getOperation().equals( "JR" ) || item.getOperation().equals( "BRA" ) ) )
        {
          int skip = data[ 1 ];
          pfCount = dbOut( index + 2, index + 2 + skip, addr, processor.getStartOffset(), processor );
          pdCount = skip + processor.getStartOffset() - pfCount - 3;
          dialog.interpretPFPD();
          index += data[ 1 ];
        }
        index += opLength;
      }
      
      // Create EQU statements for any unidentified labels (which are likely to be errors)
      int n = 0;
      for ( Integer address : labels.keySet() )
      {
        if ( state.relUsed.contains( address ) ) continue;
        item = new AssemblerItem();
        item.setLabel( labels.get( address) + ":" );
        item.setOperation( "EQU" );
        format = processor.getAddressModes().get( "EQU4" ).format;
        item.setArgumentText( String.format( format, address ) );
        itemList.add( n++, item );

      }
      // Create EQU statements for any used absolute address labels
      n = 0;
      Collections.sort( state.absUsed );
      if ( state.useFunctionConstants )
      {
        for ( int address : state.absUsed )
        {
          item = new AssemblerItem();
          item.setLabel( processor.getAbsLabels().get( address ) + ":" );
          item.setOperation( "EQU" );
          format = processor.getAddressModes().get( "EQU4" ).format;
          item.setArgumentText( String.format( format, address ) );
          itemList.add( n++, item );
        }
      }

      // Create EQU statements for any used zero-page or register address labels
      n = 0;
      Collections.sort( state.zeroUsed );
      if ( state.useRegisterConstants )
      {
        for ( int address : state.zeroUsed )
        {
          item = new AssemblerItem();
          item.setZeroLabel( processor, address, null, ":" );
          item.setOperation( "EQU" );
          format = null;
          if ( processor.getAddressModes().get( "EQUR" ) == null )
          {
            format = processor.getAddressModes().get( "EQU2" ).format;
          }
          else
          {
            format = processor.getAddressModes().get( "EQUR" ).format;
          }  
          item.setArgumentText( String.format( format, address ) );
          itemList.add( n++, item );
        }
      }
    }
    itemList.add( new AssemblerItem() );
    fireTableDataChanged();
  }

  public int dbOut( int start, int end, int ramAddress, int offset, Processor p )
  {
    // Set addresses and indexes to correspond to full hex code rather than subhex
    // used by disassemble().
    ramAddress -= offset;
    start += offset;
    end += offset;
    data = hex.getData();
    int pfIndex = 5;
    int pdIndex = 0;
    for ( int i = start; i < end;  )
    {
      AssemblerItem item = new AssemblerItem();
      item.setErrorCode( 0 );
      AssemblerOpCode opCode = new AssemblerOpCode();
      item.setOpCode( opCode );
      opCode.setLength( 0 );
      AddressMode mode = opCode.getMode();
      item.setOperation( "DB" );
      int n = Math.min( 4, end - i );
      String comments = null;
      String rp = p.getRegisterPrefix();
      if ( i < 5 )
      {
        n = 1;
        double time = 0;
        switch ( i - ( p.getStartOffset() == 0 ? 2 : 0 ) )
        {
          case 0:
            dialog.getBasicValues()[ 0 ] = data[ i ];
            time = ( data[ i ] + p.getCarrierOnOffset() ) * 1000000.0 / p.getOscillatorFreq();
            comments = "Carrier ON: " + String.format( "%.3f", time ) + "uSec";
            break;
          case 1:
            dialog.getBasicValues()[ 1 ] = data[ i ];
            time = ( data[ i ] + p.getCarrierTotalOffset() - p.getCarrierOnOffset() ) * 1000000.0 / p.getOscillatorFreq();
            comments = "Carrier OFF: " + String.format( "%.3f", time ) + "uSec";
            break;
          case 2:
            dialog.getBasicValues()[ 2 ] = data[ i ];
            comments = "dev " + ( data[ i ] >> 4 ) + ", cmd " + ( data[ i ] & 0x0F ) + " bytes";
            break;
        }
      }
      else if ( i == pfIndex )
      {
        n = 1;
        if ( i < dialog.getPfValues().length + 5 )
        {
          dialog.getPfValues()[ i - 5 ] = data[ i ];
        }
        comments = String.format( "pf%X: %s%02X", i - 5, rp, p.getZeroAddresses().get( "PF0" ) + i - 5 );
        if ( data[ i ] >> 7 == 1 )
        {
          pfIndex++;  // Another pf follows
        }
        pdIndex = i + 1;
        if ( pfIndex > p.getZeroSizes().get( "PF0" ) + 4 )
        {
          comments = "** Error **";   // Too many pf's
          pfIndex--;
        }
      }
      else if ( i == pdIndex )
      {
        int val = i - pfIndex - 1;
        int za = p.getZeroAddresses().get( "PD00" );
        int pdLimit = dialog.getPdValues().length;
        int pdSize = p.getZeroSizes().get( "PD00" );
        if ( val < pdLimit ) dialog.getPdValues()[ val ] = data[ i ];
        if ( val == pdSize - 1 || i == end - 1 )
        {
          n = 1;
          if ( val < pdSize ) comments = String.format( "pd%02X: %s%02X", val, rp, za + val );
        }
        else
        {
          n = 2;
          item.setOperation( "DW" );
          if ( val < pdSize - 1 ) comments = String.format( "pd%02X/pd%02X: %s%02X/%s%02X", val, val + 1, rp, za + val, rp, za + val + 1 );
          if ( val < pdLimit - 1 ) dialog.getPdValues()[ val + 1 ] = data[ i + 1 ];
        }
        pdIndex += n;
        if ( pdIndex > pfIndex + p.getZeroSizes().get( "PD00" ) )
        {
          pdIndex--;
        }
      }

      String argText = "";
      item.setAddress( ramAddress + i );
      item.setHex( hex.subHex( i, n ) );
      item.setComments( comments );
      opCode.setName( item.getOperation() );
      if ( item.getOperation().equals( "DB" ) )
      {
        mode.length = 1;
        for ( int j = 0; j < n; j++ )
        {
          if ( j > 0 )
          {
            argText += ", ";
          }
          argText += String.format( p.getAddressModes().get( "EQU2" ).format, data[ i + j ] );
        }
      }
      else // DW
      {
        mode.length = 2;
        argText = String.format( p.getAddressModes().get( "EQU4" ).format, hex.get( i ) );
      }
      item.setArgumentText( argText );
      i += n;
      itemList.add(  item  );
    }
    return pfIndex - 4;   // Count of PF values when processed
  }
  
//  public void interpretPFPD( Processor p, int ramAddress )
//  {
//    // DataStyle values:
//    //   0 = S3C80
//    //   1 = HCS08
//    //   2 = 6805-RC16/18
//    //   3 = 6805-C9
//    //   4 = P8/740
//    
//    // Set ramAddress to correspond to full hex code rather than subhex
//    // used by disassemble().
//    int offset = ( ramAddress - p.getRAMAddress() ) & 0xF;
//    ramAddress -= offset;
//    int dataStyle = p.getDataStyle();
//    int pd = 5 + pfCount;
//    Arrays.fill( pf, null );
//    for ( int i = 0; i < pfCount; i++ )
//    {
//      pf[ i ] = ( int )data[ i + 5 ];
//    }
//    
//    if ( ( (DefaultComboBoxModel )dialog.devBits1.getModel() ).getSize() == 0 )
//    {
//      // Populate those combo boxes whose content is fixed
//      dialog.populateComboBox( dialog.devBits1, CommonData.to8 );
//      dialog.populateComboBox( dialog.cmdBits1, CommonData.to8 );
//      dialog.populateComboBox( dialog.xmit0rev, CommonData.noYes );
//      dialog.populateComboBox( dialog.leadInStyle, CommonData.leadInStyle );
//      dialog.populateComboBox( dialog.offAsTotal, CommonData.noYes );
//      dialog.populateComboBox( dialog.useAltLeadOut, CommonData.noYes );
//    }
//    
//    boolean is2 = dialog.devBytes.getSelectedIndex() == 2;
//    dialog.devBits1lbl.setText( is2 ? "Bits/Dev1" : "Bits/Dev" );
//    dialog.devBits2lbl.setVisible( is2 );
//    dialog.populateComboBox( dialog.devBits2, is2 ? CommonData.to8 : null );
//    dialog.devBits2.setEnabled( is2 );
//
//    is2 = dialog.cmdBytes.getSelectedIndex() == 2 && ( dataStyle < 3 );
//    dialog.cmdBits1lbl.setText( is2 ? "Bits/Cmd1" : "Bits/Cmd" );
//    dialog.cmdBits2lbl.setVisible( is2 );
//    dialog.populateComboBox( dialog.cmdBits2, is2 ? CommonData.to8 : null );
//    dialog.cmdBits2.setEnabled( is2 );
//    
//    dialog.burstMidFrameLbl.setVisible( dataStyle < 3 );
//    dialog.burstMidFrame.setEnabled( dataStyle < 3 );
//    dialog.populateComboBox( dialog.burstMidFrame, ( dataStyle < 3 ) ? CommonData.noYes : null );
//    dialog.afterBitsLbl.setVisible( dataStyle < 3 );
//    dialog.afterBits.setEnabled( dataStyle < 3 );
//    dialog.populateComboBox( dialog.leadOutStyle, ( dataStyle < 3 ) ? CommonData.leadOutStyle012 : CommonData.leadOutStyle34 );
//    dialog.altFreqLbl.setVisible( dataStyle < 3 );
//    dialog.altFreq.setEnabled( dataStyle < 3 );
//    dialog.altDutyLbl.setVisible( dataStyle < 3 );
//    dialog.altDuty.setEnabled( dataStyle < 3 );
//
//    if ( dataStyle < 3 )
//    {
//      dialog.populateComboBox( dialog.sigStruct, CommonData.sigStructs012 );
//      dialog.populateComboBox( dialog.devBitDbl, CommonData.bitDouble012 );
//      dialog.populateComboBox( dialog.cmdBitDbl, CommonData.bitDouble012 );
//      dialog.populateComboBox( dialog.rptType, CommonData.repeatType012 );
//      dialog.populateComboBox( dialog.rptHold, CommonData.repeatHeld012 );
//      
//      dialog.devBits1.setSelectedIndex( ( pdCount > 0 && data[ pd + 0 ] <= 8 ) ? data[ pd + 0 ] : 0 );
//      dialog.cmdBits1.setSelectedIndex( ( pdCount > 1 && data[ pd + 1 ] <= 8 ) ? data[ pd + 1 ] : 0 );
//      if ( dialog.devBits2.isEnabled() )
//      {
//        int n = ( dataStyle < 2 ) ? 0x10 : 0x0E;
//        dialog.devBits2.setSelectedIndex( ( pdCount > n && data[ pd + n ] <= 8 ) ? data[ pd + n ] : 0 );
//      }
//      if ( dialog.cmdBits2.isEnabled() )
//      {
//        int n = ( dataStyle < 2 ) ? 0x12 : 0x10;
//        dialog.cmdBits2.setSelectedIndex( ( pdCount > n && data[ pd + n ] <= 8 ) ? data[ pd + n ] : 0 );
//      }
//      dialog.sigStruct.setSelectedIndex( ( pf[ 0 ] >> 4 ) & 0x03 );
//      dialog.devBitDbl.setSelectedIndex( ( pfCount > 2 ) ? pf[ 2 ] & 3 : 0 );
//      dialog.cmdBitDbl.setSelectedIndex( ( pfCount > 2 ) ? ( pf[ 2 ] >> 2 ) & 3 : 0 );
//      int n = ( dataStyle < 2 ) ? 0x11 : 0x0F;
//      dialog.rptType.setSelectedIndex( ( pfCount > 1 && ( ( pf[ 1 ] & 0x10 ) != 0 ) && pdCount > n && data[ pd + n ] != 0xFF  ) ? 0 : 1 );
//      dialog.rptValue.setText( ( dialog.rptType.getSelectedIndex() == 0 ) ? "" + data[ pd + n ] : "" );
//      dialog.rptHold.setSelectedIndex( ( pfCount > 1 ) ? pf[ 1 ] & 0x03 : 0 );
//      dialog.xmit0rev.setSelectedIndex( ( pfCount > 2 ) ? ( pf[ 2 ] >> 4 ) & 1 : 0 );
//      dialog.leadInStyle.setSelectedIndex( ( pfCount > 1 ) ? ( pf[ 1 ] >> 2 ) & 3 : 0 );
//      n = dataStyle + ( ( dataStyle == 0 && ramAddress == 0x8000 ) ? 0 : 1 );
//      Hex setMidFrame1 = new Hex( CommonData.midFrameCode1[ n ] );
//      Hex setMidFrame2 = new Hex( CommonData.midFrameCode2[ n ] );
//      n = pd + pdCount; // start of code
//      boolean b = hex.indexOf( setMidFrame1, n ) >= 0 || hex.indexOf( setMidFrame2, n ) >= 0;
//      dialog.burstMidFrame.setSelectedIndex( b ? 1 : 0 );
//      dialog.afterBits.setText( ( b && pdCount > 0x13 ) ? "" + ( data[ pd + 0x13 ] - 1 ) : "" );
//      dialog.leadOutStyle.setSelectedIndex( ( pfCount > 1 ) ? ( pf[ 1 ] >> 5 ) & 3 : 0 );
//      dialog.offAsTotal.setSelectedIndex( ( pf[ 0 ] >> 6 ) & 1 );
//      dialog.useAltLeadOut.setSelectedIndex( ( pfCount > 3 ) ? ( pf[ 3 ] >> 5 ) & 1 : 0 );
//      b = pfCount > 3 && ( pf[ 3 ] & 0x40 ) == 0x40 && pdCount > 0x14 && hex.get( pd + 0x13 ) != 0xFFFF;
//      dialog.altFreq.setText( b ?  getFrequency( p, data[ 0x13 ], data[ 0x14 ] ) : "" );
//      dialog.altDuty.setText( b ? getDutyCycle( p, data[ 0x13 ], data[ 0x14 ] ) : "" );
//      
//      if ( dataStyle < 2 )
//      {
//        dialog.burst1On.setText( ( pdCount > 3 && hex.get( pd + 2 ) > 0 ) ? "" + hex.get( pd + 2 ) * 2 : "" );
//        dialog.burst1Off.setText( ( pdCount > 5 && hex.get( pd + 4 ) > 0 ) ? "" + ( hex.get( pd + 4 ) * 2 + ( ( dataStyle == 0 ) ? 40 : 0 ) ) : "" );
//        dialog.burst0On.setText( ( pdCount > 7 && hex.get( pd + 6 ) > 0 ) ? "" + hex.get( pd + 6 ) * 2 : "" );
//        dialog.burst0Off.setText( ( pdCount > 9 && hex.get( pd + 8 ) > 0 ) ? "" + ( hex.get( pd + 8 ) * 2 + ( ( dataStyle == 0 ) ? 40 : 0 ) ) : "" );
//        dialog.leadInOn.setText( ( dialog.leadInStyle.getSelectedIndex() > 0 && pdCount > 0x0D && hex.get( pd + 0x0C ) != 0xFFFF ) ?  "" + hex.get( pd + 0x0C ) * 2 : "" );
//        dialog.leadInOff.setText( ( dialog.leadInStyle.getSelectedIndex() > 0 && pdCount > 0x0F && hex.get( pd + 0x0E ) != 0xFFFF ) ?  "" + ( hex.get( pd + 0x0E ) * 2 + ( ( dataStyle == 0 ) ? 40 : 0 ) ) : "" );
//        dialog.leadOutOff.setText( ( pdCount > 0x0B && hex.get( pd + 0x0A ) > 0 ) ?  "" + hex.get( pd + 0x0A ) * 2 : "" );
//        dialog.altLeadOut.setText( ( dialog.useAltLeadOut.getSelectedIndex() == 1 && pdCount > 0x14 && hex.get( pd + 0x13 ) > 0 ) ? "" + hex.get( pd + 0x13 ) * 2 : ""  );
//
//      }
//      else
//      {
//        int t = ( pdCount > 3 ) ? ( data[ pd + 2 ] >> 4 ) * 0x100 + data[ pd + 3 ] : 0;
//        dialog.burst1On.setText( t > 0 ? "" + 4 * ( t + 1 ) : "" );
//        t = ( pdCount > 4 ) ? ( data[ pd + 2 ] & 0x0F ) * 0x100 + data[ pd + 4 ] : 0;
//        dialog.burst1Off.setText( t > 0 ? "" + 4 * t : "" );
//        t = ( pdCount > 6 ) ? ( data[ pd + 5 ] >> 4 ) * 0x100 + data[ pd + 6 ] : 0;
//        dialog.burst0On.setText( t > 0 ? "" + 4 * ( t + 1 ) : "" );
//        t = ( pdCount > 7 ) ? ( data[ pd + 5 ] & 0x0F ) * 0x100 + data[ pd + 7 ] : 0;
//        dialog.burst0Off.setText( t > 0 ? "" + 4 * t : "" );
//        t = ( pdCount > 0x0C ) ? ( data[ pd + 0x0B ] >> 4 ) * 0x100 + data[ pd + 0x0C ] : 0;
//        dialog.leadInOn.setText( dialog.leadInStyle.getSelectedIndex() > 0 && t > 0 ? "" + 4 * ( t + 1 ) : "" );
//        t = ( pdCount > 0x0D ) ? ( data[ pd + 0x0B ] & 0x0F ) * 0x100 + data[ pd + 0x0D ] : 0;
//        dialog.leadInOff.setText( dialog.leadInStyle.getSelectedIndex() > 0 && t > 0 ? "" + 4 * t : "" );
//        t = ( pdCount > 9 ) ? hex.get( pd + 8 )- 10 : 0; 
//        dialog.leadOutOff.setText( t > 0 ? "" + 4 * t : "" );
//        t = ( pdCount > 0x12 ) ? hex.get( pd + 0x11 )- 10 : 0;
//        dialog.altLeadOut.setText( dialog.useAltLeadOut.getSelectedIndex() == 1 && t > 0 ? "" + 4 * t : "" );
//      }
//
//    }
//    else
//    {
//      dialog.populateComboBox( dialog.sigStruct, CommonData.sigStructs34 );
//      dialog.populateComboBox( dialog.devBitDbl, CommonData.bitDouble34 );
//      dialog.populateComboBox( dialog.cmdBitDbl, CommonData.bitDouble34 );
//      dialog.populateComboBox( dialog.rptType, CommonData.repeatType34 );
//      dialog.populateComboBox( dialog.rptHold, CommonData.noYes );
//      if ( ( ( pf[ 0 ] & 0x58 ) == 0x08 ) )
//      {
//        dialog.devBits1.setSelectedIndex( ( pdCount > 0x0D ) ? data[ pd + 0x0D ] : 0 );
//        if ( dialog.devBits2.isEnabled() )
//        {
//          dialog.devBits2.setSelectedIndex( 0 );
//        }
//      }
//      else
//      {
//        dialog.devBits1.setSelectedIndex( ( pdCount > 1 ) ? data[ pd + 1 ] : 0 );
//        if ( dialog.devBits2.isEnabled() )
//        {
//          dialog.devBits2.setSelectedIndex( ( pdCount > 0x0D ) ? data[ pd + 0x0D ] : 0 );
//        }
//      }
//      dialog.cmdBits1.setSelectedIndex( ( pdCount > 2 ) ? data[ pd + 2 ] : 0 );
//      String sig = "";
//      String items[] = { "devs", "dev", "cmd", "!dev", "dev2", "cmd", "!cmd" };
//      int key = ( ( pf[ 0 ] >> 1 ) & 0x3C ) | ( ( pf[ 0 ] >> 2 ) & 1 );
//      if ( ( pf[ 0 ] & 0x41 ) == 0x41 )
//      {
//        key ^= 0x60;  // replace bit for "dev" by that for "devs"
//      }
//      if ( ( pf[ 0 ] & 0x22 ) == 0x22 )
//      {
//        key ^= 0x12;  // replace bit for first "cmd" by that for second one
//      }
//      for ( int i = 0; i < 7; i++ )
//      {
//        if ( ( ( key << i ) & 0x40 ) == 0x40 )
//        {
//          sig += items[ i ] + "-";
//        }
//      }
//      sig = sig.substring( 0, sig.length() - 1 );
//      dialog.sigStruct.setSelectedItem( sig );
//      dialog.devBitDbl.setSelectedIndex( ( pfCount > 2 ) ? ( pf[ 2 ] >> 1 ) & 1 : 0 );
//      dialog.cmdBitDbl.setSelectedIndex( ( pfCount > 2 ) ? ( pf[ 2 ] >> 1 ) & 1 : 0 );
//      dialog.rptType.setSelectedIndex( ( pfCount > 1 && ( ( pf[ 1 ] & 0x02 ) != 0 ) ) ? 0 : 1 );
//      dialog.rptValue.setText( "" );
//      dialog.rptHold.setSelectedIndex( ( pfCount > 1 && ( ( pf[ 1 ] & 0x02 ) != 0 ) ) ? 1 : 0 );
//      dialog.burst1On.setText( getONtime34( 0, null ) );
//      dialog.burst0On.setText( ( pfCount > 2  && ( pf[ 2 ] & 0x08 ) == 0x08 ) ? getONtime34( 0x0E, null ) : getONtime34( 0, null ) );
//      dialog.burst1Off.setText( getOFFtime34( 3, CommonData.burstOFFoffsets34, dataStyle ) );
//      dialog.burst0Off.setText( getOFFtime34( 5, CommonData.burstOFFoffsets34, dataStyle ) );
//      dialog.xmit0rev.setSelectedIndex( ( pfCount > 2 && ( pf[ 2 ] & 0x1C ) == 0x04 ) ? 1 : 0 );
//      dialog.leadInStyle.setSelectedIndex( ( pfCount > 1 && (( pf[ 1 ] & 0x10 ) == 0x10 ) ) ? 
//         (  ( pf[ 1 ] & 0x04 ) == 0x04 && pdCount > 0x11 && hex.get( pd + 0x10 ) != hex.get( pd + 0x0A ) ) ? 3 : 1 : 0 );
//      dialog.leadInOn.setText( dialog.leadInStyle.getSelectedIndex() > 0 ? getONtime34( 9, 0x0C ) : "" );
//      dialog.leadInOff.setText( dialog.leadInStyle.getSelectedIndex() > 0 ? getOFFtime34( 0x0A, CommonData.leadinOFFoffsets34, dataStyle ) : "" );
//      dialog.offAsTotal.setSelectedIndex( ( dataStyle == 4 && pfCount > 2 ) ? pf[ 2 ] & 1 : 0 );
//      dialog.leadOutStyle.setSelectedIndex( ( pfCount > 1 ) ? ( dialog.offAsTotal.getSelectedIndex() == 1 ? 2 : ( pf[ 1 ] >> 5 ) & 2 ) + ( ( pf[ 1 ] >> 5 ) & 1 ) : 0 );
//      
//      dialog.leadOutOff.setText( ( dataStyle == 3 ) ? getOFFtime34( 7, CommonData.leadinOFFoffsets34, dataStyle ) : ( pdCount > 8 && hex.get( pd + 7 ) > 0 ) ? "" + ( hex.get( pd + 7 ) * 4 - 40 ) : "" );
//      
//      dialog.useAltLeadOut.setSelectedIndex( ( pfCount > 2 && ( pf[ 1 ] & 4 ) == 4 && ( pf[ 2 ] & 8 ) == 0 && pdCount > 0x0F && hex.get( pd + 0x0E ) != hex.get( pd + 0x07 ) ) ? 1 : 0 );
//      dialog.altLeadOut.setText( ( dialog.useAltLeadOut.getSelectedIndex() == 1  ) ? getOFFtime34( 0x0E, CommonData.altLeadoutOffsets34, dataStyle ) : "" );
//    }
//  }

//  private String getFrequency( Processor p, int on, int off )
//  {
//    burstUnit = 0;
//    if ( on > 0 && off > 0 )
//    {
//      double f = p.getOscillatorFreq()/( on + off + p.getCarrierTotalOffset() );
//      burstUnit = ( int )( Math.round( 1000000000 / f ) );
//      return String.format( "%.3f", f/1000 );
//    }
//    else if ( on == 0 && off == 0 )
//    {
//      return "No carrier";
//    }
//    else
//    {
//      return "** Error **";
//    }
//  }
//  
//  private String getDutyCycle( Processor p, int on, int off )
//  {
//    int totOffset = p.getCarrierTotalOffset();
//    int onOffset = p.getCarrierOnOffset();
//    if ( on > 0 && off > 0 )
//    {
//      double dc = 100.0 * ( on + onOffset ) / ( on + off + totOffset );
//      return String.format( "%.2f", dc );
//    }
//    else    // Error case handled by dbOut()
//    {
//      return "";
//    }
//  }
  
  private String getONtime34( int pdIndex1, Integer pdIndex2 )
  {
    int pd = 5 + pfCount;
    if ( pdCount <= pdIndex1 )
    {
      return "";
    }
    else if ( pfCount > 2 && ( pf[ 2 ] & 0x7C ) == 0x40 )
    {
      int t = ( data[ pd + pdIndex1 ] + 255 ) % 256 + 1;
      return "" + ( 3 * t + 2 );
    }
    else
    {
      int t = ( data[ pd + pdIndex1 ] + 255 ) % 256 + 1;
      if ( pdIndex2 != null && pfCount > 1 && ( pf[ 1 ] & 0x08 ) == 0x08 && pdCount > pdIndex2 )
      {
        t += ( ( data[ pd + pdIndex2 ] + 255 ) % 256 ) * 256;
      }
      return "" + burstUnit * t / 1000;
    }
  }

  private String getOFFtime34( int pdIndex, int[] offsets, int dataStyle )
  {
    int pd = 5 + pfCount;
    if ( pdCount < pdIndex + 1 )
    {
      return "";
    }
    else
    {
      int t = ( data[ pd + pdIndex + 1 ] + 255 ) % 256;
      t += ( ( data[ pd + pdIndex ] + 255 ) % 256 ) * ( ( dataStyle == 3 ) ? 257 : 257.5 );
      t = ( dataStyle == 3 ) ? 3 * t + offsets[ 0 ] : 2 * t + offsets[ 1 ];
      return "" + t;
    }
  }
  
  public int getPfCount()
  {
    return pfCount;
  }

  public int getPdCount()
  {
    return pdCount;
  }

  public void setPfCount( int pfCount )
  {
    this.pfCount = pfCount;
  }

  public void setPdCount( int pdCount )
  {
    this.pdCount = pdCount;
  }

  private static class AssemblerCellRenderer extends DefaultTableCellRenderer
  {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int col ) 
    {
      Component c = super.getTableCellRendererComponent( table, value, isSelected, hasFocus, row, col );
      AssemblerTableModel model = ( AssemblerTableModel )table.getModel();
      c.setForeground( model.getRow( row ).getErrorCode() == 0 ? Color.BLACK : Color.RED );
      return c;
    }
  }
  
  public List< AssemblerItem > getItemList()
  {
    return itemList;
  }
  
  public void setItemList( List< AssemblerItem > itemList )
  {
    this.itemList = itemList;
//    setData( itemList );
//    fireTableDataChanged();
  }

  private SelectAllCellEditor selectAllEditor = new SelectAllCellEditor();
  private AssemblerCellRenderer assemblerCellRenderer = new AssemblerCellRenderer();
  
}
