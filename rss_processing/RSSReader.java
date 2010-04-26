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

public class RSSReader {
	private URL rssURL = null;
	private RssParser parser = null;
	private Rss rssReader = null;
	private Vector<Atom> atoms = null;
	
	public void setRssURL (String rssURLString) {
		try {
			this.rssURL = new URL(rssURLString);
			this.rssReader = this.parser.parse(this.rssURL);
		} catch (MalformedURLException mue) {
			System.err.printf("[RSSReader.setRssURL] %s is not a well formed URL\n", rssURLString);
		} catch (RssParserException rpe) {
			System.err.println("[RSSReader.setRssURL] Parser error\n");
		} catch (IOException e) {
			System.err.printf("[RSSReader.setRssURL] I/O error reading %s\n", rssURLString);
		}
	}
	
	public Vector<Atom> getRssAtoms () {
		return this.atoms;
	}
	
	public void setRssAtoms () {
		Channel channel = rssReader.getChannel();
		System.out.printf("No. of items: %d\n", channel.getItems().size());
		Object[] items = channel.getItems().toArray();
		
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				this.atoms.add(new Atom((Item) items[i]));
			} else {
				System.err.printf("[RSSReader] Item %d is null!\n", i);
			}
		}
	}
	
	public RSSReader (String rssURLString) throws RssParserException {
		this.parser = RssParserFactory.createDefault();
		this.atoms = new Vector<Atom>();
		this.setRssURL(rssURLString);
	}
	
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
	}
}
