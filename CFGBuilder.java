import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

class CFG {
    CFGNode entry, exit;
    List<CFGNode> nodes = new ArrayList<>();

    public void addNode(CFGNode node) {
        nodes.add(node);
    }

    public void addEdge(CFGNode from, CFGNode to) {
        from.succs.add(to);
        to.preds.add(from);
    }

    public void printCFG() {
        System.out.println("Control Flow Graph:");
        for (CFGNode node : nodes) {
            System.out.println("Node " + node + ":");

            System.out.print("  Successors: ");
            if (node.succs.isEmpty()) System.out.println("None");
            else node.succs.forEach(succ -> System.out.print(succ + " "));
            System.out.println();

            System.out.print("  Predecessors: ");
            if (node.preds.isEmpty()) System.out.println("None");
            else node.preds.forEach(pred -> System.out.print(pred + " "));
            System.out.println();

            System.out.println("  Statements: " + (node.stmts.isEmpty() ? "None" : String.join(", ", node.stmts)));
        }
    }

    public void exportToDot(String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("digraph CFG {\n  node [shape=box];\n");
            for (CFGNode node : nodes) {
                String label = node + "\\n" + String.join("\\n", node.stmts);
                writer.write("  " + node.id + " [label=\"" + label + "\"];\n");
                for (CFGNode succ : node.succs) {
                    writer.write("  " + node.id + " -> " + succ.id + ";\n");
                }
            }
            writer.write("}\n");
            System.out.println("DOT file written to: " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class CFGNode {
    static int nextId = 0;
    final int id;
    List<String> stmts = new ArrayList<>();
    List<CFGNode> succs = new ArrayList<>();
    List<CFGNode> preds = new ArrayList<>();

    CFGNode(String label) {
        this.id = nextId++;
        this.stmts.add(label);
    }

    String getLabel() {
        return stmts.isEmpty() ? "" : stmts.getFirst();
    }

    @Override
    public String toString() {
        return "Block" + id;
    }
}

public class CFGBuilder extends PlSqlParserBaseListener {
    protected CFG cfg;
    protected CFGNode current;
    private final Stack<CFGNode> afterStack = new Stack<>();
    private final Stack<CFGNode> thenStack = new Stack<>();
    private final Stack<CFGNode> elseStack = new Stack<>();
    private final Stack<CFGNode> loopExitStack = new Stack<>();

    public CFGNode getCurrent() {
        return current;
    }

    public CFG build(ParseTree tree) {
        cfg = new CFG();
        cfg.entry = new CFGNode("ENTRY");
        cfg.exit = new CFGNode("EXIT");
        cfg.addNode(cfg.entry);
        cfg.addNode(cfg.exit);
        current = cfg.entry;

        ParseTreeWalker.DEFAULT.walk(this, tree);

        if (!current.succs.contains(cfg.exit)) {
            current.succs.add(cfg.exit);
            cfg.exit.preds.add(current);
        }

        return cfg;
    }

    // ============ IF AND CASE ============
    @Override
    public void enterIf_statement(PlSqlParser.If_statementContext ctx) {
        CFGNode ifCondition = new CFGNode("IF_CONDITION: " + ctx.condition().getText());
        CFGNode afterIf = new CFGNode("AFTER_IF");
        cfg.addNode(ifCondition);
        cfg.addNode(afterIf);
        cfg.addEdge(current, ifCondition);

        CFGNode prevCond = ifCondition;

        // THEN block
        CFGNode thenBlock = new CFGNode("THEN_BLOCK");
        cfg.addNode(thenBlock);
        cfg.addEdge(ifCondition, thenBlock);
        current = thenBlock;

        boolean thenTerminates = false;
        for (var stmt : ctx.seq_of_statements().statement()) {
            for (CFGNode node : buildStatementNodes(stmt)) {
                cfg.addNode(node);
                cfg.addEdge(current, node);
                current = node;

                if (isTerminating(node)) thenTerminates = true;
            }
        }
        if (!thenTerminates) {
            cfg.addEdge(current, afterIf);
            cfg.addEdge(ifCondition, afterIf); // false branch if THEN does not terminate
        }

        // ELSIF blocks
        for (PlSqlParser.Elsif_partContext elsifCtx : ctx.elsif_part()) {
            CFGNode elsifCond = new CFGNode("ELSIF_CONDITION: " + elsifCtx.condition().getText());
            CFGNode elsifBlock = new CFGNode("ELSIF_BLOCK");
            cfg.addNode(elsifCond);
            cfg.addNode(elsifBlock);

            if (!isTerminating(prevCond)) {
                cfg.addEdge(prevCond, elsifCond); // false edge from previous condition
            }

            cfg.addEdge(elsifCond, elsifBlock); // true edge
            current = elsifBlock;

            boolean elsifTerminates = false;
            for (var stmt : elsifCtx.seq_of_statements().statement()) {
                for (CFGNode node : buildStatementNodes(stmt)) {
                    cfg.addNode(node);
                    cfg.addEdge(current, node);
                    current = node;
                    if (isTerminating(node)) elsifTerminates = true;
                }
            }

            if (!elsifTerminates) {
                cfg.addEdge(current, afterIf);
                cfg.addEdge(elsifCond, afterIf); // false branch to afterIf
            }

            prevCond = elsifCond;
        }

        // ELSE block or fallback
        if (ctx.else_part() != null && ctx.else_part().seq_of_statements() != null) {
            CFGNode elseBlock = new CFGNode("ELSE_BLOCK");
            cfg.addNode(elseBlock);
            if (!isTerminating(prevCond)) {
                cfg.addEdge(prevCond, elseBlock); // false edge
            }
            current = elseBlock;

            boolean elseTerminates = false;
            for (var stmt : ctx.else_part().seq_of_statements().statement()) {
                for (CFGNode node : buildStatementNodes(stmt)) {
                    cfg.addNode(node);
                    cfg.addEdge(current, node);
                    current = node;
                    if (isTerminating(node)) elseTerminates = true;
                }
            }

            if (!elseTerminates) {
                cfg.addEdge(current, afterIf);
            }
        } else {
            if (!isTerminating(prevCond)) {
                cfg.addEdge(prevCond, afterIf); // false edge if no ELSE
            }
        }

        current = afterIf;
        afterStack.push(afterIf);
        thenStack.push(thenBlock);
    }

    @Override
    public void exitIf_statement(PlSqlParser.If_statementContext ctx) {
        afterStack.pop();
        thenStack.pop();
    }

    private boolean isTerminating(CFGNode node) {
        String text = node.getLabel().toUpperCase();
        return text.startsWith("EXIT") || text.startsWith("RETURN") ||
                text.startsWith("RAISE") || text.startsWith("CONTINUE");
    }


    //Case Statement
    @Override
    public void enterSearched_case_statement(PlSqlParser.Searched_case_statementContext ctx) {
        processCaseStatement(ctx.case_when_part_statement(), ctx.case_else_part_statement());
    }

    @Override
    public void enterSimple_case_statement(PlSqlParser.Simple_case_statementContext ctx) {
        processCaseStatement(ctx.case_when_part_statement(), ctx.case_else_part_statement());
    }
    // For CASE statements (with full THEN/ELSE blocks)
    private void processCaseStatement(
            List<PlSqlParser.Case_when_part_statementContext> whenParts,
            PlSqlParser.Case_else_part_statementContext elsePart
    ) {
        CFGNode caseStart = new CFGNode("CASE_START");
        cfg.addNode(caseStart);
        cfg.addEdge(current, caseStart);

        CFGNode afterCase = new CFGNode("AFTER_CASE");
        cfg.addNode(afterCase);

        for (PlSqlParser.Case_when_part_statementContext whenCtx : whenParts) {
            CFGNode whenCond = new CFGNode("WHEN: " + whenCtx.expression().getText());
            cfg.addNode(whenCond);
            cfg.addEdge(caseStart, whenCond);

            CFGNode currentBranch = whenCond;
            for (var stmt : whenCtx.seq_of_statements().statement()) {
                if (stmt.sql_statement() != null) {
                    buildSQLNodes(stmt.sql_statement());
                } else {
                    List<CFGNode> stmtNodes = buildStatementNodes(stmt);
                    for (CFGNode node : stmtNodes) {
                        cfg.addNode(node);
                        cfg.addEdge(currentBranch, node);
                        currentBranch = node;
                    }
                }
            }

            cfg.addEdge(currentBranch, afterCase);
        }

        if (elsePart != null) {
            CFGNode elseNode = new CFGNode("CASE_ELSE_BLOCK");
            cfg.addNode(elseNode);
            cfg.addEdge(caseStart, elseNode);

            CFGNode currentBranch = elseNode;
            for (var stmt : elsePart.seq_of_statements().statement()) {
                if (stmt.sql_statement() != null) {
                    buildSQLNodes(stmt.sql_statement());
                } else {
                    List<CFGNode> stmtNodes = buildStatementNodes(stmt);
                    for (CFGNode node : stmtNodes) {
                        cfg.addNode(node);
                        cfg.addEdge(currentBranch, node);
                        currentBranch = node;
                    }
                }
            }

            cfg.addEdge(currentBranch, afterCase);
        }

        current = afterCase;
    }

    // For CASE expressions (used in assignments)
    private void processCaseExpression(
            List<PlSqlParser.Case_when_part_expressionContext> whenExprs,
            PlSqlParser.Case_else_part_expressionContext elseExpr
    ) {
        if (whenExprs == null || whenExprs.isEmpty()) return;

        CFGNode caseStart = new CFGNode("CASE_START");
        cfg.addNode(caseStart);
        cfg.addEdge(current, caseStart);

        CFGNode afterCase = new CFGNode("AFTER_CASE");
        cfg.addNode(afterCase);

        for (PlSqlParser.Case_when_part_expressionContext whenCtx : whenExprs) {
            String cond = whenCtx.expression(0).getText();
            String result = whenCtx.expression(1).getText();

            CFGNode whenNode = new CFGNode("WHEN: " + cond);
            cfg.addNode(whenNode);
            cfg.addEdge(caseStart, whenNode);

            CFGNode thenNode = new CFGNode("THEN_EXPR: " + result);
            cfg.addNode(thenNode);
            cfg.addEdge(whenNode, thenNode);

            // 🔍 Detect and process nested CASE inside THEN
            PlSqlParser.Case_expressionContext nestedCase = findCaseExpression(whenCtx.expression(1));
            if (nestedCase != null) {
                current = thenNode;
                if (nestedCase.simple_case_expression() != null) {
                    var simple = nestedCase.simple_case_expression();
                    if (simple.case_when_part_expression() != null) {
                        processCaseExpression(
                                simple.case_when_part_expression(),
                                simple.case_else_part_expression()
                        );
                    }
                } else if (nestedCase.searched_case_expression() != null) {
                    var searched = nestedCase.searched_case_expression();
                    if (searched.case_when_part_expression() != null) {
                        processCaseExpression(
                                searched.case_when_part_expression(),
                                searched.case_else_part_expression()
                        );
                    }
                }
            }

            cfg.addEdge(thenNode, afterCase);
        }

        if (elseExpr != null && elseExpr.expression() != null) {
            String elseVal = elseExpr.expression().getText();

            CFGNode elseNode = new CFGNode("CASE_ELSE_BLOCK");
            CFGNode elseExprNode = new CFGNode("ELSE_EXPR: " + elseVal);
            cfg.addNode(elseNode);
            cfg.addNode(elseExprNode);

            cfg.addEdge(caseStart, elseNode);
            cfg.addEdge(elseNode, elseExprNode);

            // 🔍 Detect and process nested CASE inside ELSE
            PlSqlParser.Case_expressionContext nestedCase = findCaseExpression(elseExpr.expression());
            if (nestedCase != null) {
                current = elseExprNode;
                if (nestedCase.simple_case_expression() != null) {
                    var simple = nestedCase.simple_case_expression();
                    if (simple.case_when_part_expression() != null) {
                        processCaseExpression(
                                simple.case_when_part_expression(),
                                simple.case_else_part_expression()
                        );
                    }
                } else if (nestedCase.searched_case_expression() != null) {
                    var searched = nestedCase.searched_case_expression();
                    if (searched.case_when_part_expression() != null) {
                        processCaseExpression(
                                searched.case_when_part_expression(),
                                searched.case_else_part_expression()
                        );
                    }
                }
            }

            cfg.addEdge(elseExprNode, afterCase);
        }

        current = afterCase;
    }


    // Helper method to search the tree for any case_expression
    private PlSqlParser.Case_expressionContext findCaseExpression(ParseTree node) {
        if (node instanceof PlSqlParser.Case_expressionContext caseExpr) {
            return caseExpr;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            PlSqlParser.Case_expressionContext result = findCaseExpression(node.getChild(i));
            if (result != null) return result;
        }

        return null;
    }

    // ============ LOOPS ============


    private final Stack<CFGNode> loopContinueStack = new Stack<>();
    private Deque<CFGNode> continueTargetStack = new ArrayDeque<>();

    @Override
    public void enterLoop_statement(PlSqlParser.Loop_statementContext ctx) {
        String labelPrefix = String.valueOf(ctx.getStart().getLine());

        CFGNode loopEntry = createNode("LOOP_ENTRY_" + labelPrefix);
        CFGNode loopCond = createNode(generateLoopConditionLabel(ctx, labelPrefix));
        CFGNode loopBody = createNode("LOOP_BODY_" + labelPrefix);
        CFGNode loopExit = createNode("LOOP_EXIT_" + labelPrefix);

        link(current, loopEntry);
        link(loopEntry, loopCond);
        link(loopCond, loopBody);  // true path
        link(loopCond, loopExit);  // false path

        loopContinueStack.push(loopCond);
        loopExitStack.push(loopExit);

        current = loopBody;

        if (ctx.seq_of_statements() != null) {
            processStatementBlock(ctx.seq_of_statements().statement());
        }

        link(current, loopCond); // back edge
        current = loopExit;
    }

    @Override
    public void exitLoop_statement(PlSqlParser.Loop_statementContext ctx) {
        if (!loopExitStack.isEmpty()) loopExitStack.pop();
        if (!loopContinueStack.isEmpty()) loopContinueStack.pop();
    }
    private String generateLoopConditionLabel(PlSqlParser.Loop_statementContext ctx, String labelPrefix) {
        if (ctx.cursor_loop_param() != null) {
            String cleaned = clean(ctx.cursor_loop_param().getText());
            return "CURSOR_FOR_LOOP_" + labelPrefix + ": emp IN (" + cleaned + ")";
        }
        if (ctx.WHILE() != null && ctx.condition() != null) {
            return "WHILE_CONDITION_" + labelPrefix + ": " + clean(ctx.condition().getText());
        }
        if (ctx.FOR() != null) {
            return "FOR_CONDITION_" + labelPrefix + ": (loop range)";
        }
        return "LOOP_CONDITION_" + labelPrefix;
    }

    private String clean(String text) {
        text = text.replaceAll("\\s+", " ").trim();
        return text.length() > 80 ? text.substring(0, 77) + "..." : text;
    }


    private CFGNode createNode(String label) {
        CFGNode node = new CFGNode(label);
        cfg.addNode(node);
        return node;
    }

    private void link(CFGNode from, CFGNode to) {
        cfg.addEdge(from, to);
    }

// ============ Statement Processing Core ============

    private void processStatementBlock(List<PlSqlParser.StatementContext> stmts) {
        for (int i = 0; i < stmts.size(); i++) {
            var stmtCtx = stmts.get(i);

            // Handle FETCH INTO with cursor followed by loop
            if (isCursorFetchIntoCursor(stmtCtx)) {
                i = buildFetchWithCursorBlock(i, stmts);
                continue;
            }

            // Handle SQL statements (INSERT, UPDATE, SELECT, etc.)
            if (stmtCtx.sql_statement() != null) {
                buildSQLNodes(stmtCtx.sql_statement());
            }

            // Handle general PL/SQL statements
            else if (stmtCtx.assignment_statement() != null ||
                    stmtCtx.call_statement() != null ||
                    stmtCtx.return_statement() != null ||
                    stmtCtx.null_statement() != null) {

                for (CFGNode node : buildStatementNodes(stmtCtx)) {
                    cfg.addNode(node);
                    cfg.addEdge(current, node);
                    current = node;
                }
            }

            // Handle EXIT statements
            else if (stmtCtx.exit_statement() != null) {
                List<CFGNode> nodes = buildStatementNodes(stmtCtx);
                if (!nodes.isEmpty()) {
                    CFGNode exitNode = nodes.get(0);
                    cfg.addNode(exitNode);
                    cfg.addEdge(current, exitNode);

                    if (!loopExitStack.isEmpty()) {
                        CFGNode loopExitNode = loopExitStack.peek();
                        cfg.addEdge(exitNode, loopExitNode);  // EXIT true branch
                    }

                    current = exitNode;
                }
            }
        }
    }
    private boolean isCursorFetchIntoCursor(PlSqlParser.StatementContext stmtCtx) {
        if (stmtCtx.sql_statement() == null) return false;
        var cur = stmtCtx.sql_statement().cursor_manipulation_statements();
        if (cur == null || cur.fetch_statement() == null) return false;

        return cur.fetch_statement().getText().contains(",");
    }
    private int buildFetchWithCursorBlock(int i, List<PlSqlParser.StatementContext> stmts) {
        var stmtCtx = stmts.get(i);
        var fetch = stmtCtx.sql_statement().cursor_manipulation_statements().fetch_statement();
        String fetchText = fetch.getText();

        // Add FETCH node
        CFGNode fetchNode = new CFGNode("FETCH_CURSOR: " + fetchText);
        cfg.addNode(fetchNode);
        cfg.addEdge(current, fetchNode);
        current = fetchNode;

        // Do NOT generate a fake EXIT node here — it's handled later if the source includes one

        // If next statement is a loop, process it immediately
        if (i + 1 < stmts.size()) {
            var nextStmt = stmts.get(i + 1);
            if (nextStmt.loop_statement() != null) {
                i++; // consume the loop
                processStatementBlock(List.of(nextStmt));

                // After the loop, process remaining statements in the block
                List<PlSqlParser.StatementContext> remaining = stmts.subList(i + 1, stmts.size());
                processStatementBlock(remaining);
                return stmts.size(); // stop outer loop
            }
        }

        return i;
    }


// ============ SQL ============

    private void buildSQLNodes(PlSqlParser.Sql_statementContext sql) {
        if (sql.cursor_manipulation_statements() != null) {
            var cur = sql.cursor_manipulation_statements();
            addStatementNode("OPEN_CURSOR", cur.open_statement());
            addStatementNode("FETCH_CURSOR", cur.fetch_statement());
            addStatementNode("CLOSE_CURSOR", cur.close_statement());
        }

        if (sql.data_manipulation_language_statements() != null) {
            var dml = sql.data_manipulation_language_statements();

            if (dml.merge_statement() != null) {
                handleMergeStatement(dml.merge_statement());
                return;
            }

            addStatementNode("INSERT", dml.insert_statement());
            addStatementNode("UPDATE", dml.update_statement());
            addStatementNode("DELETE", dml.delete_statement());

            if (dml.select_statement() != null) {
                String text = dml.select_statement().getText();
                boolean hasInto = text.toUpperCase().contains("INTO");
                addStatementNode(hasInto ? "SELECT_INTO" : "SELECT", dml.select_statement());
            }
        }

        if (sql.transaction_control_statements() != null) {
            var txn = sql.transaction_control_statements().getText().toUpperCase();
            if (txn.contains("COMMIT")) {
                addStaticNode("COMMIT");
            } else if (txn.contains("ROLLBACK")) {
                addStaticNode("ROLLBACK");
            }
        }
    }

    private void addStatementNode(String prefix, ParserRuleContext stmt) {
        if (stmt != null) {
            CFGNode node = new CFGNode(prefix + ": " + stmt.getText());
            cfg.addNode(node);
            cfg.addEdge(current, node);
            current = node;
        }
    }

    private void addStaticNode(String label) {
        current = appendNode(current, label);
    }

    private CFGNode appendNode(CFGNode from, String label) {
        CFGNode node = new CFGNode(label);
        cfg.addNode(node);
        cfg.addEdge(from, node);
        return node;
    }

    private void handleMergeStatement(PlSqlParser.Merge_statementContext merge) {
        CFGNode mergeInto = new CFGNode("MERGE_INTO: " + merge.tableview_name().getText());
        CFGNode mergeCond = new CFGNode("MERGE_CONDITION: " + merge.condition().getText());
        CFGNode matchedBranch = new CFGNode("MERGE_MATCHED");
        CFGNode notMatchedBranch = new CFGNode("MERGE_NOT_MATCHED");
        CFGNode mergeEnd = new CFGNode("MERGE_END");

        cfg.addNode(mergeInto);
        cfg.addNode(mergeCond);
        cfg.addNode(matchedBranch);
        cfg.addNode(notMatchedBranch);
        cfg.addNode(mergeEnd);

        cfg.addEdge(current, mergeInto);
        cfg.addEdge(mergeInto, mergeCond);
        cfg.addEdge(mergeCond, matchedBranch);
        cfg.addEdge(mergeCond, notMatchedBranch);

        CFGNode matchedCurrent = matchedBranch;
        if (merge.merge_update_clause() != null) {
            var update = merge.merge_update_clause();
            matchedCurrent = appendNode(matchedCurrent, "MERGE_UPDATE: " + update.getText());

            if (update.where_clause() != null) {
                matchedCurrent = appendNode(matchedCurrent, "MERGE_UPDATE_WHERE: " + update.where_clause().getText());
            }

            if (update.merge_element() != null) {
                for (var elem : update.merge_element()) {
                    matchedCurrent = appendNode(matchedCurrent, "MERGE_SET: " + elem.getText());
                }
            }

            if (update.merge_update_delete_part() != null) {
                matchedCurrent = appendNode(matchedCurrent, "MERGE_UPDATE_DELETE: " + update.merge_update_delete_part().getText());
            }
        }
        cfg.addEdge(matchedCurrent, mergeEnd);

        CFGNode notMatchedCurrent = notMatchedBranch;
        if (merge.merge_insert_clause() != null) {
            var insert = merge.merge_insert_clause();
            notMatchedCurrent = appendNode(notMatchedCurrent, "MERGE_INSERT: " + insert.getText());

            if (insert.values_clause() != null) {
                String valText = insert.values_clause().getText();
                String label = valText.toUpperCase().contains("SELECT")
                        ? "MERGE_INSERT_SELECT: " + valText
                        : "MERGE_INSERT_VALUES: " + valText;
                notMatchedCurrent = appendNode(notMatchedCurrent, label);
            }

            if (insert.where_clause() != null) {
                notMatchedCurrent = appendNode(notMatchedCurrent, "MERGE_INSERT_WHERE: " + insert.where_clause().getText());
            }
        }
        cfg.addEdge(notMatchedCurrent, mergeEnd);
        current = mergeEnd;
    }
    private List<CFGNode> buildStatementNodes(PlSqlParser.StatementContext stmtCtx) {
        List<CFGNode> result = new ArrayList<>();

        // 🔍 Debug info
        System.out.println("🔍 Processing statement: " + stmtCtx.getText());
        System.out.println("    Has block()? " + (stmtCtx.block() != null));
        System.out.println("    Has body()? " + (stmtCtx.body() != null));

        // ✅ Assignment
        if (stmtCtx.assignment_statement() != null) {
            var assign = stmtCtx.assignment_statement();
            result.add(new CFGNode("ASSIGNMENT: " + assign.getText()));
            return result;
        }

        // ✅ Call
        if (stmtCtx.call_statement() != null) {
            result.add(new CFGNode("CALL: " + stmtCtx.call_statement().getText()));
        }

        // ✅ Raise
        else if (stmtCtx.raise_statement() != null) {
            result.add(new CFGNode("RAISE: " + stmtCtx.raise_statement().getText()));
        }

        // ✅ Return
        else if (stmtCtx.return_statement() != null) {
            result.add(new CFGNode("RETURN: " + stmtCtx.return_statement().getText()));
        }

        // ✅ Exit
        else if (stmtCtx.exit_statement() != null) {
            result.add(new CFGNode("EXIT: " + stmtCtx.exit_statement().getText()));
        }

        // ✅ Continue
        else if (stmtCtx.continue_statement() != null) {
            CFGNode cont = new CFGNode("CONTINUE: " + stmtCtx.continue_statement().getText());
            result.add(cont);
            if (!loopContinueStack.isEmpty()) {
                cfg.addEdge(cont, loopContinueStack.peek());
            }
        }

        // ✅ NULL
        else if (stmtCtx.null_statement() != null) {
            result.add(new CFGNode("NULL_STATEMENT"));
        }

        return result;
    }
    @Override
    public void enterAnonymous_block(PlSqlParser.Anonymous_blockContext ctx) {
        System.out.println("Entering Anonymous Block...");

        // Handle DECLARE
        if (ctx.DECLARE() != null && ctx.seq_of_declare_specs() != null) {
            for (var spec : ctx.seq_of_declare_specs().declare_spec()) {
                CFGNode declareNode = new CFGNode("DECLARE: " + spec.getText());
                cfg.addNode(declareNode);
                cfg.addEdge(current, declareNode);
                current = declareNode;
            }

            CFGNode endDeclareNode = new CFGNode("END_DECLARE_BLOCK");
            cfg.addNode(endDeclareNode);
            cfg.addEdge(current, endDeclareNode);
            current = endDeclareNode;
        }

        // ✅ Handle BEGIN ... END (the body is the seq_of_statements directly)
        if (ctx.BEGIN() != null && ctx.seq_of_statements() != null) {
            processBody(ctx);  // Modified below
        }
    }

    @Override
    public void exitAnonymous_block(PlSqlParser.Anonymous_blockContext ctx) {
        addStaticNode("EXIT");
    }
    private void processBody(PlSqlParser.Anonymous_blockContext ctx) {
        CFGNode beginNode = new CFGNode("BEGIN_BLOCK");
        cfg.addNode(beginNode);
        cfg.addEdge(current, beginNode);
        current = beginNode;

        processStatementBlock(ctx.seq_of_statements().statement());

        CFGNode endNode = new CFGNode("END_BLOCK");
        cfg.addNode(endNode);
        cfg.addEdge(current, endNode);
        current = endNode;

        if (ctx.exception_handler() != null && !ctx.exception_handler().isEmpty()) {
            handleExceptionHandlers(ctx.exception_handler());
        }
    }


    private void handleExceptionHandlers(List<PlSqlParser.Exception_handlerContext> handlers) {
        CFGNode normalFlowEnd = new CFGNode("NORMAL_FLOW_END");
        cfg.addNode(normalFlowEnd);
        cfg.addEdge(current, normalFlowEnd);

        CFGNode exceptionSwitch = new CFGNode("EXCEPTION_SWITCH");
        cfg.addNode(exceptionSwitch);
        cfg.addEdge(normalFlowEnd, exceptionSwitch);

        CFGNode afterException = new CFGNode("AFTER_EXCEPTION_HANDLERS");
        cfg.addNode(afterException);

        for (var handler : handlers) {
            String label = handler.exception_name().isEmpty()
                    ? "OTHERS"
                    : handler.exception_name().stream()
                    .map(ParseTree::getText)
                    .collect(Collectors.joining(" OR "));
            CFGNode whenNode = new CFGNode("WHEN: " + label);
            cfg.addNode(whenNode);
            cfg.addEdge(exceptionSwitch, whenNode);

            CFGNode currentBranch = whenNode;
            for (var stmt : handler.seq_of_statements().statement()) {
                List<CFGNode> stmtNodes = buildStatementNodes(stmt);
                for (CFGNode node : stmtNodes) {
                    cfg.addNode(node);
                    cfg.addEdge(currentBranch, node);
                    currentBranch = node;
                }
            }

            cfg.addEdge(currentBranch, afterException);
        }

        CFGNode noException = new CFGNode("NO_EXCEPTION");
        cfg.addNode(noException);
        cfg.addEdge(exceptionSwitch, noException);
        cfg.addEdge(noException, afterException);

        current = afterException;
    }

//---------------------------- Finalization

    @Override
    public void exitSql_script(PlSqlParser.Sql_scriptContext ctx) {
        if (!current.succs.contains(cfg.exit)) {
            cfg.addEdge(current, cfg.exit);
        }
    }

}

