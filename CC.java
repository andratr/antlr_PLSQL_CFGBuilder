import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;


public class CC {
    public static void main(String[] args) throws Exception {
        File svgFile  = new File("antlr4_parse_tree.svg"); // Replace with your SVG file path

        // Parse the SVG file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(svgFile);

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Step 1: Extract all <line> elements from the SVG
        NodeList lineNodes = document.getElementsByTagName("line");

        // Step 2: Set up data structures to count nodes and edges
        Set<String> nodes = new HashSet<>();
        int edgeCount = 0;

        // Step 3: Process each <line> element
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Node lineNode = lineNodes.item(i);
            NamedNodeMap attributes = lineNode.getAttributes();

            // Get coordinates of the line (x1, y1, x2, y2)
            String x1 = attributes.getNamedItem("x1").getTextContent();
            String y1 = attributes.getNamedItem("y1").getTextContent();
            String x2 = attributes.getNamedItem("x2").getTextContent();
            String y2 = attributes.getNamedItem("y2").getTextContent();

            // Create unique node identifiers for both start and end points
            String startNode = x1 + "," + y1;
            String endNode = x2 + "," + y2;

            // Add nodes to the set (Set automatically handles uniqueness)
            nodes.add(startNode);
            nodes.add(endNode);

            // Increment edge count
            edgeCount++;
        }

        // Step 4: Calculate Cyclomatic Complexity: CC = E - N + 2P
        int nodeCount = nodes.size();
        int cyclomaticComplexity = edgeCount - nodeCount + 2;

        // Step 5: Output the results
        System.out.println("Nodes: " + nodeCount);
        System.out.println("Edges: " + edgeCount);
        System.out.println("Cyclomatic Complexity: " + cyclomaticComplexity);
    }
}

