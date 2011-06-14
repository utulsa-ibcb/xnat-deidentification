package org.ibcb.xnat.redaction.config;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.*;

import org.ibcb.xnat.redaction.exceptions.CompileException;

/**
 * This is a class for compiling and interpreting written rule-set files, according to the 
 * language as defined at {@link https://intranet.truedigitalsecurity.com/mediawiki/index.php/Alert_Monitoring_System_Filter:_Rule_Language_Specification_and_Examples}
 * And for filtering based upon these compiled rules.
 * @author mkmatlock
 *
 */
public class CheckoutRuleset {
	private static int BUF_LEN = 50;
	private static boolean DEBUG = false;
	
	private static final int S_PASS = 0;
	private static final int S_DROP = 1;
	private static final int S_PASS_MODE = 2;
	private static final int S_PASS_DEFAULT = 3;
	private static final int S_DROP_ON_SIGHT = 4;
	private static final int S_RESOLVE_PASS = 5;
	private static final int S_RESOLVE_DROP = 6;
	private static final int S_STRING = 7;
	private static final int S_L_BRACKET = 8;
	private static final int S_R_BRACKET = 9;
	private static final int S_CONDITION = 10;
	private static final int S_RULE = 11;
	private static final int S_STRING_LITERAL = 12;
	private static final int S_WHITE_SPACE = 13;
	private static final int S_ASSIGN = 14;
	private static final int S_NASSIGN = 15;
	private static final int S_COMMA = 16;
	private static final int S_EOF = 17;
	private static final int S_L_PAREN = 18;
	private static final int S_R_PAREN = 19;
	private static final int S_OR_OP = 20;
	private static final int S_AND_OP = 21;
	private static final int S_XOR_OP = 22;
	private static final int S_NOT_OP = 23;
	
	private static final int COND_MODE_NONE = 0;
	private static final int COND_MODE_REGEX = 1;
	private static final int COND_MODE_CIDR = 2;
	private static final int COND_MODE_IPRANGE = 3; 
	private static final int COND_MODE_IP = 4;
	private static final int COND_MODE_NUMRANGE = 5;
	private static final int COND_MODE_NUM = 6;
	private static final int COND_MODE_COMPARE_INTEGER = 7;
	
	private static final int OR_OP = 0;
	private static final int AND_OP = 1;
	private static final int XOR_OP = 2;
	private static final int NOT_OP = 3;
	private static final int NOP = 4;
	private static final String opStrings[] = {"|","&","^","~", ""};
	
	private static final String symStrings[] = {"PASS", "DROP", "MODE", "DEFAULT", "DROP_ON_SIGHT", "RESOLVE_PASS", 
												"RESOLVE_DROP", "<string>", "{","}","CONDITION","RULE", "<string literal>",
												"<white space>", "=>", "=/>", ",", "EOF", "(", ")", "|", "&", "^", "~"};
	
	private static final ConditionExpr privateRange = new ConditionExpr();
	
	static {
		Condition base = new Condition();
		privateRange.op = OR_OP;
		
		ConditionExpr a = new ConditionExpr();
		a.op = OR_OP;
		
		base.field = "IP";
		base.mode=COND_MODE_CIDR;
		base.bitmask_len = 8;
		base.bitmask = createBitMask(base.bitmask_len);
		base.l = translateIP("10.0.0.0");		
		a.a = new ConditionExpr(base);
		base = new Condition();
		base.field = "IP";
		base.mode=COND_MODE_CIDR;
		base.bitmask_len = 16;
		base.bitmask = createBitMask(base.bitmask_len);
		base.l = translateIP("192.168.0.0");		
		a.b = new ConditionExpr(base);
		
		privateRange.a = a;
		
		ConditionExpr b = new ConditionExpr();
		b.op=OR_OP;
		
		base = new Condition();
		base.field = "IP";
		base.mode=COND_MODE_CIDR;
		base.bitmask_len = 8;
		base.bitmask = createBitMask(base.bitmask_len);
		base.l = translateIP("169.254.0.0");
		b.a = new ConditionExpr(base);
		
		base = new Condition();
		base.field = "IP";
		base.mode=COND_MODE_IPRANGE;
		base.l = translateIP("172.16.0.0");
		base.r = translateIP("172.31.255.255");
		b.b = new ConditionExpr(base);
		
		privateRange.b = b;
	}
	/** 
	 * Checks to see if a given IP is in the range reserved for private usage.
	 * @param ip The ip address in x.x.x.x format
	 * @return A boolean indicating IP classification
	 */
	public static boolean isPrivateIP(String ip, CheckoutRuleset filter){
		HashMap<String,String> ipfields = new HashMap<String, String>();
		ipfields.put("IP", ip);
		boolean b = privateRange.checkFields(ipfields, filter);
		if(DEBUG) System.out.println("IP: " + ip + " is " + (b ? "" : " not ") + "in the private range.");
		return b;
	}
	
	/**
	 * A helper function for returning a similarity value (like a diff) between two strings, this compares string a to string b using a diff like method, returning the length of identical character streams
	 * @param a The first string
	 * @param b The second string
	 * @return The similarity value, higher is better
	 */
	public static int similarity2(String a, String b){
		int cnt = 0;
		int sim = 0;
		int dif = 0;
		while(a.length()>0){
			while(cnt < a.length() && cnt < b.length() && a.charAt(cnt) == b.charAt(cnt)){
//				System.out.println(a.charAt(cnt) + "==" + b.charAt(cnt));
				sim++;
				cnt++;
			}
//			System.out.println(a + " " + b);
			a = (cnt==a.length()) ? "" : a.substring(cnt+1, a.length());
			b = (cnt==b.length()) ? "" : b.substring(cnt, b.length());
			
//			System.out.println(a + " " + b);
			cnt = 0;
			dif++;
		}
		return sim-dif+1;
	}
	
	/**
	 * A function for returning a similarity value (like a diff) between two strings
	 * @param a The first string
	 * @param b The second string
	 * @return The similarity value, higher is better
	 */
	public static int similarity(String a, String b){
		return Math.max(similarity2(a,b), similarity2(b,a));
	}
	
	/**
	 * Creates a 32 bit bitmask with a specified number of 1s starting at the 32nd bit and descending
	 * @param bitmask_length The number of 1s from the left most (32nd bit) descending
	 * @return The bitmask in long format
	 */
	public static long createBitMask(int bitmask_length){
		long bitmask = 0;
		for(int a = 0; a < bitmask_length; a++){
			bitmask += (long) Math.pow(2, 31-a);
		}
		return bitmask;
	}
	
	/**
	 * Uses the string similarity function to determine which string in the validFields[] array most closely matches a given string.
	 * This is used to help a coder correct spelling errors in his ruleset.
	 * @param s The string
	 * @return A list of Strings sorted from most to least similar
	 */
	public LinkedList<String> getMostSimilarField(String s){
		int max = 0;
		
		LinkedList<Integer> fields = new LinkedList<Integer>();
		
		int cnt=0;
		for(String field : validFields){
			int sim = similarity(s,field);
			fields.add(sim);
			if(sim > max) {max = sim;} 
			cnt++;
		}
		LinkedList<String> fieldnames = new LinkedList<String>();
		
		cnt = 0;
		for(Integer i : fields){
			if(i==max) fieldnames.add(validFields.get(cnt));
			cnt++;
		}
		
		return fieldnames;
	}
	
	/**
	 * Translates an ip from decimal to x.x.x.x format
	 * @param ipdecimal An ip in decimal format
	 * @return An ip in x.x.x.x format
	 */
	public static String translateIP(long ipdecimal) {
		long val = ipdecimal;
		long a,b,c,d;
		a = val>>24;
		b = (val-(a<<24))>>16;
		c = (val-(a<<24)-(b<<16))>>8;
		d = (val-(a<<24)-(b<<16)-(c<<8));
		return ""+a+"."+b+"."+c+"."+d;
	}
	/**
	 * Translates an ip from x.x.x.x to decimal foramt
	 * @param ip An ip in x.x.x.x format
	 * @return An ip in decimal format
	 */
	public static long translateIP(String ip) {
		StringTokenizer st = new StringTokenizer(ip, ".");
		long lip=0;
		
		lip += (Long.parseLong(st.nextToken()) << 24);
		lip += (Long.parseLong(st.nextToken()) << 16);
		lip += (Long.parseLong(st.nextToken()) << 8);
		lip += Long.parseLong(st.nextToken());
		
		return lip;
	}
	
	
	public static class Symbol {
		public int symb;
		public String text;
		public boolean backup = false;
		public int line = 0;
	}
	
	/**
	 * A class to store condition expression leaf nodes for rules in the ruleset.
	 * @author mkmatlock
	 *
	 */
	public static class Condition {
		String field;				// Field in question
		Pattern regex;				// Precompiled java regular expression
		boolean neg;
		
		int comparison;
		int mode = COND_MODE_NONE;
		
		long l;
		long r;
		long bitmask;
		int bitmask_len;
		
		/**
		 * Evaluates this condition expression on a given set of field data using the specified comparison mode
		 * @param fields The set of field values
		 * @return A boolean
		 */
		public boolean check(Map<String,String> fields){
			
			if(mode==COND_MODE_REGEX){
				Matcher mat = regex.matcher(fields.get(field));
				boolean b1 = neg ? !mat.matches() : mat.matches();
				if(DEBUG) System.out.println(fields.get(field) + (neg ? " =/> " : " => ") + regex.toString() + " = " + b1);
				return b1;
			}
			else if(mode==COND_MODE_COMPARE_INTEGER){
				Long val = Long.parseLong(fields.get(field));
				
				boolean b1=true;
				if(comparison == -1)
					b1 = val < l;
				else if(comparison==1)
					b1 = val > l;
				
				b1 = neg ? !b1 : b1;
				
				if(DEBUG) {
					System.out.println((neg ? "~" : "") + "( " + val  + (comparison==-1 ? " < " : " > ") + l + " ) = " + b1);
					System.out.println("Comparison: " + comparison);
				}
				return b1;
			}
			else if(mode==COND_MODE_CIDR){
				long num = translateIP(fields.get(field));
				boolean b1= (num & bitmask) == (l & bitmask);
				b1 = neg ? !b1 : b1;
				if(DEBUG) System.out.println((neg ? "~" : "") + "( " + num + " & " + bitmask + " == " + l + " & " + bitmask + " ) = " + b1);
				return neg ? !b1 : b1;
			}
			else if(mode==COND_MODE_IPRANGE){
				long num = translateIP(fields.get(field));
				boolean b1=((l <= num) && (num <= r));
				b1 = neg ? !b1 : b1;
				if(DEBUG) System.out.println((neg ? "~" : "") + "( " + l + "<= " + num + " <= " + r + " ) = " + b1);
				return b1;	
			}
			else if(mode==COND_MODE_IP){
				long num = translateIP(fields.get(field));
				boolean b1=(l==num);
				b1 = neg ? !b1 : b1;
				if(DEBUG) System.out.println((neg ? "~" : "") + "( " + l + " == " + num + " ) = " + b1);
				return b1;	
			}
			else if(mode==COND_MODE_NUMRANGE){
				long num = Long.parseLong(fields.get(field));
				boolean b1=((l <= num) && (num <= r));
				b1 = neg ? !b1 : b1;
				if(DEBUG) System.out.println((neg ? "~" : "") + "( " + l  + " <= " + num + " <= " + r + " ) = " + b1);
				return b1;			
			}
			else if(mode==COND_MODE_NUM){
				long num = Long.parseLong(fields.get(field));
				boolean b1=(l==num);
				b1 = neg ? !b1 : b1;
				if(DEBUG) System.out.println((neg ? "~" : "") + "( " + l  + " == " + num + " ) = " + b1);
				return b1;			
			}
			return true;		
		}
		
		/**
		 * Converts the condition expression to a string (in the valid format which can be parsed)
		 */
		public String toString(){
			String prefix = '"' + field + '"' + (neg ? " =/> " : " => ");
			
			if(mode==COND_MODE_REGEX)
				return  prefix + '"' + '@' + regex.toString() + '"';
			else if(mode==COND_MODE_COMPARE_INTEGER){
				return prefix + '"' + (comparison==-1 ? "<" : ">") + (l) + '"';
			}
			else if(mode==COND_MODE_CIDR){
				return prefix + '"' + translateIP(l) + "/" + bitmask_len + '"';
			}
			else if(mode==COND_MODE_IPRANGE){
				return prefix + '"' + translateIP(l) + "-" + translateIP(r) + '"';
			}
			else if(mode==COND_MODE_IP){
				return prefix + '"' + translateIP(l) + '"';
			}
			else if(mode==COND_MODE_NUMRANGE){
				return prefix + '"' + (l) + "-" + (r) + '"';
			}
			else if(mode==COND_MODE_NUM){
				return prefix + '"' + (l) + '"';
			}
			return "";
		}
	}
	
	

	/**
	 * A class to store condition expressions for rules in the ruleset.
	 * @author mkmatlock
	 *
	 */
	public static class ConditionExpr {
		ConditionExpr a, b;
		int op;
		Condition condition;
		String namedCondition=null;
		
		/**
		 * Construct a null condition expression with no op.
		 *
		 */
		public ConditionExpr(){
			a=null;b=null;op=NOP;condition=null;
		}
		/**
		 * Construct a condition expression with given condition (this is a leaf node).
		 * @param c
		 */
		public ConditionExpr(Condition c){
			a=null;b=null;op=NOP;condition=c;
		}
		/**
		 * Converts the condition expression to a string recurssively (in the valid format which can be parsed)
		 */
		public String toString(){			
			if(condition!=null){
				return "{ " + condition.toString() + " }";
			}
			if(namedCondition!=null){
				return namedCondition;
			}
			if(a!=null && b!=null){
				return '(' + a.toString() + ") " + opStrings[op] + " (" + b.toString() + ')';
			}
			else if(a!=null) {
				return opStrings[op] + '(' + a.toString() + ')';
			}
			return "";
		}
		
		/**
		 * Recursivelly evaluate this condition expression on a given set of field values.
		 * @param fields The field values
		 * @return A boolean indicating validity of the rules on fields
		 */
		public boolean checkFields(Map<String,String> fields, CheckoutRuleset filter){
			if(condition!=null){
				boolean b1 = condition.check(fields);
				if(DEBUG) System.out.println("cond: " + b1);
				return b1;
			}
			else if(namedCondition!=null){
				boolean b1 = filter.globalConditions.get(namedCondition).checkFields(fields, filter);
				if(DEBUG) System.out.println("ncond: " + b1);
				return b1;
			}
			
			switch(op){
			case OR_OP:
				boolean b1 = a.checkFields(fields, filter);
				boolean b2 = b.checkFields(fields, filter);
				if(DEBUG) System.out.println("" + b1 + " | " + b2 + " = " + (b1 || b2));
				return b1 || b2; 
			case AND_OP:
				b1 = a.checkFields(fields, filter);
				b2 = b.checkFields(fields, filter);
				if(DEBUG) System.out.println("" + b1 + " & " + b2 + " = " + (b1 && b2));
				return b1 && b2; 
			case XOR_OP:
				b1 = a.checkFields(fields, filter);
				b2 = b.checkFields(fields, filter);
				if(DEBUG) System.out.println("" + b1 + " ^ " + b2 + " = " + (b1 ^ b2));
				return b1 ^ b2; 
			case NOT_OP:
				b1 = a.checkFields(fields, filter);
				if(DEBUG) System.out.println("~" + b1 + " = " + (!b1));
				return !b1;
			case NOP:
				b1 = a.checkFields(fields, filter);
				if(DEBUG) System.out.println("nop " + b1 + " = " + (b1));
				return b1;
			}			
			return true;
		}
	}
	
	/**
	 * An encapsulation class for rules as defined in the rule set. Each rule has an operation (PASS OR DROP)
	 * and a condition expression.
	 * @author mkmatlock
	 *
	 */
	public static class Rule {
		boolean op;
		ConditionExpr conditions;
		
		/**
		 * Default constructor
		 *
		 */
		public Rule(){
			conditions = null;
		}
		
		/**
		 * Recursivelly evaluate this condition expression on a given set of field values.
		 * @param fields The field values
		 * @return A boolean indicating validity of the rules on fields
		 */
		public boolean checkFields(Map<String,String> fields, CheckoutRuleset filter){
			if(conditions!=null) return conditions.checkFields(fields, filter);
			return true;
		}
		
		/**
		 * Return the op associated with this rule (PASS = true, DROP = false)
		 * @return
		 */
		public boolean getOp(){
			return op;
		}
		
		/**
		 * Convert this rule to a string parseable by the compiler
		 */
		public String toString(){
			return "RULE " + (op ? "PASS" : "DROP") + " " + conditions.toString();
		}
	}
	
	/**
	 * Class to handle states in the finite state machines utilized by the lexical analyzer
	 * @author mkmatlock
	 *
	 */
	public static class State {
		LinkedList<Character> symbols;
		int rvalue = -1;		
		
		boolean backup = false;
		boolean printed = false;
		LinkedList<State> nextStates = new LinkedList<State>();
		
		/**
		 * 
		 *
		 */
		public State(){
			this.symbols=new LinkedList<Character>();
		}
		
		/**
		 * Add a valid symbol to this state.
		 * @param c The new symbol
		 */
		public void addSymbol(char c){
			symbols.add(c);
		}
		
		/**
		 * Add a child to this state in the FSM
		 * @param s The child state
		 */
		public void addChild(State s){
			nextStates.add(s);
		}
		/**
		 * Return the first child state matching the character given.
		 * @param c The character to advance state on.
		 * @return The next state, from the list of possible child states.
		 */
		public State advanceState(char c){
			for(State s : nextStates){
				for(char c2 : s.getSymbols()){
					if(c2==c){
						return s;
					}
				}
			}
			return null;
		}
		public LinkedList<Character> getSymbols(){
			return symbols;
		}
		
		/**
		 * Get the return value of this state. A state has a return value only if it is a final state. For example, when we know that we have completed a full lexical token.
		 * @return
		 */
		public int getRValue(){
			return rvalue;
		}
		public void setRValue(int rvalue){
			this.rvalue=rvalue;
		}
		
		/**
		 * Check to see if the character string needs to backup one character when this state is evaluated. Set only if this is a final state (ie it has an RValue).
		 * This is necessary for such instances as 'string string12345'. When the state sees a space, it knows it is done with a string, but whitespace is to be handled by a different FSM. Therefore, this whitespace must not be consumed, but must be passed on.
		 * @return
		 */
		public boolean streamBackup(){
			return backup;
		}
		public void setBackup(boolean backup){
			this.backup=backup;
		}
		
		/**
		 * Convert this state to a string, prefacing it with a number of tab characters. Evaluated recursively.
		 * @param line The number of tab characters, used for tree printing.
		 * @return The state, and all child states in a printable string.
		 */
		public String toString(int line){
			printed =true;
			String tabs = "";
			for(int a = 0; a < line; a++){
				tabs+="  ";
			}
			String rv = tabs + rvalue + ": ";
			for(char c : symbols){
				rv += c + " ";
			}
			rv+="\n";
			for(State s : nextStates){
				if(!s.printed)
					rv+=s.toString(line+1);
			}
			return rv;
		}
	}
	/**
	 * A finite state machine class for use by the lexical analyzer system
	 * @author mkmatlock
	 *
	 */
	public static class StateMachine {
		String name;
		State base;
		State current;
		boolean didLastBackup = false;
		public String getName(){
			return name;
		}
		
		public StateMachine(String name){
			this.name=name;
			this.base = new State();
			reset();
		}
		/**
		 * Reset this state machine to its initial state.
		 *
		 */
		public void reset(){
			current = base;
		}
		
		/**
		 * Get the root of the finite state machine's state tree.
		 * @return
		 */
		public State getBase(){
			return base;
		}
		
		/**
		 * Advance the state of this finite state machine on character c. If no valid next state exists, then the machine will fault.
		 * @param c The next character in the character stream
		 * @return The return value of the next state (-1 if there is not return value). A return value indicates that the string is fully interpretted by this machine.
		 * @throws CompileException Thrown whenever the current character is unexpected given the set of next possible states. Indicates that this machine cannot parse the given string.
		 */
		public int nextChar(char c) throws CompileException {
			State ns = current.advanceState(c);
			if(ns==null){
				if(c == '\n')
					throw new CompileException("Unexpected end of line");
				else
					throw new CompileException("Unexpected character in stream: " + c);
			}
			else{
				didLastBackup=ns.backup;
				current = ns;
				if(current.rvalue != -1)
					return current.rvalue;
			}
			return -1;
		}
		
		public String toString(){
			return "" + name + "\n-------------\n" + base.toString(0); 
		}
		/**
		 * Check to see if the last state required the stream to backup after evaluation.
		 * @return
		 */
		public boolean lastBackup(){
			return didLastBackup;
		}
	}
	
	public static class StatePair{
		State a,b;
	}
	
	/**
	 * Create a whitespace state, with a given rvalue
	 * @param rvalue The return value for the whitespace state
	 * @return The new whitespace state
	 */
	public static State createWhiteSpace(int rvalue){
		State whitespace = new State();
		whitespace.addSymbol(' ');
		whitespace.addSymbol('\t');
		whitespace.addSymbol('\n');
		whitespace.addSymbol('\r');
		whitespace.addSymbol('\0');
		whitespace.rvalue=rvalue;
		return whitespace;
	}
	/**
	 * Create a chain of states based upon a given string, with a given return value.
	 * @param s The string
	 * @param rvalue The return value
	 * @return The newly created state pair object, consisting of two states, the initial, and final
	 */
	public static StatePair createStateChain(String s, int rvalue){
		String s2 = s.toLowerCase();
		
		State l1,l2;
		
		l1 = new State(); l1.addSymbol(s.charAt(0)); l1.addSymbol(s2.charAt(0));
		
		StatePair p = new StatePair();
		
		p.a = l1;
		
		for(int a = 1; a < s.length(); a++){
			l2 = new State(); l2.addSymbol(s.charAt(a)); l2.addSymbol(s2.charAt(a));
			l1.addChild(l2);
			l1 = l2;
		}
		
		l1.rvalue = rvalue;
		p.b = l1;
		
		return p;
	}
	
	/**
	 * The lexical analysis engine of the rule interpretter. See dragon book for details.
	 * @author mkmatlock
	 *
	 */
	public static class LexicalAnalyzer {
		
		LinkedList<StateMachine> machines;
		LinkedList<StateMachine> validatingMachines;
		
		String cstring = "";	
		
		/**
		 * Initialize this lexical analyzer. This function creates the state machines, resets them, and prints any debug information.
		 * 
		 */
		public LexicalAnalyzer(){
			
			machines = new LinkedList<StateMachine>();
			validatingMachines = new LinkedList<StateMachine>();
			cstring="";
			
			// build state machines
			
			// symbol parser
			
			StateMachine symbols = new StateMachine("SYM Parser");
			State base = symbols.getBase();
			StatePair pretemp;
			StatePair temp;
			StatePair temp2;
			temp = createStateChain("MODE", S_PASS_MODE);
			base.addChild(temp.a);			
			temp = createStateChain("PASS", S_PASS);
			base.addChild(temp.a);	
			
			pretemp = createStateChain("D", -1);
			base.addChild(pretemp.a);
			temp = createStateChain("EFAULT", S_PASS_DEFAULT);
			pretemp.b.addChild(temp.a);	
			temp = createStateChain("ROP", -1);
			pretemp.b.addChild(temp.a);	
			temp2 = createStateChain("_ON_SIGHT", S_DROP_ON_SIGHT);
			temp.b.addChild(temp2.a);	
			temp.b.addChild(createWhiteSpace(S_DROP));
			
			pretemp = createStateChain("R", -1);
			base.addChild(pretemp.a);	
			temp = createStateChain("ULE", S_RULE);
			pretemp.b.addChild(temp.a);	
			temp = createStateChain("ESOLVE_",-1);
			pretemp.b.addChild(temp.a);	
			temp2 = createStateChain("PASS", S_RESOLVE_PASS);
			temp.b.addChild(temp2.a);
			temp2 = createStateChain("DROP", S_RESOLVE_DROP);
			temp.b.addChild(temp2.a);

			temp = createStateChain("{", S_L_BRACKET);
			base.addChild(temp.a);	
			temp = createStateChain("}", S_R_BRACKET);
			base.addChild(temp.a);	
			temp = createStateChain("(", S_L_PAREN);
			base.addChild(temp.a);	
			temp = createStateChain(")", S_R_PAREN);
			base.addChild(temp.a);	
			temp = createStateChain("CONDITION", S_CONDITION);
			base.addChild(temp.a);	
			
			pretemp = createStateChain("=", -1);
			base.addChild(pretemp.a);	
			temp = createStateChain(">", S_ASSIGN);
			pretemp.b.addChild(temp.a);
			temp = createStateChain("/>", S_NASSIGN);
			pretemp.b.addChild(temp.a);			
			
			temp = createStateChain(",", S_COMMA);
			base.addChild(temp.a);
			temp = createStateChain("|", S_OR_OP);
			base.addChild(temp.a);
			temp = createStateChain("&", S_AND_OP);
			base.addChild(temp.a);
			temp = createStateChain("^", S_XOR_OP);
			base.addChild(temp.a);
			temp = createStateChain("~", S_NOT_OP);
			base.addChild(temp.a);
			
			
			machines.add(symbols);
			
			// strings
			
			StateMachine strings = new StateMachine("Var name parser");
			base = strings.getBase();
			
			State letter = new State();
			char s1 = 'a';
			char s2 = 'A';
			for(char a = 0; a < 26; a++){
				letter.addSymbol((char)(s1+a));
				letter.addSymbol((char)(s2+a));
			}
			letter.addSymbol('_');
			
			State letterornumber = new State();
			s1 = 'a';
			s2 = 'A';
			for(char a = 0; a < 26; a++){
				letterornumber.addSymbol((char)(s1+a));
				letterornumber.addSymbol((char)(s2+a));
			}
			letterornumber.addSymbol('_');
			
			letterornumber.addSymbol('0');letterornumber.addSymbol('1');letterornumber.addSymbol('2');
			letterornumber.addSymbol('5');letterornumber.addSymbol('4');letterornumber.addSymbol('3');
			letterornumber.addSymbol('6');letterornumber.addSymbol('7');letterornumber.addSymbol('8');
			letterornumber.addSymbol('9');
			
			
			
			base.addChild(letter);
			
			State whitespace = new State();
			whitespace.addSymbol(' ');
			whitespace.addSymbol('\t');
			whitespace.addSymbol('\n');
			whitespace.addSymbol('\r');
			whitespace.addSymbol(')');
			whitespace.addSymbol('(');
			whitespace.addSymbol('&');
			whitespace.addSymbol('|');
			whitespace.addSymbol('~');
			whitespace.addSymbol('^');
			
			whitespace.rvalue = S_STRING;
			letterornumber.addChild(whitespace);
			letterornumber.addChild(letterornumber);
			letter.addChild(letterornumber);
			letter.addChild(whitespace);
			
			whitespace.backup=true;
			
			machines.add(strings);
			
			// string literals
			
			StateMachine string_literals = new StateMachine("String literal parser");
			base = string_literals.getBase();
			
			State quote1 = new State();
			quote1.addSymbol('"');
			base.addChild(quote1);
			
			State catchall = new State();

			s1 = 'a';
			s2 = 'A';
			for(char a = 0; a < 26; a++){
				catchall.addSymbol((char)(s1+a));
				catchall.addSymbol((char)(s2+a));
			}
			catchall.addSymbol('_');
			catchall.addSymbol(' ');
			catchall.addSymbol('\t');
			catchall.addSymbol('0');catchall.addSymbol('1');catchall.addSymbol('2');
			catchall.addSymbol('5');catchall.addSymbol('4');catchall.addSymbol('3');
			catchall.addSymbol('6');catchall.addSymbol('7');catchall.addSymbol('8');
			catchall.addSymbol('9');
			catchall.addSymbol(':');catchall.addSymbol(';');catchall.addSymbol('{');
			catchall.addSymbol('}');catchall.addSymbol('[');catchall.addSymbol(']');
			catchall.addSymbol('-');catchall.addSymbol('=');catchall.addSymbol('+');
			catchall.addSymbol('\'');catchall.addSymbol('/');catchall.addSymbol('?');
			catchall.addSymbol('<');catchall.addSymbol(',');catchall.addSymbol('>');
			catchall.addSymbol('.');catchall.addSymbol('!');catchall.addSymbol('@');
			catchall.addSymbol('#');catchall.addSymbol('$');catchall.addSymbol('%');
			catchall.addSymbol('^');catchall.addSymbol('&');catchall.addSymbol('*');
			catchall.addSymbol('(');catchall.addSymbol(')');catchall.addSymbol('~');
			catchall.addSymbol('`');catchall.addSymbol('\\');catchall.addSymbol('|');
			
			
			catchall.addChild(catchall);
			quote1.addChild(catchall);
			
			State quote2 = new State();
			quote2.addSymbol('"');
			base.addChild(quote2);
			catchall.addChild(quote2);
			
			quote2.rvalue = S_STRING_LITERAL;
		
			machines.add(string_literals);
			
			// whitespace machine
			
			StateMachine voidEater = new StateMachine("Void Eater");
			base = voidEater.getBase();
			
			whitespace = new State();
			whitespace.addSymbol(' ');
			whitespace.addSymbol('\t');
			whitespace.addSymbol('\n');
			whitespace.addSymbol('\r');
			whitespace.addSymbol('\0');
			base.addChild(whitespace);
			whitespace.addChild(whitespace);
			
			State nonVoid = new State();
			for(int a = 1; a < 256; a++){
				if(((char)a) != ' ' && ((char)a) != '\t' && ((char)a) != '\n' )
					nonVoid.addSymbol((char)(a));
			}
			whitespace.addChild(nonVoid);
			nonVoid.rvalue = S_WHITE_SPACE;
			nonVoid.backup=true;
			
			State comment = new State();
			comment.addSymbol('#');
			base.addChild(comment);
			
			catchall = new State();
			for(int a = 1; a < 256; a++){
				if(a!=(int)('\n') && a!=(int)('\r'))
					catchall.addSymbol((char)(a));
			}
			comment.addChild(catchall);
			
			State endline = new State();
			endline.addSymbol('\n');
			endline.addSymbol('\r');
			endline.addSymbol('\0');
			endline.rvalue = S_WHITE_SPACE;
			
			catchall.addChild(catchall);
			catchall.addChild(endline);
			
			machines.add(voidEater);
			
			if(DEBUG){
				for(StateMachine m : machines){
					System.out.println(m.toString());
				}
			}
		}
		/**
		 * cleanup after a string has been parsed. Resets all the machines and clears the current string.
		 * 
		 */
		public void cleanup(){
			for(StateMachine m : machines){
				m.reset();
			}
			validatingMachines.clear();
			cstring = "";
		}
		/**
		 * Interprets the next character in the stream, returns a new symbol object if this character completed a token.
		 * @param c The next character
		 * @return Null if there was no token, or a symbol object if a token was completed.
		 * @throws CompileException Thrown whenever there is a lexical analysis error.
		 */
		public Symbol nextChar(char c) throws CompileException {
			if(validatingMachines.size()==0){
				for(StateMachine m : machines) {
					validatingMachines.add(m);
				}
			}
			cstring+=c;
			LinkedList<Integer> faults = new LinkedList<Integer>();	
			int cnt = 0;
			
			if(DEBUG) System.out.println("Checking: " + c + " current string: " + cstring);
			
			for(StateMachine m : validatingMachines){
				try{
					if(DEBUG) System.out.println("Using machine: " + m.getName());
					int rvalue = m.nextChar(c);
					if(DEBUG) System.out.println("RValue: " + rvalue);
					if(rvalue!=-1){
						// process symbol
						Symbol nsym = new Symbol();
						nsym.symb = rvalue;
						nsym.text = cstring;
						if(m.lastBackup()) {
							nsym.backup = true;
							nsym.text = nsym.text.substring(0, nsym.text.length()-1);
						}
						cleanup();
						return nsym;
					}
				}catch(CompileException e){
					if(DEBUG) System.out.println("Machine Fault: " + m.getName());
					faults.add(cnt);
					if(faults.size() == validatingMachines.size()){
						System.out.println("Parse Fail");
						cleanup();
						throw e;
					}
				}
				cnt++;
			}
			
			Collections.reverse(faults);
			
			for(Integer i : faults){
				if(DEBUG) System.out.println("Remove: " + validatingMachines.get(i).getName());
				validatingMachines.remove(i.intValue());
			}
			
			if(DEBUG) System.out.println("Validating: " + validatingMachines.size());
			if(DEBUG) System.out.println("------------------------\n");
			
			return null;
		}
	}
	/**
	 * The syntax analyzer for the rule system, see dragon book for details
	 * @author mkmatlock
	 *
	 */
	public class SyntaxAnalyzer {
		
		LinkedList<Symbol> symbols;
		int pos = 0;
		
		Rule lastRule = null;
		Stack<ConditionExpr> conditionStack = new Stack<ConditionExpr>();
		ConditionExpr lastpop;
//		private void pushStack(ConditionExpr c){
//			conditionStack.push(c);
//		}
		
		private ConditionExpr popStack(){
			lastpop = conditionStack.peek();
			conditionStack.pop();
			return lastpop;
		}
		
//		private ConditionExpr getLastPop(){
//			return lastpop;
//		}
		
		private Symbol nextSymbol() throws CompileException {
			Symbol rval = null;
			if(symbols.size() > pos) rval= symbols.get(pos++);
			else throw new CompileException("Unexpected end of symbol stream encountered");
			
			if(DEBUG) System.out.println(" " + symStrings[rval.symb]);
			return rval;
		}
		public Symbol curSymbol() {
			if(pos < symbols.size())
				return symbols.get(pos);
			return null;
		}
		private void backupSymbol() {
			if(pos > 0) pos--;
		}
		
		private boolean checkNames(String name) {
			return globalConditions.containsKey(name);
		}
		
//		private void createCondition(String name) {
//			globalConditions.put(name, new ConditionExpr());
//		}
		private void createCondition(String name, ConditionExpr c) {
			globalConditions.put(name, c);
		}
		
		String last_name = "";
		
		public void analyzeSymbols(LinkedList<Symbol> sym) throws CompileException {
			symbols = sym;
			
			root();
		}
		
		public void force(Symbol s, int rval) throws CompileException {
			if(s.symb != rval) throw new CompileException("Unexpected symbol in stream: " + s.symb + "-'" + s.text + "' Expected: " + rval + "-'" + symStrings[rval] + "'");
		}
		
		public void error(Symbol s) throws CompileException {
			throw new CompileException("Unexpected symbol in stream: " + s.symb + "-'" + s.text + "'");
		}
		
		public void root() throws CompileException {
			if(DEBUG) System.out.println("root");
			
			global_setting();
			global_setting();	
			condition_decs();
			rule_decs();
		}
		
		public void global_setting() throws CompileException {
			if(DEBUG) System.out.println("global_setting");
			
			Symbol s = nextSymbol();
			switch(s.symb){
			case S_PASS_MODE:
				backupSymbol();
				global_pass_mode();
				
				break;
			case S_PASS_DEFAULT:
				backupSymbol();
				global_pass_default();
				break;
			default:
				error(s);
			break;
			}
		}
				
		public void global_pass_mode()  throws CompileException {
			if(DEBUG) System.out.println("global_pass_mode");
			
			Symbol s = nextSymbol();
			force(s, S_PASS_MODE);
			s = nextSymbol();
			
			switch(s.symb){
				case S_DROP_ON_SIGHT:
					PASS_MODE = drop_on_sight;
					break;
				case S_RESOLVE_PASS:
					PASS_MODE = resolve_pass;
					break;
				case S_RESOLVE_DROP:
					PASS_MODE = resolve_drop;
					break;
				default:
					error(s);
				break;
			}
		}
		public void global_pass_default()  throws CompileException {
			if(DEBUG) System.out.println("global_pass_default");
			
			Symbol s = nextSymbol();
			force(s, S_PASS_DEFAULT);
			s = nextSymbol();
			
			switch(s.symb){
				case S_PASS:
					PASS_DEFAULT = true;
					break;
				case S_DROP:
					PASS_DEFAULT = false;
					break;
				default:
					error(s);
				break;
			}
		}
		
		public void condition_decs() throws CompileException {
			if(DEBUG) System.out.println("condition_decs");
			Symbol s = nextSymbol();
			switch(s.symb){
			case S_CONDITION:
				
				s = nextSymbol();
				force(s, S_STRING);
				
				if(checkNames(s.text)) throw new CompileException("Condition variable name already used!");
				ConditionExpr expr = new ConditionExpr();
				expr.op = NOP;
				conditionStack.push(expr);
				condition_expr();
				expr = conditionStack.pop();
			
				createCondition(s.text, expr);
				
				if(DEBUG) System.out.println("Creating: CONDITION " + s.text + " " + expr.toString());
				
				condition_decs();
				break;
			case S_RULE:
				backupSymbol();
				break;
			case S_EOF:
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr() throws CompileException {
			if(DEBUG) System.out.println("condition_expr");
			Symbol s = nextSymbol();
			ConditionExpr a;
			
			switch(s.symb){
			case S_L_PAREN:
			case S_L_BRACKET:
			case S_STRING:
			case S_NOT_OP:
				backupSymbol();
				a = new ConditionExpr();
				if(conditionStack.size() > 0)
					conditionStack.peek().a=a;
				conditionStack.push(a);
				condition_expr_med();
				popStack();
				condition_expr_prime_or();
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr_med() throws CompileException {
			if(DEBUG) System.out.println("condition_expr_med");
			Symbol s = nextSymbol();
			ConditionExpr a;
			
			switch(s.symb){
			case S_L_PAREN:
			case S_L_BRACKET:
			case S_STRING:
			case S_NOT_OP:
				backupSymbol();
				a = new ConditionExpr();
				if(conditionStack.size() > 0)
					conditionStack.peek().a=a;
				conditionStack.push(a);
				condition_expr_final();
				popStack();
				condition_expr_prime_xor();
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr_final() throws CompileException {
			if(DEBUG) System.out.println("condition_expr_final");
			
			Symbol s = nextSymbol();
			ConditionExpr a;
			
			switch(s.symb){
			case S_L_PAREN:
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);
				condition_expr();
				s = nextSymbol();
				force(s, S_R_PAREN);
				popStack();
				
				condition_expr_prime_and();
				
				break;
			case S_L_BRACKET:
				backupSymbol();
				
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);
				bracketed_condition();
				popStack();	
				
				condition_expr_prime_and();				
				break;
			case S_STRING:
				backupSymbol();
				
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);				
				named_condition();
				popStack();
				
				condition_expr_prime_and();
				break;
			case S_NOT_OP:
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);
				a.op = NOT_OP;
				condition_expr_atom();
				popStack();
				condition_expr_prime_and();
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr_atom() throws CompileException {
			if(DEBUG) System.out.println("condition_expr_atom");
			
			Symbol s = nextSymbol();
			
			ConditionExpr a;
			
			switch(s.symb){
			case S_L_PAREN:
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);
				condition_expr();
				s = nextSymbol();
				force(s, S_R_PAREN);
				popStack();				
				break;
			case S_L_BRACKET:
				backupSymbol();
				
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);
				bracketed_condition();
				popStack();
				break;
			case S_STRING:
				backupSymbol();
				
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);				
				named_condition();
				popStack();
				break;
			case S_NOT_OP:
				a = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().a = a;
				conditionStack.push(a);
				a.op = NOT_OP;
				condition_expr_atom();
				popStack();
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr_prime_and() throws CompileException {
			if(DEBUG) System.out.println("condition_expr_prime_and");
			Symbol s = nextSymbol();
			
			ConditionExpr b;
			switch(s.symb){
			case S_AND_OP:
				if(conditionStack.size()>0)
					conditionStack.peek().op = AND_OP;
				b = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().b = b;
				conditionStack.push(b);
				condition_expr();
				popStack();
				break;
			case S_OR_OP:
			case S_XOR_OP:
			case S_R_PAREN:
			case S_RULE:
			case S_CONDITION:
			case S_EOF:	
				backupSymbol();
				// collapse the stack man
				if(DEBUG) System.out.println(conditionStack.size() + " " + conditionStack.get(0).toString());
				if(conditionStack.size()>1){
					ConditionExpr up = popStack();
					ConditionExpr remove = popStack();
					
					if(remove.op == NOP){
						if(conditionStack.size()>0){
							if(conditionStack.peek().a == remove) conditionStack.peek().a = up;
							else conditionStack.peek().b = up;
						}
						conditionStack.push(up);
						conditionStack.push(remove);
					}
					else{
						conditionStack.push(remove);
						conditionStack.push(up);
					}
				}
				if(DEBUG) System.out.println(conditionStack.size() + " " + conditionStack.get(0).toString());
				
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr_prime_or() throws CompileException {
			if(DEBUG) System.out.println("condition_expr_prime_or");
			Symbol s = nextSymbol();
			
			ConditionExpr b;
			switch(s.symb){
			case S_OR_OP:
				if(conditionStack.size()>0)
					conditionStack.peek().op = OR_OP;
				b = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().b = b;
				conditionStack.push(b);
				condition_expr();
				popStack();
				break;
			case S_AND_OP:
			case S_XOR_OP:
			case S_R_PAREN:
			case S_RULE:
			case S_CONDITION:
			case S_EOF:	
				backupSymbol();
				// collapse the stack man
				if(DEBUG) System.out.println(conditionStack.size() + " " + conditionStack.get(0).toString());
				if(conditionStack.size()>1){
					ConditionExpr up = popStack();
					ConditionExpr remove = popStack();
					
					if(remove.op == NOP){
						if(conditionStack.size()>0){
							if(conditionStack.peek().a == remove) conditionStack.peek().a = up;
							else conditionStack.peek().b = up;
						}
						conditionStack.push(up);
						conditionStack.push(remove);
					}
					else{
						conditionStack.push(remove);
						conditionStack.push(up);
					}
				}
				if(DEBUG) System.out.println(conditionStack.size() + " " + conditionStack.get(0).toString());
				break;
			default:
				error(s);
				break;
			}
		}
		
		public void condition_expr_prime_xor() throws CompileException {
			if(DEBUG) System.out.println("condition_expr_prime_xor");
			Symbol s = nextSymbol();
			
			ConditionExpr b;
			switch(s.symb){
			case S_XOR_OP:
				if(conditionStack.size()>0)
					conditionStack.peek().op = XOR_OP;
				b = new ConditionExpr();
				if(conditionStack.size()>0)
					conditionStack.peek().b = b;
				conditionStack.push(b);
				condition_expr();
				popStack();
				break;
			case S_AND_OP:
			case S_OR_OP:
			case S_R_PAREN:
			case S_RULE:
			case S_CONDITION:
			case S_EOF:	
				backupSymbol();
				// collapse the stack man
				if(DEBUG) System.out.println(conditionStack.size() + " " + conditionStack.get(0).toString());
				if(conditionStack.size()>1){
					ConditionExpr up = popStack();
					ConditionExpr remove = popStack();
					
					if(remove.op == NOP){
						if(conditionStack.size()>0){
							if(conditionStack.peek().a == remove) conditionStack.peek().a = up;
							else conditionStack.peek().b = up;
						}
						conditionStack.push(up);
						conditionStack.push(remove);
					}
					else{
						conditionStack.push(remove);
						conditionStack.push(up);
					}
				}
				if(DEBUG) System.out.println(conditionStack.size() + " " + conditionStack.get(0).toString());
				break;
			default:
				error(s);
				break;
			}
		}
		


		public void named_condition() throws CompileException {
			if(DEBUG) System.out.println("named_condition");
			Symbol s = nextSymbol();
			force(s, S_STRING);
			
			if(!checkNames(s.text)) throw new CompileException("Attempted to reference undeclared condition variable");
			
			if(conditionStack.size()>0)
				conditionStack.peek().namedCondition = s.text;
			else throw new CompileException("Internal Error: Condition Stack should NOT be empty!");
		}
		

		
		public void bracketed_condition() throws CompileException {
			if(DEBUG) System.out.println("bracketed_condition");
			
			Condition c = new Condition();
			
			Symbol s = nextSymbol();
			force(s, S_L_BRACKET);
		 		
			s = nextSymbol();
			force(s, S_STRING_LITERAL);
			
			c.field = s.text.substring(1, s.text.length()-1);
			
			boolean pass = false;
			for(String field : validFields){
				if(c.field.equals(field)) pass = true;
			}
			if(!pass) {
				LinkedList<String> mostsimilar = getMostSimilarField(c.field);
				
				String fieldstring="";
				for(String p : mostsimilar){
					fieldstring+='"'+p+"\", ";
				}
				if(fieldstring.length()>0) fieldstring = fieldstring.substring(0,fieldstring.length()-2);
				throw new CompileException("Invalid field specified: \"" + c.field + '"' + (fieldstring.length()>0 ? ", you might have meant one of: [" + fieldstring + "]" : ", No suggestions available"));
				
			}
			
			s = nextSymbol();
			
			switch(s.symb){
			case S_ASSIGN:
				c.neg=false;
				break;
			case S_NASSIGN:
				c.neg=true;
				break;
			default:
				error(s);
				break;
			}
			
			s = nextSymbol();
			force(s, S_STRING_LITERAL);
			
			String literal = s.text.substring(1, s.text.length()-1);
			if(literal.charAt(0) == '@'){
				c.mode = COND_MODE_REGEX;		
				try{
					c.regex = Pattern.compile(literal.substring(1, literal.length()));
				}catch(PatternSyntaxException e){
					throw new CompileException("Pattern Syntax Exception in Regex: " + e.getMessage());
				}
			}
			else if(literal.charAt(0) == '>' || literal.charAt(0) == '<'){
				c.mode = COND_MODE_COMPARE_INTEGER;		
				
				c.comparison = literal.charAt(0) == '>' ? 1 : -1;
				
				try{
					c.l = Long.parseLong(literal.substring(1));
				}catch(Exception e){
					throw new CompileException("Bad number format for comparison condition");
				}
			}
			else if(literal.contains(".") && literal.contains("/")){
				c.mode = COND_MODE_CIDR;
				try{
					StringTokenizer st = new StringTokenizer(literal, "/");
					c.l = translateIP(st.nextToken());
					int bitmask_length = Integer.parseInt(st.nextToken());
					for(int a = 0; a < bitmask_length; a++){
						c.bitmask += (long) Math.pow(2, 31-a);
					}
				}catch(Exception e){
					throw new CompileException("Bad CIDR Syntax in Condition Expression");
				}
			}
			else if(literal.contains(".") && literal.contains("-")){
				c.mode = COND_MODE_IPRANGE;
				try{
					StringTokenizer st = new StringTokenizer(literal, "-");
					
					c.l = translateIP(st.nextToken());
					c.r = translateIP(st.nextToken());
				}catch(Exception e){
					throw new CompileException("Bad IP Range Syntax in Condition Expression");
				}
			}
			else if(literal.contains(".")){
				c.mode = COND_MODE_IP;
				try{
					c.l = translateIP(literal);
				}catch(Exception e){
					throw new CompileException("Bad IP Syntax in Condition Expression");
				}
			}
			else if(literal.contains("-")){
				c.mode = COND_MODE_NUMRANGE;
				try{
					StringTokenizer st = new StringTokenizer(literal, "-");
				
					c.l = Long.parseLong(st.nextToken());
					c.r = Long.parseLong(st.nextToken());
				}catch(Exception e){
					throw new CompileException("Bad Number Range Syntax in Condition Expression");
				}
			}
			else {
				try{
					long num = Long.parseLong(literal);
					c.mode = COND_MODE_NUM;
					c.l = num;
				}catch(Exception e){
					throw new CompileException("Unrecognized condition statement");
				}
			}
			
			s = nextSymbol();
			force(s, S_R_BRACKET);
			
			if(conditionStack.size()>0)
				conditionStack.peek().condition=c;
			else throw new CompileException("Internal Error: Condition Stack should NOT be empty!");
		}
		
		public void rule_decs() throws CompileException {
			if(DEBUG) System.out.println("rule_decs");
			
			Symbol s = nextSymbol();
			switch(s.symb){
			case S_RULE:
				lastRule = new Rule();
				s = nextSymbol();
				switch(s.symb) {
				case S_PASS:
					lastRule.op=true;
					break;
				case S_DROP:
					lastRule.op=false;
					break;
				default:
					error(s);
					break;
				}
				ConditionExpr expr = new ConditionExpr();
				expr.op = NOP;
				conditionStack.push(expr);
				condition_expr();
				expr = conditionStack.pop();

				lastRule.conditions = expr;
				
				if(DEBUG) System.out.println("Creating: " + lastRule.toString());
				
				rules.add(lastRule);
				rule_decs();
				
				break;
			case S_EOF:
				break;
			default:
				error(s);
				break;
			}
		}
	}
	
	static final int drop_on_sight = 0;
	static final int resolve_pass  = 1;
	static final int resolve_drop  = 2;
	
	int PASS_MODE;
	boolean PASS_DEFAULT;
	
	LinkedList<Integer> lastRules;

	Map< String, ConditionExpr > globalConditions;
	LinkedList<Rule> rules;	
	
	/**
	 * Load the ruleset and compile it.
	 * @param filename The full path to the rule file.
	 * @return A boolean indicating success or failure.
	 */
	public boolean loadRuleSet(String filename){
		File file = new File(filename);
		
		LinkedList<Symbol> symbols = new LinkedList<Symbol>();
		
		if(!file.exists()) {
			System.out.println("File not found!");
			return false;
		}
		FileInputStream fis;
		try{
			fis = new FileInputStream(file);
		}catch(Exception e){
			System.out.println("File not found!");
			return false;
		}
		
		LexicalAnalyzer parse = new LexicalAnalyzer();
		int line = 1;
		try{
			byte buffer[] = new byte[BUF_LEN];
			
			int amnt_read = fis.read(buffer);
			
			int cnt = 0;
			
			while(amnt_read > 0){
				char next = (char)buffer[cnt];			
				if(next=='\n') line++;
				try{
					Symbol sym = parse.nextChar(next);
					if(sym!=null) {
						cnt-= sym.backup ? 1 : 0;
						sym.line = line;
						if(sym.symb != S_WHITE_SPACE)
							symbols.add(sym);
					}
				}catch(CompileException e){
					System.out.println("Lexical Parsing Error: Line " + line + ": " + e.getMessage());
					return false;
				}
				cnt++;
				if(cnt==amnt_read){
					cnt=0;
					amnt_read = fis.read(buffer);
				}
			}
			
			try{
				Symbol sym = parse.nextChar('\0');
				if(sym!=null) {
					cnt-= sym.backup ? 1 : 0;
					sym.line = line;
					if(sym.symb != S_WHITE_SPACE)
						symbols.add(sym);
				}
			}catch(CompileException e){
				System.out.println("Lexical Parsing Error: Line " + line + ": " + e.getMessage());
				return false;
			}
			
			Symbol sym = new Symbol();
			sym.line = line;
			sym.symb = S_EOF;
			sym.text = "EOF";
			symbols.add(sym);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
		if(DEBUG){
			System.out.println("\nId          | String           ");
			System.out.println("----------------------------------------------------");
			for(Symbol s : symbols){
				String out = ""+s.symb;
				while(out.length() < 12) out += " ";
				System.out.println(out + "  " + s.text);
			}
		}
		
		// grammar parsing
		
		SyntaxAnalyzer syntax = new SyntaxAnalyzer();
		
		globalConditions = new HashMap<String,ConditionExpr >();
		rules = new LinkedList<Rule>();
		lastRules = new LinkedList<Integer>();
				
		try{
			// next syntax item
			syntax.analyzeSymbols(symbols);
		}catch(CompileException e){
			System.out.println("Syntax Parsing Error: Line " + (syntax.curSymbol()!=null ? syntax.curSymbol().line : line) + ": " + e.getMessage());
			return false;
		}
		
		if(DEBUG) {
			System.out.println("Default Passing Mode: " + (PASS_DEFAULT ? "pass" : "drop"));
			System.out.println("Default Resolution Mode: " + (PASS_MODE == drop_on_sight ? "Drop On Sight" : (PASS_MODE == resolve_pass ? "Resolve Passing" : "Resolve Failing")));
			System.out.println();
			printConditions();
			System.out.println();
			printRules();
			System.out.println();
		}
		
		return true;
	}
	/**
	 * Print the conditions found in the compiled rule set file.
	 *
	 */
	public void printConditions(){
		for(String s : globalConditions.keySet()){
			System.out.println("CONDITION " + s + " " + globalConditions.get(s).toString());
		}
	}
	/**
	 * Print the rules found in the compiled rule set file.
	 *
	 */
	public void printRules(){
		for(Rule r : rules){
			System.out.println(r.toString());
		}
	}
	/**
	 * Gets the last set of rules that were invoked when filtering the last alert passed through this system.
	 * @return
	 */
	public String getLastRuleList(){
		String r ="";
		for(Integer i : lastRules){
			r+=""+i+", ";
		}
		if(lastRules.size() > 0)
			return r.substring(0, r.length()-2);
		return r;
	}
	/**
	 * Filter an alert with a given set of field values using the rule set which was compiled prior to filtering.
	 * @param fields The field values for the alert.
	 * @return A boolean indicating whether or not the rule passed filtering
	 */
	public boolean filter(Map<String,String> fields){
		boolean pass = false;
		lastRules.clear();
		
		if(DEBUG) System.out.println("---------------------\nAttempting filtering!\n---------------------");
		
		if(PASS_MODE == drop_on_sight){
			int cnt = 0;
			for(Rule r : rules){
				try{
					if(r.checkFields(fields, this)){
						if(DEBUG) System.out.println("Rule " + cnt + " passed conditions, invoking");
						lastRules.add(cnt);
						if(!r.op){
							if(DEBUG) System.out.println("Rule " + cnt + " dropped\n");
							return false;
						}
						else if(DEBUG) System.out.println("Rule " + cnt + " passed\n");
					}
					else if(DEBUG) System.out.println("Rule " + cnt + " failed conditions\n");
				}catch(Exception e){
					System.out.println("Exception Generated When Processing Rule " + cnt + ": " + r.toString());
					System.out.println("Data Set:");
					for(String s : fields.keySet()){
						System.out.println('"' + s + '"' + " => " + '"' + fields.get(s) + '"');
					}
					e.printStackTrace();
				}
				cnt++;
			}
			return lastRules.size()>0 ? true : PASS_DEFAULT;
		}
		else if(PASS_MODE == resolve_pass){
			int cnt = 0;
			for(Rule r : rules){
				try{
					if(r.checkFields(fields, this)){
						lastRules.add(cnt);
						pass |= r.op;
					}
				}catch(Exception e){
					System.out.println("Exception Generated When Processing Rule " + cnt + ": " + r.toString());
					System.out.println("Data Set:");
					for(String s : fields.keySet()){
						System.out.println('"' + s + '"' + " => " + '"' + fields.get(s) + '"');
					}
					e.printStackTrace();
				}
				cnt++;
			}
			return lastRules.size()>0 ? pass : PASS_DEFAULT;
		}
		else if(PASS_MODE == resolve_drop){
			pass = true;
			
			int cnt = 0;
			for(Rule r : rules){
				try{
					if(r.checkFields(fields, this)){
						lastRules.add(cnt);
						pass &= r.op;
					}
				}catch(Exception e){
					System.out.println("Exception Generated When Processing Rule " + cnt + ": " + r.toString());
					System.out.println("Data Set:");
					for(String s : fields.keySet()){
						System.out.println('"' + s + '"' + " => " + '"' + fields.get(s) + '"');
					}
					e.printStackTrace();
				}
				cnt++;
			}
			
			return lastRules.size()>0 ? pass : PASS_DEFAULT;
		}
		
		return true;
	}
	/**
	 * Check to see if a given IP is in the private range.
	 * @param ip
	 */
	public static void testIP(String ip, CheckoutRuleset filter){
		boolean b = isPrivateIP(ip, filter);
		System.out.println("IP: " + ip + " is " + (b ? "" : " not ") + "in the private range.");
	}
	
	/**
	 * Print the list of values in the map.
	 * @param map The map
	 * @return A printable string with a value list.
	 */
	public static String printMap(Map<String,String> map){
		String r = "";
		for(String s : map.keySet()){
			r+=(s + ": " + map.get(s))+"\n";
		}
		return r;
	}
	
	public static final String DEFAULT_LOCATION="/root/rules";
	
	/** This is a list of valid fields to use for filtering */
	private LinkedList<String> validFields = new LinkedList<String>();
	
	public void clearValidFields(){
		validFields.clear();
	}
	
	public void setFields(String ... fields){
		for(String s : fields){
			validFields.add(s);
		}
	}
	
	/**
	 * Do not use, testing purposes only.
	 * @deprecated
	 * @param enable_filtering
	 * @param fields
	 */
	public void tryFilter(boolean enable_filtering, Map<String,String> fields){
        // Filter the alert
        if(enable_filtering && !filter(fields)){
        	System.out.println("Alert filtered due to ruleset: " + getLastRuleList());
        }
        else System.out.println("Alert passed filtering!");
	}
}
