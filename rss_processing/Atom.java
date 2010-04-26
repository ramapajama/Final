package rss_processing;

import java.util.Calendar;
import java.util.TimeZone;

import com.sun.cnpi.rss.elements.Author;
import com.sun.cnpi.rss.elements.BasicElement;
import com.sun.cnpi.rss.elements.Description;
import com.sun.cnpi.rss.elements.Guid;
import com.sun.cnpi.rss.elements.Item;
import com.sun.cnpi.rss.elements.Link;
import com.sun.cnpi.rss.elements.PubDate;
import com.sun.cnpi.rss.elements.Title;

public class Atom implements Comparable {
	private Title title = null;
	private Link link = null;
	private Description description = null;
	private PubDate pubDate = null;
	private Author author = null;
	private Guid guid = null;
	private String tagline = null;
	private String category = null;
	
	private static final int DATE_ELEMENT_LEN = 6;
	private static final String TAG_ELEM_START = "<p class=\"standfirst\">";
	private static final String TAG_ELEM_END = "</p>";
	private static final int CATEGORY_INDEX = 3; //Valid only for Guardian URLs
	
	private String getElementString (BasicElement element) {
		String retval = "";
		
		if (element != null) {
			retval = element.getText();
		}
		
		return retval;
	}
	
	public String getPubDate () {
		return this.getElementString(this.pubDate);
	}
	
	public String getTitle () {
		return this.getElementString(this.title);
	}
	
	public String getTagline () {
		return this.tagline;
	}
	
	public String getGuid () {
		return this.getElementString(this.guid);
	}
	
	public String getDescriptionText () {
		String retval = "";
		
		retval = this.getElementString(this.description);
		retval = retval.replaceAll("<br.*?>", "\n");
		retval = retval.replaceAll("</p>", "\n");
		retval = retval.replaceAll("<.+?>", "");
		
		return retval;
	}
	
	public String getCategory () {
		return this.category;
	}//end String getCategory ()
	
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
	
	private String extractCategory (String articleURL) {
		String retval = null;
		
		String[] urlElements = articleURL.split("/");
		
		if (urlElements.length > CATEGORY_INDEX) {
			retval = urlElements[CATEGORY_INDEX];
		}
		
		return retval;
	}//end String extractCategory (String)
	
	public String toString () {
		String retval = "";
		
		retval += String.format("Title: %s\n", this.getElementString(this.title));
		retval += String.format("Link: %s\n", this.getElementString(this.link));
		retval += String.format("Desc: %s\n", this.getDescriptionText());
		retval += String.format("Date: %s\n", this.getElementString(this.pubDate));
		retval += String.format("Author: %s\n", this.getElementString(this.author));
		
		return retval;
	}
	
	public String toHeadlineHTML () {
		String retval = "";
		
		retval += String.format("<a href=\"%s\">%s</a>", this.getElementString(this.link), this.getTitle());
		
		return retval;
	}
	
	private int getMonth (String monthString) {
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
	}
	
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
	}//end int compareTo (Object)

	public Atom (Item item) {
		this.title = item.getTitle();
		this.link = item.getLink();
		this.description = item.getDescription();
		this.pubDate = item.getPubDate();
		this.author = item.getAuthor();
		this.guid = item.getGuid();
		this.tagline = this.extractTagline(this.description.getText());
		this.category = this.extractCategory(this.getElementString(this.guid));
	}//end constructor
}//end class Atom
