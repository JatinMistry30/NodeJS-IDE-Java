import javax.swing.*; //Provides Swing components for creating graphical user interfaces
import javax.swing.tree.*; //Provides classes for tree strucutres used with components like JTree
import javax.swing.text.*; //Provides classes for text components and document models.
import java.awt.*; //Provides Older(pre-swing) GUI Components and utilities
import java.awt.event.*; //Provides event handling classes for GUI interactions.
import java.io.*; //Provides input and output (I/O) classes for reading and writing data
import java.nio.file.*; //New I/O (NIO) API for modern file handling — introduced in Java 7
import java.util.*;

public class NodeJSIDE extends JFrame {
    private JTree fileTree; // Displays Data in a hierarchical tree structure
    private DefaultTreeModel treeModel; // Data model that holds and manages the strucutre of your JTree
    private DefaultMutableTreeNode rootNode; // Represents Single Node in a JTree.

    private File workingDirectory;
    private JTabbedPane editorTabs;
    // Declare our UI components
    private JPanel mainPanel;
    private JPanel leftPanel;
    private JPanel centerPanel;
    private JPanel rightPanel;

    // Command Panel, Terminal
    private JTextPane terminalArea;
    private JTextField commandField;
    private StyledDocument terminalDoc;

    // Constructor - this runs when we create the window
    public NodeJSIDE() {
        setTitle("Node.js IDE"); // Window title
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close button stops program
        setLocationRelativeTo(null); // Center the window on screen

        // Set working directory
        workingDirectory = new File(System.getProperty("user.home"));

        // Initialize Components
        initComponents();

        // Create Menu bar
        createMenuBar();
        // Make the window visible
        setVisible(true);
    }

    private void initComponents() {
        mainPanel = new JPanel(new BorderLayout()); // Create a main panel with border layout

        // Create three panels for our three main areas
        leftPanel = new JPanel();
        centerPanel = new JPanel();
        rightPanel = new JPanel();
        // Give them different colors so we can see them
        leftPanel.setBackground(Color.LIGHT_GRAY);
        centerPanel.setBackground(Color.WHITE);
        rightPanel.setBackground(Color.LIGHT_GRAY);
        // Add borders with titles
        leftPanel.setBorder(BorderFactory.createTitledBorder("File Explorer"));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Editor"));
        rightPanel.setBorder(BorderFactory.createTitledBorder("Terminal"));
        // Build the left panel(File Explorer)
        setupFileExplorer();
        setupEditor();
        setupTerminal();
        // using Split panes to divide the space
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        mainSplit.setDividerLocation(300); // Set initial divider position
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, rightPanel);
        verticalSplit.setDividerLocation(600);
        // Add everything to main panel
        mainPanel.add(verticalSplit, BorderLayout.CENTER);

        // Add main panel to window
        add(mainPanel);
    }

    // Setup the terminal
    private void setupTerminal() {
        rightPanel.setLayout(new BorderLayout());

        // Creating terminal area(For output)
        terminalArea = new JTextPane();
        terminalArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        terminalArea.setEditable(false); // Users can't edit the output
        terminalArea.setBackground(new Color(20, 20, 20)); // Dark background
        terminalArea.setForeground(Color.WHITE); // White text
        terminalArea.setCaretColor(Color.WHITE); // White cursor

        // Get the document for styling text
        terminalDoc = terminalArea.getStyledDocument();

        // Put terminal in scroll pane
        JScrollPane terminalScroll = new JScrollPane(terminalArea);
        // Create command input area at the bottom
        JPanel commandPanel = new JPanel(new BorderLayout(5, 5));
        JLabel promptLabel = new JLabel("$");
        promptLabel.setForeground(Color.GREEN);

        commandField = new JTextField();
        commandField.setFont(new Font("Consolas", Font.PLAIN, 13));
        commandField.setBackground(new Color(30, 30, 30));
        commandField.setForeground(Color.WHITE);
        commandField.setCaretColor(Color.WHITE);

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton executeBtn = new JButton("Execute");
        JButton clearBtn = new JButton("Clear");

        executeBtn.addActionListener(e -> {
            String command = commandField.getText();
            if (!command.isEmpty()) {
                appendToTerminal("$ " + command + "\n", Color.CYAN);
                appendToTerminal("Command executed: " + command + "\n", Color.WHITE);
                commandField.setText(""); // Clear the input field
            }
        });

        clearBtn.addActionListener(e -> clearTerminal());

        buttonPanel.add(executeBtn);
        buttonPanel.add(clearBtn);

        // For Enter Key
        commandField.addActionListener(e -> {
            String command = commandField.getText();
            if (!command.isEmpty()) {
                appendToTerminal("$ " + command + "\n", Color.CYAN);
                appendToTerminal("Command executed: " + command + "\n", Color.WHITE);
                commandField.setText("");
            }
        });

        // Add everything to command panel
        commandPanel.add(promptLabel, BorderLayout.WEST);
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(buttonPanel, BorderLayout.EAST);

        // Add welcome message to terminal
        appendToTerminal("=== Node.js Terminal ===\n", Color.GREEN);
        appendToTerminal("Type Node.js commands below\n\n", Color.WHITE);

        // Add everything to right panel
        rightPanel.add(terminalScroll, BorderLayout.CENTER);
        rightPanel.add(commandPanel, BorderLayout.SOUTH);
    }

    private void appendToTerminal(String text, Color color) {
        try {
            // Create a style for the color
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, color);

            // Add the text with the style
            terminalDoc.insertString(terminalDoc.getLength(), text, style);

            // Scroll to bottom
            terminalArea.setCaretPosition(terminalDoc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create the menu bar
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newFileItem = new JMenuItem("New File");
        JMenuItem newFolderItem = new JMenuItem("New Folder");
        JMenuItem openDirItem = new JMenuItem("Open Directory");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Adding action listeners
        newFileItem.addActionListener(e -> {
            appendToTerminal("New File menu clicked\n", Color.YELLOW);

        });
        newFolderItem.addActionListener(e -> {
            appendToTerminal("New Folder menu clicked\n", Color.YELLOW);
        });

        openDirItem.addActionListener(e -> {
            appendToTerminal("Open Directory menu clicked\n", Color.YELLOW);
        });

        saveItem.addActionListener(e -> {
            appendToTerminal("Save menu clicked\n", Color.YELLOW);
        });

        exitItem.addActionListener(e -> System.exit(0));
        // Add items to file menu
        fileMenu.add(newFileItem);
        fileMenu.add(newFolderItem);
        fileMenu.addSeparator(); // Adds a separator line
        fileMenu.add(openDirItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem refreshItem = new JMenuItem("Refresh Explorer");

        deleteItem.addActionListener(e -> {
            appendToTerminal("Delete menu clicked\n", Color.YELLOW);
        });

        renameItem.addActionListener(e -> {
            appendToTerminal("Rename menu clicked\n", Color.YELLOW);
        });

        refreshItem.addActionListener(e -> refreshFileTree());

        editMenu.add(deleteItem);
        editMenu.add(renameItem);
        editMenu.addSeparator();
        editMenu.add(refreshItem);

        // Terminal Menu
        JMenu terminalMenu = new JMenu("Terminal");
        JMenuItem clearTerminalItem = new JMenuItem("Clear Terminal");
        JMenuItem runFileItem = new JMenuItem("Run Current File");

        clearTerminalItem.addActionListener(e -> clearTerminal());
        runFileItem.addActionListener(e -> {
            appendToTerminal("Run Current File menu clicked\n", Color.YELLOW);
        });

        terminalMenu.add(clearTerminalItem);
        terminalMenu.add(runFileItem);

        // Add all menus to menu bar
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(terminalMenu);

        // Set the menu bar for our window
        setJMenuBar(menuBar);
    }

    // Method to clear the terminal
    private void clearTerminal() {
        try {
            terminalDoc.remove(0, terminalDoc.getLength());
            appendToTerminal("Terminal cleared\n", Color.GREEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFileExplorer() {
        // Remove the previous simple setup
        leftPanel.setLayout(new BorderLayout());

        // Create the root node for our file tree
        rootNode = new DefaultMutableTreeNode(workingDirectory.getName());

        treeModel = new DefaultTreeModel(rootNode);

        // Create the actual tree components
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);

        // Put the tree in a scroll pane
        JScrollPane scrollPane = new JScrollPane(fileTree);

        // Buttons at the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton newFileBtn = new JButton("New File");

        buttonPanel.add(refreshBtn);
        buttonPanel.add(newFileBtn);

        // Add everything to left panel
        leftPanel.add(buttonPanel, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Load the actual files
        refreshFileTree();
    }

    // Setup the editor area with tabs
    private void setupEditor() {
        centerPanel.setLayout(new BorderLayout());
        // Create toolbar at the top
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton runBtn = new JButton("Run");
        JButton saveBtn = new JButton("Save");
        JButton closeBtn = new JButton("Close Tab");

        toolbar.add(runBtn);
        toolbar.add(saveBtn);
        toolbar.add(closeBtn);
        // Create the tabbed pane for multiple files
        editorTabs = new JTabbedPane();
        editorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT); // Scroll tabs if too many

        // Add a welcome tab
        addWelcomeTab();

        // Add everything to center panel
        centerPanel.add(toolbar, BorderLayout.NORTH);
        centerPanel.add(editorTabs, BorderLayout.CENTER);
    }

    private void addWelcomeTab() {
        JTextArea welcomeEditor = new JTextArea();
        welcomeEditor.setText("Welcome to Node.js IDE!\n\n" +
                "• Double-click files in the explorer to open them\n" +
                "• Use the toolbar to run and save files\n" +
                "• Multiple files open in tabs");
        welcomeEditor.setFont(new Font("Consolas", Font.PLAIN, 14));
        welcomeEditor.setEditable(false); // Read-only welcome message

        JScrollPane scrollPane = new JScrollPane(welcomeEditor);
        editorTabs.addTab("Welcome", scrollPane);
    }

    private void refreshFileTree() {
        // Clear existing nodes
        rootNode.removeAllChildren();

        // set the root name
        rootNode.setUserObject(workingDirectory.getName());

        // Load files in background thread so GUI appears immediately
        new Thread(() -> {
            loadDirectory(workingDirectory, rootNode);

            // Update the tree on the GUI thread
            SwingUtilities.invokeLater(() -> {
                treeModel.reload();
                fileTree.expandRow(0);
            });
        }).start();
    }

    private void loadDirectory(File dir, DefaultMutableTreeNode node) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                // Only show normal files (not hidden ones)
                if (!file.getName().startsWith(".")) {
                    // Create node for this file/folder
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                    childNode.setUserObject(file);
                    node.add(childNode);
                    // If its a directory load its contents too
                    if (file.isDirectory()) {
                        loadDirectory(file, childNode);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // Create our window
        new NodeJSIDE();
    }
}
