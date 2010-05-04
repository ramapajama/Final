package rss_processing;

//import java.util.Calendar;
//import java.util.TimeZone;

import com.sun.cnpi.rss.elements.Author;
import com.sun.cnpi.rss.elements.BasicElement;
import com.sun.cnpi.rss.elements.Description;
import com.sun.cnpi.rss.elements.Guid;
import com.sun.cnpi.rss.elements.Item;
//import com.sun.cnpi.rss.elements.Link;
import com.sun.cnpi.rss.elements.PubDate;
import com.sun.cnpi.rss.elements.Title;

/**
 * The Atom class is a container for the pertinent elements from the RSS "atoms", in this case
 * the individual news articles. These elements are obtained from the Item class defined by the
 * RSS Utilities library (rssutils.jar)
 */
public class Atom { //implements Comparable {
	private Title title = null; //article title
	private Description description = null; //for the Guardian, this is the article text
	private PubDate pubDate = null; //the publication date of the article
	private Author author = null; //the article author, undefined for the Guardian
	private Guid guid = null; //the URL of the original article
	private String tagline = null; //the article tag line
	private String category = null; //the article category
	
	private static final String TAG_ELEM_START = "<p class=\"standfirst\">"; //HTML elements
	private static final String TAG_ELEM_END = "</p>";
	private static final int CATEGORY_INDEX = 3; //Valid only for Guardian URLs
	private static final String MISC_CAT = "Miscellaneous"; //Default category in case an article does not have one
	
	/**
	 * Generic method for converting the given BasicElement to a String. This method is only
	 * used internally.
	 * 
	 * @param element The BasicElement to convert
	 * @return The String value of the given BasicElement
	 * 
	 * @see com.sun.cnpi.rss.elements.BasicElement
	 */
	private String getElementString (BasicElement element) {
		String retval = "";
		
		if (element != null) {
			retval = element.getText();
		}
		
		return retval;
	}//end String getElementString (BasicElement)
	
	/**
	 * Returns the String of the publication date.
	 * 
	 * @return The String of the publication date
	 */
	public String getPubDate () {
		return this.getElementString(this.pubDate);
	}//end String getPubDate ()
	
	/**
	 * Returns the String of the article title.
	 * 
	 * @return The String of the article title
	 */
	public String getTitle () {
		return this.getElementString(this.title);
	}//end String getTitle ()
	
	/**
	 * Returns the String of the article tag line.
	 * 
	 * @return The String of the article tag line
	 */
	public String getTagline () {
		return this.tagline;
	}//end String getTagline ()
	
	/**
	 * Returns the String of the original article's URL.
	 * 
	 * @return The String of the original article's URL
	 */
	public String getGuid () {
		return this.getElementString(this.guid);
	}//end String getGuid ()
	
	/**
	 * Returns the String of the article text with HTML formatting removed.
	 * 
	 * @return The String of the article text
	 */
	public String getDescriptionText () {
		String retval = "";
		
		retval = this.getElementString(this.description);
		retval = retval.replaceAll("<br.*?>", "\n");
		retval = retval.replaceAll("</p>", "\n");
		retval = retval.replaceAll("<.+?>", "");
		
		return retval;
	}//end String getDescriptionText ()
	
	/**
	 * Returns the String of the article category.
	 * 
	 * @return The String of the article category
	 */
	public String getCategory () {
		return this.category;
	}//end String getCategory ()
	
	/**
	 * Extracts the article tag line from the <description> tag.
	 * 
	 * @param description The String of the <description> tag text with HTML formatting
	 * @return The String of the article tag line, or "" if it is not found
	 */
	private String extractTagline (String description) {
		String retval = "";
		
		int tagElementStart = description.indexOf(TAG_ELEM_START);
		
		if (tagElementStart != -1) {
			int tagElementEnd = description.indexOf(TAG_ELEM_END, tagElementStart);
			int tagTextStart = tagElementStart + TAG_ELEM_START.length();
			int tagTextEnd = tagElementEnd;
			
			retval = description.substring(tagTextStart, tagTextEnd);
		}
		
		return retval;
	}//end String extractTagline (String)
	
	/**
	 * Extracts the article category from article's URL; for Guardian articles, this occurs at the
	 * same place within the URL.
	 * 
	 * @param articleURL The String of the article URL
	 * @return The String of the article category, or MISC_CAT if it was not found
	 */
	private String extractCategory (String articleURL) {
		String retval = MISC_CAT;
		
		String[] urlElements = articleURL.split("/");
		
		if (urlElements.length > CATEGORY_INDEX) {
			retval = urlElements[CATEGORY_INDEX];
		}
		
		return retval;
	}//end String extractCategory (String)
	
	/**
	 * Implementation of the toString method - used for debugging purposes.
	 * 
	 * @return The String of this Atom object
	 */
	public String toString () {
		String retval = "";
		
		retval += String.format("Title: %s\n", this.getElementString(this.title));
		retval += String.format("Desc: %s\n", this.getDescriptionText());
		retval += String.format("Date: %s\n", this.getElementString(this.pubDate));
		retval += String.format("Author: %s\n", this.getElementString(this.author));
		
		return retval;
	}//end String toString ()
	
	
	/*private int getMonth (String monthString) {
		int retval = 0;
		
		if (monthString.equals("Jan")) {
			retval = 1;
		} else if (monthString.equals("Feb")) {
			retval = 2;
		} else if (monthString.equals("Mar")) {
			retval = 3;
		} else if (monthString.equals("Apr")) {
			retval = 4;
		} else if (monthString.equals("May")) {
			retval = 5;
		}
		
		return retval;
	}//end int getMonth (String)
	
	public Calendar convertDateStringToCalendar (String dateString) {
		String[] elements = dateString.split("(, | )");
		Calendar retval = Calendar.getInstance();
		
		if (elements.length == DATE_ELEMENT_LEN) {
			String[] timeElements = elements[4].split(":");
			
			int date = Integer.parseInt(elements[1]);
			int month = this.getMonth(elements[2]);
			int year = Integer.parseInt(elements[3]);
			int hour = 0;
			int min = 0;
			int sec = 0;
			
			if (timeElements.length == 3) {
				hour = Integer.parseInt(timeElements[0]);
				min = Integer.parseInt(timeElements[1]);
				sec = Integer.parseInt(timeElements[2]);
			}

			retval.setTimeZone(TimeZone.getTimeZone(elements[5]));
			retval.set(year, month, date, hour, min, sec);
		}
		
		return retval;
	}//end Calendar convertDateStringToCalendar (String)
	
	public int compareTo (Object toCompare) {
		int retval = 0;
		
		if (toCompare.getClass() == Atom.class) {
			Atom compareAtom = (Atom) toCompare;
			
			Calendar thisCalendar = this.convertDateStringToCalendar(this.getPubDate());
			Calendar compareCalendar = this.convertDateStringToCalendar(compareAtom.getPubDate());
			
			retval = thisCalendar.compareTo(compareCalendar);
		}
		
		return retval;
	}//end int compareTo (Object)*/

	/**
	 * Constructor, which initializes element values.
	 * 
	 * @param item The Item which represents the RSS atom (article)
	 */
	public Atom (Item item) {
		this.title = item.getTitle();
		this.description = item.getDescription();
		this.pubDate = item.getPubDate();
		this.author = item.getAuthor();
		this.guid = item.getGuid();
		this.tagline = this.extractTagline(this.description.getText());
		this.category = this.extractCategory(this.getElementString(this.guid));
	}//end constructor
}//end class Atom
