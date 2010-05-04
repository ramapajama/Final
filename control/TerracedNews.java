package control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import net.sf.classifier4J.summariser.SimpleSummariser;

import com.sun.cnpi.rss.parser.RssParserException;

import rss_processing.Atom;
import rss_processing.RSSReader;

/**
 * The TerracedNews class is the entry point for the AtAGlance web service. It runs as a separate
 * thread, downloading the target RSS feed at a set time interval (which is accomplished using the
 * RSSReader class). Three summaries of each article are created using Classifier4J: a short, medium,
 * and long summary. Once these summaries have been created, an HTML file is generated for display
 * in the user's web browser. Also at this time, the AtAGlance index file is generated, which
 * includes links to all of the generated summaries.
 */
public class TerracedNews extends Thread {
	private RSSReader reader = null; //The RSS feed reader
	private HashMap<String, Vector<Atom>> articles = null; //A hashmap of the extracted news articles
	private String rssFeedURLString = null; //The String of the URL for the RSS feed to follow
	private String summaryFilesFolderPath = null; //The file path of the directory where summaries are stored
	private File indexFile = null; //The File object for the AtAGlance main page
	private SimpleSummariser summarizer = null; //The article summarizer
	
	//Predefined article summary sentence lengths
	private static final int SHORT = 5;
	private static final int MEDIUM = 20;
	private static final int LONG = 100;
	
	private static final long SLEEP_TIME = 300000; //Sleep time between RSS feed updates - 5 minutes
	private static final String MISC_CAT = "Miscellaneous"; //Default category in case an article does not have one
	
	//HTML formatting
	private static final String ARTICLE_PATH_FORMAT = "%s/%s_%d.html";
	private static final String SUMMARY_HEADER = "<html>\n<head>\n<style type=\"text/css\" media=\"all\">\n@import \"css/main.css\";</style>\n</head>\n";
	private static final String DOCTYPE_STR = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
	private static final String HTML_START = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n";
	private static final String MAIN_HEADER = "<head><title>Terraced News: The Guardian</title>\n<style type=\"text/css\" media=\"all\">\n@import \"css/main.css\";\n@import \"css/overlay-apple.css\";\n</style>\n</head>\n";
	private static final String CONTENT_CLOSER = "\t</div><!--content-->\n";
	private static final String BODY_CLOSER = "\t<div class=\"footer\">\n\t</div><!--container-->\n</html>\n";
	
	private static final int COL_NUM = 3; //Number of articles to display side-by-side
	
	/**
	 * Clears out the summary files in the summaries directory; done when reading in new articles
	 * from the RSS feed.
	 */
	private void deleteArticleFiles () {
		File summaryFileDir = new File(this.summaryFilesFolderPath);
		
		if (summaryFileDir.isDirectory()) {
			File[] summaryFiles = summaryFileDir.listFiles();
			
			for (int i = 0; i < summaryFiles.length; i++) {
				summaryFiles[i].delete();
			}
		}
	}//end void deleteArticleFiles ()
	
	/**
	 * Adds the given articles from the RSS reader to the hashmap. The hashmap is keyed by article
	 * category, so the value is a Vector of articles under that category. If a category for an
	 * article to add does not exist, then it is added.
	 * 
	 * @param articlesToAdd The Vector<Atom> of articles to add to the hashmap
	 */
	public void addArticles (Vector<Atom> articlesToAdd) {
		if (articlesToAdd != null) {
			this.deleteArticleFiles();
			this.articles.clear(); //delete the current articles in the hashmap
			
			for (int i = 0; i < articlesToAdd.size(); i++) {
				Atom currentAtom = articlesToAdd.get(i);
				String category = currentAtom.getCategory();
				
				//If no category specified, use default
				if (category == null) {
					category = MISC_CAT;
				}
				
				//If the hashmap does not inclue the given category, create it
				if (this.articles.containsKey(category) != true) {
					this.articles.put(category, new Vector<Atom>());
				}
				
				this.articles.get(category).add(currentAtom);
			}//end for
		}//end if
	}//end void addArticles (Vector<Atom>)
	
	/**
	 * Runs the RSS feed reader, adding the articles it returns to the hashmap
	 */
	public void runReader () {
		try {
			this.reader = new RSSReader(this.rssFeedURLString); //create the RSS reader
		} catch (RssParserException rpe) {
			System.err.println("[TerracedNews] Error initializing RSS parser");
			System.exit(2);
		}
		
		this.reader.setRssAtoms();
		this.addArticles(this.reader.getRssAtoms());
	}//end void runReader ()
	
	/**
	 * Summarizes the given article and generates its HTML file
	 * 
	 * @param article The Atom of the news article to summarize
	 * @param summaryFilePath The String of the target path for the summary HTML file
	 * @param length The int length of the summary to generate
	 */
	private void writeSummary (Atom article, String summaryFilePath, int length) {
		File summaryFile = new File(summaryFilePath);
		
		//Checks that the target HTML file can be written to
		if (summaryFile != null) {
			//Catches any I/O errors that occur when writing to the HTML file
			try {
				FileWriter summaryWriter = new FileWriter(summaryFile);
				
				//Generate the summary of the article
				String summarizedText = this.summarizer.summarise(article.getDescriptionText(), length);
				String summaryTitle = String.format("%s", article.getTitle()); //Get the original article's title
				String articleLinkStr = article.getGuid(); //Get the orignal article's URL
				
				//Replaces all breaklines with the <br> tag
				summarizedText = summarizedText.replaceAll("\\n", "<br>");
				
				summaryWriter.write(SUMMARY_HEADER);
				summaryWriter.write("<body>\n");
				summaryWriter.write("<div id=\"overlayHeader\">\n");
				summaryWriter.write(String.format("<h1>%s</h1>\n", summaryTitle));
				summaryWriter.write("</div>\n");
				summaryWriter.write("<p class=\"articleText\">\n");
				
				summaryWriter.write(summarizedText);
				
				summaryWriter.write("</p>\n");
				summaryWriter.write("<div id=\"overlayFooter\">\n");
				summaryWriter.write(String.format("<a href=\"%s\" target=\"_blank\">Full article</a>\n", articleLinkStr));
				summaryWriter.write("</div>\n");
				summaryWriter.write("</body>\n</html>\n");		
				
				summaryWriter.flush();
				summaryWriter.close();
			} catch (IOException ioe) {
				System.err.printf("[TerracedNews.writeSummary] Error writing summary file %s\n", summaryFilePath);
			}//end try/catch
		}//end if
	}//end void writeSummary (Atom, String, int)
	
	/**
	 * Writes the three different lengthed summaries for the given article.
	 * 
	 * @param article The Atom of the news article to summarize
	 * @param summaryLinks The String[] of the HTML file pathes to generate
	 */
	private void writeSummaries (Atom article, String[] summaryLinks) {
		this.writeSummary(article, summaryLinks[0], SHORT);
		this.writeSummary(article, summaryLinks[1], MEDIUM);
		this.writeSummary(article, summaryLinks[2], LONG);
	}//end void writeSummaries (Atom, String[])
	
	/**
	 * Since not all news articles have URI friendly titles, replaces all spaces with underscores
	 * and removes any characters not in [A-Za-z0-9_]
	 * 
	 * @param articleTitle The String of the article title
	 * @return The sanitized article title
	 */
	private String generateArticleFileName (String articleTitle) {
		String retval = null;
		
		retval = articleTitle.replaceAll(" ", "_");
		retval = retval.replaceAll("\\W", "");
		
		return retval;
	}//end String generateArticleFileName (String)
	
	/**
	 * Generates the summarized HTML files for the given article
	 * 
	 * @param article The Atom of the article to generate the summarized HTML files for
	 * @return The String[] of file paths for the generated HTML files
	 */
	private String[] generateSummaryFiles (Atom article) {
		String[] summaryLinks = new String[3];
		String articleTitle = article.getTitle();
		
		//Sanitize the article title
		articleTitle = this.generateArticleFileName(articleTitle);
		
		if (articleTitle != null) {
			summaryLinks[0] = String.format(ARTICLE_PATH_FORMAT, this.summaryFilesFolderPath, articleTitle, SHORT);
			summaryLinks[1] = String.format(ARTICLE_PATH_FORMAT, this.summaryFilesFolderPath, articleTitle, MEDIUM);
			summaryLinks[2] = String.format(ARTICLE_PATH_FORMAT, this.summaryFilesFolderPath, articleTitle, LONG);
		
			File shortSummary = new File(summaryLinks[0]);
		
			if (shortSummary.exists() != true) {
				this.writeSummaries(article, summaryLinks);
			}
		} else {
			summaryLinks = null;
		}
		
		return summaryLinks;
	}//end String[] generateSummaryFiles (Atom)
	
	/**
	 * Writes the HTML element for the given news article on the main page, including links to the
	 * generated article summaries.
	 * 
	 * @param fw The FileWriter for the main page
	 * @param article The Atom of the news article
	 * @param summaryLinks The String[] of file paths to the generated article summaries
	 * @throws IOException
	 */
	private void writeLink (FileWriter fw, Atom article, String[] summaryLinks) throws IOException {
		String summaryType = "Short";
		
		fw.write("\t\t\t\t<div class=\"article\">\n"); //article HTML element
		fw.write("\t\t\t\t\t<h4 class=\"articleTitle\">\n");
		fw.write(String.format("\t\t\t\t\t\t%s\n", article.getTitle())); //article title
		fw.write("\t\t\t\t\t</h4>\n");
		fw.write("\t\t\t\t\t<p class=\"hook\">\n");
		fw.write(String.format("\t\t\t\t\t\t%s\n", article.getTagline())); //article hook
		fw.write("\t\t\t\t\t</p>\n");
		fw.write("\t\t\t\t\t<ul class=\"summaryList\">\n"); //list of summary links
	
		for (int j = 0; j < summaryLinks.length; j++) {
			switch(j) {
			case 0: summaryType = "short"; break;
			case 1: summaryType = "medium"; break;
			case 2: summaryType = "long"; break;
			default: summaryType = "full";
			}
		
			fw.write(String.format("\t\t\t\t\t\t<li><a href=\"%s\" class=\"%s\" rel=\"#overlay\">%s</a></li>\n", summaryLinks[j], summaryType, summaryType));
		}//end for-j
		
		fw.write(String.format("\t\t\t\t\t\t<li><a href=\"%s\" class=\"goto\" target=\"_blank\">full</a></li>\n", article.getGuid()));
	
		fw.write("\t\t\t\t\t</ul>\n");
		fw.write("\t\t\t\t</div>\n");
		fw.write("\t\t\t</div>\n");
	}//end void writeLink (FileWriter, Atom, String[])
	
	/**
	 * Generates the HTML elements for each of the news articles. Each element includes the article
	 * title, hook, and links to the different generated summaries.
	 * 
	 * @param fw The FileWriter for the main page
	 * @throws IOException
	 */
	private void writeLinks (FileWriter fw) throws IOException {
		Vector<String> categories = new Vector<String>(this.articles.keySet()); //retrieve the article categories
		String[] summaryLinks = null;
		
		fw.write("\t<table>\n\t\t<tbody>\n\t\t\t<tr>\n"); //open the table for the article elements
		
		for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
			//if the maximum number of columns is reached, start a new row
			if ((catIdx > 0) && ((catIdx % COL_NUM) == 0)) {
				fw.write("\t\t\t</tr>\n\t\t\t<tr>\n");
			}
			
			Vector<Atom> atoms = this.articles.get(categories.get(catIdx)); //get the articles for the current category
			
			//start a new table element for the category
			fw.write("\t\t\t\t<td>\n");
			fw.write("\t\t\t\t\t<div class=\"feedTopic\">\n");
			fw.write(String.format("\t\t\t\t\t\t<h3>%s</h3>\n", categories.get(catIdx))); //category title
			
			//generate elements for the different articles in the category
			int artIdx = 0;
			while ((artIdx < atoms.size()) && (artIdx < 3)) {
				summaryLinks = this.generateSummaryFiles(atoms.get(artIdx));
				this.writeLink(fw, atoms.get(artIdx), summaryLinks);
				artIdx++;
			}
			
			//close up
			fw.write("\t\t\t\t\t</div>\n");
			fw.write("\t\t\t\t</td>\n");
		}
		
		fw.write("\t\t\t</tr>\n\t\t</tbody>\n\t</table>\n");
	}//end void writeLinks (FileWriter)
	
	/**
	 * Writes the header for the main page HTML document
	 * 
	 * @param fw The FileWriter for the main page
	 * @throws IOException
	 */
	private void writeHeader (FileWriter fw) throws IOException {
		fw.write(DOCTYPE_STR);
		fw.write(HTML_START);
		fw.write(MAIN_HEADER);

		fw.write("  <div class=\"container\">\n");
		fw.write("    <div id=\"header\">\n");
		fw.write("      <div id=\"spacerTop\">\n");
		fw.write("      </div>\n");
		fw.write("      <h1>\n");
		fw.write("        At A Glance\n");
		fw.write("      </h1>\n");

		fw.write("      <div id=\"mainNav\">\n");
		fw.write("        <ul>\n");
		fw.write("          <li>\n");				
		fw.write("            <button class=\"current\">\n");
		fw.write("              The Guardian\n");
		fw.write("            </button>\n");
		fw.write("          </li>\n");
		fw.write("          <li>\n");
		fw.write("            <button>\n");
		fw.write("              New York Times\n");
		fw.write("            </button>\n");
		fw.write("          </li>\n");
		fw.write("          <li>\n");
		fw.write("            <button>\n");
		fw.write("              Wall Street Journal\n");
		fw.write("            </button>\n");
		fw.write("          </li>\n");
		fw.write("        </ul>\n");
		fw.write("      </div>\n");
		fw.write("      <div id=\"spacerBottom\">\n");
		fw.write("      </div>\n");
		fw.write("    </div>\n");
	}//end voi writeHeader (FileWriter)
	
	/**
	 * Writes the JavaScript code that appears on the main page
	 * 
	 * @param fw The FileWriter for the main page
	 * @throws IOException
	 */
	private void writeScriptCode (FileWriter fw) throws IOException {
		fw.write("<!-- overlayed element - this should be positioned off screen so we don't see a flicker on load -->\n");
		fw.write("<div class=\"apple_overlay\" id=\"overlay\">\n");
		fw.write("\n");
		fw.write("<!-- the external content is loaded inside this tag -->\n");
		fw.write("<div class=\"contentWrap\"></div>\n");
		fw.write("\n");
		fw.write("</div>\n");
		fw.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"css/overlay-apple.css\"/>");
		fw.write("<script src='js/jquery-1.3.2.min.js'></script>\n");
		fw.write("<script src=\"js/jquery.tools.min.js\"></script>\n");
		fw.write("<script>\n");
		fw.write("\n");
		fw.write("$(function() {\n");
		fw.write("\n");
	  	fw.write("// if the function argument is given to overlay,\n");
	  	fw.write("// it is assumed to be the onBeforeLoad event listener\n");
	  	fw.write("$(\"a[rel]\").overlay({\n");
	  	fw.write("\n");
	    fw.write("// some expose tweaks suitable for modal dialogs\n");
	    fw.write("expose: {\n");
	    fw.write("color: '#333',\n");
	    fw.write("loadSpeed: 200,\n");
	    fw.write("opacity: 0.9\n");
	    fw.write("},\n");
	  	fw.write("effect: 'apple',\n");
	  	fw.write("\n");
	  	fw.write("onBeforeLoad: function() {\n");
	  	fw.write("\n");
	  	fw.write("// grab wrapper element inside content\n");
	  	fw.write("var wrap = this.getContent().find(\".contentWrap\");\n");
	  	fw.write("\n");
	  	fw.write("// load the page specified in the trigger\n");
	  	fw.write("wrap.load(this.getTrigger().attr(\"href\"));\n");
	  	fw.write("}\n");
	  	fw.write("\n");
	  	fw.write("});\n");
	  	fw.write("});\n");
	  	fw.write("</script>\n");
	}//end void writeScriptCode (FileWriter)
	
	/**
	 * Runs the different methods to generate the AtAGlance main page and the article summaries
	 * pages.
	 */
	public void generateFiles () {
		if (indexFile != null) {
			try {
				FileWriter fw = new FileWriter(indexFile); //the FileWriter for the main page
				
				this.writeHeader(fw);
				this.writeLinks(fw);
				fw.write(CONTENT_CLOSER);
				this.writeScriptCode(fw);
				fw.write(BODY_CLOSER);
				
				fw.flush();
				fw.close();
			} catch (IOException ioe) {
				System.err.println("[TerracedNews.generateIndexFile] Error opening index.html");
				ioe.printStackTrace(System.err);
			}
		}
	}//end void generateIndexFile
	
	/**
	 * Implementing the run method, which runs the RSS reader and generates the HTML files, then
	 * sleeps for SLEEP_TIME
	 */
	public void run () {
		try {
			while (true) {
				this.runReader();
				this.generateFiles();
				Thread.sleep(SLEEP_TIME);
			}
		} catch (InterruptedException ie) {
			System.err.println("[TerracedNews] Sleep interrupted");
		}
	}//end void run ()
	
	/**
	 * Constructor, which does initialization
	 * 
	 * @param rssFeedURLString The String of the RSS feed URL
	 * @param indexFileName The String of the main page file path
	 * @param summaryFolderPath The String of the summary files directory path
	 */
	public TerracedNews (String rssFeedURLString, String indexFileName, String summaryFolderPath) {
		this.articles = new HashMap<String, Vector<Atom>>();
		this.rssFeedURLString = rssFeedURLString;
		this.summaryFilesFolderPath = summaryFolderPath;
		this.summarizer = new SimpleSummariser(); //create the summarizer
		
		File summaryFilesFolder = new File(this.summaryFilesFolderPath);
		
		if (summaryFilesFolder.exists() != true) {
			if (summaryFilesFolder.mkdir() != true) {
				System.err.println("[TerracedNews] Error creating summary files folder");
				System.exit(3);
			}
		}
		
		this.indexFile = new File(indexFileName);
	}//end constructor
	
	/**
	 * Main method, which runs the program
	 * 
	 * @param args [0] is the RSS feed URL, [1] is the index file path, [2] is the summary files folder path
	 */
	public static void main (String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: AtAGlance [news feed URL] [index file] [summary files folder]");
			System.exit(1);
		}
		
		TerracedNews tn = new TerracedNews(args[0], args[1], args[2]);
		tn.start();
	}//end main
}//end class TerracedNews
