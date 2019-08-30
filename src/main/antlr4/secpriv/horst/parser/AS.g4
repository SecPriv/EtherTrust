// Grammar for horn claused based abstract program semantics

grammar AS;

/*** Abstract programs ***/

abstractProgram:
 (abstractDomainDeclaration
 | operationDefinition
 | predicateDeclaration
 | selectorFunctionDeclaration
 | ruleDefinition
 | globalMacroDefinition
 | initialization
 | query
 | test
 | constDefinition)*EOF;

 /** constants ***/

constDefinition: 'const' constID ':=' exp ';'; //TODO check for constness

/*** Abstract domains ***/
/* abstract datatype declaraion: Allows for defining (non-recursive) datatypes */
abstractDomainDeclaration:
    (DATATYPE|EQTYPE) typeID ':=' abstractDomainElements ';'
    ;

abstractDomainElements :
    (abstractDomainElement '|')* abstractDomainElement;

abstractDomainElement : elementID ('<' types '>')? // defined abstract datatypes can either be enums or constructors
                        ;

/*** Abstract Operations **/
// the abstract operations section is made for defining 1) new operations on the abstract domain 2) additional (not pre-implemented) operations on the basetypes

operationDefinition: 'op' opID ('{' parameters '}')? '('arguments')' ':' type ':=' exp ';';
arguments: (argument ',')* argument;
argument: ID ':' type;

/*** Types ***/
// TODO: What is about tuple types? (might be nice but not necessary)

types: (type '*')* type; // star-separated list of types
type : baseType | arrayType | typeID;
arrayType : 'array' '<' type '>';
baseType : INT | BOOL ;
baseTypes: (baseType '*')* baseType;

//TODO: Not used at the moment (actually abstract datatypes are sufficient)
tupleType: (tupleType2 '*')* tupleType2; /* does not allow for tuples with one element */
tupleType2: '(' (tupleType '*')+ tupleType ')' | 'unit' | type;

tupleBaseType: (tupleBaseType2 '*')* tupleBaseType2; /* does not allow for tuples with one element */
tupleBaseType2: '(' (tupleBaseType '*')+ tupleBaseType ')' | 'unit' | baseType;

/*** Predicates ***/
predicateDeclaration:
    'pred' predID '{' (baseTypes)? '}' ':' types ';'; //TODO: Are eqtypes enough? Parametrization by arrays does not make too much sense.

/*** Selector Functions ***/

/* selector function that ranges over closed expressions */
/* these functions will be implemented by the developer */
/* Requirement: selectors functions need to return an iterable Collection */
selectorFunctionDeclaration :
    'sel' selectFunID ':' selectorFunctionArgTypes  '->' '[' baseTypes ']' ';';

selectorFunctionArgTypes: baseTypes | unit;
unit : UNIT;

/*** Rules ***/
ruleDefinition:
    'rule' ruleID ':=' ruleBody ';';

ruleBody:
     selectorExp? ('let' macros 'in')? clauses;

selectorExp: 'for' selectorInvocation (',' selectorInvocation)*;

selectorInvocation: '(' parameters ')' 'in' selectorApp;

/* rule parameters: (typed) parameters for the rules that can be used for 1) parametrizing the rule 2) as constants within the rule and that are restricted by selector */
parameters:
    (parameter ',')* parameter;

/* rule Parameters are restricted to be basetypes: this is as this is the interface to the java implementation and we want to be independent of encodings of more complex types*/
parameter: paramID ':' baseType;

/* a list of selctor function applications can be specified that restricts the set of parameters that rules are created for */
/* a parameter can be restricted by several sets: in this case the intersection of those sets is used */
// selectors: '[' (selectorExp ',')* selectorExp ']';
//TODO[TYPECHECKING]: ensure that all parameters are bound by at least one selectorExp

selectorApp:
    selectFunID '(' (selectFunArgs)? ')';

selectFunArgs:
    (exp ',')* exp; //TODO[TYPECHECKING]: check for closedness
    // TODO: Question: do we allow for other parameters here? (alternatively: can we allow for tuples over parameters?)

/* A set of macros that might be used in premises of the rules in order to improve readability */
/* allows for the introduction of bound variables + free variables */
/* closedness will be evaluated within the context of the rule, in case that a free variable is not explicitely declared, a warning is reported */
macros: (localMacroDef ',')* localMacroDef;
localMacroDef: 'macro' macroDef;
macroDef: macroSignature ':=' macroBody macroFreeVars?;
macroSignature : macroID ('(' macroDefArgs ')')?;
macroBody:  prems;
macroFreeVars: 'free' '[' freeVars ']';
//TODO[TYPECHECKING]: check for closedness up to freeVars (print warning!)
macroDefArgs: (MACRO_PAR_ID ':' type ',')* MACRO_PAR_ID ':' type;
freeVars: (freeVar':' type ',')* freeVar ':' type;
//TODO: what is about parameters? can they assumed to be bound in the rule? (would make sense)

/* A set of clause definitions that share name and parametrization */
clauses: (clauseDef ',')* clauseDef;
clauseDef: 'clause' ('[' freeVars ']')? clause;
clause: prems '=>' conc;
prems: (prop ',')* prop;


/* propositions are either predicate applications or boolean expressions */
prop: predApp | exp | macroApp; //TODO[TYPECHECKING]: check that exp is of type bool

/* predicate application */
predApp: predID('{'predParams'}')? '(' (predArgs)? ')';
predParams: (predParam ',')* predParam;
predParam: exp; //TODO[TYPECHECKING]: check for closedness up to parameters
predArgs: (predArg ',')* predArg;
predArg: exp; //TODO[TYPECHECKING]: check for well-typedness
// predArg: predArgID; //TODO: We should allow for all kind of constants here, do we really need constants or could we put all checks outside (in principle we could)
/* Here we have an interesting switch for the implementation: Does it make a difference replacing all arguments by (fresh) variables + insert corresponding equality checks */

/* macro application */
macroApp: macroID ('(' macroArgs ')')?;
macroArgs: (macroArg ',')* macroArg;
macroArg: exp; //TODO[TYPECHECKING]: check for well-typedness
/* experimenting with macros might be also interesting -> in principle we could make them own predicates */

/* expressions */
/* Requirement: variables per rule shall be disjoint */
exp: constant
    | macroVar
    | parenExp
    | storeExp
    | selectExp
    | appExp
    | constructorAppExp
    | arrayInitExp
    | var //TODO[TYPECHECKING]: check that in rule there are no vars! (these are only for function definitions)
    | parVar
    | constID
    | freeVar
    | <assoc=left> exp intOPPred1 exp
    | <assoc=left> exp intOPPred2 exp
    | BVNEG exp
    | <assoc=left> exp BVAND exp
    | <assoc=left> exp BVXOR exp
    | <assoc=left> exp BVOR exp
    | NEG exp
    | <assoc=left> exp compOP exp //TODO[TYPECHECKING]: make sure that '=' only works with expressions of eqtype
    | <assoc=left> exp AND exp
    | <assoc=left> exp OR exp
    | condExp
    | sumExp
    | matchExp;

intOPPred2 : PLUS | MINUS ;
intOPPred1 : MUL | DIV | MOD;

macroVar : MACRO_PAR_ID;

parenExp : '(' exp')' ;

storeExp : 'store' exp exp exp ;

selectExp : 'select' exp exp ;

condExp : '(' exp ')' '?''(' exp ')' ':' '(' exp ')' ;

//negExp : '~' exp ;

arrayInitExp : '[' exp ']'; //TODO[TYPECHECKING]: exp must be const (:= closed up to bound parameters)


//TODO problem at the moment: if we want to use them for function defs as well we should also allow for normal variables

/* veeExps allow for big disjunctions over a selector function range */
sumExp: selectorExp ':' (simpleSumOperation | customSumOperation);

simpleSumOperation: (OR | AND | PLUS | MUL) exp;

customSumOperation:  ID ':' type '->' exp ',' exp;

/* constructor application expression: allows for applying expression to constructors */
constructorAppExp: elementID ('(' (exp ',')* exp ')')?;

/* match expression: allows for pattern matching on (pre) defined datatypes */
matchExp: 'match' exps 'with' ( '|' patterns '=>' exp )* '|' '_' '=>' exp; //TODO[TYPECHECKING]: test for exhaustiveness?
// tupleexp: exp | '(' (exp ',')* exp ')';
patterns: pattern | '(' (pattern ',')+ pattern ')';
pattern:  wildCardPattern | baseConst | constructorPattern;
constructorPattern: elementID ('(' ( pattern ',')* pattern')')?;
wildCardPattern: UNDERSCORE | var;
exps: exp | '(' (exp ',')+ exp ')';

appExp: opID ('{' opPars '}')? '(' opArgs ')';
opPars: (opPar ',')* opPar;
opPar: exp; //TODO[TYPECHECKING]: check that arguments are expressions of the right type and const
opArgs: (opArg ',')* opArg;
opArg: exp; //TODO[TYPECHECKING]: check that arguments are expressions of the right type

/* integer operators */
compOP: '>' | GE | '<' | LE | EQ | NEQ;

conc: predApp; //TODO should it really be the same? Do we want to allow for terms in the arguments here? (For this we would need to find out whether it makes a difference for z3!

/*** Global Macros ***/
/* macros that can be used in all rules and that in contrast to local macros cannot refer to freeVars or Parameters */

globalMacroDefinition: 'g-macro' macroDef ';'; //TODO[TYPECHECKING]: check for closedness

//IDEA: all normal variables need to be bound, no allquantified ones need to be declared and will be renamed if necessary in the rules (enforces that they are disjoint with the existing ones)

/*** Initialization ***/
initialization:
    'init' ruleBody ';';

/*** Queries **/
//TODO: to which extend should this be separated? Keeping a model and allow for new queries?
query: 'query'  queryID queryBody ';'; //TODO[TYPECHECKING]: check for closedness (no parameters, only variables)? Actually this could be enforced syntactically at the moment..

test: 'test' testID 'expect' testResult queryBody ';';

queryBody: selectorExp? ('[' freeVars ']')? prems;

testResult: 'SAT' | 'UNSAT';


/*** constants ***/

intConst : NUM;
boolConst: TRUE | FALSE;
baseConst: intConst | boolConst;
// arrayConst: '[' constant ']';  /* a constant array with all elements being the defined constant */
constant: baseConst;

/*** different ID kinds ***/
// done for modularization, requires that they are matched to tokens! (otherwise their texts can't be accessed in the rules above
typeID : PRED_ID;
macroID: MACRO_ID;
elementID : ELEMENT_ID;
opID : ID;
predID: PRED_ID;
selectFunID: ID;
ruleID: ID;
paramID: PAR_ID;
clauseID: ID;
queryID: ID;
testID: ID;
constID: CONST_ID;
parVar: PAR_ID;
freeVar: VAR_ID;
var: ID;

/*** comments ***/
/* copied from java grammar, not nested */
COMMENT
    :   '/*' .*? '*/'    -> channel(HIDDEN) // match anything between /* and */
    ;

LINE_COMMENT
    : '//' ~[\r\n]* '\r'? '\n' -> channel(HIDDEN)
    ;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines

/*** Lexing symbols ***/
// Problem: need to be disjoint
EQTYPE: 'eqtype';
DATATYPE: 'datatype';
UNIT: 'unit';

PLUS: '+';
MINUS: '-';
MUL: '*';
DIV: '/';
MOD: 'mod';
BVNEG: 'bvneg';
BVAND: 'bvand';
BVXOR: 'bvxor';
BVOR: 'bvor';
//GT: '>';
//LT: '<';
GE: '>=';
LE: '<=';
EQ: '=';
NEQ: '!=';
AND: '&&';
OR: '||';
NEG: '~';

BOOL : 'bool';
INT : 'int';
TRUE : 'true';
FALSE : 'false';
UNDERSCORE: '_';
ID : [a-z] ([A-Z] | [a-z])* [0-9]* ;
CONST_ID: [A-Z]+ [0-9]*;
PRED_ID : [A-Z] ([A-Z] | [a-z])* [0-9]*;
NUM : ('~')?[0-9]+;
VAR_ID: '?'[a-z]+ [0-9]*; // free variables start with '?'
PAR_ID: '!' [a-z]+ [0-9]*; // parameters start with '!'
MACRO_PAR_ID: '$' [a-z]+ [0-9]*; // macro arguments start with '$'
MACRO_ID: '#' [A-Z] ([A-Z] | [a-z])* [0-9]*; // macros are same as predicates, but prefixed with with '#'
GLOBALMACRO_ID: '$' [A-Z] ([A-Z] | [a-z])* [0-9]*;
ELEMENT_ID: '@'[A-Z][A-Za-z]* [0-9]*;

