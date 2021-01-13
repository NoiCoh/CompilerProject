/***************************/
/* Based on a template by Oren Ish-Shalom */
/***************************/

/*************/
/* USER CODE */
/*************/
import java_cup.runtime.*;



/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%%

/************************************/
/* OPTIONS AND DECLARATIONS SECTION */
/************************************/

/*****************************************************/
/* Lexer is the name of the class JFlex will create. */
/* The code will be written to the file Lexer.java.  */
/*****************************************************/
%class Lexer

/********************************************************************/
/* The current line number can be accessed with the variable yyline */
/* and the current column number with the variable yycolumn.        */
/********************************************************************/
%line
%column

/******************************************************************/
/* CUP compatibility mode interfaces with a CUP generated parser. */
/******************************************************************/
%cup

/****************/
/* DECLARATIONS */
/****************/
/*****************************************************************************/
/* Code between %{ and %}, both of which must be at the beginning of a line, */
/* will be copied verbatim (letter to letter) into the Lexer class code.     */
/* Here you declare member variables and functions that are used inside the  */
/* scanner actions.                                                          */
/*****************************************************************************/
%{
	/*********************************************************************************/
	/* Create a new java_cup.runtime.Symbol with information about the current token */
	/*********************************************************************************/
	private Symbol symbol(int type)               {return new Symbol(type, yyline, yycolumn);}
	private Symbol symbol(int type, Object value) {return new Symbol(type, yyline, yycolumn, value);}

	/*******************************************/
	/* Enable line number extraction from main */
	/*******************************************/
	int firstline;
	public int getLine()    { return yyline + 1; }
	public int getCharPos() { return yycolumn;   }
%}

/***********************/
/* MACRO DECALARATIONS */
/***********************/

LineTerminator		= \r|\n|\r\n
TAB					= [\t ]
WhiteSpace			= {TAB} | {LineTerminator}
INTEGER				= 0 | [1-9][0-9]*
ALPHABET			= [a-zA-Z]
UNDERSCORE          = "_"
CHAR 				= {ALPHABET} | [0-9]
ID 					= {ALPHABET}+({CHAR}|{UNDERSCORE})*
PARENTHESIS			= "(" | ")" | "{" | "}" | "[" | "]"
SpecialChar			= "?" | "+" | "." | "," | ";" | "-" | "!" | "_" | "=" |"\""
CommentTexts		= {SpecialChar} |{PARENTHESIS} | {CHAR} | {TAB}
SingleLineComment	= "//"({CommentTexts} | "/" | "*")*{LineTerminator}



/******************************/
/* DOLAR DOLAR - DON'T TOUCH! */
/******************************/

%state MULITYLINESCOMMENT
%%

/************************************************************/
/* LEXER matches regular expressions to actions (Java code) */
/************************************************************/

/**************************************************************/
/* YYINITIAL is the state at which the lexer begins scanning. */
/* So these regular expressions will only be matched if the   */
/* scanner is in the start state YYINITIAL.                   */
/**************************************************************/

<YYINITIAL> {
"public"            { return symbol(sym.PUBLIC); }
","			 		{ return symbol(sym.COMMA); }
"+"          	    { return symbol(sym.PLUS); }
"-"           	    { return symbol(sym.MINUS); }
"*"          	    { return symbol(sym.MULT); }
"("           	    { return symbol(sym.LPAREN); }
")"          	    { return symbol(sym.RPAREN); }
"["					{ return symbol(sym.LSBRACK); }
"]"					{ return symbol(sym.RSBRACK); }
"{"					{ return symbol(sym.LCBRACK); }
"}"					{ return symbol(sym.RCBRACK); }
";"     	        { return symbol(sym.SEMICOLON); }
"."        		    { return symbol(sym.DOT); }
"="					{ return symbol(sym.EQUAL); }
"<"					{ return symbol(sym.LT); }
"!"					{ return symbol(sym.NOT); }
"&&"                { return symbol(sym.AND2); }
"class"             { return symbol(sym.CLASS); }
"new"               { return symbol(sym.NEW); }
"extends"           { return symbol(sym.EXTENDS); }
"return"            { return symbol(sym.RETURN); }
"while"             { return symbol(sym.WHILE); }
"if"                { return symbol(sym.IF); }
"else"                { return symbol(sym.ELSE); }
"String[]"  	   	{ return symbol(sym.STRARRAY); }
"int[]"  	   		{ return symbol(sym.INTARRAY); }
"new"  	   		    { return symbol(sym.NEW); }
"length"            { return symbol(sym.LEN);  }
"true"              { return symbol(sym.TRUE);  }
"false"             { return symbol(sym.FALSE);  }
"System.out.println" { return symbol(sym.SYSOUT); }
"this"              { return symbol(sym.THIS);  }
"void"				{ return symbol(sym.VOID); }
"static"			{ return symbol(sym.STATIC); }
"main"				{ return symbol(sym.MAIN); }
"int"               { return symbol(sym.INT);  }
"boolean"           { return symbol(sym.BOOL); }
"return"            { return symbol(sym.RETURN); }
{ID}			    { return symbol(sym.ID, new String(yytext())); }
{INTEGER}           { return symbol(sym.NUMBER, Integer.parseInt(yytext())); }
{WhiteSpace}        { /* do nothing */ }

{SingleLineComment} { /* do nothing */ }
"/*"				{yybegin(MULITYLINESCOMMENT); firstline = getLine();}

<<EOF>>				{ return symbol(sym.EOF); }
}

<MULITYLINESCOMMENT> {
	"*/"                { yybegin(YYINITIAL); }
	{CommentTexts}      { /* do nothing */ }
	{LineTerminator}    { /* do nothing */ }
	"*"                 { /* do nothing */ }
	"/"                 { /* do nothing*/ }
	<<EOF>>             { System.err.print("Syntax error at line " + firstline + " of input.\n");System.exit(1);}
}