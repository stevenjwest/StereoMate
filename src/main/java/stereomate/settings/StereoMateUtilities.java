package stereomate.settings;

import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionListener;

import ij.IJ;
import ij.ImageJ;
import ij.io.LogStream;
import ij.text.TextPanel;
import ij.text.TextWindow;

public class StereoMateUtilities {
	
	static TextPanel logPanel;
	static ImageJ ij;
	
	static JTextArea logArea;
	static JPanel logPanelArea;
	static JFrame logFrame;

	/**
	 * This method writes the StereoMate title to the log window in ImageJ.  The StereoMate text
	 * is surrounded by #, and the text itself is written in ASCI art oldskool style.
	 */
	public static void stereomateTitleToLog() {
		 String sm0 = "############################################################# \n";
		 String sm1 = "####  _____ _                             ___  ___      _        #### \n";
		 String sm2 = "#### /   ___| |                              |  \\/  |     | |       #### \n";
		 String sm3 = "#### \\ `--.| |_ ___ _ __ ___  ___ | .  . | __ _| |_ ___  #### \n";
		 String sm4 = "####  `--. \\ __/ _ \\ '__/ _ \\/ _ \\| |\\/| |/ _` | __/ _ \\ #### \n";
		 String sm5 = "#### /\\__/ / ||  __/ | |  __/ (_) | |  | | (_| | ||  __/ #### \n";
		 String sm6 = "#### \\____/ \\__\\___|_|  \\___|\\___/\\_|  |_/\\__,_|\\__\\___| #### \n";
		 
		 
		 // Need to get the logWindow 
		 ij = IJ.getInstance(); //get ij instance, will get log window ref in the log() method!

		 
		 log(sm0);
		 log(sm0);
		 log(sm1);
		 log(sm2);
		 log(sm3);
		 log(sm4);
		 log(sm5);
		 log(sm6);
		 log(sm0);
		 log(sm0);
		 
		//Font f = logPanel.getFont();
		
		//log("Font: "+f.toString() );
		
	}
	
	
	public static void stereomateTitleToLog2() {
		String sm0 = "  _____ _                     ___  ___      _        \n";
		String sm1 = " /  ___| |                    |  \\/  |     | |       \n";
		String sm2 = " \\ `--.| |_ ___ _ __ ___  ___ | .  . | __ _| |_ ___  \n";
		String sm3 = "  `--. \\ __/ _ \\ '__/ _ \\/ _ \\| |\\/| |/ _` | __/ _ \\ \n";
		String sm4 = " /\\__/ / ||  __/ | |  __/ (_) | |  | | (_| | ||  __/ \n";
		String sm5 = " \\____/ \\__\\___|_|  \\___|\\___/\\_|  |_/\\__,_|\\__\\___| \n";
		
		log(" \n");
		log("Welcome to... \n");
		log(" \n");
		log(sm0);
		 log(sm1);
		 log(sm2);
		 log(sm3);
		 log(sm4);
		 log(sm5);
		                                                    
		                                                    
	}
	
	
	public static void stereomateTitleToLogIJ() {
		String sm0 = "# # # # # # # # # # # # # # # #";
		String sm1 = "# #   S T E R E O M A T E   # #";
		
		IJ.log(sm0);
		IJ.log(sm1);
		IJ.log(sm0);
	}
	
	
	public static void stereomateTitleToLogIJTwoPoint() {
		String sm0 = "#################################";
		String sm1 = "## (~_|_ _._ _ _ |\\/| _ _|_ _ ##";
		String sm2 = "## _) | }_| }_(_)|  |(_| | }_ ##";
		
		IJ.log(sm0);
		IJ.log(sm1);
		IJ.log(sm2);
		IJ.log(sm0);
		
	}
	
	
	/**
	 * This method writes the StereoMate title to the log window in ImageJ.  The StereoMate text
	 * is surrounded by #, and the text itself is written in ASCI art oldskool style.
	 */
	public static void stereomateTitleToLogIJDoom() {
		 String sm0 = "#################################################";
		 String sm1 = "####  _____ _                                     ___    ___          _               ####";
		 String sm2 = "#### /   ___| |                                    |    \\/    |          | |              ####";
		 String sm3 = "#### \\ `--.| |_ ___  _  __  ___    ___  |   .    .   |  __  _| |_ ___      ####";
		 String sm4 = "####  `--. \\ __/  _  \\ ' __/  _  \\/  _  \\|  | \\/ |  |/  _` | __/  _  \\   ####";
		 String sm5 = "#### /\\__/ / |  |  __/   |  |   __/   (_)  |  |      |  |  (_|  |  |  |  __/   ####";
		 String sm6 = "#### \\____/ \\__\\___|_|   \\___| \\___/\\_|      |_/\\__,_|\\__\\___|    ####";
		 
		 
		 // Need to get the logWindow 
		 ij = IJ.getInstance(); //get ij instance, will get log window ref in the log() method!

		 
		 IJ.log(sm0);
		 IJ.log(sm0);
		 IJ.log(sm1);
		 IJ.log(sm2);
		 IJ.log(sm3);
		 IJ.log(sm4);
		 IJ.log(sm5);
		 IJ.log(sm6);
		 IJ.log(sm0);
		 IJ.log(sm0);
		
	}
	
	
	
	public static void log2(String s) {
		if (s==null) return;
		if(logPanel == null) {
			IJ.log("logPanel is NULL");
		}
		if (logPanel==null && ij!=null) {
			TextWindow logWindow = new TextWindow("StereoMate Log", "", 400, 250);
			logWindow.setFont(new Font("Monospaced", Font.PLAIN, 16));
			logPanel = logWindow.getTextPanel();
			logPanel.setFont(new Font("Monospaced", Font.PLAIN, 16));
		}
			
		logPanel.append(s);

	}
	
	
	public static void log(String s) {
		if (s==null) return;
		if(logPanel == null) {
		}
		if (logArea == null) {
			logFrame = new JFrame("StereoMate Log");
			logArea = new JTextArea();
			logArea.setEditable(false);
			logArea.setFont( new Font("Monospaced", Font.PLAIN, 16));
			logFrame.add(logArea);
			logFrame.setSize(800, 800);
			logFrame.setVisible(true);
			//TextWindow logWindow = new TextWindow("StereoMate Log", "", 400, 250);
			//logWindow.setFont(new Font("Monospaced", Font.PLAIN, 16));
			//logPanel = logWindow.getTextPanel();
			//logPanel.setFont(new Font("Monospaced", Font.PLAIN, 16));
		}
			
		//logPanel.append(s);
		logArea.append(s);

	}

	
	public static void removeActionListener(JComboBox jb) {
		if(jb != null) {
		ActionListener[] als = jb.getActionListeners();
		for(int a=0; a<als.length; a++) {
			jb.removeActionListener(als[a]);
		}
		}
	}
	
	public static void removeActionListener(JFileChooser jb) {
		if(jb != null) {
			ActionListener[] als = jb.getActionListeners();
			for(int a=0; a<als.length; a++) {
				jb.removeActionListener(als[a]);
			}
		}
	}
	
	public static void removeActionListener(JTextField jb) {
		if(jb != null) {
		ActionListener[] als = jb.getActionListeners();
		for(int a=0; a<als.length; a++) {
			jb.removeActionListener(als[a]);
		}
		}
	}
	
	public static void removeActionListener(JCheckBox jb) {
		if(jb != null) {
		ActionListener[] als = jb.getActionListeners();
		for(int a=0; a<als.length; a++) {
			jb.removeActionListener(als[a]);
		}
		}
	}
	
	public static void removeActionListener(JRadioButton jb) {
		if(jb != null) {
		ActionListener[] als = jb.getActionListeners();
		for(int a=0; a<als.length; a++) {
			jb.removeActionListener(als[a]);
		}
		}
	}
	
	public static void removeActionListener(AbstractButton jb) {
		if(jb != null) {
		ActionListener[] als = jb.getActionListeners();
		for(int a=0; a<als.length; a++) {
			jb.removeActionListener(als[a]);
		}
		}
	}
	
	public static void removeChangeListener(JSpinner jb) {
		if(jb != null) {
		ChangeListener[] als = jb.getChangeListeners();
		for(int a=0; a<als.length; a++) {
			jb.removeChangeListener(als[a]);
		}
		}
	}
	
	public static void removeListSelectionListener(JList l) {
		if(l != null) {
		ListSelectionListener[] lss = l.getListSelectionListeners();
		for(int a=0; a<lss.length;a++) {
			l.removeListSelectionListener(lss[a]);
		}
		}
	}
	
}
