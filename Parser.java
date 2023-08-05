import java.util.*;
import java.util.regex.*;


public class Parser {
    static final Pattern NUMPAT = Pattern.compile("-?[1-9][0-9]*|0"); 
    static final Pattern OPENPAREN = Pattern.compile("\\(");
    static final Pattern CLOSEPAREN = Pattern.compile("\\)");
    static final Pattern OPENBRACE = Pattern.compile("\\{");
    static final Pattern CLOSEBRACE = Pattern.compile("\\}");
    static final Pattern LOOP_PAT = Pattern.compile("loop");
    static final Pattern IF_PAT = Pattern.compile("if");
    static final Pattern WHILE_PAT = Pattern.compile("while");
    static final Pattern ACT_PAT = Pattern.compile("move|turnL|turnR|takeFuel|wait|shieldOn|shieldOff|turnAround");
    static final Pattern SENS_PAT = Pattern.compile("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist");
    static final Pattern OP_PAT = Pattern.compile("add|sub|mul|div");
    static final Pattern COND_PAT = Pattern.compile("and|or|not");
    static final Pattern VAR_PAT = Pattern.compile("\\$[A-Za-z][A-Za-z0-9]*");
    
    //----------------------------------------------------------------
    /**
     * The top of the parser, which is handed a scanner containing
     * the text of the program to parse.
     * Returns the parse tree.
     */
    ProgramNode parse(Scanner s) {
        // Set the delimiter for the scanner.
        s.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");
        // Call the parseProg method for the first grammar rule (PROG) and return the node
        ProgramNode p = parseProgNode(s);
        return p;
    }

    /** Parses program nodes
       [ STMT ]*       
       */
    ProgramNode parseProgNode(Scanner s){
        List<ProgramNode> statementList = new ArrayList<ProgramNode>();
        while (s.hasNext()){
            ProgramNode statement = parseStatement(s);
            statementList.add(statement);
        }
        return new ProgNode(statementList);
    }
    
    /** Parses statement nodes. */
    ProgramNode parseStatement(Scanner s){
        if (!s.hasNext()){
            fail("No Statement Found", s);
        }
        if (s.hasNext(LOOP_PAT)){return parseLoop(s);}
        
        if (s.hasNext(ACT_PAT)){
            ProgramNode p = parseAction(s);
            require(";", "Expecting semi-colon (;)", s);
            return p;
        }
        if (s.hasNext(WHILE_PAT)){
            ProgramNode p = parseWhile(s);
            return p;
        }
        if (s.hasNext(IF_PAT)){
            ProgramNode p = parseIf(s);
            return p;
        }
 
        if (s.hasNext(VAR_PAT)){
            ProgramNode p = parseAssgn(s);
            require(";", "Expecting ';'", s);
            return p;
        }
        fail("Statement Invalid", s);
        return null;
    }
        
    /**
     * Parses ASSGN Nodes 
     * Possible ASSGN node is 
     * VAR "=" EXPR
     */
    ProgramNode parseAssgn(Scanner s){
        String variable = "";
        
        if (s.hasNext(VAR_PAT)){
            variable = s.next();
        }
        require("=", "Expecting '='", s);
        IntNode num = parseExpr(s);
        return new AssgnNode(variable, num);
    }
    
    
    /**
     * Parses VAR Nodes
     * VAR Node can be "\\$[A-Za-z][A-Za-z0-9]*"       
     */
    IntNode parseVar(Scanner s){
        String var = s.next();
        return new VarNode(var);
    }
    
    /** Parses block nodes
       Possible block nodes are "{" STMT+ "}"
       */
    ProgramNode parseBlock(Scanner s){
        List<ProgramNode> statementList = new ArrayList<ProgramNode>();
        if (!checkFor(OPENBRACE, s)){fail("Expecting open brace '{'", s);}
        do{
            if (s.hasNext(CLOSEBRACE)){
                fail("Expecting statement", s);
            }
            statementList.add(parseStatement(s));
        }
        while (!s.hasNext(CLOSEBRACE));
        if (!checkFor(CLOSEBRACE, s)){fail("Expecting close brace '}'", s);}
        if (!statementList.isEmpty()){return new BlockNode(statementList);}
        fail("Expecting at least one statement", s);
        return null;
    }
    
    /** Parses action keywords
       Possible actions are 
       "move" [ "(" EXPR ")" ] | "turnL" | "turnR" | "turnAround" | 
          "shieldOn" | "shieldOff" | "takeFuel" | "wait" [ "(" EXPR ")" ]*/
    ProgramNode parseAction(Scanner s){
        if (s.hasNext("move")){return parseMove(s);}
        if (s.hasNext("turnL")){return parseTurnL(s);}
        if (s.hasNext("turnR")){return parseTurnR(s);}
        if (s.hasNext("takeFuel")){return parseTakeFuel(s);}
        if (s.hasNext("wait")){return parseWait(s);}
        if (s.hasNext("turnAround")){return parseTurnAround(s);}
        if (s.hasNext("shieldOn")){return parseShieldOn(s);}
        if (s.hasNext("shieldOff")){return parseShieldOff(s);}
        fail("Invalid Action", s);
        return null;
    }
    
    /** Parses loop nodes
       Loop structure is "loop" BLOCK */
    ProgramNode parseLoop(Scanner s){
        require("loop", "Expecting 'loop'", s);
        return new LoopNode(parseBlock(s));
    }
    
    /** Parses move nodes*/
    ProgramNode parseMove(Scanner s){
        require("move", "Expecting 'move'", s); 
        if (s.hasNext(OPENPAREN)){
            s.next();
            ProgramNode distance = new MoveVarNode(parseExpr(s));
            require(CLOSEPAREN, "Expecting ')'", s);
            return distance;
        }       
        return new MoveNode();
    }
    
    /** Parses turn left command */ 
    ProgramNode parseTurnL(Scanner s){
        require("turnL", "Expecting 'turnL'", s);
        return new TurnLNode();
    }
    
    /** Parses turn right command */ 
    ProgramNode parseTurnR(Scanner s){
        require("turnR", "Expecting 'turnR'", s);
        return new TurnRNode();
    }
    
    /** Parses takeFuel command */ 
    ProgramNode parseTakeFuel(Scanner s){
        require("takeFuel", "Expecting 'takeFuel'", s);
        return new TakeFuelNode();
    }
    
    /** Parses wait command*/
    ProgramNode parseWait(Scanner s){
        require("wait", "Expecting 'wait'", s);
        if (s.hasNext(OPENPAREN)){
            s.next();
            ProgramNode distance = new WaitVarNode(parseExpr(s));
            require(CLOSEPAREN, "Expecting ')'", s);
            return distance;
        }    
        return new WaitNode();
    }
    
    /** Parses sheildOn command*/
    ProgramNode parseShieldOn(Scanner s){
        require("shieldOn", "Expecting 'sheildOn'", s);
        return new ShieldOnNode();
    }
    
    /** Parses shieldOff command */
    ProgramNode parseShieldOff(Scanner s){
        require("shieldOff", "Expecting 'sheildOff'", s);
        return new ShieldOffNode();
    }
    
    /** parses turnAround command */
    ProgramNode parseTurnAround(Scanner s){
        require("turnAround", "Expecting 'turnAround'", s);
        return new TurnAroundNode();
    }
    
    /** Parses if statement 
        "if" "(" COND ")" BLOCK [ "elif"  "(" COND ")"  BLOCK ]* [ "else" BLOCK ]       
       */
    ProgramNode parseIf(Scanner s){
       ProgramNode blockT = null;
       ProgramNode blockF = null;
       
       List<Map<BooleanNode, ProgramNode>> elseIfMapList = new ArrayList<Map<BooleanNode, ProgramNode>>();
       
       require("if", "Expecting 'if'", s);
       
       require(OPENPAREN, "Expecting '('", s);
       
       BooleanNode cond = parseCond(s);
       
       require(CLOSEPAREN, "Expecting ')'", s);
       
       blockT = parseBlock(s);
      
       while (s.hasNext("elif")){
           s.next();
           require(OPENPAREN, "Expecting '('", s);
           BooleanNode elseIfCond = parseCond(s);
           require(CLOSEPAREN, "Expecting ')'", s);
           ProgramNode elseIfBlock = parseBlock(s);
           Map<BooleanNode, ProgramNode> elseIfMap = new HashMap<BooleanNode, ProgramNode>();
           elseIfMap.put(elseIfCond, elseIfBlock);
           elseIfMapList.add(elseIfMap);
       }
       
       // take optional else clauses here
       if (s.hasNext("else")){ 
           s.next();
           blockF = parseBlock(s);
       }
       if (blockF == null && !elseIfMapList.isEmpty()){
           return new IfElseIfNode(cond, blockT, elseIfMapList);
       }
       if (blockF != null && !elseIfMapList.isEmpty()){
           return new IfElseIfElseNode(cond, blockT, blockF, elseIfMapList);
       }
       return new IfNode(cond, blockT, blockF);
    }
    
    /** Parses while command 
     * "while" "(" COND ")" BLOCK
     */
    ProgramNode parseWhile(Scanner s){
        require("while", "Expecting 'while'", s);
        require(OPENPAREN, "Expecting '('", s);
        BooleanNode cond = parseCond(s);
        require(CLOSEPAREN, "Expecting ')'", s);
        ProgramNode block = parseBlock(s);
        
        return new WhileNode(cond, block);
    }
    
    /** Parses COND command 
     * RELOP "(" EXPR "," EXPR ")"  | and ( COND, COND ) | or ( COND, COND )  | not ( COND )  
     */
    BooleanNode parseCond(Scanner s){
        if (s.hasNext("lt")){return parseLessThan(s);}
        if (s.hasNext("gt")){return parseGreaterThan(s);}
        if (s.hasNext("eq")){return parseEqual(s);}
        if (s.hasNext(COND_PAT)){return parseAndOrNot(s);}
        
        fail("Unknown conditon", s);
        return null;
    }

    /** Parses SENS command 
     * "fuelLeft" | "oppLR" | "oppFB" | "numBarrels" |
          "barrelLR" [ "(" EXPR ")" ] | "barrelFB" [ "(" EXPR ")" ] | "wallDist"
     */
    IntNode parseSens(Scanner s){
        if (s.hasNext("fuelLeft")){return parseFuelLeft(s);}
        if (s.hasNext("oppLR")){return parseOppLr(s);}
        if (s.hasNext("oppFB")){return parseOppFb(s);}
        if (s.hasNext("numBarrels")){return parseNumBarrels(s);}
        if (s.hasNext("barrelLR")){return parseBarrelLr(s);}
        if (s.hasNext("barrelFB")){return parseBarrelFb(s);}
        if (s.hasNext("wallDist")){return parseWallDist(s);}
        
        fail("Unknown instruction", s);
        return null;
    }
    
    /** Parses less than command */
    BooleanNode parseLessThan(Scanner s){
        require("lt", "Expecting 'lt'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode action = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode num = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);        
        
        return new LessNode(action, num);
    }
    
    /** Parses greater than command */
    BooleanNode parseGreaterThan(Scanner s){
        require("gt", "Expecting 'gt'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode action = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode num = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);
        
        return new GreaterNode(action, num);
    }
    
    /** Parses equal command */
    BooleanNode parseEqual(Scanner s){
        require("eq", "Expecting 'eq'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode action = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode num = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);
    
        return new EqualNode(action, num);
    }
    
    /** Parses Number */
    IntNode parseNum(Scanner s){
        String number = "";
        if (s.hasNext(NUMPAT)){
            while (s.hasNext(NUMPAT)){
                number += s.next();
        }
        int num = Integer.parseInt(number);
        return new NumNode(num);
        }
        fail("Expecting a number", s); 
        
        return null;
    }
    
    /** Parses fuelLeft */
    IntNode parseFuelLeft(Scanner s){
        require("fuelLeft", "Expecting 'fuelLeft'", s);
        return new FuelLeftNode();
    }
    
    /** Parses oppLR */
    IntNode parseOppLr(Scanner s){
        require("oppLR", "Expecting 'oppLR'", s);
        return new OppLrNode();
    }
    
    /** Parses oppFB */
    IntNode parseOppFb(Scanner s){
        require("oppFB", "Expecting 'oppFB'", s);
        return new OppFbNode();
    }
    
    /** Parses nyumBarrels */
    IntNode parseNumBarrels(Scanner s){
        require("numBarrels", "Expecting 'numBarrels'", s);
        return new NumBarrelsNode();
    }
    
    /** Parses BarrelLr */
    IntNode parseBarrelLr(Scanner s){
        require("barrelLR", "Expecting 'barrelLR'", s);
        if (s.hasNext(OPENPAREN)){
            s.next();
            IntNode num = parseExpr(s);
            require(CLOSEPAREN, "Expecting ')'", s);
            return new BarrelLrNode(num);
        }
        return new BarrelLrNode(null);
    }
    
    /** Parses barrelFb */
    IntNode parseBarrelFb(Scanner s){
        require("barrelFB", "Expecting 'barrelFB'", s);
        if (s.hasNext(OPENPAREN)){
            s.next();
            IntNode num = parseExpr(s);
            require(CLOSEPAREN, "Expecting ')'", s);
            return new BarrelFbNode(num);
        }
        return new BarrelFbNode(null);
    }
    
    /** Parses wallDist */
    IntNode parseWallDist(Scanner s){
        require("wallDist", "Expecting 'wallDist'", s);
        return new WallDistNode();
    }
    
    /** Parses Operators command
       "add" | "sub" | "mul" | "div" 
       */
    IntNode parseOperators(Scanner s){
        ProgramNode op;
        if (s.hasNext("add")){return parseAdd(s);}
        if (s.hasNext("sub")){return parseSub(s);}
        if (s.hasNext("mul")){return parseMul(s);}
        if (s.hasNext("div")){return parseDiv(s);}
        fail("Invalid operator", s);
        return null;
    }
    
    /** Parses addition */
    IntNode parseAdd(Scanner s){
        require("add", "Expecting 'add'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode expr = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode expr2 = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);
        return new AddNode(expr, expr2);
    }
    
    /** Parses subtraction */
    IntNode parseSub(Scanner s){
        require("sub", "Expecting 'sub'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode expr = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode expr2 = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);
        return new SubNode(expr, expr2);
    }
    
    /** Parses multiplication */
    IntNode parseMul(Scanner s){
        require("mul", "Expecting 'mul'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode expr = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode expr2 = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);
        return new MulNode(expr, expr2);
    }
    
    /** Parses division */
    IntNode parseDiv(Scanner s){
        require("div", "Expecting 'sub'", s);
        require(OPENPAREN, "Expecting '('", s);
        IntNode expr = parseExpr(s);
        require(",", "Expecting ','", s);
        IntNode expr2 = parseExpr(s);
        require(CLOSEPAREN, "Expecting ')'", s);
        return new DivNode(expr, expr2);
    }
    
    /** Parses Expression
       NUM | SENS | VAR | OP "(" EXPR "," EXPR ")"  
       */
    IntNode parseExpr(Scanner s){
        if (s.hasNext(SENS_PAT)){return parseSens(s);}
        if (s.hasNext(NUMPAT)){return parseNum(s);}
        if (s.hasNext(OP_PAT)){return parseOperators(s);}
        if (s.hasNext(VAR_PAT)){return parseVar(s);}//AssgnNode p = parseAssgn(s); return p.getNum();}
        fail("Invalid expression", s);
        return null;
    }
    
    /** Parses and or not command */
    BooleanNode parseAndOrNot(Scanner s){
        if (s.hasNext("and")){return parseAnd(s);}
        if (s.hasNext("or")){return parseOr(s);}
        if (s.hasNext("not")){return parseNot(s);}
        fail("Invalid expression", s);
        return null;
    }

    /** Parses and command */
    BooleanNode parseAnd(Scanner s){
        require("and", "Expecting 'and'", s);
        require(OPENPAREN, "Expecting '('", s);
        BooleanNode cond1 = parseCond(s);
        require(",", "Expecting ','", s);
        BooleanNode cond2 = parseCond(s);
        require(CLOSEPAREN, "Expecting ')'", s);  
        return new AndNode(cond1, cond2);
    }
    
    /** Parses not command */
    BooleanNode parseNot(Scanner s){
        require("not", "Expecting 'not'", s);
        require(OPENPAREN, "Expecting '('", s);
        BooleanNode cond1 = parseCond(s);
        require(CLOSEPAREN, "Expecting ')'", s);        
        return new NotNode(cond1);
    }
    
    /** Parses or command */
    BooleanNode parseOr(Scanner s){
        require("or", "Expecting 'or'", s);
        require(OPENPAREN, "Expecting '('", s);
        BooleanNode cond1 = parseCond(s);
        require(",", "Expecting ','", s);
        BooleanNode cond2 = parseCond(s);
        require(CLOSEPAREN, "Expecting ')'", s);     
        return new OrNode(cond1, cond2);
    }
    
    // utility methods for the parser
    // - fail(..) reports a failure and throws exception
    // - require(..) consumes and returns the next token as long as it matches the pattern
    // - requireInt(..) consumes and returns the next token as an int as long as it matches the pattern
    // - checkFor(..) peeks at the next token and only consumes it if it matches the pattern

    /**
     * Report a failure in the parser.
     */
    static void fail(String message, Scanner s) {
        String msg = message + "\n   @ ...";
        for (int i = 0; i < 5 && s.hasNext(); i++) {
            msg += " " + s.next();
        }
        throw new ParserFailureException(msg + "...");
    }

    /**
     * Requires that the next token matches a pattern if it matches, it consumes
     * and returns the token, if not, it throws an exception with an error
     * message
     */
    static String require(String p, String message, Scanner s) {
        if (s.hasNext(p)) {return s.next();}
        fail(message, s);
        return null;
    }

    static String require(Pattern p, String message, Scanner s) {
        if (s.hasNext(p)) {return s.next();}
        fail(message, s);
        return null;
    }

    /**
     * Requires that the next token matches a pattern (which should only match a
     * number) if it matches, it consumes and returns the token as an integer
     * if not, it throws an exception with an error message
     */
    static int requireInt(String p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {return s.nextInt();}
        fail(message, s);
        return -1;
    }

    static int requireInt(Pattern p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {return s.nextInt();}
        fail(message, s);
        return -1;
    }

    /**
     * Checks whether the next token in the scanner matches the specified
     * pattern, if so, consumes the token and return true. Otherwise returns
     * false without consuming anything.
     */
    static boolean checkFor(String p, Scanner s) {
        if (s.hasNext(p)) {s.next(); return true;}
        return false;
    }

    static boolean checkFor(Pattern p, Scanner s) {
        if (s.hasNext(p)) {s.next(); return true;} 
        return false;
    }

}

class BlockNode implements ProgramNode{
    final List<ProgramNode> statementList;
    
    public BlockNode(List<ProgramNode> statementList){this.statementList = statementList;}
    
    public void execute(Robot robot){
        for (ProgramNode p : statementList){
            p.execute(robot);
        }
    }
    
    public String toString(){
        String ans = "{";
        for (ProgramNode n : statementList){
            ans += n.toString();
        }
        return ans + "}";
    }
}

class StatementNode implements ProgramNode{
    final ProgramNode statement;
    
    public StatementNode(ProgramNode p){this.statement = p;}
   
    public void execute(Robot robot){
        statement.execute(robot);
    }
    
    public String toString(){
        return statement.toString();
    }
    
}

class ActionNode implements ProgramNode{
    final ProgramNode action;
    
    public ActionNode(ProgramNode action){this.action = action;}

    public void execute(Robot robot){
        action.execute(robot);
    }
    
    public String toString(){return action.toString() + ";";}
    
}

class ProgNode implements ProgramNode{
    final List<ProgramNode> programList;
    
    public ProgNode(List<ProgramNode> p){this.programList = p;}
    
    public void execute(Robot robot){for (ProgramNode p : programList){p.execute(robot);}}
    
    public String toString(){
        String ans = "";
        for (ProgramNode n : programList){
            ans += n.toString() + " ";
        }
        return ans;
    }
    
}

class LoopNode implements ProgramNode{
    final ProgramNode block;
    
    public LoopNode(ProgramNode block){this.block = block;}
    
    public void execute(Robot robot){
        while (true){
            block.execute(robot);
        }
    }
    
    public String toString() {return "loop " + this.block.toString();}

}

// Node classes for each of the action nodes (move, turnR, turnL, ...)
class MoveNode implements ProgramNode{
    final String action = "move;";
    
    public String toString(){return action;}
    
    public void execute(Robot robot) {robot.move();}
    
}

class TurnRNode implements ProgramNode{
    final String action = "turnR;";
    
    public String toString(){return action;}
    
    public void execute(Robot robot) {robot.turnRight();}
    
}

class TurnLNode implements ProgramNode{
    final String action = "turnL;";
    
    public String toString(){return action;}
    
    public void execute(Robot robot) {robot.turnLeft();}

}

class TakeFuelNode implements ProgramNode{
    final String action = "takeFuel;";
    
    public String toString(){return action;}
    
    public void execute(Robot robot) {robot.takeFuel();}
}

class WaitNode implements ProgramNode{
    final String action = "wait;";
    
    public String toString(){return action;}
    
    public void execute(Robot robot) {robot.idleWait();}
}

class ShieldOnNode implements ProgramNode{
    final String action = "shieldOn;";
    public String toString(){return action;}
    public void execute(Robot robot){robot.setShield(true);}
}

class ShieldOffNode implements ProgramNode{
    final String action = "shieldOff;";
    public String toString(){return action;}
    public void execute(Robot robot){robot.setShield(false);}
}

class TurnAroundNode implements ProgramNode{
    final String action = "turnAround;";
    public String toString(){return action;}
    public void execute(Robot robot){robot.turnAround();}
}

class NumNode implements IntNode{
    private int value;
    
    public NumNode(int value){
        this.value = value;
    }
    
    public int value(){
        return this.value();
    }
    
    public int evaluate(Robot robot){return this.value;}
    
    public String toString(){
        return String.valueOf(value);
    }
    
}

class IfNode implements ProgramNode{
    BooleanNode condition;
    ProgramNode block;
    ProgramNode block2;
    
    public IfNode(BooleanNode cond, ProgramNode block, ProgramNode block2){
        condition = cond; this.block = block; this.block2 = block2;
    }
    
    public void execute(Robot robot){
        if (condition.evaluate(robot)){
            block.execute(robot);
            
        }
        else{
            if (block2 != null){
                block2.execute(robot);
            }
        }
    }
    
    public String toString(){
        if (block2 == null){
            return "if (" + condition.toString() + ")" + block.toString();
        }
        else{
            return "if (" + condition.toString() + ")" + block.toString() + "else" + block2.toString();
        }
    }
    
}

class IfElseIfNode implements ProgramNode{
    BooleanNode cond;
    ProgramNode blockT;
    List<Map<BooleanNode, ProgramNode>> listOfMaps; // list of maps containing boolean node linked to program node
    
    public IfElseIfNode(BooleanNode cond, ProgramNode blockT, List<Map<BooleanNode, ProgramNode>> listOfMaps){
        this.cond = cond;
        this.blockT = blockT;
        this.listOfMaps = listOfMaps;
    }
    
    public void execute(Robot robot){
        if (cond.evaluate(robot)){ // if condition true
            blockT.execute(robot); // execute the true block (programNode) 
        }
        else{ // otherwise
            for (Map<BooleanNode, ProgramNode> mapping : listOfMaps){ // for each entry in map of boolean node -> program node
                for (BooleanNode condition : mapping.keySet()){
                    if (condition.evaluate(robot)){ // if condition is true
                        mapping.get(condition).execute(robot); // execute it
                        return; // returns out after the first else if statement is executed 
                    }
                }
            }
        }
    }
    
    public String toString(){
        String ans = "if (" + cond.toString() + ")" + blockT.toString();
        
        for (int i = 0; i < listOfMaps.size(); i++){
            Map<BooleanNode, ProgramNode> map = listOfMaps.get(i);
            Set<BooleanNode> set = map.keySet();
            
            for (BooleanNode condition : set){
                ProgramNode blockToCopy = map.get(condition);
                ans = ans + "elif(" + condition + ")" + blockToCopy;
            }
        }
        return ans;
    }
}

class IfElseIfElseNode implements ProgramNode{
    
    BooleanNode cond;
    ProgramNode blockT;
    ProgramNode blockF;
    
    List<Map<BooleanNode, ProgramNode>> listOfMaps; // list of maps of BooleanNode -> ProgramNode
    
    public IfElseIfElseNode(BooleanNode cond, ProgramNode blockT, ProgramNode blockF, List<Map<BooleanNode, ProgramNode>> elseIfMapList){
        this.cond = cond;
        this.blockT = blockT;
        this.blockF = blockF;
        this.listOfMaps = elseIfMapList;
    }
    
    public void execute(Robot robot){
        if (cond.evaluate(robot)){
            blockT.execute(robot); // if first if block is true, execute it
        }
        else{
            for (Map<BooleanNode, ProgramNode> mapping : listOfMaps){ // otherwise, for each map
                for (BooleanNode condition : mapping.keySet()){ // get the boolean Node
                    if (condition.evaluate(robot)){ // if true, execute it and return out
                        mapping.get(condition).execute(robot);
                        return; // return out after first is executed
                    }
                }
            }
            blockF.execute(robot); // if it gets through all without executing, execute the else statement
        }
    }
    
   
    public String toString(){
        String ans = "if (" + cond +")"+ blockT;
        for (int i = 0; i < listOfMaps.size(); i++){
            Map<BooleanNode, ProgramNode> map = listOfMaps.get(i);
            Set<BooleanNode> set = map.keySet();
            for (BooleanNode b : set){
                ProgramNode blockToCopy = map.get(b);
                ans = ans + " elif(" + b + ")" +  blockToCopy ;
            }
        }
        ans = ans + " else " + blockF;
        return ans;
    }
    
}

class WhileNode implements ProgramNode{
    BooleanNode condition;
    ProgramNode block;
    
    public WhileNode(BooleanNode condition, ProgramNode block){this.condition = condition; this.block = block;}
    public void execute(Robot robot){while (condition.evaluate(robot)){block.execute(robot);}}
    
    public String toString(){
        return "while (" + condition.toString() + ")" + block.toString();
    }
}

class GreaterNode implements BooleanNode{
    IntNode compare;
    IntNode value;
    
    public GreaterNode(IntNode compare, IntNode value){
        this.compare = compare;
        this.value = value;
    }
    
    public boolean evaluate(Robot robot){
        if (compare.evaluate(robot) > value.evaluate(robot)){
            return true;
        }
        else{
            return false;
        }
    }
    
    public String toString(){
        return "gt(" + String.valueOf(compare) + "," + String.valueOf(value) + ")";
    }

}

class LessNode implements BooleanNode{
    IntNode compare;
    IntNode value;
    
    public LessNode(IntNode compare, IntNode value){
        this.compare = compare;
        this.value = value;
    }
    
    public boolean evaluate(Robot robot){
        return (compare.evaluate(robot) < value.evaluate(robot));
    }
    
    public String toString(){
        return "lt(" + String.valueOf(compare) + "," + String.valueOf(value) + ")";
    }
    
}

class EqualNode implements BooleanNode{
    IntNode compare;
    IntNode value;
    
    public EqualNode(IntNode compare, IntNode value){
        this.compare = compare;
        this.value = value;
    }
    
    public boolean evaluate(Robot robot){
        if (compare.evaluate(robot) == value.evaluate(robot)){
            return true;
        }
        else{
            return false;
        }
    }
    
    public String toString(){
        return "eq(" + String.valueOf(compare) + "," + String.valueOf(value) + ")";
    }
    
}

class FuelLeftNode implements IntNode{
    
    public int evaluate(Robot robot){return robot.getFuel();}
    
    public String toString(){
        return "fuelleft";
    }
    
}

class OppLrNode implements IntNode{
    public int evaluate(Robot robot){return robot.getOpponentLR();}
    
    public String toString(){
        return "oppLr";
    }
}

class OppFbNode implements IntNode{
    public int evaluate(Robot robot){return robot.getOpponentFB();}
    
    public String toString(){
        return "oppFb";
    }
}

class NumBarrelsNode implements IntNode{
    public int evaluate(Robot robot){return robot.numBarrels();}
    
    public String toString(){
        return "numBarrels";
    }
}

class BarrelLrNode implements IntNode{
    IntNode num;
    
    public BarrelLrNode(IntNode num){
        this.num = num;
        
    }
    
    public int evaluate(Robot robot){
        if (this.num == null){
            return robot.getClosestBarrelLR();
        }
        else{
            return robot.getBarrelLR(num.evaluate(robot));
        }
    }
    
    public String toString(){
        if (num != null){
            return "barrelLR(" + num.toString() +")";
        }
        else{
            return "barrelLR";
        }
    }
}

class BarrelFbNode implements IntNode{
    IntNode num;
    
    public BarrelFbNode(IntNode num){
        this.num = num;
    }
    
    public int evaluate(Robot robot){if (this.num == null){
            return robot.getClosestBarrelFB();
        }
        else{
            return robot.getBarrelFB(num.evaluate(robot));
        }
    }
    
    public String toString(){
        if (num != null){
            return "barrelFB(" + num.toString() + ")";
        }
        else{
            return "barrelFB";
        }
    }
}

class WallDistNode implements IntNode{
    public int evaluate(Robot robot){return robot.getDistanceToWall();}
    
    public String toString(){
        return "wallDist";
    }
}

class ExprNode implements IntNode{
    int value;
    
    public ExprNode(int value){
        this.value = value;
    }
    
    public int evaluate(Robot robot){
        return value;
    }
    
    public String toString(){
        return String.valueOf(value);
    }
}


class AddNode implements IntNode{
    final String operator = "+";
    
    IntNode expr1;
    
    IntNode expr2;
    
    public AddNode(IntNode expr1, IntNode expr2){
        this.expr1 = expr1;
        this.expr2 = expr2;
    }
    
    
    public int evaluate(Robot robot){
        return expr1.evaluate(robot) + expr2.evaluate(robot);
    }
    
    public String toString(){
        return "add(" + expr1.toString() + "," + expr2.toString() + ")";
    }
}

class SubNode implements IntNode{
    final String operator = "-";
    IntNode expr1;
    
    IntNode expr2;
    
    public SubNode(IntNode expr1, IntNode expr2){
        this.expr1 = expr1;
        this.expr2 = expr2;
    }
    
    
    public int evaluate(Robot robot){
        return expr1.evaluate(robot) - expr2.evaluate(robot);
    }
    
    public String toString(){
        return "sub(" + expr1.toString() + "," + expr2.toString() + ")";
    }
}

class MulNode implements IntNode{
    final String operator = "*";
    
    IntNode expr1;
    
    IntNode expr2;
    
    public MulNode(IntNode expr1, IntNode expr2){
        this.expr1 = expr1;
        this.expr2 = expr2;
    }
    
    
    public int evaluate(Robot robot){
        return expr1.evaluate(robot) * expr2.evaluate(robot);
    }
    
    public String toString(){
        return "mul(" + expr1.toString() + "," + expr2.toString() + ")";
    }
    
}

class DivNode implements IntNode{
    final String operator = "/";
    
    IntNode expr1;
    
    IntNode expr2;
    
    public DivNode(IntNode expr1, IntNode expr2){
        this.expr1 = expr1;
        this.expr2 = expr2;
    }
    
    
    public int evaluate(Robot robot){
        return expr1.evaluate(robot) / expr2.evaluate(robot);
    }
    
    public String toString(){
        return "div(" + expr1.toString() + "," + expr2.toString() + ")";
    }
    
}

class MoveVarNode implements ProgramNode{
     IntNode expr;
     
     public MoveVarNode(IntNode expr){
         this.expr = expr;
     }
     
     public void execute(Robot robot){
         
         int numTimes = expr.evaluate(robot);
         for (int i = 0; i < numTimes; i++){
             robot.move();
         }
         
     }
     
     public String toString(){
         return "move(" + expr.toString() + ");";
     }
}

class WaitVarNode implements ProgramNode{
    IntNode expr;
    
    public WaitVarNode(IntNode expr){
        this.expr = expr;
    }
    
    public void execute(Robot robot){
        
        int numTimes = expr.evaluate(robot);
        for (int i = 0; i < numTimes; i++){
            robot.idleWait();
        }

        
    }
    
    public String toString(){
         return "wait(" + expr.toString() + ");";
     }
}

class AndNode implements BooleanNode{
    BooleanNode cond1;
    BooleanNode cond2;
    
    public AndNode(BooleanNode cond1, BooleanNode cond2){
        this.cond1 = cond1;
        this.cond2 = cond2;
    }
    
    public boolean evaluate(Robot robot){
        return (cond1.evaluate(robot) && cond2.evaluate(robot));
    }
    
    public String toString(){
        return "and(" + cond1.toString() + "," + cond2.toString();
    }
    
}

class NotNode implements BooleanNode{
    BooleanNode cond1;
    
    public NotNode(BooleanNode cond1){
        this.cond1 = cond1;
    }
    
    public boolean evaluate(Robot robot){
        return !cond1.evaluate(robot);
    }
    
    public String toString(){
        return "not(" + cond1.toString() + ")";
    }
    
}

class OrNode implements BooleanNode{
    BooleanNode cond1;
    BooleanNode cond2;
    
    public OrNode(BooleanNode cond1, BooleanNode cond2){
        this.cond1 = cond1;
        this.cond2 = cond2;
    }
    
    public boolean evaluate(Robot robot){
       return (cond1.evaluate(robot) || cond2.evaluate(robot));
    }
    
    public String toString(){
        return "or(" + cond1.toString() + "," + cond2.toString();
    }
    
}

class AssgnNode implements ProgramNode{
    String variable;
    IntNode num;
    
    public AssgnNode(String variable, IntNode num){
        this.variable = variable;
        this.num = num;
        
    }
    public void execute(Robot robot){
        robot.variables.put(this.variable, num.evaluate(robot));
        
    }
    
    public String toString(){
        return variable + "=" + num.toString() + ";";
    }
}

class VarNode implements IntNode{
    String name;
    public VarNode(String name){
        this.name = name;
    }
    
    public int evaluate(Robot robot){
        if (robot.variables.containsKey(this.name)){
            return robot.variables.get(this.name);
        }
        else{
            robot.variables.put(this.name, 0);
            return 0;
        }
    }
    
    public String toString(){
        return name;
    }
    
}