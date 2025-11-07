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

    // Track opened files
    private Map<String, EditorTab> openFiles;

    // Track the current running process
    private Process currenProcess;
    private SimpleAttributeSet normalStyle, errorStyle, successStyle, commandStyle; // Terminal styles

    // Constructor - this runs when we create the window
    public NodeJSIDE() {
        setTitle("Node.js IDE"); // Window title
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close button stops program
        setLocationRelativeTo(null); // Center the window on screen

        // Set working directory
        workingDirectory = new File(System.getProperty("user.home"));

        // Initialize open files map
        openFiles = new HashMap<>();

        // Initalize terminal styles
        initTerminalStyles();

        // Initialize Components
        initComponents();

        // Create Menu bar
        createMenuBar();
        // Make the window visible
        setVisible(true);
    }

    private void initTerminalStyles() {
        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, new Color(200, 200, 200));

        errorStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(errorStyle, new Color(255, 100, 100));
        StyleConstants.setBold(errorStyle, true);

        successStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(successStyle, new Color(100, 255, 100));
        StyleConstants.setBold(successStyle, true);

        commandStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(commandStyle, new Color(100, 200, 255));
        StyleConstants.setBold(commandStyle, true);
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

        // Execute button action - runs commands in terminal
        executeBtn.addActionListener(e -> {
            String command = commandField.getText();
            if (!command.isEmpty()) {
                appendToTerminal("$ " + command + "\n", Color.CYAN);
                appendToTerminal("Command executed: " + command + "\n", Color.WHITE);
                commandField.setText(""); // Clear the input field
            }
        });

        // Clear button action - clears terminal output
        clearBtn.addActionListener(e -> clearTerminal());

        buttonPanel.add(executeBtn);
        buttonPanel.add(clearBtn);

        // For Enter Key - allows executing commands by pressing Enter
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

    // Helper method to add colored text to terminal
    private void appendToTerminal(String text, Color color) {
        try {
            // Create a style for the color
            SimpleAttributeSet style = new SimpleAttributeSet();
            StyleConstants.setForeground(style, color);

            // Add the text with the style
            terminalDoc.insertString(terminalDoc.getLength(), text, style);

            // Scroll to bottom to show latest output
            terminalArea.setCaretPosition(terminalDoc.getLength());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Create the menu bar with File, Edit, and Terminal menus
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu - contains file operations
        JMenu fileMenu = new JMenu("File");
        JMenuItem newFileItem = new JMenuItem("New File");
        JMenuItem newFolderItem = new JMenuItem("New Folder");
        JMenuItem openDirItem = new JMenuItem("Open Directory");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Adding action listeners for file operations
        newFileItem.addActionListener(e -> createNewFile());
        newFolderItem.addActionListener(e -> {
            appendToTerminal("New Folder menu clicked\n", Color.YELLOW);
        });
        openDirItem.addActionListener(e -> {
            appendToTerminal("Open Directory menu clicked\n", Color.YELLOW);
        });
        saveItem.addActionListener(e -> saveCurrentFile());
        exitItem.addActionListener(e -> System.exit(0));

        // Add items to file menu
        fileMenu.add(newFileItem);
        fileMenu.add(newFolderItem);
        fileMenu.addSeparator(); // Adds a separator line
        fileMenu.add(openDirItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Edit Menu - contains editing operations
        JMenu editMenu = new JMenu("Edit");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem refreshItem = new JMenuItem("Refresh Explorer");

        // Edit menu actions
        deleteItem.addActionListener(e -> deleteSelectedFile());
        renameItem.addActionListener(e -> {
            appendToTerminal("Rename menu clicked\n", Color.YELLOW);
        });
        refreshItem.addActionListener(e -> refreshFileTree());

        editMenu.add(deleteItem);
        editMenu.add(renameItem);
        editMenu.addSeparator();
        editMenu.add(refreshItem);

        // Terminal Menu - contains terminal operations
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

    // Method to clear the terminal output
    private void clearTerminal() {
        try {
            terminalDoc.remove(0, terminalDoc.getLength());
            appendToTerminal("Terminal cleared\n", Color.GREEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Setup file explorer with tree view and buttons
    private void setupFileExplorer() {
        // Remove the previous simple setup
        leftPanel.setLayout(new BorderLayout());

        // Create the root node for our file tree
        rootNode = new DefaultMutableTreeNode(workingDirectory.getName());

        // Create tree model to manage the tree structure
        treeModel = new DefaultTreeModel(rootNode);

        // Create the actual tree components
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);

        // Double Click to open files - main file interaction
        fileTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click detection
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject(); // Get the associated File object
                        if (userObject instanceof File) {
                            File file = (File) userObject;
                            if (file.isFile()) { // Only open files, not folders
                                openFile(file);
                            }
                        }
                    }
                }
            }
        });

        // Put the tree in a scroll pane for large directories
        JScrollPane scrollPane = new JScrollPane(fileTree);

        // Buttons at the top for common operations
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton newFileBtn = new JButton("New File");

        // Event for refresh button - reloads file tree
        refreshBtn.addActionListener(e -> refreshFileTree());
        // Event for new file button - creates new files
        newFileBtn.addActionListener(e -> createNewFile());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(newFileBtn);

        // Add everything to left panel
        leftPanel.add(buttonPanel, BorderLayout.NORTH);
        leftPanel.add(scrollPane, BorderLayout.CENTER);

        // Load the actual files from disk
        refreshFileTree();
    }

    // Setup the editor area with tabs for multiple file editing
    private void setupEditor() {
        centerPanel.setLayout(new BorderLayout());
        // Create toolbar at the top with action buttons
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton runBtn = new JButton("Run");
        JButton saveBtn = new JButton("Save");
        JButton closeBtn = new JButton("Close Tab");

        // Action Listeners for editor buttons
        runBtn.addActionListener(e -> {
            appendToTerminal("Run button clicked - we'll implement this later\n", Color.YELLOW);
        });
        saveBtn.addActionListener(e -> saveCurrentFile());
        closeBtn.addActionListener(e -> closeCurrentTab());

        toolbar.add(runBtn);
        toolbar.add(saveBtn);
        toolbar.add(closeBtn);

        // Create the tabbed pane for multiple files
        editorTabs = new JTabbedPane();
        editorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT); // Scroll tabs if too many

        // Add a welcome tab with instructions
        addWelcomeTab();

        // Add everything to center panel
        centerPanel.add(toolbar, BorderLayout.NORTH);
        centerPanel.add(editorTabs, BorderLayout.CENTER);
    }

    // Add welcome tab with user instructions
    private void addWelcomeTab() {
        JTextArea welcomeEditor = new JTextArea();
        welcomeEditor.setText("Welcome to Node.js IDE!\n\n" +
                "• Double-click files in the explorer to open them\n" +
                "• Use the toolbar to run and save files\n" +
                "• Multiple files open in tabs\n" +
                "• Use the terminal to run Node.js commands");
        welcomeEditor.setFont(new Font("Consolas", Font.PLAIN, 14));
        welcomeEditor.setEditable(false); // Read-only welcome message
        welcomeEditor.setBackground(new Color(30, 30, 30));
        welcomeEditor.setForeground(new Color(200, 200, 200));

        JScrollPane scrollPane = new JScrollPane(welcomeEditor);
        editorTabs.addTab("Welcome", scrollPane);
    }

    // Refresh file tree - reloads directory structure
    private void refreshFileTree() {
        // Clear existing nodes
        rootNode.removeAllChildren();

        // set the root name to current directory
        rootNode.setUserObject(workingDirectory.getName());

        // Load files in background thread so GUI appears immediately
        new Thread(() -> {
            loadDirectory(workingDirectory, rootNode);

            // Update the tree on the GUI thread
            SwingUtilities.invokeLater(() -> {
                treeModel.reload();
                fileTree.expandRow(0); // Expand root node by default
            });
        }).start();
    }

    // Recursively load directory contents into tree nodes
    private void loadDirectory(File dir, DefaultMutableTreeNode node) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                // Only show normal files (not hidden ones that start with dot)
                if (!file.getName().startsWith(".")) {
                    // Create node for this file/folder
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(file.getName());
                    childNode.setUserObject(file); // Store File object for later access
                    node.add(childNode);
                    // If its a directory load its contents too (recursive)
                    if (file.isDirectory()) {
                        loadDirectory(file, childNode);
                    }
                }
            }
        }
    }

    // Open a file in the editor - creates new tab with file content
    private void openFile(File file) {
        try {
            String filePath = file.getAbsolutePath();

            // Check if file is already open to avoid duplicates
            if (openFiles.containsKey(filePath)) {
                // Switch to existing tab instead of opening new one
                EditorTab tab = openFiles.get(filePath);
                editorTabs.setSelectedComponent(tab.scrollPane);
                return;
            }

            // Read the file content from disk
            String content = Files.readString(file.toPath());

            // Create a text area for editing with code-friendly settings
            JTextArea editor = new JTextArea(content);
            editor.setFont(new Font("Consolas", Font.PLAIN, 14));
            editor.setTabSize(2); // Set tab to 2 spaces for better code formatting
            editor.setBackground(new Color(30, 30, 30)); // Dark theme
            editor.setForeground(new Color(200, 200, 200)); // Light text
            editor.setCaretColor(Color.WHITE); // Visible cursor

            // Put editor in scroll pane for large files
            JScrollPane scrollPane = new JScrollPane(editor);

            // Create and store the tab information
            EditorTab tab = new EditorTab(file, editor, scrollPane);
            openFiles.put(filePath, tab);

            // Add tab for this file and switch to it
            editorTabs.addTab(file.getName(), scrollPane);
            editorTabs.setSelectedComponent(scrollPane);

            // Show success message in terminal
            appendToTerminal("[Opened: " + file.getName() + "]\n", Color.GREEN);

        } catch (IOException e) {
            appendToTerminal("[Error opening file: " + e.getMessage() + "]\n", Color.RED);
        }
    }

    // Create a new file with user-specified name
    private void createNewFile() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name:", "newfile.js");
        if (fileName != null && !fileName.trim().isEmpty()) {
            File newFile = new File(workingDirectory, fileName.trim());
            try {
                if (newFile.createNewFile()) {
                    refreshFileTree(); // Update file explorer
                    openFile(newFile); // Open the new file
                    appendToTerminal("[Created: " + fileName + "]\n", Color.GREEN);
                } else {
                    appendToTerminal("[File already exists]\n", Color.RED);
                }
            } catch (IOException e) {
                appendToTerminal("[Error creating file: " + e.getMessage() + "]\n", Color.RED);
            }
        }
    }

    // Save the currently active file
    private void saveCurrentFile() {
        int index = editorTabs.getSelectedIndex();
        if (index == -1) {
            appendToTerminal("[No file open to save]\n", Color.RED);
            return;
        }

        JScrollPane scrollPane = (JScrollPane) editorTabs.getComponentAt(index);

        // Find the tab in our openFiles map
        for (EditorTab tab : openFiles.values()) {
            if (tab.scrollPane == scrollPane) {
                saveFile(tab);
                break;
            }
        }
    }

    // Save file content to disk
    private void saveFile(EditorTab tab) {
        try {
            Files.writeString(tab.file.toPath(), tab.editor.getText());
            appendToTerminal("[Saved: " + tab.file.getName() + "]\n", Color.GREEN);
        } catch (IOException e) {
            appendToTerminal("[Error saving: " + e.getMessage() + "]\n", Color.RED);
        }
    }

    // Delete selected file from file explorer
    private void deleteSelectedFile() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) {
            appendToTerminal("[No file selected]\n", Color.RED);
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (userObject instanceof File) {
            File file = (File) userObject;
            // Confirm deletion with user
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete " + file.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (file.delete()) {
                    // Remove from open files if it's currently open
                    String filePath = file.getAbsolutePath();
                    if (openFiles.containsKey(filePath)) {
                        openFiles.remove(filePath);
                    }
                    refreshFileTree(); // Update the file explorer
                    appendToTerminal("[Deleted: " + file.getName() + "]\n", Color.GREEN);
                } else {
                    appendToTerminal("[Error deleting file]\n", Color.RED);
                }
            }
        }
    }

    // Close current editor tab
    private void closeCurrentTab() {
        int selectedIndex = editorTabs.getSelectedIndex();
        if (selectedIndex != -1) {
            JScrollPane scrollPane = (JScrollPane) editorTabs.getComponentAt(selectedIndex);
            String tabTitle = editorTabs.getTitleAt(selectedIndex);

            // Remove from openFiles map to free memory
            for (Map.Entry<String, EditorTab> entry : openFiles.entrySet()) {
                if (entry.getValue().scrollPane == scrollPane) {
                    openFiles.remove(entry.getKey());
                    break;
                }
            }

            editorTabs.remove(selectedIndex);
            appendToTerminal("[Closed tab: " + tabTitle + "]\n", Color.YELLOW);
        }
    }

    // Helper class to track editor tab information
    class EditorTab {
        File file;
        JTextArea editor;
        JScrollPane scrollPane;

        EditorTab(File file, JTextArea editor, JScrollPane scrollPane) {
            this.file = file;
            this.editor = editor;
            this.scrollPane = scrollPane;
        }
    }

    // Main method - program entry point
    public static void main(String[] args) {
        // Create our window - starts the application
        new NodeJSIDE();
    }
}