package rss_processing;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import com.sun.cnpi.rss.elements.Channel;
import com.sun.cnpi.rss.elements.Item;
import com.sun.cnpi.rss.elements.Rss;
import com.sun.cnpi.rss.parser.RssParser;
import com.sun.cnpi.rss.parser.RssParserException;
import com.sun.cnpi.rss.parser.RssParserFactory;

/**
 * The RSSReader class is used to connect to the given RSS feed, download and parse the retrieved
 * XML file. It uses the RSS Utilities library (rssutils.jar) to accomplish this.
 * 
 * @see com.sun.cnpi.rss.parser.RssParser
 * @see rss_processing.Atom
 */
public class RSSReader {
	private URL rssURL = null; //the URL of the RSS feed
	private RssParser parser = null; //the RSS feed reader and XML document parser
	private Rss rssStore = null; //stores the RSS atoms
	private Vector<Atom> atoms = null; //the Vector of RSS atom metadata read from the feed
	
	/**
	 * Sets the URL for the RSS feed to read from, then reads and parses the XML document from that
	 * feed, storing the resulting atoms.
	 * 
	 * @param rssURLString The String of the RSS feed URL
	 */
	public void setRssURL (String rssURLString) {
		try {
			this.rssURL = new URL(rssURLString);
			this.rssStore = this.parser.parse(this.rssURL);
		} catch (MalformedURLException mue) {
			System.err.printf("[RSSReader.setRssURL] %s is not a well formed URL\n", rssURLString);
		} catch (RssParserException rpe) {
			System.err.println("[RSSReader.setRssURL] Parser error\n");
		} catch (IOException e) {
			System.err.printf("[RSSReader.setRssURL] I/O error reading %s\n", rssURLString);
		}
	}//end void setRssURL (String)
	
	/**
	 * Returns the Vector of RSS atoms (articles).
	 * 
	 * @return The Vector of RSS atoms
	 */
	public Vector<Atom> getRssAtoms () {
		return this.atoms;
	}//end Vector<Atom> getRssAtoms ()
	
	/**
	 * Iterates through the individual Item objects stored in rssReader and converts them into
	 * Atom objects.
	 */
	public void setRssAtoms () {
		Channel channel = rssStore.getChannel();
		Object[] items = channel.getItems().toArray();
		
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				this.atoms.add(new Atom((Item) items[i]));
			} else {
				System.err.printf("[RSSReader] Item %d is null!\n", i);
			}
		}
	}//end void setRssAtoms ()
	
	/**
	 * Initializes the RssParser and the Vector of Atoms.
	 * 
	 * @param rssURLString The String of the RSS feed URL
	 * @throws RssParserException
	 */
	public RSSReader (String rssURLString) throws RssParserException {
		this.parser = RssParserFactory.createDefault();
		this.atoms = new Vector<Atom>();
		this.setRssURL(rssURLString);
	}//end constructor (String)
	
	/**
	 * Test main
	 */
	public static void main (String[] args) {
		RSSReader r = null;
		
		try {
			r = new RSSReader("http://feeds.guardian.co.uk/theguardian/rss");
			r.setRssAtoms();
			
			Vector<Atom> atoms = r.getRssAtoms();
			
			for (int i = 0; i < atoms.size(); i++) {
				System.out.print(atoms.get(i).toString());
			}
			
			TreeSet<Atom> ts = new TreeSet<Atom>(atoms);
			
			Iterator i = ts.iterator();
			
			while (i.hasNext()) {
				System.out.println(((Atom) i.next()).getPubDate());
			}
		} catch (RssParserException rpe) {
			System.err.println("Error initializing parser!");
			System.exit(1);
		}
	}//end void main (String[])
}//end class RSSReader
