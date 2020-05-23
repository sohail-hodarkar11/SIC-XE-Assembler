// Java implementation of an SIC-XE Assebmler that supports program relocation and symbol-defining statements + expressions

import java.io.*;
import java.util.*;

public class Assembler
{
	private final String format1[] = {"FIX", "FLOAT", "HIO", "NORM", "SIO", "TIO"};
	private final String reserve[] = {"BYTE", "WORD", "RESB", "RESW"};
	private final String directive[] = {"START", "BASE", "NOBASE", "END"};
	private String programLength;
	private String startingAddress;

	private final HashMap<String, String> opTab = new HashMap<>();
	private final HashMap<String, String> symTab = new HashMap<>();
	private final HashMap<String, String> regTab = new HashMap<>();

	// initialization functions
	public void initOptab() throws IOException															// initializes the OpTab using Instructions.txt
	{
		Scanner sc = new Scanner(new FileReader("Instructions.txt"));
		while(sc.hasNextLine())
		{
			String fields[] = sc.nextLine().split("\\s+");
			opTab.put(fields[0], fields[1]);
		}
		sc.close();
		File file = new File("Instructions.txt");
		file.delete();
	}
	public void initRegtab() throws IOException															// initializes the mnemonic registers using Registers.txt
	{
		Scanner sc = new Scanner(new FileReader("Registers.txt"));
		while(sc.hasNextLine())
		{
			String fields[] = sc.nextLine().split("\\s+");
			regTab.put(fields[0], fields[1]);
		}
		sc.close();
		File file = new File("Registers.txt");
		file.delete();
	}

	// functions to support program relocation
	public void addModificationRecord(String mod) throws IOException									// creates a list of modification records
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter("Modifications.txt", true));
		PrintWriter pw = new PrintWriter(bw);

		pw.println(mod);
		pw.close();
	}
	public void clearModifications() throws IOException													// writes the modification records to the Object Program
	{
		Scanner sc = new Scanner(new FileReader("Modifications.txt"));

		BufferedWriter bw = new BufferedWriter(new FileWriter("Output.txt", true));
		PrintWriter pw = new PrintWriter(bw);

		while(sc.hasNextLine())
		{
			pw.println(sc.nextLine().toUpperCase());
		}
		pw.println("E^" + padField(startingAddress, '0', 6).toUpperCase());
		pw.close();
		sc.close();

		File file1 = new File("Modifications.txt");
		file1.delete();
		File file2 = new File("Intermediate.txt");
		file2.delete();
	}

	// utility functions
	public String padField(String str, char ch, int n) throws IOException								// function to pad the given string to the given length
	{
		while(str.length() != n)
		{
			if(ch == '0')
			{
				str = (ch + str);
			}
			else
			{
				str = (str + ch);
			}
		}
		return str;
	}
	public int getCount(String str, char ch)throws IOException											// function to return the frequency of given character
	{
		int ctr = 0;
		for(int i=0;i<str.length();i++)
		{
			if(str.charAt(i) == ch)
				ctr++;
		}
		return ctr;
	}
	public String getHexString(String val) throws IOException											// converting String to its hexadecimal equivalent
	{
		String ans = "";
		for(int i=0;i<val.length();i++)
		{
			ans = (ans + Integer.toHexString(val.charAt(i)));
		}
		return ans;
	}
	public String getHexString(int val, int n) throws IOException										// converting Integer to its hexadecimal equivalent
	{
		String ans = Integer.toHexString(val);
		while(ans.length() != n)
		{
			ans = ("0" + ans);
		}
		return ans;
	}
	public String binaryToHex(String inst) throws IOException											// converting 4-bit binary to hexadecimal
	{
		String fields[] = {inst.substring(0, 4) , inst.substring(4, 8) , inst.substring(8)};

		String ans = "";
		for(int i=0;i<fields.length;i++)
		{
			ans = (ans + Integer.toHexString(Integer.parseInt(fields[i], 2)));
		}

		return ans;
	}
	public String convertCode(String code) throws IOException											// converting hexadecimal opcode to 6-bit binary
	{
		String ans = Integer.toBinaryString(Integer.parseInt(code, 16));
		while(ans.length() != 8)
		{
			ans = ("0" + ans);
		}
		return ans.substring(0, 6);
	}
	public String alterLength(String str, int n) throws IOException										// altering the length of given string
	{
		if(str.length() > n)
		{
			return str.substring(str.length() - n);
		}
		else
		{
			while(str.length() != n)
			{
				str = ("0" + str);
			}
			return str;
		}
	}

	// functions dealing with listing lines
	public boolean recordFull(String record, String macCode) throws IOException							// function to check if the given listing is full
	{
		String str = record + macCode;
		int extra = 7 + getCount(str, '^');

		return ((str.length() - extra) > 60) ? true : false;
	}
	public void writeRecord(String record) throws IOException											// function to write the listing to the Object Program
	{
		BufferedWriter bw = new BufferedWriter(new FileWriter("Output.txt", true));
		PrintWriter pw = new PrintWriter(bw);

		int len = record.length() - (7 + getCount(record, '^'));
		String length = Integer.toHexString(len/2);

		record = record.substring(0, 9) + padField(length, '0', 2).toUpperCase() + "^" + record.substring(9);
		pw.println(record);

		pw.close();
	}

	// functions dealing with instructions
	public int getFormat(String opc) throws IOException													// recognizing the instruction format
	{
		for(String code : directive)
		{
			if (opc.equals(code))
			{
				return -1;
			}
		}
		for(String code : reserve)
		{
			if (opc.equals(code))
			{
				return 0;
			}
		}
		for(String code : format1)
		{
			if (opc.equals(code))
			{
				return 1;
			}
		}
		if(opc.equals("CLEAR") || opc.charAt(opc.length() - 1) == 'R')
		{
			return 2;
		}
		else if (opc.charAt(0) == '+')
		{
			return 4;
		}
		else
		{
			return 3;
		}
	}
	public String getCode(String opc) throws IOException												// getting the opcode from the OpTab
	{
		opc = (getFormat(opc) == 4) ? opc.substring(1) : opc;

		String code = "00";
		if(opTab.containsKey(opc))
		{
			code = opTab.get(opc);
		}
		return code;
	}

	// function to get the register number from the mnemonic
	public String getRegister(String reg) throws IOException
	{
		if(regTab.containsKey(reg))
		{
			return regTab.get(reg);
		}
		else
		{
			return "-1";
		}
	}

	// recognizing the addressing mode used
	public boolean isIndirect(String operand) throws IOException										// checks for indirect addressing
	{
		return (operand.charAt(0) == '@') ? true : false;
	}
	public boolean isImmediate(String operand) throws IOException										// checks for immediate addressing
	{
		return (operand.charAt(0) == '#') ? true : false;
	}
	public boolean isIndexed(String operand, String opcode) throws IOException							// checks for indexed addressing
	{
		return ((getFormat(opcode) != 2) && (operand.endsWith(",X"))) ? true : false;
	}

	// function to check for PC relative addressing
	public boolean pcPossible(String target, String pcLoc) throws IOException
	{
		int num = Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16);
		return ((num <= 2047) && (num >= -2048)) ? true : false;
	}

	// function to get the value of the symbol from the SymTab
	public String getAddress(String operand) throws IOException
	{
		if(symTab.containsKey(operand))
		{
			return symTab.get(operand);
		}
		else
		{
			return "ERROR";
		}
	}

	// first pass of the assembler - Input = Assembly Program, Output = Intermediate file with locctr values
	public void passOne() throws IOException
	{
		Scanner sc = new Scanner(new FileReader("Input.txt"));

		BufferedWriter bw = new BufferedWriter(new FileWriter("Intermediate.txt", true));
		PrintWriter pw = new PrintWriter(bw);

		String locctr = "0";
		while (sc.hasNextLine())
		{
			String line = sc.nextLine();
			String fields[] = line.trim().split("\\s+");

			if (!(fields[0].equals(".")))
			{
				if (fields.length == 3)
				{
					// checking for duplicacy of symbols
					if(symTab.containsKey(fields[0]))
					{
						System.out.println("ERROR! Symbol already exists");
						return;
					}
					// handling expressions and symbol-defining statements
					if(fields[1].equals("EQU"))
					{
						if(fields[2].equals("*"))
						{
							symTab.put(fields[0], locctr);
						}
						else
						{
							Scanner s = new Scanner(fields[2]).useDelimiter("-");
							String op1 = s.next();
							String op2 = s.next();

							symTab.put(fields[0], Integer.toHexString(Integer.parseInt(symTab.get(op1), 16) - Integer.parseInt(symTab.get(op2), 16)));
							s.close();
						}
						pw.println(locctr + "\t" + line);
						continue;
					}
					else
					{
						symTab.put(fields[0], locctr);
					}
				}

				pw.println(locctr + "\t" + line);

				int format = (fields.length == 3) ? getFormat(fields[1]) : getFormat(fields[0]);
				if (format == -1)
				{
					continue;
				}
				else if (format == 0)
				{
					String opcode = (fields.length == 3) ? fields[1] : fields[0];
					String operand = (fields.length == 3) ? fields[2] : fields[1];

					if (opcode.equals("WORD"))
					{
						locctr = Integer.toHexString(Integer.parseInt(locctr, 16) + 3);
					}
					else if (opcode.equals("RESB"))
					{
						locctr = Integer.toHexString(Integer.parseInt(locctr, 16) + (1 * Integer.parseInt(operand)));
					}
					else if (opcode.equals("RESW"))
					{
						locctr = Integer.toHexString(Integer.parseInt(locctr, 16) + (3 * Integer.parseInt(operand)));
					}
					else
					{
						if (operand.charAt(0) == 'C')
						{
							locctr = Integer.toHexString(Integer.parseInt(locctr, 16) + (operand.length() - 3));
						}
						else
						{
							locctr = Integer.toHexString(Integer.parseInt(locctr, 16) + 1);
						}
					}
				}
				else
				{
					locctr = Integer.toHexString(Integer.parseInt(locctr, 16) + format);
				}
			}
			else
			{
				pw.println(locctr + "\t" + line);
			}
		}
		pw.close();
		sc.close();

		programLength = locctr;
	}

	// second pass of the assembler - Input = Intermediate file, Output = Object Program
	public void passTwo() throws IOException
	{
		String baseLoc = "";

		Scanner inter = new Scanner(new FileReader("Intermediate.txt"));

		String record = "";

		// PC points to the first instruction
		String pc = inter.nextLine();
		startingAddress = pc.split("\\s+")[0];

		// writing the header record
		BufferedWriter bw = new BufferedWriter(new FileWriter("Output.txt"));
		PrintWriter pw = new PrintWriter(bw);
		pw.println("H^" + padField(pc.split("\\s+")[1], ' ', 6).toUpperCase() + "^" + padField(startingAddress, '0' , 6) + "^" + padField(programLength, '0', 6));
		pw.close();

		while(inter.hasNextLine())
		{
			String line = pc;
			pc = inter.nextLine();

			String pcLoc = pc.split("\\s+")[0];
			String fields[] = line.split("\\s+");

			// getting the opcode of the instruction
			String opcode = (fields.length == 4) ? fields[2] : fields[1];
			String operand = fields[fields.length - 1];;

			// building the machine code for the instruction
			String macCode = "";

			// updating the base location
			if(opcode.equals("BASE"))
			{
				if(isImmediate(operand))
				{
					if(Character.isLetter(operand.charAt(1)))
					{
						baseLoc = getAddress(operand.substring(1));
					}
					else
					{
						baseLoc = operand.substring(1);
					}
				}
				else
				{
					baseLoc = getAddress(operand);
				}
				continue;
			}
			// clearing the base register
			else if(opcode.equals("NOBASE"))
			{
				baseLoc = "";
				continue;
			}
			// handling expressions and symbol-defining statements
			else if(opcode.equals("EQU"))
			{
				continue;
			}
			else if(!(opcode.equals(".") || opcode.equals("START") || opcode.equals("END") || opcode.equals("RSUB") || opcode.equals("+RSUB")))
			{
				// getting the operands for the instruction
				String vars[] = operand.split(",");

				if(!(opcode.equals("BYTE") || opcode.equals("WORD") || opcode.equals("RESB") || opcode.equals("RESW")))
				{
					// format 1 instructions
					if(getFormat(opcode) == 1)
					{
						macCode = getCode(opcode);
					}
					// format 2 instructions
					else if(getFormat(opcode) == 2)
					{
						macCode = getCode(opcode);

						if(vars.length == 1)			// single operand
						{
							macCode = (macCode + getRegister(vars[0]) + "0");
						}
						else							// two operands
						{
							macCode = (macCode + getRegister(vars[0]) + getRegister(vars[1]));
						}
					}
					// format 3 instructions
					else if(getFormat(opcode) == 3)
					{
						// 1 bit flags
						int n, i, x, b, p, e;
						e = 0;		// format 3 instruction

						// immediate addressing mode
						if(isImmediate(operand))
						{
							n = 0;
							i = 1;
							x = 0;

							if(!Character.isLetter(operand.charAt(1)))
							{
								// neither base nor pc relative
								b = 0;
								p = 0;

								macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);
								macCode = (macCode + getHexString(Integer.parseInt(operand.substring(1)), 3));
							}
							else
							{
								String target = getAddress(operand.substring(1));
								b = (!pcPossible(target, pcLoc)) ? 1 : 0;
								p = (pcPossible(target, pcLoc)) ? 1 : 0;

								macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);
								if(pcPossible(target, pcLoc))
								{
									macCode = (macCode + getHexString(Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16), 3));
								}
								else
								{
									macCode = (macCode + getHexString(Integer.parseInt(target, 16) - Integer.parseInt(baseLoc, 16), 3));
								}
							}
						}
						// indirect addressing mode
						else if(isIndirect(operand))
						{
							n = 1;
							i = 0;
							x = 0;

							String target = getAddress(operand.substring(1));
							b = (!pcPossible(target, pcLoc)) ? 1 : 0;
							p = (pcPossible(target, pcLoc)) ? 1 : 0;

							macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);

							if(pcPossible(target, pcLoc))
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16));
								macCode = (macCode + alterLength(str, 3));
							}
							else
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(baseLoc, 16));
								macCode = (macCode + alterLength(str, 3));
							}
						}
						// indexed addressing mode
						else if(isIndexed(operand, opcode))
						{
							n = 1;
							i = 1;
							x = 1;

							String target = getAddress(vars[0]);
							b = (!pcPossible(target, pcLoc)) ? 1 : 0;
							p = (pcPossible(target, pcLoc)) ? 1 : 0;

							macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);

							if(pcPossible(target, pcLoc))
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16));
								macCode = (macCode + alterLength(str, 3));
							}
							else
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(baseLoc, 16));
								macCode = (macCode + alterLength(str, 3));
							}
						}
						else
						{
							n = 1;
							i = 1;
							x = 0;

							String target = getAddress(operand);
							b = (!pcPossible(target, pcLoc)) ? 1 : 0;
							p = (pcPossible(target, pcLoc)) ? 1 : 0;

							macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);
							if(pcPossible(target, pcLoc))
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16));
								macCode = (macCode + alterLength(str, 3));
							}
							else
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(baseLoc, 16));
								macCode = (macCode + alterLength(str, 3));
							}
						}
					}
					// format 4 instructions
					else
					{
						// 1 bit flags
						int n, i, x	, b, p, e;
						e = 1;		// format 4 instruction

						// immediate addressing mode
						if(isImmediate(operand))
						{
							n = 0;
							i = 1;
							x = 0;

							if(!Character.isLetter(operand.charAt(1)))
							{
								// neither base nor pc relative
								b = 0;
								p = 0;

								macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);
								macCode = (macCode + getHexString(Integer.parseInt(operand.substring(1)), 5));
							}
							else
							{
								String target = getAddress(operand.substring(1));
								b = 0;
								p = 0;

								macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);
								macCode = (macCode + padField(symTab.get(operand.substring(1)), '0', 5));
							}
						}
						// indirect addressing mode
						else if(isIndirect(operand))
						{
							n = 1;
							i = 0;
							x = 0;

							String target = getAddress(operand.substring(1));
							b = (!pcPossible(target, pcLoc)) ? 1 : 0;
							p = (pcPossible(target, pcLoc)) ? 1 : 0;

							macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);

							if(pcPossible(target, pcLoc))
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16));
								macCode = (macCode + alterLength(str, 5));
							}
							else
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(baseLoc, 16));
								macCode = (macCode + alterLength(str, 5));
							}
						}
						// indexed addressing mode
						else if(isIndexed(operand, opcode))
						{
							n = 1;
							i = 1;
							x = 1;

							String target = getAddress(vars[0]);
							b = (!pcPossible(target, pcLoc)) ? 1 : 0;
							p = (pcPossible(target, pcLoc)) ? 1 : 0;

							macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);

							if(pcPossible(target, pcLoc))
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(pcLoc, 16));
								macCode = (macCode + alterLength(str, 5));
							}
							else
							{
								String str = Integer.toHexString(Integer.parseInt(target, 16) - Integer.parseInt(baseLoc, 16));
								macCode = (macCode + alterLength(str, 5));
							}
						}

						else
						{
							n = 1;
							i = 1;
							x = 0;

							String target = getAddress(operand);
							b = 0;
							p = 0;

							macCode = binaryToHex(convertCode(getCode(opcode)) + n + i + x + b + p + e);
							macCode = (macCode + alterLength(target, 5));
						}
					}
				}
				else
				{
					if(opcode.equals("BYTE"))
					{
						if(operand.charAt(0) == 'X')
						{
							macCode = operand.substring(2,4);
						}
						else
						{
							operand = operand.substring(2, operand.length() - 1);
							macCode = getHexString(operand);
						}
					}
					else if(opcode.equals("WORD"))
					{
						// word
					}
					else
					{
						macCode = "N/A";
					}
				}
			}
			else if(opcode.equals("RSUB"))
			{
				macCode = "4F0000";
			}
			else if(opcode.equals("+RSUB"))
			{
				macCode = "4F1000";
			}
			else
			{
				continue;
			}
			// generating modification records
			if((getFormat(opcode) == 4) && (!isImmediate(operand)))
			{
				String mod = "M^" + padField(Integer.toHexString(Integer.parseInt(fields[0], 16) + 1), '0', 6) + "^05";
				addModificationRecord(mod);
			}
			// breaking the record when an instruction with no machine code is encountered
			if(macCode.equals("N/A"))
			{
				// empty machine code => break the lisitng line
				if(record.length() != 0)
				{
					writeRecord(record.toUpperCase());
				}
				record = "";
			}
			else
			{
				if(record.length() == 0)
				{
					record = "T^" + padField(fields[0], '0', 6) + "^" + macCode;
				}
				else if(!recordFull(record, macCode))
				{
					record = (record + "^" + macCode);
				}
				else
				{
					writeRecord(record.toUpperCase());
					record = "T^" + padField(fields[0], '0', 6) + "^" + macCode;
				}
			}
		}
		inter.close();
		writeRecord(record.toUpperCase());
		clearModifications();
	}

	// Main method
	public static void main(String Args[]) throws IOException
	{
		Assembler a1 = new Assembler();
		a1.initOptab();
		a1.initRegtab();
		a1.passOne();
		a1.passTwo();
	}
}