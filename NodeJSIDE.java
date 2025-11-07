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


    // Process management and terminal styles
    private Process currentProcess;
    private SimpleAttributeSet normalStyle, errorStyle, successStyle, commandStyle;


    // Constructor - this runs when we create the window
    public NodeJSIDE() {
        setTitle("Node.js IDE"); // Window title
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close button stops program
        setLocationRelativeTo(null); // Center the window on screen


        // Set working directory to user home initially
        workingDirectory = new File(System.getProperty("user.home"));


        // Initialize open files map
        openFiles = new HashMap<>();


        // Initialize terminal styles
        initTerminalStyles();


        // Initialize Components
        initComponents();


        // Create Menu bar
        createMenuBar();
        // Make the window visible
        setVisible(true);
    }


    // Initialize terminal text styles for colored output
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


    // Setup the terminal with command execution capabilities
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
        JButton stopBtn = new JButton("Stop Process");


        // Execute button action - runs commands in terminal
        executeBtn.addActionListener(e -> executeCommand());
        // Clear button action - clears terminal output
        clearBtn.addActionListener(e -> clearTerminal());
        // Stop button - stops currently running process
        stopBtn.addActionListener(e -> stopProcess());


        buttonPanel.add(executeBtn);
        buttonPanel.add(clearBtn);
        buttonPanel.add(stopBtn);


        // For Enter Key - allows executing commands by pressing Enter
        commandField.addActionListener(e -> executeCommand());


        // Add everything to command panel
        commandPanel.add(promptLabel, BorderLayout.WEST);
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(buttonPanel, BorderLayout.EAST);


        // Quick command buttons for common Node.js commands
        JPanel quickCommandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        String[] quickCommands = {"node --version", "npm --version", "npm init -y", "npm install"};


        for (String cmd : quickCommands) {
            JButton quickBtn = new JButton(cmd);
            quickBtn.addActionListener(e -> executeQuickCommand(cmd));
            quickCommandPanel.add(quickBtn);
        }


        // Create top panel for terminal title and quick commands
        JPanel terminalTopPanel = new JPanel(new BorderLayout());
        terminalTopPanel.add(new JLabel("Terminal"), BorderLayout.WEST);
        terminalTopPanel.add(quickCommandPanel, BorderLayout.CENTER);


        // Add welcome message to terminal
        appendToTerminal("=== Node.js Terminal ===\n", successStyle);
        appendToTerminal("Working directory: " + workingDirectory.getAbsolutePath() + "\n", normalStyle);
        appendToTerminal("Type Node.js commands below or use quick buttons\n\n", normalStyle);


        // Add everything to right panel
        rightPanel.add(terminalTopPanel, BorderLayout.NORTH);
        rightPanel.add(terminalScroll, BorderLayout.CENTER);
        rightPanel.add(commandPanel, BorderLayout.SOUTH);
    }


    // Helper method to add styled text to terminal
    private void appendToTerminal(String text, SimpleAttributeSet style) {
        try {
            terminalDoc.insertString(terminalDoc.getLength(), text, style);
            terminalArea.setCaretPosition(terminalDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    // Overloaded method for backward compatibility with color parameter
    private void appendToTerminal(String text, Color color) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        StyleConstants.setForeground(style, color);
        appendToTerminal(text, style);
    }


    // Execute command from terminal input
    private void executeCommand() {
        String command = commandField.getText().trim();
        if (command.isEmpty()) return;
        
        appendToTerminal("\n$ " + command + "\n", commandStyle);
        commandField.setText(""); // Clear input field
        
        // Execute command in background thread to keep GUI responsive
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder();
                String os = System.getProperty("os.name").toLowerCase();
                
                // Set command based on operating system
                if (os.contains("win")) {
                    pb.command("cmd.exe", "/c", command);
                } else {
                    pb.command("sh", "-c", command);
                }
                
                pb.directory(workingDirectory); // Set working directory
                pb.redirectErrorStream(true); // Combine stdout and stderr
                
                currentProcess = pb.start();
                
                // Read command output line by line
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream())
                );
                
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line + "\n";
                    SwingUtilities.invokeLater(() -> appendToTerminal(output, normalStyle));
                }
                
                // Wait for process to complete and get exit code
                int exitCode = currentProcess.waitFor();
                
                // Show completion message based on exit code
                SwingUtilities.invokeLater(() -> {
                    if (exitCode == 0) {
                        appendToTerminal("[✓ Process completed]\n", successStyle);
                    } else {
                        appendToTerminal("[✗ Process exited with code: " + exitCode + "]\n", errorStyle);
                    }
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    appendToTerminal("[Error: " + e.getMessage() + "]\n", errorStyle)
                );
            }
        }).start();
    }


    // Execute quick commands from buttons
    private void executeQuickCommand(String command) {
        commandField.setText(command);
        executeCommand();
    }


    // Stop currently running process
    private void stopProcess() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            appendToTerminal("\n[Process stopped by user]\n", errorStyle);
        } else {
            appendToTerminal("[No process running]\n", errorStyle);
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
        newFolderItem.addActionListener(e -> createNewFolder());
        openDirItem.addActionListener(e -> changeWorkingDirectory());
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
        renameItem.addActionListener(e -> renameSelectedFile());
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
        runFileItem.addActionListener(e -> runCurrentFile());


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
            appendToTerminal("Terminal cleared\n", successStyle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Change working directory - this is the key fix for your issue
    private void changeWorkingDirectory() {
        JFileChooser chooser = new JFileChooser(workingDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Working Directory");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            // Update working directory
            workingDirectory = chooser.getSelectedFile();
            
            // Completely rebuild the tree with the new directory as the absolute root
            rootNode.removeAllChildren();
            rootNode.setUserObject(new FileNode(workingDirectory.getName(), workingDirectory, true));
            
            // Reload the tree
            refreshFileTree();
            
            appendToTerminal("\n[Working directory changed to: " + workingDirectory.getAbsolutePath() + "]\n\n", successStyle);
        }
    }


    // Create new folder
    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (folderName != null && !folderName.trim().isEmpty()) {
            File newFolder = new File(workingDirectory, folderName.trim());
            if (newFolder.mkdir()) {
                refreshFileTree();
                appendToTerminal("[Created folder: " + folderName + "]\n", successStyle);
            } else {
                appendToTerminal("[Error creating folder]\n", errorStyle);
            }
        }
    }


    // Rename selected file
    private void renameSelectedFile() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) {
            appendToTerminal("[No file selected]\n", errorStyle);
            return;
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        
        if (userObject instanceof FileNode) {
            FileNode fileNode = (FileNode) userObject;
            File file = fileNode.getFile();
            String newName = JOptionPane.showInputDialog(this, "New name:", file.getName());
            
            if (newName != null && !newName.trim().isEmpty()) {
                File newFile = new File(file.getParent(), newName.trim());
                if (file.renameTo(newFile)) {
                    refreshFileTree();
                    appendToTerminal("[Renamed to: " + newName + "]\n", successStyle);
                } else {
                    appendToTerminal("[Error renaming file]\n", errorStyle);
                }
            }
        }
    }


    // Setup file explorer with tree view and buttons - shows only filenames
    private void setupFileExplorer() {
        // Remove the previous simple setup
        leftPanel.setLayout(new BorderLayout());


        // Create the root node for our file tree - starts with working directory name only
        rootNode = new DefaultMutableTreeNode(new FileNode(workingDirectory.getName(), workingDirectory, true));


        // Create tree model to manage the tree structure
        treeModel = new DefaultTreeModel(rootNode);


        // Create the actual tree components
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);


        // Custom tree cell renderer to show only filenames
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object userObject = node.getUserObject();
                    
                    if (userObject instanceof FileNode) {
                        FileNode fileNode = (FileNode) userObject;
                        setText(fileNode.getName()); // Show only filename
                        
                        // Set appropriate icons
                        if (fileNode.isDirectory()) {
                            setIcon(expanded ? UIManager.getIcon("Tree.openIcon") : UIManager.getIcon("Tree.closedIcon"));
                        } else {
                            setIcon(UIManager.getIcon("Tree.leafIcon"));
                        }
                    }
                }
                return this;
            }
        });


        // Double Click to open files - main file interaction
        fileTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click detection
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object userObject = node.getUserObject(); // Get the associated File object
                        if (userObject instanceof FileNode) {
                            FileNode fileNode = (FileNode) userObject;
                            if (!fileNode.isDirectory()) { // Only open files, not folders
                                openFile(fileNode.getFile());
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
        runBtn.addActionListener(e -> runCurrentFile());
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


    // Run current JavaScript file with Node.js
    private void runCurrentFile() {
        int index = editorTabs.getSelectedIndex();
        if (index == -1) {
            appendToTerminal("[No file open to run]\n", errorStyle);
            return;
        }
        
        JScrollPane scrollPane = (JScrollPane) editorTabs.getComponentAt(index);
        
        // Find the current file and run it
        for (EditorTab tab : openFiles.values()) {
            if (tab.scrollPane == scrollPane) {
                saveFile(tab); // Save file before running
                executeQuickCommand("node " + tab.file.getName());
                break;
            }
        }
    }


    // Add welcome tab with user instructions
    private void addWelcomeTab() {
        JTextArea welcomeEditor = new JTextArea();
        welcomeEditor.setText("Welcome to Node.js IDE!\n\n" +
                "• Double-click files in the explorer to open them\n" +
                "• Use Run button to execute current JavaScript file\n" +
                "• Use terminal to run Node.js and npm commands\n" +
                "• Quick commands: node --version, npm --version, etc.\n" +
                "• Stop running processes with Stop Process button\n" +
                "• Change working directory from File menu\n" +
                "• Create new files and folders with File menu\n" +
                "• Save files with Save button or Ctrl+S\n" +
                "• Refresh file tree to see external changes");
        welcomeEditor.setFont(new Font("Consolas", Font.PLAIN, 14));
        welcomeEditor.setEditable(false);
        welcomeEditor.setBackground(new Color(30, 30, 30));
        welcomeEditor.setForeground(new Color(200, 200, 200));


        JScrollPane scrollPane = new JScrollPane(welcomeEditor);
        editorTabs.addTab("Welcome", scrollPane);
    }


    // Refresh file tree - reloads directory structure from the working directory only
    private void refreshFileTree() {
        // Clear existing child nodes (but keep the root as is)
        rootNode.removeAllChildren();


        // Ensure root represents the current working directory
        rootNode.setUserObject(new FileNode(workingDirectory.getName(), workingDirectory, true));


        // Load files in background thread so GUI stays responsive
        new Thread(() -> {
            // Load only the contents of the working directory, not parent directories
            loadDirectory(workingDirectory, rootNode);


            // Update the tree on the GUI thread
            SwingUtilities.invokeLater(() -> {
                treeModel.reload();
                fileTree.expandRow(0); // Expand root node by default
            });
        }).start();
    }


    // Recursively load directory contents into tree nodes - only subdirectories of working directory
    private void loadDirectory(File dir, DefaultMutableTreeNode node) {
        File[] files = dir.listFiles();
        if (files != null) {
            // Sort files for better organization - directories first, then files alphabetically
            Arrays.sort(files, (f1, f2) -> {
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });
            
            for (File file : files) {
                // Only show normal files (not hidden ones that start with dot)
                // Also skip node_modules for cleaner view
                if (!file.getName().startsWith(".") && !file.getName().equals("node_modules")) {
                    // Create node for this file/folder using FileNode wrapper
                    FileNode fileNode = new FileNode(file.getName(), file, file.isDirectory());
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(fileNode);
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
            appendToTerminal("[Opened: " + file.getName() + "]\n", successStyle);
            
        } catch (IOException e) {
            appendToTerminal("[Error opening file: " + e.getMessage() + "]\n", errorStyle);
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
                    appendToTerminal("[Created: " + fileName + "]\n", successStyle);
                } else {
                    appendToTerminal("[File already exists]\n", errorStyle);
                }
            } catch (IOException e) {
                appendToTerminal("[Error creating file: " + e.getMessage() + "]\n", errorStyle);
            }
        }
    }


    // Save the currently active file
    private void saveCurrentFile() {
        int index = editorTabs.getSelectedIndex();
        if (index == -1) {
            appendToTerminal("[No file open to save]\n", errorStyle);
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
            appendToTerminal("[Saved: " + tab.file.getName() + "]\n", successStyle);
        } catch (IOException e) {
            appendToTerminal("[Error saving: " + e.getMessage() + "]\n", errorStyle);
        }
    }


    // Delete selected file from file explorer
    private void deleteSelectedFile() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) {
            appendToTerminal("[No file selected]\n", errorStyle);
            return;
        }
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();
        
        if (userObject instanceof FileNode) {
            FileNode fileNode = (FileNode) userObject;
            File file = fileNode.getFile();
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
                    appendToTerminal("[Deleted: " + file.getName() + "]\n", successStyle);
                } else {
                    appendToTerminal("[Error deleting file]\n", errorStyle);
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


    // Helper class to store file information while displaying only the name
    class FileNode {
        private String name;
        private File file;
        private boolean isDirectory;
        
        public FileNode(String name, File file, boolean isDirectory) {
            this.name = name;
            this.file = file;
            this.isDirectory = isDirectory;
        }
        
        public String getName() {
            return name;
        }
        
        public File getFile() {
            return file;
        }
        
        public boolean isDirectory() {
            return isDirectory;
        }
        
        @Override
        public String toString() {
            return name; // This ensures only the filename is displayed in the tree
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
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Create our window - starts the application
        SwingUtilities.invokeLater(() -> {
            new NodeJSIDE();
        });
    }
}
