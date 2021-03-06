/*
 * CHIMP 1.2.0 - Cyber Helper Internet Monkey Program
 * Copyright (C) 2001-2015 Bill Havanki
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package havanki.chimp;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;
import com.apple.eawt.Application;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.QuitEvent;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import java.io.*;
import java.util.logging.Logger;

import org.w3c.dom.*;
import org.xml.sax.*;

public class Chimp extends JFrame implements ActionListener, MouseListener {

  private static final Logger LOG = Logger.getLogger(Chimp.class.getName());

  private static final String NEW = "New";
  private static final String OPEN = "Open...";
  private static final String SAVE = "Save";
  private static final String SAVE_AS = "Save As...";
  private static final String EXIT = "Exit";

  private static final String ADD = "Add New Item...";
  private static final String MODIFY = "Modify...";
  private static final String REMOVE = "Remove";
  private static final String COPY = "Copy Item to Clipboard";
  private static final String CLEAR = "Clear Clipboard";

  private static final String HP_MODE = "Hide Passwords";

  private static final String ABOUT = "About";

  static final Font OPEN_SANS_REGULAR;
  static final Font OPEN_SANS_REGULAR_16;

  static {
    InputStream in = Chimp.class.getResourceAsStream("/OpenSans-Regular.ttf");
    if (in == null) {
      LOG.warning("Cannot find Open Sans font");
      OPEN_SANS_REGULAR = null;
    } else {
      Font openSansRegular = null;
      try {
        openSansRegular = Font.createFont(Font.TRUETYPE_FONT, in);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(openSansRegular);
      } catch (IOException|FontFormatException e) {
        LOG.warning("Failed to load Open Sans font: " + e.getMessage());
        openSansRegular = null;
      } finally {
        try { in.close(); } catch (IOException e) {}
      }

      OPEN_SANS_REGULAR = openSansRegular;
    }

    if (OPEN_SANS_REGULAR != null) {
      OPEN_SANS_REGULAR_16 = OPEN_SANS_REGULAR.deriveFont(16.0f);
    } else {
      OPEN_SANS_REGULAR_16 = null;
    }
  }

  RecentFileList recentFiles = RecentFileList.loadFromPreferences();
  JMenuItem openMenuItem;
  JButton openButton;
  JMenu recentMenu;
  JCheckBoxMenuItem hpmodeItem;
  JList itemList;

  File currFile = null;
  SecureItemTable tbl = null;
  boolean hidePasswords = true;

  private void fillList() {
    if (tbl == null) {
      itemList.setListData(new String[0]);
    } else {
      String[] titles = tbl.getTitlesInOrder();
      itemList.setListData(titles);
    }
    itemList.invalidate();
    return;
  }

  private void setDocumentModifiedFlag(SecureItemTable table) {
    getRootPane().putClientProperty("Window.documentModified",
                                    Boolean.valueOf(table.isDirty()));
  }

  private static final String ABOUT_HTML =
      "<html>" +
      "<div class='text-align: center'>" +
      "<h2>CHIMP</h2>" +
      "Because everything is better with monkeys.<br><br>" +
      "Copyright (c) 2001-2015 Bill Havanki<br><br>" +
      "Use of CHIMP is subject to the GNU General Public License Version 2.<br>" +
      "See gpl.txt or visit <a href='http://www.gnu.org/'>www.gnu.org</a> to view it." +
      "</div>" +
      "</html>";

  public void actionPerformed(ActionEvent e) {
    this.repaint();
    String command = e.getActionCommand();

    if (command.equals(EXIT)) {
      if (okToExit()) {
        System.exit(0);
      }
    }

    if (command.equals(ABOUT)) {
      JOptionPane.showMessageDialog(this, ABOUT_HTML,
                                    "About CHIMP", JOptionPane.PLAIN_MESSAGE,
                                    loadImageIconAsResource("face-monkey.png"));
      return;
    }
    if (command.equals(HP_MODE)) {
      hidePasswords = hpmodeItem.isSelected();
    }

    if (command.equals(NEW)) {
      if (tbl != null) {
        int rc = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to start over?",
            "Confirm", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (rc == JOptionPane.NO_OPTION) return;  // = abort
      }

      currFile = null;
      tbl = new SecureItemTable();
      setDocumentModifiedFlag(tbl);

      fillList();
    }

    if (command.equals(OPEN)) {
      if (tbl != null && tbl.isDirty()) {
        int rc = JOptionPane.showConfirmDialog(this,
            "There are unsaved changes. Are you sure you want to open a new file?",
            "Confirm", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (rc == JOptionPane.NO_OPTION) return;  // abort
      }

      this.repaint();

      if (e.getSource().equals(openMenuItem) ||
          e.getSource().equals(openButton)) {
        FileDialog fdialog =
            new FileDialog(this, "Open a password file", FileDialog.LOAD);
        fdialog.show();
        String chosenFile = fdialog.getFile();
        if (chosenFile == null) { return; }
        currFile = new File(fdialog.getDirectory() +
                            System.getProperty("file.separator") + chosenFile);
      } else {
        String path = ((JMenuItem) e.getSource()).getText();
        currFile = new File(path);
      }

      PassFileReader pfr = new PassFileReader(currFile);

      do {
        PasswordDialog pd = new PasswordDialog(this, "Decrypting file:", false);
        char[] password = pd.getPassword();
        if (password == null) return;

        try {
          tbl = pfr.read(password);
          PasswordDialog.cleanCharArray(password);
          if (tbl == null) {
            JOptionPane.showMessageDialog(this,
                "I think you entered the wrong password. Try again.",
                "Parsing Error", JOptionPane.ERROR_MESSAGE);
          }
        } catch (IOException exc) {
          LOG.severe("Error reading: " + exc);
          exc.printStackTrace(System.err);
        }
      } while (tbl == null);

      setDocumentModifiedFlag(tbl);
      fillList();

      recentFiles.add(currFile);
      buildRecentMenu();
      recentFiles.saveToPreferences();
    }

    if (command.equals(SAVE) || command.equals(SAVE_AS)) {
      if (command.equals(SAVE_AS) || currFile == null) {
        FileDialog fdialog = new FileDialog(this, "Save a password file",
                                            FileDialog.SAVE);
        fdialog.show();
        String chosenFile = fdialog.getFile();
        if (chosenFile == null) { return; }
        currFile = new File(fdialog.getDirectory() +
                            System.getProperty("file.separator") + chosenFile);
      }
      PassFileWriter pfw = new PassFileWriter(currFile);

      PasswordDialog pd = new PasswordDialog(this, "Encrypting file:", true);
      char[] password = pd.getPassword();
      if (password == null) return;

      try {
        pfw.write(tbl, password);
        setDocumentModifiedFlag(tbl);

        recentFiles.add(currFile);
        buildRecentMenu();
        recentFiles.saveToPreferences();
      } catch (IOException exc) {
        LOG.severe("Error writing: " + exc);
      }
      PasswordDialog.cleanCharArray (password);
    }

    // - - - - -

    if (command.equals(ADD) || command.equals(MODIFY)) {
      // Get the item. ADD = new item, MODIFY = selected item if any.
      SecureItem item;
      String selectedTitle = null;

      if (command.equals(ADD)) {
        item = new SecureItemBuilder("New item").build();
      } else {
        selectedTitle = (String) itemList.getSelectedValue();
        if (selectedTitle == null) return;
        item = tbl.get(selectedTitle);
      }

      // Do the dialog.
      ItemEdit editDialog = new ItemEdit(this, item, tbl, command.equals(ADD),
                                         hidePasswords);
      setDocumentModifiedFlag(tbl);
      fillList();
    }

    if (command.equals(REMOVE)) {
      String selectedTitle = (String) itemList.getSelectedValue();
      if (selectedTitle == null) return;

      int rc = JOptionPane.showConfirmDialog(this,
          "Are you sure you want to delete \"" + selectedTitle + "\"?",
          "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      if (rc == JOptionPane.NO_OPTION) return;  // = abort

      tbl.remove(selectedTitle);
      setDocumentModifiedFlag(tbl);

      fillList();
    }

    // - - - - -

    if (command.equals(COPY)) {
      String selectedTitle = (String) itemList.getSelectedValue();
      if (selectedTitle == null) return;

      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      SecureItem item = tbl.get(selectedTitle);

      StringSelection ss =
          new StringSelection(new String(item.getPassword()));
      cb.setContents(ss, ss);
    }
    if (command.equals(CLEAR)) {
      Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
      StringSelection ss = new StringSelection("");
      cb.setContents(ss, ss);
    }
  }

  private boolean okToExit() {
    if (tbl != null && tbl.isDirty()) {
      int rc = JOptionPane.showConfirmDialog(this,
          "There are unsaved changes. Are you sure you want to quit?",
          "Confirm", JOptionPane.YES_NO_OPTION,
          JOptionPane.WARNING_MESSAGE);
      return rc == JOptionPane.YES_OPTION;
    }
    return true;
  }

  public void mouseClicked(MouseEvent e)
  {
    // This is just looking for double clicks in the item list.
    if (e.getClickCount() == 2) {
      // Simulate a MODIFY action.
      ActionEvent ae = new ActionEvent(itemList, -1, MODIFY);
      actionPerformed(ae);
    }
  }
  public void mousePressed(MouseEvent e) {}
  public void mouseReleased(MouseEvent e) {}
  public void mouseEntered(MouseEvent e) {}
  public void mouseExited(MouseEvent e) {}

  public Chimp() {
    super("CHIMP!");

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    Container cp = getContentPane();
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());

    // OS DETECTION
    String osString = System.getProperty("os.name");
    boolean imAMac =
        osString.toLowerCase(java.util.Locale.US).indexOf("mac") != -1;
    if (imAMac) {
      System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    // MENU
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);

    // MENU - File
    JMenu menu = new JMenu("File");
    menu.setMnemonic(KeyEvent.VK_F);
    menuBar.add(menu);

    JMenuItem menuItem = new JMenuItem(NEW, KeyEvent.VK_N);
    setAccelerator(menuItem, KeyEvent.VK_N, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(NEW);
    menu.add(menuItem);
    openMenuItem = new JMenuItem(OPEN, KeyEvent.VK_O);
    setAccelerator(openMenuItem, KeyEvent.VK_O, imAMac);
    openMenuItem.addActionListener(this);
    openMenuItem.setActionCommand(OPEN);
    menu.add(openMenuItem);

    recentMenu = new JMenu("Open recent");
    menu.add(recentMenu);
    buildRecentMenu();

    menuItem = new JMenuItem(SAVE, KeyEvent.VK_S);
    setAccelerator(menuItem, KeyEvent.VK_S, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(SAVE);
    menu.add(menuItem);
    menuItem = new JMenuItem(SAVE_AS, KeyEvent.VK_A);
    //    setAccelerator(menuItem, KeyEvent.VK_A, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(SAVE_AS);
    menu.add(menuItem);
    if (imAMac) {
      Application macApplication = Application.getApplication();
      macApplication.setQuitHandler(new QuitHandler() {
        @Override
        public void handleQuitRequestWith(QuitEvent quitEvent,
                                          QuitResponse quitResponse) {
          if (okToExit()) {
            quitResponse.performQuit();
          } else {
            quitResponse.cancelQuit();
          }
        }
      });
    } else {
      menu.addSeparator();
      menuItem = new JMenuItem(EXIT, KeyEvent.VK_X);
      setAccelerator(menuItem, KeyEvent.VK_X, imAMac);
      menuItem.addActionListener(this);
      menuItem.setActionCommand(EXIT);
      menu.add(menuItem);
    }

    // MENU - Edit
    menu = new JMenu("Edit");
    menu.setMnemonic(KeyEvent.VK_E);
    menuBar.add(menu);

    menuItem = new JMenuItem(ADD, KeyEvent.VK_A);
    setAccelerator(menuItem, KeyEvent.VK_A, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(ADD);
    menu.add(menuItem);
    menuItem = new JMenuItem(MODIFY, KeyEvent.VK_M);
    setAccelerator(menuItem, KeyEvent.VK_M, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(MODIFY);
    menu.add(menuItem);
    menuItem = new JMenuItem(REMOVE, KeyEvent.VK_R);
    setAccelerator(menuItem, KeyEvent.VK_R, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(REMOVE);
    menu.add(menuItem);
    menu.addSeparator();
    menuItem = new JMenuItem(COPY, KeyEvent.VK_C);
    setAccelerator(menuItem, KeyEvent.VK_C, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(COPY);
    menu.add(menuItem);
    menuItem = new JMenuItem(CLEAR, KeyEvent.VK_L);
    setAccelerator(menuItem, KeyEvent.VK_L, imAMac);
    menuItem.addActionListener(this);
    menuItem.setActionCommand(CLEAR);
    menu.add(menuItem);

    // MENU - Options
    menu = new JMenu("Options");
    menu.setMnemonic(KeyEvent.VK_O);
    menuBar.add(menu);

    hpmodeItem = new JCheckBoxMenuItem(HP_MODE, hidePasswords);
    hpmodeItem.setMnemonic(KeyEvent.VK_P);
    setAccelerator(hpmodeItem, KeyEvent.VK_P, imAMac);
    hpmodeItem.addActionListener(this);
    hpmodeItem.setActionCommand(HP_MODE);
    menu.add(hpmodeItem);

    // MENU - Help
    menu = new JMenu("Help");
    menu.setMnemonic(KeyEvent.VK_H);
    menuBar.add(menu);

    if (imAMac) {
      Application macApplication = Application.getApplication();
      macApplication.setAboutHandler(new AboutHandler() {
        @Override
        public void handleAbout(AboutEvent aboutEvent) {
          ActionEvent e = new ActionEvent(Chimp.this, -1, ABOUT);
          actionPerformed(e);
        }
      });
    } else {
      menuItem = new JMenuItem(ABOUT, KeyEvent.VK_A);
      menuItem.addActionListener(this);
      menuItem.setActionCommand(ABOUT);
      menu.add(menuItem);
    }

    // TOOLBAR
    JToolBar toolbar = new JToolBar();
    JButton tbButton = new JButton(loadImageIconAsResource("new.png"));
    tbButton.addActionListener(this);
    tbButton.setActionCommand(NEW);
    tbButton.setToolTipText("New File");
    toolbar.add(tbButton);
    openButton = new JButton(loadImageIconAsResource("table_go.png"));
    openButton.addActionListener(this);
    openButton.setActionCommand(OPEN);
    openButton.setToolTipText("Open File");
    toolbar.add(openButton);
    tbButton = new JButton(loadImageIconAsResource("table_save.png"));
    tbButton.addActionListener(this);
    tbButton.setActionCommand(SAVE);
    tbButton.setToolTipText("Save File");
    toolbar.add(tbButton);

    toolbar.addSeparator();

    tbButton = new JButton(loadImageIconAsResource("add.png"));
    tbButton.addActionListener(this);
    tbButton.setActionCommand(ADD);
    tbButton.setToolTipText("Add Item");
    toolbar.add(tbButton);
    tbButton = new JButton(loadImageIconAsResource("delete.png"));
    tbButton.addActionListener(this);
    tbButton.setActionCommand(REMOVE);
    tbButton.setToolTipText("Remove Item");
    toolbar.add(tbButton);
    tbButton = new JButton(loadImageIconAsResource("page_copy.png"));
    tbButton.addActionListener(this);
    tbButton.setActionCommand(COPY);
    tbButton.setToolTipText("Copy to Clipboard");
    toolbar.add(tbButton);
    tbButton = new JButton(loadImageIconAsResource("cross.png"));
    tbButton.addActionListener(this);
    tbButton.setActionCommand(CLEAR);
    tbButton.setToolTipText("Clear Clipboard");
    toolbar.add(tbButton);

    // FRAME CONTENTS
    mainPanel.add(toolbar, BorderLayout.NORTH);

    itemList = new JList() {
      public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int idx = locationToIndex(p);
        if (idx >= 0) {
          String username = (tbl.getUsernamesInTitleOrder()) [idx];
          if (username != null && !(username.trim().equals(""))) {
              return "Username: " + username;
          } else {
              return "Username unknown";
          }
        }
        return null;
      }
    };
    itemList.setVisibleRowCount(25);
    itemList.addMouseListener(this);
    if (OPEN_SANS_REGULAR != null) {
      itemList.setFont(OPEN_SANS_REGULAR_16);
    }
    ToolTipManager.sharedInstance().registerComponent(itemList);
    JScrollPane scrollPane = new JScrollPane(itemList);
    scrollPane.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(0,5,5,5),
        BorderFactory.createCompoundBorder(
            BorderFactory.createEtchedBorder(),
            BorderFactory.createEmptyBorder(5,5,5,5))));
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    cp.add(mainPanel);

    // Do a new to initialize.
    ActionEvent e = new ActionEvent(this, -1, NEW);
    actionPerformed(e);
  }

  private void buildRecentMenu() {
    recentMenu.removeAll();
    for (File f : recentFiles) {
      JMenuItem recentFileItem = new JMenuItem(f.getAbsolutePath());
      recentFileItem.addActionListener(this);
      recentFileItem.setActionCommand(OPEN);
      recentMenu.add(recentFileItem);
    }
  }

  private ImageIcon loadImageIconAsResource(String filename) {
    try {
      InputStream in =
        ClassLoader.getSystemClassLoader().getResourceAsStream(filename);
      if (in == null) {
        LOG.severe("Could not open " + filename);
        return null;
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      int b;
      while ((b = in.read()) != -1) {
        baos.write(b);
      }
      in.close();
      ImageIcon icon = new ImageIcon(baos.toByteArray());
      baos.close();
      return icon;
    } catch (IOException exc) {
      LOG.severe("Could not get image " + filename + ": " + exc.getMessage());
      return null;
    }
  }

  public void setIconImage(String filename)
  {
    ImageIcon i = loadImageIconAsResource(filename);
    if (i != null) super.setIconImage(i.getImage());
  }

  private void setAccelerator(JMenuItem jmi, int key, boolean imAMac) {
    if (imAMac) {
      jmi.setAccelerator(KeyStroke.getKeyStroke(key, Event.META_MASK));
    } else {
      jmi.setAccelerator(KeyStroke.getKeyStroke(key, Event.CTRL_MASK));
    }
  }

  public static void main(String args[]) {
    System.out.println ("CHIMP 1.2, Copyright (C) 2001-2015 Bill Havanki");
    System.out.println ("CHIMP comes with ABSOLUTELY NO WARRANTY. This is free software, and you are");
    System.out.println ("welcome to redistribute it under certain conditions. For details, see");
    System.out.println ("Help, About.");
    System.out.println ("");

    java.security.Security.addProvider(new com.sun.crypto.provider.SunJCE());

    Chimp chimp = new Chimp();
    //chimp.setIconImage ("chimpy.gif");
    chimp.pack();

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (screenSize.width - chimp.getWidth()) / 2;
    int y = (screenSize.height - chimp.getHeight()) / 2;
    chimp.setLocation(x, y);

    chimp.setVisible(true);
  }
}
