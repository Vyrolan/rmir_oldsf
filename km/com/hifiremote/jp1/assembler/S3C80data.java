package com.hifiremote.jp1.assembler;

public class S3C80data
{
  private String wp = "W";    // Work prefix
 
  public static final String[][] AddressModes = {
    { "Nil", "", "" },
    { "R1", "B1Z1", "R%02X" },
    { "IR1", "B1Z1", "@R%02X" },
    { "W1W2", "N12", "W%X, W%X" },
    { "W1IW2", "N12", "W%X, @W%X" },
    { "R2R1", "B2Z3", "R%2$02X, R%1$02X" },
    { "IR2R1", "B2Z3", "R%2$02X, @R%1$02X" },
    { "R1IM", "B2Z1", "R%02X, #%02XH" },
    { "W1R2", "N1B1Z1", "W%X, R%02X" },
    { "W2R1", "N1B1Z1", "R%2$02X, W%1$X" },
    { "W1RA", "N1B1R1", "W%X, %04XH" },
    { "CCRA", "C1N1B1R1", "%s%04XH" }, 
    { "W1IM", "N1B1", "W%X, #%02XH" },
    { "CCDA", "C1N1B2A1", "%s%02X%02XH" },
    { "W1", "N1", "W%X" },
    { "IR1R2", "B2Z3", "@R%02X, R%02X" },
    { "DA1", "B2A1", "%02X%02XH" },
    { "W0Rb", "N12B1Z1", "W%1$X, R%3$02X.%2$d" },
    { "W0RbZ", "N12B1Z1", "R%3$02X.%2$d, W%1$X" },
    { "W1bR2", "N12B1Z1", "W%1$X, R%3$2X.%2$d" },
    { "W2bRA", "N12B1R1", "%3$04XH, W%1$X.%2$d" },
    { "W1b", "N12", "W%X.%d" },
    { "IMRR1", "B2Z2", "R%2$02X, #%1$02XH" },
    { "W1xW2", "N12B1", "W%1$X, #%3$02XH[W%2$X]" },
    { "W2xW1", "N12B1", "#%3$02XH[W%2$X], W%1$X" },
    { "RR1IML", "B3A2Z1", "R%02X, #%02X%02XH" },
    { "W1IWW2", "N12", "W%X, @W%X" },
    { "W2IWW1", "N12", "@W%2$X, W%1$X" },
    { "IR1IM", "B2Z1", "@R%02X, #%02XH" },
    { "IW1W2", "N12", "@W%X, W%X" },
    { "R2IR1", "B2Z3", "@R%2$02X, R%1$02X" },
    { "W1IWW2xs", "N12B1", "W%1$X, #%3$02XH[W%2$X]" }, 
    { "W2IWW1xs", "N12B1", "#%3$02XH[W%2$X], W%1$X" },
    { "W1IWW2xL", "N12B2A1", "W%1$X, #%4$02X%3$02XH[W%2$X]" },
    { "W1IWW2xLZ", "N4B2A1", "W%1$X, %3$02X%2$02XH" },
    { "W2IWW1xL", "N12B2A1", "#%4$02X%3$02XH[W%2$X], W%1$X" },
    { "W2IWW1xLZ", "N4B2A1", "%3$02X%2$02XH, W%1$X" },
    { "IW2W1RA", "N12B1R1", "W%2$X, @W%1$X, %3$04XH" }, 
    { "IA1", "B1", "#%02XH" },
    { "IM", "B1", "#%02XH" },
    { "EQU4", "", "%04XH" },
    { "EQU2", "", "%02XH" },
    { "EQUR", "", "R%02X" }
  };
  
  public static final String[][] Instructions = {
    { "DEC", "R1" },              { "DEC", "IR1" },
    { "ADD", "W1W2" },            { "ADD", "W1IW2" },
    { "ADD", "R2R1" },            { "ADD", "IR2R1" },
    { "ADD", "R1IM" },            { "BOR", "W0Rb", "1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "NEXT", "Nil" },

    { "RLC", "R1" },              { "RLC", "IR1" },
    { "ADC", "W1W2" },            { "ADC", "W1IW2" },
    { "ADC", "R2R1" },            { "ADC", "IR2R1" },
    { "ADC", "R1IM" },            { "BCP", "W1bR2", "1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "ENTER", "Nil" },

    { "INC", "R1" },              { "INC", "IR1" },
    { "SUB", "W1W2" },            { "SUB", "W1IW2" },
    { "SUB", "R2R1" },            { "SUB", "IR2R1" },
    { "SUB", "R1IM" },            { "BXOR", "W0Rb", "1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "EXIT", "Nil" },

    { "JP", "IR1" },              { "SRP", "IM", "6" },
    { "SBC", "W1W2" },            { "SBC", "W1IW2" },
    { "SBC", "R2R1" },            { "SBC", "IR2R1" },
    { "SBC", "R1IM" },            { "BTJR", "W2bRA", "2" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "WFI", "Nil" },

    { "DA", "R1" },               { "DA", "IR1" },
    { "OR", "W1W2" },             { "OR", "W1IW2" },
    { "OR", "R2R1" },             { "OR", "IR2R1" },
    { "OR", "R1IM" },             { "LDB", "W0Rb", "1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "SB0", "Nil" },

    { "POP", "R1" },              { "POP", "IR1" },
    { "AND", "W1W2" },            { "AND", "W1IW2" },
    { "AND", "R2R1" },            { "AND", "IR2R1" },
    { "AND", "R1IM" },            { "BITC", "W1b", "1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "SB1", "Nil" },

    { "COM", "R1" },              { "COM", "IR1" },
    { "TCM", "W1W2" },            { "TCM", "W1IW2" },
    { "TCM", "R2R1" },            { "TCM", "IR2R1" },
    { "TCM", "R1IM" },            { "BAND", "W0Rb", "1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "IDLE", "Nil" },

    { "PUSH", "R1" },             { "PUSH", "IR1" },
    { "TM", "W1W2" },             { "TM", "W1IW2" },
    { "TM", "R2R1" },             { "TM", "IR2R1" },
    { "TM", "R1IM" },             { "BIT", "W1b", "3" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "STOP", "Nil" },

    { "DECW", "R1" },             { "DECW", "IR1" },
    { "PUSHUD", "IR1R2" },        { "PUSHUI", "IR1R2" },
    { "MULT", "R2R1" },           { "MULT", "IR2R1" },
    { "MULT", "IMRR1" },          { "LD", "W1xW2" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "DI", "Nil" },

    { "RL", "R1" },               { "RL", "IR1" },
    { "POPUD", "IR2R1" },         { "POPUI", "IR2R1" },
    { "DIV", "R2R1" },            { "DIV", "IR2R1" },
    { "DIV", "IMRR1" },           { "LD", "W2xW1" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "EI", "Nil" },

    { "INCW", "R1" },             { "INCW", "IR1" },
    { "CP", "W1W2" },             { "CP", "W1IW2" },
    { "CP", "R2R1" },             { "CP", "IR2R1" },
    { "CP", "R1IM" },             { "LDC", "W1IWW2xL", "4" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "RET", "Nil" },

    { "CLR", "R1" },              { "CLR", "IR1" },
    { "XOR", "W1W2" },            { "XOR", "W1IW2" },
    { "XOR", "R2R1" },            { "XOR", "IR2R1" },
    { "XOR", "R1IM" },            { "LDC", "W2IWW1xL", "4" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "IRET", "Nil" },

    { "RRC", "R1" },              { "RRC", "IR1" },
    { "CPIJE", "IW2W1RA" },       { "LDC", "W1IWW2", "5" },
    { "LDW", "R2R1" },            { "LDW", "IR2R1" },
    { "LDW", "RR1IML" },          { "LD", "W1IW2" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "RCF", "Nil" },

    { "SRA", "R1" },              { "SRA", "IR1" },
    { "CPIJNE", "IW2W1RA" },      { "LDC", "W2IWW1", "5" },
    { "CALL", "IA1" },            { "*", "Nil" },
    { "LD", "IR1IM" },            { "LD", "IW1W2" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "SCF", "Nil" },

    { "RR", "R1" },               { "RR", "IR1" },
    { "LDCD", "W1IWW2", "5" },    { "LDCI", "W1IWW2", "5" },
    { "LD", "R2R1" },             { "LD", "IR2R1" },
    { "LD", "R1IM" },             { "LDC", "W1IWW2xs", "5" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "CCF", "Nil" },

    { "SWAP", "R1" },             { "SWAP", "IR1" },
    { "LDCPD", "W2IWW1", "5" },   { "LDCPI", "W2IWW1", "5" },
    { "CALL", "IR1" },            { "LD", "R2IR1" },
    { "CALL", "DA1" },            { "LDC", "W2IWW1xs", "5" },
    { "LD", "W1R2" },             { "LD", "W2R1" },
    { "DJNZ", "W1RA" },           { "JR", "CCRA" },
    { "LD", "W1IM" },             { "JP", "CCDA" },
    { "INC", "W1" },              { "NOP", "Nil" }
  };
  
  public static final String[][] absLabels_C80 = {
    { "XMITIR", "0133" } };
  
  public static final String[][] absLabels_F80 = {
    { "XMITIR", "0146" } };
  
  public static final String[][] zeroLabels = {
    { "DCBUF", "03", "DCBUF+", "0A" },
    { "PF0", "28", "PF", "05" },
    { "PD00", "12", "PD", "16" },
    { "DBYTES", "10" },
    { "CBYTES", "11" },
    { "FLAGS", "00" }
  };
  
  public static final String[] oscData = { "8000000", "4" };

  
}
