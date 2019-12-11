package stereomate.settings;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

/**
 * Class to implement the Int Document Filter, for JTextFields to only
 * accept int characters for input.  EDIT: Further modified the test() method
 * to allow this class to accept positive and negative integers.
 * @author stevenwest
 *
 */
public class MyIntFilter extends DocumentFilter {
		   @Override
		   public void insertString(FilterBypass fb, int offset, String string,
		         AttributeSet attr) throws BadLocationException {

			  Document doc = fb.getDocument();
		      StringBuilder sb = new StringBuilder();
		      sb.append(doc.getText(0, doc.getLength()));
		      sb.insert(offset, string);

		      if (test(sb.toString())) {
		         super.insertString(fb, offset, string, attr);
		      } else {
		         // warn the user and don't allow the insert
		      }
		   }

		   private boolean test(String text) {
			   //Added if statement to allow the TextField to be
			   //deleted to no Characters.
			   	//Without this if statement it is impossible to delete
			   	//the Text Field to be blank...
			   if(text.length() == 0) {
				   return true;
			   }
			   
			   
		      try {
		    	  if(text.substring(0,1).equals("-") && text.length() > 1 ) {
		    		  Integer.parseInt(text.substring(1,text.length() ) );
				   }
		    	  else if(text.substring(0,1).equals("-") && text.length() == 1) {
		    		  
		    	  }
		    	  else {
		         Integer.parseInt(text);
		    	  }
		         return true;
		      } catch (NumberFormatException e) {
		         return false;
		      }
		   }

		   @Override
		   public void replace(FilterBypass fb, int offset, int length, String text,
		         AttributeSet attrs) throws BadLocationException {

			  Document doc = fb.getDocument();
		      StringBuilder sb = new StringBuilder();
		      sb.append(doc.getText(0, doc.getLength()));
		      sb.replace(offset, offset + length, text);

		      if (test(sb.toString())) {
		         super.replace(fb, offset, length, text, attrs);
		      } else {
		         // warn the user and don't allow the insert
		      }

		   }

		   @Override
		   public void remove(FilterBypass fb, int offset, int length)
		         throws BadLocationException {
			  Document doc = fb.getDocument();
		      StringBuilder sb = new StringBuilder();
		      sb.append(doc.getText(0, doc.getLength()));
		      sb.delete(offset, offset + length);

		      if (test(sb.toString())) {
		         super.remove(fb, offset, length);
		      } else {
		         // warn the user and don't allow the insert
		      }

		   }
	}
