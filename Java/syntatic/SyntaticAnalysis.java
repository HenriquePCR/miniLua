package syntatic;

// IMPORTAR PACOTES PARA COMPILAR!!!

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import interpreter.command.AssignCommand;
import interpreter.command.BlocksCommand;
import interpreter.command.Command;
import interpreter.command.IfCommand;
import interpreter.command.PrintCommand;
import interpreter.command.RepeatCommand;
import interpreter.command.WhileCommand;
import interpreter.expr.BinaryExpr;
import interpreter.expr.BinaryOp;
import interpreter.expr.ConstExpr;
import interpreter.expr.Expr;
import interpreter.expr.SetExpr;
import interpreter.expr.UnaryExpr;
import interpreter.expr.UnaryOp;
import interpreter.expr.Variable;
import interpreter.value.BooleanValue;
import interpreter.value.NumberValue;
import interpreter.value.StringValue;
import interpreter.value.Value;
import lexical.Lexeme;
import lexical.LexicalAnalysis;
import lexical.TokenType;

public class SyntaticAnalysis {

    private LexicalAnalysis lex;
    private Lexeme current;

    public SyntaticAnalysis(LexicalAnalysis lex) {
        this.lex = lex;
        this.current = lex.nextToken();
    }

    public Command start() {
        BlocksCommand cmds = procCode();
        eat(TokenType.END_OF_FILE);

        return cmds;//
    }

    private void advance() {
        System.out.println("Advanced (\"" + current.token + "\", " + current.type +
                ")");
        current = lex.nextToken();
    }

    private void eat(TokenType type) {
        System.out.println("Expected (..., " + type + "), found (\"" + current.token
                + "\", " + current.type + ")");
        if (type == current.type) {
            current = lex.nextToken();
        } else {
            showError();
        }
    }

    private void showError() {
        System.out.printf("%02d: ", lex.getLine());

        switch (current.type) {
            case INVALID_TOKEN:
                System.out.printf("Lexema inválido [%s]\n", current.token);
                break;
            case UNEXPECTED_EOF:
            case END_OF_FILE:
                System.out.printf("Fim de arquivo inesperado\n");
                break;
            default:
                System.out.printf("Lexema não esperado [%s]\n", current.token);
                break;
        }

        System.exit(1);
    }

    // <code> ::= { <cmd> }
    private BlocksCommand procCode() {
        int line = lex.getLine();
        List<Command> cmds = new ArrayList<Command>();
        while (current.type == TokenType.IF ||
                current.type == TokenType.WHILE ||
                current.type == TokenType.REPEAT ||
                current.type == TokenType.FOR ||
                current.type == TokenType.PRINT ||
                current.type == TokenType.ID) {
            Command cmd = procCmd();
            cmds.add(cmd);
        }

        BlocksCommand bc = new BlocksCommand(line, cmds);
        return bc;
    }

    // <cmd> ::= (<if> | <while> | <repeat> | <for> | <print> | <assign>) [';']
    private Command procCmd() {
        Command cmd = null;
        if (current.type == TokenType.IF) {
            cmd = procIf();
        } else if (current.type == TokenType.WHILE) {
            cmd = procWhile();
        } else if (current.type == TokenType.REPEAT) {
            cmd = procRepeat();
        } else if (current.type == TokenType.FOR) {
            procFor();
        } else if (current.type == TokenType.PRINT) {
            cmd = procPrint();
        } else if (current.type == TokenType.ID) {
            cmd = procAssign();
        } else {
            showError();
        }
        if (current.type == TokenType.SEMI_COLON) {
            advance();
        }
        return cmd;
    }

    // <if> ::= if <expr> then <code> { elseif <expr> then <code> } [ else <code> ]
    // end
    private IfCommand procIf() {

        Expr expr = null;
        Command thenCmds = null;

        eat(TokenType.IF);

        expr = procExpr();

        eat(TokenType.THEN);

        thenCmds = procCode();

        // Won't run unless previous ifs fail
        while (current.type == TokenType.ELSEIF) {
            advance();

            expr = procExpr();

            eat(TokenType.THEN);

            thenCmds = procCode();
        }

        int line = lex.getLine();

        IfCommand ic = new IfCommand(line, expr, thenCmds);

        if (current.type == TokenType.ELSE) {
            ic.setElseCommands(procCode());
        }

        eat(TokenType.END);

        return ic;
    }

    // <while> ::= while <expr> do <code> end
    private WhileCommand procWhile() {
        eat(TokenType.WHILE);
        int line = lex.getLine();
        Expr expr = procExpr();
        eat(TokenType.DO);
        Command cmds = procCode();
        eat(TokenType.END);

        WhileCommand wc = new WhileCommand(line, expr, cmds);
        return wc;
    }

    // <repeat> ::= repeat <code> until <expr>
    private RepeatCommand procRepeat() {

        int line = lex.getLine();
        Expr expr = null;

        eat(TokenType.REPEAT);
        Command cmds = procCode();

        eat(TokenType.UNTIL);
        expr = procExpr();

        RepeatCommand rc = new RepeatCommand(line, expr, cmds);
        return rc;
    }

    // for <name> (('=' <expr> ',' <expr> [',' <expr>]) | ([',' <name>] in <expr>))
    // do <code> end
    private void procFor() {
        eat(TokenType.FOR);
        procName();

        if (current.type == TokenType.ASSIGN) {
            advance();
            procExpr();
            eat(TokenType.COLON);
            procExpr();
            if (current.type == TokenType.COLON) {
                advance();
                procExpr();
            }
        } else {
            if (current.type == TokenType.COLON) {
                advance();
                procName();
            }
            eat(TokenType.IN);
            procExpr();
        }

        eat(TokenType.DO);
        procCode();
        eat(TokenType.END);
    }

    // <print> ::= print '(' [ <expr> ] ')'
    private PrintCommand procPrint() {
        eat(TokenType.PRINT);
        eat(TokenType.OPEN_PAR);

        Expr expr = null;
        int line = lex.getLine();

        // CHECAR
        if (current.type == TokenType.OPEN_PAR ||
                current.type == TokenType.SUB ||
                current.type == TokenType.SIZE ||
                current.type == TokenType.NOT ||
                current.type == TokenType.NUMBER ||
                current.type == TokenType.STRING ||
                current.type == TokenType.FALSE ||
                current.type == TokenType.TRUE ||
                current.type == TokenType.NIL ||
                current.type == TokenType.READ ||
                current.type == TokenType.TONUMBER ||
                current.type == TokenType.TOSTRING ||
                current.type == TokenType.OPEN_CUR ||
                current.type == TokenType.ID) {
            expr = procExpr();
        }
        eat(TokenType.CLOSE_PAR);

        PrintCommand command = new PrintCommand(line, expr);
        return command;
    }

    // <assign> ::= <lvalue> { ',' <lvalue> } '=' <expr> { ',' <expr> }
    private AssignCommand procAssign() {
        Vector<SetExpr> lhs = new Vector<SetExpr>();
        Vector<Expr> rhs = new Vector<Expr>();

        lhs.add(procLValue());

        while (current.type == TokenType.COLON) {
            advance();
            lhs.add(procLValue());
        }

        eat(TokenType.ASSIGN);
        int line = lex.getLine();

        rhs.add(procExpr());
        while (current.type == TokenType.COLON) {
            advance();
            rhs.add(procExpr());
        }

        AssignCommand ac = new AssignCommand(line, lhs, rhs);
        return ac;
    }

    // <expr> ::= <rel> { (and | or) <rel> } => ARRUMAR
    private Expr procExpr() {
        Expr expr = procRel();
        if (current.type == TokenType.AND) {
            BinaryExpr be = null;
            while (current.type == TokenType.AND) {
                advance();
                BinaryOp op = BinaryOp.AndOp;
                Expr expr2 = procTerm();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                advance();
            }
            return be;
        } else if (current.type == TokenType.OR) {
            BinaryExpr be = null;
            while (current.type == TokenType.OR) {
                advance();
                BinaryOp op = BinaryOp.OrOp;
                Expr expr2 = procRel();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                advance();
            }
            return be;
        } else {
            return expr;
        }
    }

    // <rel> ::= <concat> [ ('<' | '>' | '<=' | '>=' | '~=' | '==') <concat> ]
    private Expr procRel() {
        Expr expr = procConcat();

        if (current.type == TokenType.LOWER_THAN) {
            advance();
            BinaryOp op = BinaryOp.LowerThanOp;
            Expr expr2 = procConcat();
            BinaryExpr be = new BinaryExpr(lex.getLine(), expr, op, expr2);
            return be;
        } else if (current.type == TokenType.GREATER_THAN) {
            advance();
            BinaryOp op = BinaryOp.GreaterThanOp;
            Expr expr2 = procConcat();
            BinaryExpr be = new BinaryExpr(lex.getLine(), expr, op, expr2);
            return be;
        } else if (current.type == TokenType.LOWER_EQUAL) {
            advance();
            BinaryOp op = BinaryOp.LowerEqualOp;
            Expr expr2 = procConcat();
            BinaryExpr be = new BinaryExpr(lex.getLine(), expr, op, expr2);
            return be;
        } else if (current.type == TokenType.GREATER_EQUAL) {
            advance();
            BinaryOp op = BinaryOp.GreaterEqualOp;
            Expr expr2 = procConcat();
            BinaryExpr be = new BinaryExpr(lex.getLine(), expr, op, expr2);
            return be;
        } else if (current.type == TokenType.NOT_EQUAL) {
            advance();
            BinaryOp op = BinaryOp.NotEqualOp;
            Expr expr2 = procConcat();
            BinaryExpr be = new BinaryExpr(lex.getLine(), expr, op, expr2);
            return be;
        } else if (current.type == TokenType.EQUAL) {
            advance();
            BinaryOp op = BinaryOp.EqualOp;
            Expr expr2 = procConcat();
            BinaryExpr be = new BinaryExpr(lex.getLine(), expr, op, expr2);
            return be;
        } else {
            return expr;
        }
    }

    // <concat> ::= <arith> { '..' <arith> } => ARRUMAR
    private Expr procConcat() {
        Expr expr = procArith();
        if (current.type == TokenType.CONCAT) {
            System.out.println("concat sintático");
            BinaryExpr be = null;
            while (current.type == TokenType.CONCAT) {
                advance();
                BinaryOp op = BinaryOp.ConcatOp;
                Expr expr2 = procArith();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                System.out.println("loop");

                // advance();
            }
            return be;
        } else {

            return expr;
        }

    }

    // <arith> ::= <term> { ('+' | '-') <term> } => ARRUMAR
    private Expr procArith() {
        Expr expr = procTerm();
        if (current.type == TokenType.ADD) {
            BinaryExpr be = null;
            while (current.type == TokenType.ADD) {
                advance();
                BinaryOp op = BinaryOp.AddOp;
                Expr expr2 = procTerm();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                // advance();
            }
            return be;
        } else if (current.type == TokenType.SUB) {
            BinaryExpr be = null;
            while (current.type == TokenType.SUB) {
                advance();
                BinaryOp op = BinaryOp.SubOp;
                Expr expr2 = procTerm();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                // advance();
            }
            return be;
        } else {
            return expr;
        }
    }

    // <term> ::= <factor> { ('*' | '/' | '%') <factor> } => ARRUMAR
    private Expr procTerm() {
        Expr expr = procFactor();
        if (current.type == TokenType.MUL) {
            BinaryExpr be = null;
            while (current.type == TokenType.MUL) {
                advance();
                BinaryOp op = BinaryOp.MulOp;
                Expr expr2 = procFactor();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                advance();
            }
            return be;
        } else if (current.type == TokenType.DIV) {
            BinaryExpr be = null;
            while (current.type == TokenType.DIV) {
                advance();
                BinaryOp op = BinaryOp.DivOp;
                Expr expr2 = procFactor();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                advance();
            }
            return be;
        } else if (current.type == TokenType.MOD) {
            BinaryExpr be = null;
            while (current.type == TokenType.MOD) {
                advance();
                BinaryOp op = BinaryOp.ModOp;
                Expr expr2 = procFactor();
                be = new BinaryExpr(lex.getLine(), expr, op, expr2);
                advance();
            }
            return be;
        } else {
            return expr;
        }
    }

    // <factor> ::= '(' <expr> ')' | [ '-' | '#' | not ] <rvalue>
    private Expr procFactor() {
        Expr expr = null;
        if (current.type == TokenType.OPEN_PAR) {
            advance();
            procExpr();
            eat(TokenType.CLOSE_PAR);
        } else {
            UnaryOp op = null;
            if (current.type == TokenType.NOT) {
                advance();
                op = UnaryOp.Not;
            } else if (current.type == TokenType.SUB) {
                advance();
                op = UnaryOp.Neg;
            } else if (current.type == TokenType.SIZE) {
                advance();
                op = UnaryOp.Size;

            }

            int line = lex.getLine();
            expr = procRValue();

            if (op != null) {
                expr = new UnaryExpr(line, expr, op);
            }
        }
        return expr;
    }

    // <lvalue> ::= <name> { '.' <name> | '[' <expr> ']' }
    private SetExpr procLValue() {
        Variable var = procName();
        while (current.type == TokenType.DOT || current.type == TokenType.OPEN_BRA) {
            if (current.type == TokenType.DOT) {
                advance();
                procName();
            } else {
                advance();
                procExpr();
                eat(TokenType.CLOSE_BRA);
            }
        }
        return var;
    }

    // <rvalue> ::= <const> | <function> | <table> | <lvalue>
    private Expr procRValue() {
        Expr expr = null;
        if (current.type == TokenType.READ || current.type == TokenType.TONUMBER
                || current.type == TokenType.TOSTRING) {
            procFunction();
        } else if (current.type == TokenType.OPEN_CUR) {
            procTable();
        } else if (current.type == TokenType.NUMBER || current.type == TokenType.STRING
                || current.type == TokenType.FALSE || current.type == TokenType.TRUE || current.type == TokenType.NIL) {

            Value<?> v = procConst();
            int line = lex.getLine();
            ConstExpr ce = new ConstExpr(line, v);
            expr = ce;
        } else {
            expr = procLValue();
        }
        return expr;
    }

    // <const> ::= <number> | <string> | false | true | nil
    private Value<?> procConst() {
        Value<?> v = null;
        if (current.type == TokenType.NUMBER) {
            v = procNumber();
        } else if (current.type == TokenType.STRING) {
            v = procString();
        } else if (current.type == TokenType.FALSE) {
            advance();
            v = new BooleanValue(false);
        } else if (current.type == TokenType.TRUE) {
            advance();
            v = new BooleanValue(true);
        } else if (current.type == TokenType.NIL) {
            advance();
            v = null;
        } else {
            showError();
        }
        return v;
    }

    // <function> ::= (read | tonumber | tostring) '(' [ <expr> ] ')'
    private Value<?> procFunction() {
        Expr expr = null;
        UnaryOp op = null;
        int line = lex.getLine();

        if (current.type == TokenType.READ) {
            op = UnaryOp.Read;
        } else if (current.type == TokenType.TONUMBER) {
            op = UnaryOp.ToNumber;
        } else if (current.type == TokenType.TOSTRING) {
            op = UnaryOp.ToString;
        } else {
            showError();
        }

        eat(TokenType.OPEN_PAR);

        if (current.type == TokenType.OPEN_PAR ||
                current.type == TokenType.SUB ||
                current.type == TokenType.SIZE ||
                current.type == TokenType.NOT ||
                current.type == TokenType.NUMBER ||
                current.type == TokenType.STRING ||
                current.type == TokenType.FALSE ||
                current.type == TokenType.TRUE ||
                current.type == TokenType.NIL ||
                current.type == TokenType.READ ||
                current.type == TokenType.TONUMBER ||
                current.type == TokenType.TOSTRING ||
                current.type == TokenType.OPEN_CUR ||
                current.type == TokenType.ID) {
            expr = procExpr();
        }

        UnaryExpr ue = new UnaryExpr(line, expr, op);

        eat(TokenType.CLOSE_PAR);

        return ue.expr();
    }

    // <table> ::= '{' [ <elem> { ',' <elem> } ] '}'
    private void procTable() {
        eat(TokenType.OPEN_CUR);
        // CHECAR
        if (current.type == TokenType.OPEN_BRA) {
            procElem();
            while (current.type == TokenType.COLON) {
                advance();
                procElem();
            }
        }
        eat(TokenType.CLOSE_CUR);
    }

    // <elem> ::= [ '[' <expr> ']' '=' ] <expr>
    private void procElem() {
        if (current.type == TokenType.OPEN_BRA) {
            advance();
            procExpr();
            eat(TokenType.CLOSE_BRA);
            eat(TokenType.ASSIGN);
        }
        procExpr();
    }

    private Variable procName() {
        String name = current.token;
        int line = lex.getLine();
        eat(TokenType.ID);
        Variable var = new Variable(line, name);
        return var;
    }

    private NumberValue procNumber() {
        String tmp = current.token;
        eat(TokenType.NUMBER);

        Double d = Double.valueOf(tmp);
        NumberValue nv = new NumberValue(d);
        return nv;
    }

    private StringValue procString() {
        String tmp = current.token;
        eat(TokenType.STRING);

        StringValue sv = new StringValue(tmp);

        return sv;
    }

}
