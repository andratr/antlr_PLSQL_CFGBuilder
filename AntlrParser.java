// Created using https://www.youtube.com/watch?v=itajbtWKPGQ&t=1s

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

import org.antlr.v4.gui.TreeViewer;

import javax.swing.*;
import java.util.Arrays;

public class AntlrParser {
    public static void main(String[] args) throws IOException {
        System.out.println("Main function - ");

        CharStream input = CharStreams.fromFileName("C:\\Users\\40744\\Desktop\\learnantlr\\antlr_tutorial\\files\\plsql-3.txt");
        PlSqlLexer lexer = new PlSqlLexer(input);
        CommonTokenStream commonTokenStream = new CommonTokenStream(lexer);
        PlSqlParser plSqlParser = new PlSqlParser(commonTokenStream);


        //System.out.println("First function: " + plSqlParser.getRuleNames()[0]);

        //  String[] ruleNames = plSqlParser.getRuleNames();

//        System.out.println("PL/SQL Rule Names:");
//        for (int i = 0; i < ruleNames.length; i++) {
//            System.out.printf("Rule %2d: %s%n", i, ruleNames[i]);
//        }
//
//// Define the keywords to search for
//        String[] keywords = {"while"};
//
//// Print PL/SQL Rule Names filtered by keywords
//        System.out.println("PL/SQL Rule Names matching keywords:");
//
//        for (int i = 0; i < ruleNames.length; i++) {
//            for (String keyword : keywords) {
//                // Check if the rule name contains any of the keywords
//                if (ruleNames[i].toLowerCase().contains(keyword)) {
//                    System.out.printf("Rule %2d: %s%n", i, ruleNames[i]);
//                }
//            }
//        }
        ParseTree parseTree = plSqlParser.sql_script();
//
//        System.out.println(parseTree.toStringTree(plSqlParser));

        // Show parse tree in GUI
// //       TreeViewer viewer = new TreeViewer(Arrays.asList(plSqlParser.getRuleNames()), parseTree);
//
//        JFrame frame = new JFrame("ANTLR Parse Tree");
//        frame.add(viewer);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(800, 600);
//        frame.setVisible(true);


        ExtendedCFGBuilder extendedCfgBuilder = new ExtendedCFGBuilder();
        CFG cfg = extendedCfgBuilder.build(parseTree);

        // N = number of nodes
        int N = cfg.nodes.size();

        // E = sum of all out-edges
        int E = cfg.nodes.stream()
                .mapToInt(n -> n.succs.size())
                .sum();

        // P = 1 (assuming one connected CFG)
        int P = 1;

        cfg.printCFG();
        cfg.exportToDot("cfg.dot");


        // Cyclomatic complexity
        int V = E - N + 2 * P;
        System.out.println("Nodes: " + N + ", Edges: " + E);
        System.out.println("Cyclomatic Complexity V(G)=E-N+2P : " + V);
    }
}