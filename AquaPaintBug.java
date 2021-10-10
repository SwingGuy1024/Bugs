package com.mm.util;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.RootPaneContainer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.plaf.LayerUI;
import org.jetbrains.annotations.NotNull;

/**
 * Bug to illustrate a repaint bug when using the Mac OS X AquaLookAndFeel.
 * 
 * Synopsis:
 * Performance issue when repainting a window under AquaLookAndFeel
 * 
 * Description:
 * A repaint bug in the AquaLookAndFeel causes a window to be repainted multiple times when a window needs repainting only once. It seems to repaint the entire component once for each subcomponent that responds to user input or may change its visible state. For example, in the provided text case, it repaints once for each JButton, each JCheckBox, the JTextArea and the JScrollBar, but not for each JLabel. All the needless repainting seriously impacts performance. The issue does not show up under any other look and feel, which the sample code can demonstrate. I should note that the sample code forces a repaint by using a JLayer to repaint the window with a blurred effect when the dialog is shown. This serves as a stand-in for any repaint issues implemented using a JLayer, such as animation. This repaint bug probably makes the JLayer useless as a decorator for repainting issues, although it may still be useful for intercepting events.
 * 
 * System:
 * Mac OS X Big Sur
 * Version 11.5.2
 * 3Ghz 6-Core Intel Core i5
 * 16 GB ram
 * Radeon Pro 570X 4GB Graphics
 * 
 * Steps to Reproduce:
 * 1. Run the provided test case. 
 * 2. Click on any shared check box a few times and notice that all the shared check boxes share their selected state. 
 * 3. Click any of the JButtons. This will pop open a dialog using JOptionPane, blurring the window behind it. It will also toggle the state of all the checkboxes. 
 * 4. Close the message box as soon as it appears and watch how long it takes to disappear.
 * 5. As you open the message box, pay attention to the console output. It will print out "blurred paint" each time it repaints the window, and "Not blurred" when you close the window.
 * 5. Use the look-and-feel panel next to the main window to try this under various looks-and-feels.
 * Extra: Try changing the number of buttons, checkboxes, or labels to see how that affects how many times it gets repainted. Try also removing the JScrollPane or adding another, or reconfiguring it. You will find that changing the number of labels has no effect on the number of repaints, but the check boxes and buttons do, once per button. Adding a horizontal scroll bar to the scroll pane adds one additional repaint.
 * 
 * Expected Result:
 * 1. When the message box opens, the window behind it should repaint blurry, and should do so quickly.
 * 2. The console should print "blurred paint n" where n counts how many times it painted the window while blurred. It should print this twice, very quickly.
 * 3. If you hit the close box immediately, the message box should close immediately.
 * 4. The shared checkboxes state should toggle as the message box opens.
 * 
 * Actual Result:
 * For every look and feel except Aqua, we see the expected result.
 * For Aqua, there are several differences.
 * 1. It repaints the window 23 times, as you can see in the console output.
 * 2. If you close the message box immediately, it won't close until all the repaints complete.
 * 3. If the checkboxes were unchecked when you opened the message box, they will all get checked (which is correct) but they will also all get selected. Then, as the repaints continue, they will each gets unselected one at a time. (This may provide a clue to where the problem is.) On my system, it takes about 9 seconds before the repaints complete and the message box can be closed. I should point out that the blur-effect can't be seen as the cause of the problem, since it doesn't cause any problem under any other look and feel.
 * 
 * Workaround:
 * Use a different look and feel. (Undesirable for Mac applications.)
 * <p>Created by IntelliJ IDEA.
 * <p>Date: 9/26/21
 * <p>Time: 5:41 AM
 *
 * @author Miguel Mu\u00f1oz
 */
@SuppressWarnings({"HardCodedStringLiteral", "UseOfSystemOutOrSystemErr", "MagicNumber", "StringConcatenation", "UseOfSystemOutOrSystemErr", "MagicCharacter"})
public class AquaPaintBug extends JPanel {
  @SuppressWarnings("HardcodedLineSeparator")
  private static final String DUMMY_TEXT = "Instructions: This illustrates the use of a JLayer to add a simple special effect. " +
      "The effect is to blur the window when a dialog is showing, and unblur it when the dialog is dismissed. Unfortunately, due " +
      "apparently to a bug in the AquaLookAndFeel, this runs very slowly. In every other look and feel it runs fine.\n\n" +
      "To see the effect, click any of the \"Show Message n\" buttons. (They all do the same thing.) A dialog will appear, with " +
      "the blur effect in the main window. Use the floating window next to it to try the effect in different looks and feels. \n\n" +
      "On all but the Mac OS X look and feel, it runs fine. On the Mac, it gets a separate repaint event for every component in " +
      "the window that could be updated. These are the JTextField, the JButtons, and the JCheckBoxes, but not the jLabels. You " +
      "can see this 2 ways: by watching the buttons as they lose their \"selected\" state, and by watching the console output, " +
      "which prints \"blurred paint n\" each time it gets a repaint message.\n\n" +
      "All the buttons are there to amplify the problem. In a real application, there may be many buttons on a tool bar, for " +
      "example, which would have the same effect.\n\n" +
      "Note that unchecking the check boxes doesn't help, even though they no longer have need to update their selected state. " +
      "(The buttons that say \"shared\" may be unchecked in a single operation.) It's merely their presence that generates the " +
      "extra repaint events.";
  private final BlurControl blurControl;

  public static void main(String[] args) throws
      UnsupportedLookAndFeelException,
      ClassNotFoundException,
      InstantiationException,
      IllegalAccessException
  {
    final String lfName = UIManager.getSystemLookAndFeelClassName();
    UIManager.setLookAndFeel(lfName);

    createFrame(lfName);
  }

  private static JPanel makeLfPanel() {
    JPanel lsAndFsPanel = new JPanel(new GridLayout(0, 1));
    String currentName = UIManager.getLookAndFeel().getName();
    System.out.printf("<%s>%n", currentName); // NON-NLS
    for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
      //noinspection NonConstantStringShouldBeStringBuffer
      String simpleName = info.getName();
      if (simpleName.equals(currentName)) {
        simpleName = "\u2714 " + simpleName; // 2714 is heavy check mark
      }
      JButton button = new JButton(simpleName);
      button.addActionListener(e -> {
        JRootPane rootPaneContainer = ((JButton) e.getSource()).getRootPane();
        ((JDialog) rootPaneContainer.getParent()).getOwner().dispose();
        //noinspection OverlyBroadCatchBlock
        try {
          UIManager.setLookAndFeel(info.getClassName());
        } catch (Exception unexpected) {
          unexpected.printStackTrace();
        }
        createFrame(info.getClassName());
      });
      lsAndFsPanel.add(button);
    }
    return lsAndFsPanel;
  }

  private static void createFrame(final String lfName) {
    int dotSpot = lfName.lastIndexOf('.');
    String simpleName = lfName.substring(dotSpot + 1);
    JFrame frame = new JFrame("Multi Print Bug: " + simpleName);
    AquaPaintBug aquaPaintBug = new AquaPaintBug(frame);

    aquaPaintBug.blurControl.getContentPane().add(aquaPaintBug);
//    frame.add(multiPrintBug, BorderLayout.CENTER);

    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setLocationByPlatform(true);
    frame.pack();
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowOpened(final WindowEvent e) {
        frame.removeWindowListener(this);
        createFloatingWindow(frame);
      }
    });
    frame.setVisible(true);
    
  }

  private static void createFloatingWindow(final JFrame frame) {
    JPanel lsAndFsPanel = makeLfPanel();
    JDialog topFrame = new JDialog(frame, Dialog.ModalityType.MODELESS);
    topFrame.setAlwaysOnTop(true);
    topFrame.getContentPane().setLayout(new BorderLayout());
    topFrame.getContentPane().add(lsAndFsPanel, BorderLayout.CENTER);
    topFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    topFrame.pack();
    topFrame.setAutoRequestFocus(false);
    final Rectangle frameBounds = frame.getBounds();
    topFrame.setLocation(frameBounds.x + frameBounds.width + 10, frameBounds.y);
    topFrame.setVisible(true);
  }

  AquaPaintBug(RootPaneContainer parentFrame) {
    super(new BorderLayout());
    blurControl = new BlurControl(parentFrame, 10);
    add(makeButtonPanel(), BorderLayout.CENTER);

    JTextArea textArea = new JTextArea(30, 60);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    JScrollPane scrollPane = new JScrollPane(textArea,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    textArea.setEditable(false); // prevent perpetual repaint as cursor blinks
    add(scrollPane, BorderLayout.PAGE_END);
    textArea.setText(DUMMY_TEXT);
    add(makeLabelPanel(), BorderLayout.PAGE_START);
  }
  
  private JPanel makeLabelPanel() {
    // These labels are informational, but they're mainly here to determine if JLabels
    // affect the number of repaints. They don't.
    JPanel labelPanel = new JPanel(new GridLayout(0, 1));
    addProp("java.runtime.name", labelPanel);
    addProp("java.runtime.version", labelPanel);
    addProp("java.vendor", labelPanel);
    addProp("os.name", labelPanel);
    addProp("os.arch", labelPanel);
    addProp("os.version", labelPanel);
    addProp("java.vm.version", labelPanel);
    addProp("java.version", labelPanel);
    return labelPanel;
  }
  
  private void addProp(String propName, JPanel panel) {
    JLabel label = new JLabel(String.format("%s: %s", propName, System.getProperty(propName)));
    panel.add(label);
  }
  
  private JPanel makeButtonPanel() {
    JPanel buttonPanel = new JPanel(new GridLayout(0, 3));
    ButtonModel sharedModel = new JToggleButton.ToggleButtonModel();
    sharedModel.setSelected(true);
    ActionListener actionListener = e -> {
      blurControl.setBlurred(true);
      JOptionPane.showMessageDialog(((AbstractButton) e.getSource()).getRootPane(), "Dummy Message");
      blurControl.setBlurred(false);
      System.out.println("Not blurred");
      ((AbstractButton) e.getSource()).requestFocus();
    };
    for (int i = 0; i < 3; ++i) {
      JButton button = new JButton("Show Message " + i);
      button.addActionListener(actionListener);
      buttonPanel.add(button);
    }
    for (int i=0; i<6; ++i) {
      JCheckBox checkBox = new JCheckBox(String.format("shared %d", i));
      checkBox.setModel(sharedModel);
      buttonPanel.add(checkBox);
    }
    for (int i=0; i<3; ++i) {
      final JCheckBox checkBox = new JCheckBox(String.format("Unshared %d", i));
      checkBox.setSelected(true);
      buttonPanel.add(checkBox);
    }
    return buttonPanel;
  }

  public static class BlurControl {
    private final BlurLayerUI ui;
    private final JLayer<Component> jLayer;
    private static final Double dScale = getDisplayScale();
    private static final int displayScale = dScale.intValue();
    private static final double scale = dScale;
    private static final double inverseScale = 1.0 / dScale;

    public static final String BLURRING_FRAME_CLIENT = "BLURRING_FRAME_CLIENT";

    /**
     * Create a BlurControl using a circular blur of the specified size.
     *
     * @param jFrameOrDialog The JFrame or JDialog whose contents to blur
     * @param blurSize       The size of the blur
     */
    public BlurControl(RootPaneContainer jFrameOrDialog, int blurSize) {
      this(jFrameOrDialog, new BlurControl.BlurLayerUI(blurSize));
    }

    protected BlurControl(RootPaneContainer jFrameOrDialog, BlurControl.BlurLayerUI ui) {
      this.ui = ui;
      jLayer = new JLayer<>(new JPanel(new BorderLayout()), ui);
      JRootPane jRootPane = jFrameOrDialog.getRootPane();
      jRootPane.getContentPane().add(jLayer, BorderLayout.CENTER);
      ((JComponent) jFrameOrDialog.getContentPane()).putClientProperty(BLURRING_FRAME_CLIENT, this);
    }

    public JComponent getContentPane() {
      return (JComponent) jLayer.getView();
    }

    public boolean isBlurred() {
      return ui.isBlurred();
    }

    public void setBlurred(final boolean blurred) {
      ui.setBlur(blurred);
      jLayer.invalidate();
      jLayer.repaint();
    }

    @NotNull
    static BufferedImageOp createConvolveOperation(final int blurValue) {
      final BufferedImageOp mOperation;
      float[] blurKernel = makeFilter(blurValue);
      mOperation = new ConvolveOp(new Kernel(blurValue, blurValue, blurKernel), ConvolveOp.EDGE_NO_OP, null);
      return mOperation;
    }

    private static final class BlurLayerUI extends LayerUI<Component> {
      private final AtomicInteger printCounter = new AtomicInteger(0);

      private BufferedImage mOffscreenImage;
      private final BufferedImageOp mOperation;
      private boolean blurred = false;

      void setBlur(boolean blur) { blurred = blur; }

      boolean isBlurred() { return blurred; }

      private BlurLayerUI(int blurValue) {
        this(createConvolveOperation(blurValue));
      }

      private BlurLayerUI(BufferedImageOp operation) {
        super();
        mOperation = operation;
      }

      @Override
      public void paint(Graphics g, JComponent c) {
        if (blurred) {
          System.out.printf("blurred Paint %s%n", printCounter.incrementAndGet()); // NON-NLS
          int w = c.getWidth() * displayScale;
          int h = c.getHeight() * displayScale;
          if ((w == 0) || (h == 0)) {
            return;
          }
          // only create the offscreen image if the one we have is the wrong size.
          Graphics2D g2 = (Graphics2D) g;
          if ((mOffscreenImage == null) || (mOffscreenImage.getWidth() != w) || (mOffscreenImage.getHeight() != h)) {
            // Double size for Retina Display
            mOffscreenImage = g2.getDeviceConfiguration().createCompatibleImage(w, h);
          }
          Graphics2D ig2 = mOffscreenImage.createGraphics();
          AffineTransform savedTransform = ig2.getTransform();
          ig2.scale(scale, scale);
          super.paint(ig2, c);
          ig2.setTransform(savedTransform);
          ig2.dispose();
          AffineTransform transform = g2.getTransform();
          g2.scale(inverseScale, inverseScale);
          g2.drawImage(mOffscreenImage, mOperation, 0, 0);
          g2.setTransform(transform);
        } else {
          super.paint(g, c);
          printCounter.set(0);
        }
      }
    }

    /**
     * The apple.awt.contentScaleFactor property was supposed to return a Double for Mac, and null elsewhere. But I always
     * get null. Maybe pre-retina screens aren't supported anymore. (I don't have a way to detect this for Windows anyway!)
     * I also tried calling Desktop.getScreenResolution(), but I got five different answers, depending on which option I chose
     * in my System Preferences. (They were 67, 86, 108, 121, and 135.)
     * Unless I find another way to do this, I'm hardcoding this to 2.0.
     *
     * @return 2.0
     */
    private static double getDisplayScale() {
      // return (Double) Toolkit.getDefaultToolkit().getDesktopProperty("apple.awt.contentScaleFactor");
      return 2.0;
    }

    /**
     * Make a filter o the specified size.
     * @param size the size, in pixels
     * @return An array to be used with the ConvolveOp constructor
     */
    static float[] makeFilter(int size) {
      final int sizeSq = size * size;
      float[] data = new float[sizeSq];
      int f = 0;
      for (int i = 0; i < size; ++i) {
        for (int j = 0; j < size; ++j) {
          data[f++] = 1.0f/sizeSq;
        }
      }
      return data;
    }
  }
}
