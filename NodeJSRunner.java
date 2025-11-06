import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class NodeJSRunner extends JFrame {
    private JTree fileTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JTabbedPane editorTabs;
    private JTextPane terminalArea;
    private JTextField commandField;
    private File workingDirectory;
    private Process currentProcess;
    private StyledDocument terminalDoc;
    private Map<String, EditorTab> openFiles;
    
    // Styles for terminal
    private SimpleAttributeSet normalStyle, errorStyle, successStyle, commandStyle;

    public NodeJSRunner() {
        setTitle("Node.js IDE - Full Development Environment");
        setSize(1400, 900);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        openFiles = new HashMap<>();

        initStyles();
        
        // Ask for directory first
        if (!selectWorkingDirectory()) {
            JOptionPane.showMessageDialog(this, "No directory selected. Application will exit.");
            System.exit(0);
            return;
        }
        
        initComponents();
        setVisible(true);
        
        appendToTerminal("=== Node.js IDE Initialized ===\n", successStyle);
        appendToTerminal("Working directory: " + workingDirectory.getAbsolutePath() + "\n\n", normalStyle);
    }

    private boolean selectWorkingDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Working Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        
        // Set default to user home
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            workingDirectory = chooser.getSelectedFile();
            return true;
        }
        return false;
    }

    private void initStyles() {
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
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create menu bar
        createMenuBar();

        // Left panel - File Explorer
        JPanel leftPanel = createFileExplorer();
        
        // Center panel - Editor
        JPanel centerPanel = createEditorPanel();
        
        // Right panel - Terminal
        JPanel rightPanel = createTerminalPanel();

        // Split panes
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, centerPanel);
        mainSplit.setDividerLocation(250);
        
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, rightPanel);
        verticalSplit.setDividerLocation(550);

        mainPanel.add(verticalSplit, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem newFileItem = new JMenuItem("New File");
        JMenuItem newFolderItem = new JMenuItem("New Folder");
        JMenuItem openDirItem = new JMenuItem("Open Directory");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAllItem = new JMenuItem("Save All");
        JMenuItem closeTabItem = new JMenuItem("Close Tab");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        newFileItem.addActionListener(e -> createNewFile());
        newFolderItem.addActionListener(e -> createNewFolder());
        openDirItem.addActionListener(e -> openDirectory());
        saveItem.addActionListener(e -> saveCurrentFile());
        saveAllItem.addActionListener(e -> saveAllFiles());
        closeTabItem.addActionListener(e -> closeCurrentTab());
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(newFileItem);
        fileMenu.add(newFolderItem);
        fileMenu.addSeparator();
        fileMenu.add(openDirItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAllItem);
        fileMenu.add(closeTabItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem deleteItem = new JMenuItem("Delete File/Folder");
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem refreshItem = new JMenuItem("Refresh Explorer");
        
        deleteItem.addActionListener(e -> deleteSelected());
        renameItem.addActionListener(e -> renameSelected());
        refreshItem.addActionListener(e -> refreshFileTree());
        
        editMenu.add(deleteItem);
        editMenu.add(renameItem);
        editMenu.addSeparator();
        editMenu.add(refreshItem);
        
        // Terminal Menu
        JMenu terminalMenu = new JMenu("Terminal");
        JMenuItem clearTerminal = new JMenuItem("Clear Terminal");
        JMenuItem stopProcess = new JMenuItem("Stop Process");
        
        clearTerminal.addActionListener(e -> clearTerminal());
        stopProcess.addActionListener(e -> stopProcess());
        
        terminalMenu.add(clearTerminal);
        terminalMenu.add(stopProcess);
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(terminalMenu);
        
        setJMenuBar(menuBar);
    }

    private JPanel createFileExplorer() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("File Explorer"));
        
        rootNode = new DefaultMutableTreeNode(new FileNode(workingDirectory.getName(), workingDirectory, true));
        treeModel = new DefaultTreeModel(rootNode);
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        
        // Custom renderer to show only filenames
        fileTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object userObject = node.getUserObject();
                    
                    if (userObject instanceof FileNode) {
                        FileNode fileNode = (FileNode) userObject;
                        setText(fileNode.getName());
                        
                        // Set different icons for files and folders
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
        
        // Double-click to open file
        fileTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        openFileFromTree(path);
                    }
                }
            }
        });
        
        // Right-click context menu
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem newFileHere = new JMenuItem("New File Here");
        JMenuItem newFolderHere = new JMenuItem("New Folder Here");
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem renameItem = new JMenuItem("Rename");
        
        openItem.addActionListener(e -> {
            TreePath path = fileTree.getSelectionPath();
            if (path != null) openFileFromTree(path);
        });
        newFileHere.addActionListener(e -> createNewFile());
        newFolderHere.addActionListener(e -> createNewFolder());
        deleteItem.addActionListener(e -> deleteSelected());
        renameItem.addActionListener(e -> renameSelected());
        
        contextMenu.add(openItem);
        contextMenu.addSeparator();
        contextMenu.add(newFileHere);
        contextMenu.add(newFolderHere);
        contextMenu.addSeparator();
        contextMenu.add(deleteItem);
        contextMenu.add(renameItem);
        
        fileTree.setComponentPopupMenu(contextMenu);
        
        JScrollPane scrollPane = new JScrollPane(fileTree);
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("🔄");
        JButton newFileBtn = new JButton("📄");
        JButton newFolderBtn = new JButton("📁");
        
        refreshBtn.setToolTipText("Refresh");
        newFileBtn.setToolTipText("New File");
        newFolderBtn.setToolTipText("New Folder");
        
        refreshBtn.addActionListener(e -> refreshFileTree());
        newFileBtn.addActionListener(e -> createNewFile());
        newFolderBtn.addActionListener(e -> createNewFolder());
        
        topPanel.add(refreshBtn);
        topPanel.add(newFileBtn);
        topPanel.add(newFolderBtn);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        refreshFileTree();
        
        return panel;
    }

    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        editorTabs = new JTabbedPane();
        editorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton runBtn = new JButton("▶ Run");
        JButton saveBtn = new JButton("💾 Save");
        JButton saveAllBtn = new JButton("💾 Save All");
        JButton closeBtn = new JButton("✖ Close");
        
        runBtn.addActionListener(e -> runCurrentFile());
        saveBtn.addActionListener(e -> saveCurrentFile());
        saveAllBtn.addActionListener(e -> saveAllFiles());
        closeBtn.addActionListener(e -> closeCurrentTab());
        
        toolbar.add(runBtn);
        toolbar.add(saveBtn);
        toolbar.add(saveAllBtn);
        toolbar.add(closeBtn);
        
        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(editorTabs, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createTerminalPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Terminal area
        terminalArea = new JTextPane();
        terminalArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        terminalArea.setEditable(false);
        terminalArea.setBackground(new Color(20, 20, 20));
        terminalArea.setCaretColor(Color.WHITE);
        terminalDoc = terminalArea.getStyledDocument();
        JScrollPane terminalScroll = new JScrollPane(terminalArea);
        
        // Command input
        JPanel cmdPanel = new JPanel(new BorderLayout(5, 5));
        JLabel prompt = new JLabel("$");
        prompt.setForeground(Color.GREEN);
        commandField = new JTextField();
        commandField.setFont(new Font("Consolas", Font.PLAIN, 13));
        commandField.setBackground(new Color(30, 30, 30));
        commandField.setForeground(Color.WHITE);
        commandField.setCaretColor(Color.WHITE);
        commandField.addActionListener(e -> executeCommand());
        
        JButton execBtn = new JButton("Execute");
        JButton clearBtn = new JButton("Clear");
        JButton stopBtn = new JButton("Stop");
        
        execBtn.addActionListener(e -> executeCommand());
        clearBtn.addActionListener(e -> clearTerminal());
        stopBtn.addActionListener(e -> stopProcess());
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPanel.add(execBtn);
        btnPanel.add(clearBtn);
        btnPanel.add(stopBtn);
        
        cmdPanel.add(prompt, BorderLayout.WEST);
        cmdPanel.add(commandField, BorderLayout.CENTER);
        cmdPanel.add(btnPanel, BorderLayout.EAST);
        
        // Quick commands
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        String[] commands = {"npm init -y", "npm install", "npm install express", 
                            "node --version", "npm --version", "npm list"};
        
        for (String cmd : commands) {
            JButton btn = new JButton(cmd);
            btn.addActionListener(e -> {
                if (cmd.equals("npm install") && !cmd.equals(((JButton)e.getSource()).getText())) {
                    String pkg = JOptionPane.showInputDialog(this, "Package name:");
                    if (pkg != null && !pkg.trim().isEmpty()) {
                        executeQuickCommand("npm install " + pkg.trim());
                    }
                } else {
                    executeQuickCommand(cmd);
                }
            });
            quickPanel.add(btn);
        }
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Terminal"), BorderLayout.WEST);
        topPanel.add(quickPanel, BorderLayout.CENTER);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(terminalScroll, BorderLayout.CENTER);
        panel.add(cmdPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private void refreshFileTree() {
        rootNode.removeAllChildren();
        rootNode.setUserObject(new FileNode(workingDirectory.getName(), workingDirectory, true));
        loadDirectory(workingDirectory, rootNode);
        treeModel.reload();
        fileTree.expandRow(0);
    }

    private void loadDirectory(File dir, DefaultMutableTreeNode node) {
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
            
            for (File file : files) {
                if (!file.getName().startsWith(".") && !file.getName().equals("node_modules")) {
                    FileNode fileNode = new FileNode(file.getName(), file, file.isDirectory());
                    DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(fileNode);
                    node.add(childNode);
                    
                    if (file.isDirectory()) {
                        loadDirectory(file, childNode);
                    }
                }
            }
        }
    }

    private void openDirectory() {
        JFileChooser chooser = new JFileChooser(workingDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            workingDirectory = chooser.getSelectedFile();
            refreshFileTree();
            appendToTerminal("\n[Directory changed to: " + workingDirectory.getAbsolutePath() + "]\n\n", successStyle);
        }
    }

    private void openFileFromTree(TreePath path) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = node.getUserObject();
        
        if (obj instanceof FileNode) {
            FileNode fileNode = (FileNode) obj;
            if (!fileNode.isDirectory()) {
                openFile(fileNode.getFile());
            }
        }
    }

    private void openFile(File file) {
        String filePath = file.getAbsolutePath();
        
        if (openFiles.containsKey(filePath)) {
            int index = editorTabs.indexOfComponent(openFiles.get(filePath).scrollPane);
            editorTabs.setSelectedIndex(index);
            return;
        }
        
        try {
            String content = Files.readString(file.toPath());
            
            JTextArea editor = new JTextArea(content);
            editor.setFont(new Font("Consolas", Font.PLAIN, 14));
            editor.setTabSize(2);
            editor.setBackground(new Color(30, 30, 30));
            editor.setForeground(new Color(200, 200, 200));
            editor.setCaretColor(Color.WHITE);
            
            JScrollPane scrollPane = new JScrollPane(editor);
            
            EditorTab tab = new EditorTab(file, editor, scrollPane);
            openFiles.put(filePath, tab);
            
            editorTabs.addTab(file.getName(), scrollPane);
            editorTabs.setSelectedComponent(scrollPane);
            
            appendToTerminal("[Opened: " + file.getName() + "]\n", successStyle);
            
        } catch (IOException e) {
            appendToTerminal("[Error opening file: " + e.getMessage() + "]\n", errorStyle);
        }
    }

    private void createNewFile() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name:", "newfile.js");
        if (fileName != null && !fileName.trim().isEmpty()) {
            File newFile = new File(workingDirectory, fileName.trim());
            try {
                if (newFile.createNewFile()) {
                    refreshFileTree();
                    openFile(newFile);
                    appendToTerminal("[Created: " + fileName + "]\n", successStyle);
                } else {
                    appendToTerminal("[File already exists]\n", errorStyle);
                }
            } catch (IOException e) {
                appendToTerminal("[Error creating file: " + e.getMessage() + "]\n", errorStyle);
            }
        }
    }

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

    private void saveCurrentFile() {
        int index = editorTabs.getSelectedIndex();
        if (index == -1) return;
        
        JScrollPane scrollPane = (JScrollPane) editorTabs.getComponentAt(index);
        
        for (EditorTab tab : openFiles.values()) {
            if (tab.scrollPane == scrollPane) {
                saveFile(tab);
                break;
            }
        }
    }

    private void saveAllFiles() {
        for (EditorTab tab : openFiles.values()) {
            saveFile(tab);
        }
        appendToTerminal("[All files saved]\n", successStyle);
    }

    private void saveFile(EditorTab tab) {
        try {
            Files.writeString(tab.file.toPath(), tab.editor.getText());
            appendToTerminal("[Saved: " + tab.file.getName() + "]\n", successStyle);
        } catch (IOException e) {
            appendToTerminal("[Error saving: " + e.getMessage() + "]\n", errorStyle);
        }
    }

    private void closeCurrentTab() {
        int index = editorTabs.getSelectedIndex();
        if (index == -1) return;
        
        JScrollPane scrollPane = (JScrollPane) editorTabs.getComponentAt(index);
        
        for (Map.Entry<String, EditorTab> entry : openFiles.entrySet()) {
            if (entry.getValue().scrollPane == scrollPane) {
                openFiles.remove(entry.getKey());
                break;
            }
        }
        
        editorTabs.remove(index);
    }

    private void deleteSelected() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) return;
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = node.getUserObject();
        
        if (obj instanceof FileNode) {
            FileNode fileNode = (FileNode) obj;
            File file = fileNode.getFile();
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Delete " + file.getName() + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                if (deleteRecursive(file)) {
                    refreshFileTree();
                    appendToTerminal("[Deleted: " + file.getName() + "]\n", successStyle);
                } else {
                    appendToTerminal("[Error deleting file]\n", errorStyle);
                }
            }
        }
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteRecursive(f);
                }
            }
        }
        return file.delete();
    }

    private void renameSelected() {
        TreePath path = fileTree.getSelectionPath();
        if (path == null) return;
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object obj = node.getUserObject();
        
        if (obj instanceof FileNode) {
            FileNode fileNode = (FileNode) obj;
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

    private void runCurrentFile() {
        int index = editorTabs.getSelectedIndex();
        if (index == -1) {
            appendToTerminal("[No file open]\n", errorStyle);
            return;
        }
        
        JScrollPane scrollPane = (JScrollPane) editorTabs.getComponentAt(index);
        
        for (EditorTab tab : openFiles.values()) {
            if (tab.scrollPane == scrollPane) {
                saveFile(tab);
                executeQuickCommand("node " + tab.file.getName());
                break;
            }
        }
    }

    private void executeQuickCommand(String command) {
        commandField.setText(command);
        executeCommand();
    }

    private void executeCommand() {
        String command = commandField.getText().trim();
        if (command.isEmpty()) return;
        
        appendToTerminal("\n$ " + command + "\n", commandStyle);
        commandField.setText("");
        
        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder();
                String os = System.getProperty("os.name").toLowerCase();
                
                if (os.contains("win")) {
                    pb.command("cmd.exe", "/c", command);
                } else {
                    pb.command("sh", "-c", command);
                }
                
                pb.directory(workingDirectory);
                pb.redirectErrorStream(true);
                
                currentProcess = pb.start();
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(currentProcess.getInputStream())
                );
                
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line + "\n";
                    SwingUtilities.invokeLater(() -> appendToTerminal(output, normalStyle));
                }
                
                int exitCode = currentProcess.waitFor();
                
                SwingUtilities.invokeLater(() -> {
                    if (exitCode == 0) {
                        appendToTerminal("[✓ Done]\n", successStyle);
                    } else {
                        appendToTerminal("[✗ Exit code: " + exitCode + "]\n", errorStyle);
                    }
                    refreshFileTree();
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> 
                    appendToTerminal("[Error: " + e.getMessage() + "]\n", errorStyle)
                );
            }
        }).start();
    }

    private void stopProcess() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            appendToTerminal("\n[Process terminated]\n", errorStyle);
        }
    }

    private void clearTerminal() {
        try {
            terminalDoc.remove(0, terminalDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendToTerminal(String text, SimpleAttributeSet style) {
        try {
            terminalDoc.insertString(terminalDoc.getLength(), text, style);
            terminalArea.setCaretPosition(terminalDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
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

    public static void main(String[] args) {
        System.out.println("Starting Node.js IDE...");
        
        // Set look and feel first
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Check Node.js in background
        SwingUtilities.invokeLater(() -> {
            System.out.println("Checking Node.js installation...");
            try {
                ProcessBuilder pb = new ProcessBuilder();
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    pb.command("cmd.exe", "/c", "node --version");
                } else {
                    pb.command("sh", "-c", "node --version");
                }
                Process p = pb.start();
                p.waitFor();
                System.out.println("Node.js found!");
            } catch (Exception e) {
                System.out.println("Node.js check failed: " + e.getMessage());
                JOptionPane.showMessageDialog(null, 
                    "Warning: Node.js might not be installed!\n\nInstall from: https://nodejs.org\n\nIDE will still open.",
                    "Node.js Check", JOptionPane.WARNING_MESSAGE);
            }
            
            System.out.println("Creating IDE window...");
            new NodeJSRunner();
            System.out.println("IDE window created!");
        });
    }
}