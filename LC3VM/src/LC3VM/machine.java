package LC3VM;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.util.Queue;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Objects;

import javax.swing.*;


public class machine extends JFrame implements KeyListener {
	
	public static final char[] reg = new char[11];
	
	//registers
	public static final int R_R0 = 0;
	public static final int R_R1 = 1;
	public static final int R_R2 = 2;
	public static final int R_R3 = 3;
	public static final int R_R4 = 4;
	public static final int R_R5 = 5;
	public static final int R_R6 = 6;
	public static final int R_R7 = 7;
	public static final int R_PC = 8; /* program counter */
	public static final int R_COND = 9;
	public static final int R_COUNT = 10;
	
	//opcode
	public static final int OP_BR = 0;      /* branch */
    public static final int OP_ADD = 1;     /* add */
    public static final int OP_LD = 2;      /* load */
    public static final int OP_ST = 3;      /* store */
    public static final int OP_JSR = 4;     /* jump register */
    public static final int OP_AND = 5;     /* bitwise and */
    public static final int OP_LDR = 6;     /* load register */
    public static final int OP_STR = 7;     /* store register */
    public static final int OP_RTI = 8;     /* unused */
    public static final int OP_NOT = 9;     /* bitwise not */
    public static final int OP_LDI = 10;    /* load indirect */
    public static final int OP_STI = 11;    /* store indirect */
    public static final int OP_JMP = 12;    /* jump */
    public static final int OP_RES = 13;    /* reserved (unused) */
    public static final int OP_LEA = 14;    /* load effective address */
    public static final int OP_TRAP = 15;   /* trap */
    
    public static final int FL_POS = 1<<0;
    public static final int FL_ZRO = 1<<1;
    public static final int FL_NEG = 1<<2;
    
    public static final int MR_KBSR = 0xFE00;
    public static final int MR_KBDR = 0xFE02;
    
    public static boolean running = false;
    
    final static short PC_START = 0x3000;
    
    public static volatile Character input = null;
    
    
    public machine() {
        // Set up the JFrame
        this.setTitle("LC3VM");
        this.setSize(400, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setFocusable(true);
        this.addKeyListener(this); // Add KeyListener to the JFrame

        // Create a JLabel to display the key events
        JLabel label = new JLabel("Press any key...", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(24.0f));
        this.add(label);
    }
    
    // Override the methods of KeyListener
    @Override
    public void keyPressed(KeyEvent e) {
    	//input = e.getKeyChar();
        //System.out.println("Key Pressed: " + KeyEvent.getKeyText(e.getKeyCode()) + "\n" + input);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //System.out.println("Key Released: " + KeyEvent.getKeyText(e.getKeyCode()));
    	//input = e.getKeyChar();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        //System.out.println("Key Typed: " + e.getKeyChar());
    	input = e.getKeyChar();
    }
    
    public static char getChar() {
    	while(Objects.isNull(input));
    	return input;
    }
    
    
    
    public static void main(String[] args) {
    	
    	machine frame = new machine();
        frame.setVisible(true);
    	//load arguments
    	if (args.length < 1) {
            System.err.println("Usage: java machine [program_file]");
            System.exit(1);
        }

        try {
            read_image_file(new File(args[0]));
        } catch (IOException e) {
            System.err.println("Failed to load image: " + e.getMessage());
            System.exit(1);
        }
    	//setup
    	
    	/* since exactly one condition flag should be set at any given time, set the Z flag */
    	reg[R_COND] = FL_ZRO;
    	
    	/* set the PC to starting position */
        /* 0x3000 is the default */
    	
    	reg[R_PC] = PC_START;
    	
    	running = true;
    	while(running) {
    		
    		//Memory.bufferInput();
    		
    		
    		char instr = (char) Memory.mem_read(reg[R_PC]);
    	
    		//System.out.printf("Fetching next instruction...\nExecuting instruction: 0x%04X, OP: %s\n", (int) instr, instr >> 12);
    		
    		reg[R_PC] = (char) (reg[R_PC] + 1);
    		char op = (char) ((instr >>> 12) & 0xF);
    		
    		
    	
    		switch (op)
            {
            	case OP_ADD:
            		add(instr);
            		break;
            	case OP_AND:
            		and(instr);
            		break;
            	case OP_NOT:
            		not(instr);
            		break;
            	case OP_BR:
            		br(instr);
            		break;
            	case OP_JMP:
            		jmp(instr);
     	          break;
            	case OP_JSR:
            		jsr(instr);
            		break;
            	case OP_LD:
            		ld(instr);
            		break;
            	case OP_LDI:
            		ldi(instr);
            		break;
            	case OP_LDR:
            		ldr(instr);
            		break;
            	case OP_LEA:
            		lea(instr);
            		break;
            	case OP_ST:
            		st(instr);
            		break;
            	case OP_STI:
            		sti(instr);
            		break;
            	case OP_STR:
            		str(instr);
            		break;
            	case OP_TRAP:
            		traps(instr);
            		break;
            	case OP_RES:
            	case OP_RTI:
            	default:
            		badOpcode();
            		break;
            }
    	}
    }
    
    public static void read_image_file(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            while (dis.available() > 0) {
                char origin = (char) dis.readUnsignedShort();
                System.out.printf("Origin: 0x%04X\n", (int) origin);
                int max_read = 65536 - origin;
                for (int i = 0; i < max_read && dis.available() > 0; i++) {
                    char value = (char) dis.readShort();
                    Memory.mem_write((char) (origin + i), value);
                    System.out.printf("Memory[0x%04X] = 0x%04X\n", (origin + i), (int) value);
                }
            }
        }
        //System.exit(1);
    }
   
    public static void read_image(String filename) throws IOException {
        File file = new File(filename);
        if(file.exists()) {
        	read_image_file(file);
        }
    }

    private static char signExtend(char x, int bitCount) {
    	if(((x >>> (bitCount - 1)) & 1) != 0) {
            x |= (0xFFFF << bitCount);
        } 
        return x;
    }
    
    static void updateFlags(char register) {
        int value = reg[register];
        if (value == 0) {
        	reg[R_COND] = FL_ZRO;  // Zero flag
        } else if (value < 0) {
        	reg[R_COND] = FL_NEG;  // Negative flag
        } else {
        	reg[R_COND] = FL_POS;  // Positive flag
        }
    }
    //and
    public static void add(char instr) {
    	    /* destination register (DR) */
    	    char r0 =   (char) ((instr >>> 9) & 0x7);
    	    /* first operand (SR1) */
    	    char r1 = (char) ((instr >>> 6) & 0x7);
    	    /* whether we are in immediate mode */
    	    boolean imm_flag = ((instr >>> 5) & 1) != 0;

    	    if (imm_flag)
    	    {
    	    	char imm5 = (char) signExtend((char) (instr & 0x1F), 5);
    	    	reg[r0] = (char) (reg[r1] + imm5);
    	    }
    	    else
    	    {
    	    	char r2 = (char) (instr & 0x7);
    	    	reg[r0] = (char) (reg[r1] + reg[r2]);
    	    }

    	    updateFlags(r0);
    }
    //ldi
    public static void ldi(int instr){
    	/* destination register (DR) */
        char r0 =  (char) ((instr >>> 9) & 0x7);
        /* PCoffset 9 */
        char pc_offset = signExtend((char) (instr & 0x1FF), 9);

        // Calculate the effective address
        char effective_address = (char) (reg[R_PC] + pc_offset);
        //System.out.println(effective_address);

        // Ensure the effective address is within bounds
        if (effective_address < 0 || effective_address >= 65536) {
            throw new ArrayIndexOutOfBoundsException("Effective address 0x" + Integer.toHexString(effective_address) + " out of bounds");
        }

        // Get the address from memory at the effective address
        char address = (char) Memory.mem_read(effective_address);

        // Load the value from memory at the final address
        reg[r0] = address;

        updateFlags(r0);
    	
    }
    //badOpcode
    public static void badOpcode(){
    	System.exit(1);
    }
    //and
    public static void and(int instr) {
    	/* destination register (DR) */
	    char r0 = (char) ((instr >>> 9) & 0x7);
	    /* first operand (SR1) */
	    char r1 = (char) ((instr >>> 6) & 0x7);
	    /* whether we are in immediate mode */
	    boolean imm_flag = (instr >>> 5) == 1;
	    
	    if (imm_flag)
	    {
	    	char imm5 = signExtend((char) (instr & 0x1F), 5);
	    	reg[r0] = (char) (reg[r1] & imm5);
	    }
	    else
	    {
	    	char r2 = (char) (instr & 0x7);
	    	reg[r0] = (char) (reg[r1] & reg[r2]);
	    }
	    updateFlags(r0);
    }
    //not
    public static void not(int instr) {
    	/* destination register (DR) */
	    char r0 =  (char) ((instr >>> 9) & 0x7);
	    /* first operand (SR1) */
	    char r1 =  (char) ((instr >>> 6) & 0x7);
	    
	    reg[r0] = (char) ~reg[r1];
	    updateFlags(r0);
    }
    //Branch
    public static void br(int instr) {
    	/* PCoffset 9*/
    	int pc_offset = signExtend((char) (instr & 0x1FF), 9);
    	
    	int cond_flag = (char) ((instr >>> 9) & 0x7);
    	if((cond_flag & reg[R_COND]) != 0) {
    		reg[R_PC] += (char) (pc_offset);
    	}
    }
    //Jump
    public static void jmp(int instr) {
    	
    	char r1 = (char) ((instr >>> 6) & 0x7);
    	
    	reg[R_PC] = reg[r1];
    }
    
    //Jump reg
    public static void jsr(int instr) {
    	
    	char long_flag = (char) ((instr >>> 11) & 1);
    	reg[R_R7] = reg[R_PC];
    	
    	if(long_flag != 0) {
    		int long_pc_offset = signExtend((char) (instr & 0x7FF), 11);
    		reg[R_PC] = (char) (reg[R_PC] + long_pc_offset); //JSR
    	}
    	else {
    		int r1 = ((instr >>> 6) & 0x7);
    		reg[R_PC] = reg[r1]; //JRSS
    	}
    }
    
    //load
    public static void ld(char instr) {
    	char r0 =  (char) ((instr >>> 9) & 0x7);
    	char pc_offset = signExtend((char) (instr & 0x1FF), 9);
        reg[r0] = (char) Memory.mem_read((char) (reg[R_PC] + pc_offset));
        updateFlags(r0);
    }
    
    //Load Register
    public static void ldr(char instr) {
    	char r0 = (char) ((instr >>> 9) & 0x7);
    	char r1 = (char) ((instr >>> 6) & 0x7);
    	char offset  =  signExtend((char) (instr & 0x3F), 6);
    	//System.out.println(reg[r1]);
    	reg[r0] =  (char) Memory.mem_read((char) (reg[r1] + offset));
    	updateFlags(r0);
    }
    
    //Load effective address
    public static void lea(char instr) {
    	char r0 = (char) ((instr >>> 9) & 0x7);
    	char pc_offset  = signExtend((char) (instr & 0x1FF), 9);
    	reg[r0] = (char) (reg[R_PC] + pc_offset);
    	updateFlags(r0);
    }
    
    //Store indirect
    public static void st(char instr) {
    	char r0 = (char) ((instr >>> 9) & 0x7);
    	char pc_offset  = signExtend((char) (instr & 0x1FF), 9);
    	Memory.mem_write((char) (reg[R_PC] + pc_offset), reg[r0]);
    }
    
    //store
    public static void sti(char instr) {
    	char r0 = (char) ((instr >>> 9) & 0x7);
    	char pc_offset  = signExtend((char) (instr & 0x1FF), 9);
    	Memory.mem_write((char) (Memory.mem_read((char) (reg[R_PC] + pc_offset))), reg[r0]);
    }
    
    //store
    public static void str(char instr) {
    	char r0 = (char) ((instr >> 9) & 0x7);
    	char r1 = (char) ((instr >> 6) & 0x7);
    	char offset  = signExtend((char) (instr & 0x3F), 9);
    	Memory.mem_write((char) (reg[r1] + offset), reg[r0]);
    }
    
    
    public static final int TRAP_GETC = 0x2;  /* get character from keyboard, not echoed onto the terminal */
	public static final int TRAP_OUT = 0x21;   /* output a character */
	public static final int TRAP_PUTS = 0x22;  /* output a word string */
	public static final int TRAP_IN = 0x23;    /* get character from keyboard, echoed onto the terminal */
	public static final int TRAP_PUTSP = 0x24; /* output a byte string */
	public static final int TRAP_HALT = 0x25;   /* halt the program */
	
	
	public static void traps(char instr) {
		machine.reg[machine.R_R7] = machine.reg[machine.R_PC];
		char trapcode = (char) (instr & 0xFF);
		//System.out.printf("Handling TRAP instruction: 0x%04X\n", (int) instr);
		switch (trapcode)
		{
		    case TRAP_GETC:
		        getc();
		        break;
		    case TRAP_OUT:
		    	out();
		        break;
		    case TRAP_PUTS:
		    	puts();
		        break;
		    case TRAP_IN:
		    	in();
		        break;
		    case TRAP_PUTSP:
		    	putsp();
		        break;
		    case TRAP_HALT:
		        halt();
		        break;
		}
	}
	
	//puts
    private static void puts() {
        //System.out.println("Executing TRAP_PUTS");
        int address = reg[R_R0];
        char c;
        while ((c = Memory.mem_read((char) address)) != 0) {
            System.out.print((char) c);
            System.out.flush();
            address++;
        }
        
    }

    //getc
    private static void getc() {
    	
    	
        char c = (char) getChar();
		machine.reg[machine.R_R0] = c;
		input = null;
    }

    //output char
    private static void out() {
        //System.out.println("Executing TRAP_OUT");
        char c = (char) machine.reg[machine.R_R0];
        System.out.print(c);
        System.out.flush();
    }

    //Prompt for Input Character
    private static void in() {
        //System.out.println("Executing TRAP_IN");
        //System.out.print("Enter a character: ");
    	System.out.print("Input a character --> ");
        char c = (char) getChar();
        
        machine.reg[machine.R_R0] = c;
		System.out.println(c);
		System.out.flush();
        machine.reg[machine.R_COND] = machine.FL_ZRO;
        input = null;
    }

    //output a string
    private static void putsp() {
        //System.out.println("Executing TRAP_PUTSP");
        char address = reg[R_R0];
        char c;
        while ((c =  Memory.mem_read(address)) != 0) {
            char char1 = (char) (c & 0xFF);
            System.out.print(char1);
            System.out.flush();
            char char2 = (char) (c >> 8);
            if (char2 != 0) System.out.print(char2);
            ++address;
        }
        
    }

    //HALT!!!
    private static void halt() {
        
        System.out.println("\n----- Halting the processor ----- ");
        System.exit(1);
        
        machine.running = false;
        
    }
    
}
class Traps{
	
}
class Memory{
	
	private static char[] memory = new char[65536];
    private static Queue<Integer> inputBuffer = new LinkedList<>();
	
	public Memory() {
		memory = new char[65536];
	}

	public static char mem_read(char address) {
	    /*if (address < 0 || address >= memory.length) {
	        throw new ArrayIndexOutOfBoundsException("Memory read out of bounds: 0x" + Integer.toHexString(address));
	    }
	    if (address == machine.MR_KBSR) {
	        if (check_key()) {
	            memory[machine.MR_KBSR] = (1 << 15);
	            memory[machine.MR_KBDR] = (char) getchar();
	        } else {
	            memory[machine.MR_KBSR] = 0;
	        }
	    }*/
	    return memory[address];
	}

	public static void mem_write(char address, char value) {
	    if (address < 0 || address >= memory.length) {
	        throw new ArrayIndexOutOfBoundsException("Memory write out of bounds: 0x" + Integer.toHexString(address));
	    }
	    memory[address] = value;
	}
	
	/*private static boolean check_key() {
        return !inputBuffer.isEmpty();
    }
	
	private static int getchar() {
		while(inputBuffer.isEmpty());
        return inputBuffer.poll();
    }
	
	public static void bufferInput() {
        try {
            while (System.in.available() > 0) {
                int c = System.in.read();
                inputBuffer.add(c);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/
}
