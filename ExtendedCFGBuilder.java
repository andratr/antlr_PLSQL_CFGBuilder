public class ExtendedCFGBuilder extends CFGBuilder {
//    // Handle DECLARE block (variable declarations)
//    @Override
//    public void enterDeclare_section(PlSqlParser.Declare_sectionContext ctx) {
//        current.stmts.add("DECLARE: " + ctx.getText());
//    }
//
//    // Handle RETURN statement
//    @Override
//    public void exitReturn_statement(PlSqlParser.Return_statementContext ctx) {
//        current.succs.add(cfg.exit);
//        cfg.exit.preds.add(current);
//        current.stmts.add("RETURN: " + ctx.getText());
//    }
//
//    // Handle GOTO statement (jump)
//    @Override
//    public void exitGoto_statement(PlSqlParser.Goto_statementContext ctx) {
//        CFGNode targetNode = findTargetNode(ctx.getText());
//        if (targetNode != null) {
//            current.succs.add(targetNode);
//            targetNode.preds.add(current);
//            current.stmts.add("GOTO: " + ctx.getText());
//        }
//    }
//
//    // Handle EXIT statement (exit from loop)
//    @Override
//    public void exitExit_statement(PlSqlParser.Exit_statementContext ctx) {
//        current.succs.add(cfg.exit);
//        cfg.exit.preds.add(current);
//        current.stmts.add("EXIT: " + ctx.getText());
//    }
//
//    // Handle CONTINUE statement (skip to the next iteration of a loop)
//    @Override
//    public void exitContinue_statement(PlSqlParser.Continue_statementContext ctx) {
//        current.succs.add(current);  // Skip to next loop iteration
//        current.preds.add(current);
//        current.stmts.add("CONTINUE: " + ctx.getText());
//    }
//
//    @Override
//    public void enterOpen_for_statement(PlSqlParser.Open_for_statementContext ctx) {
//        // Create a new CFG node for the OPEN FOR statement
//        CFGNode openBlock = new CFGNode("OPEN_BLOCK");
//        cfg.nodes.add(openBlock);
//
//        // Link the current node to the OPEN FOR node
//        current.succs.add(openBlock);
//        openBlock.preds.add(current);
//
//        // Now set the current node to the open block to proceed further
//        current = openBlock;
//    }
//
//    @Override
//    public void enterFetch_statement(PlSqlParser.Fetch_statementContext ctx) {
//        // Create a new CFG node for the FETCH statement
//        CFGNode fetchBlock = new CFGNode("FETCH_BLOCK");
//        cfg.nodes.add(fetchBlock);
//
//        // Link the current node to the FETCH node
//        current.succs.add(fetchBlock);
//        fetchBlock.preds.add(current);
//
//        // Now set the current node to the fetch block to proceed further
//        current = fetchBlock;
//    }
//
//    @Override
//    public void enterClose_statement(PlSqlParser.Close_statementContext ctx) {
//        // Create a new CFG node for the CLOSE statement
//        CFGNode closeBlock = new CFGNode("CLOSE_BLOCK");
//        cfg.nodes.add(closeBlock);
//
//        // Link the current node to the CLOSE node
//        current.succs.add(closeBlock);
//        closeBlock.preds.add(current);
//
//        // Now set the current node to the close block
//        current = closeBlock;
//    }
//
//    @Override
//    public void exitFetch_statement(PlSqlParser.Fetch_statementContext ctx) {
//        // Assume after fetching, we want to go to the next processing block or the end of the cursor operation.
//        // We'll link this node to the next block (e.g., the block that processes the fetched data or closes the cursor)
//
//        CFGNode nextBlock = new CFGNode("NEXT_BLOCK");  // or some existing node representing the next operation
//        cfg.nodes.add(nextBlock);
//
//        // Connect the FETCH block to the next block
//        current.succs.add(nextBlock);
//        nextBlock.preds.add(current);
//
//        // Set the current node to the next block
//        current = nextBlock;
//    }
//
//
//
}
