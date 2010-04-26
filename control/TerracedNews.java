package control;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Vector;

import net.sf.classifier4J.summariser.SimpleSummariser;

import com.sun.cnpi.rss.parser.RssParserException;

import rss_processing.Atom;
import rss_processing.RSSReader;

public class TerracedNews extends Thread {
	private RSSReader reader = null;
	private HashMap<String, Vector<Atom>> articles = null;
	private String rssFeedURLString = null;
	private String summaryFilesFolderPath = null;
	private File indexFile = null;
	private SimpleSummariser summarizer = null;
	
	private static final int SHORT = 5;
	private static final int MEDIUM = 20;
	private static final int LONG = 100;
	private static final String ARTICLE_PATH_FORMAT = "%s%s_%d.html";
	private static final String SUMMARY_HEADER = "<html>\n<head>\n<style type=\"text/css\" media=\"all\">\n@import \"css/main.css\";</style>\n</head>\n";
	private static final long SLEEP_TIME = 300000;
	private static final String MISC_CAT = "Miscellaneous";
	
	private static final String DOCTYPE_STR = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
	private static final String HTML_START = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\n";
	private static final String MAIN_HEADER = "<head><title>Terraced News: The Guardian</title>\n<style type=\"text/css\" media=\"all\">\n@import \"css/main.css\";\n@import \"css/overlay-apple.css\";\n</style>\n</head>\n";
	private static final String CONTENT_CLOSER = "\t</div><!--content-->\n";
	private static final String BODY_CLOSER = "\t<div class=\"footer\">\n\t</div><!--container-->\n</html>\n";
	
	private static final int COL_NUM = 3;
	
	private void deleteArticleFiles () {
		File summaryFileDir = new File(this.summaryFilesFolderPath);
		
		if (summaryFileDir.isDirectory()) {
			File[] summaryFiles = summaryFileDir.listFiles();
			
			for (int i = 0; i < summaryFiles.length; i++) {
				summaryFiles[i].delete();
			}
		}
	}//end void deleteArticleFiles ()
	
	public void addArticles (Vector<Atom> articlesToAdd) {
		if (articlesToAdd != null) {
			this.deleteArticleFiles();
			this.articles.clear();
			
			for (int i = 0; i < articlesToAdd.size(); i++) {
				Atom currentAtom = articlesToAdd.get(i);
				//String currentTitle = currentAtom.getTitle();
				String category = currentAtom.getCategory();
				
				if (category == null) {
					category = MISC_CAT;
				}
				
				if (this.articles.containsKey(category) != true) {
					this.articles.put(category, new Vector<Atom>());
				}
				
				this.articles.get(category).add(currentAtom);
			}//end for
		}//end if
	}//end void addArticles (Vector<Atom>)
	
	public void runReader () {
		this.reader.setRssAtoms();
		this.addArticles(this.reader.getRssAtoms());
	}//end void runReader ()
	
	private void writeSummary (Atom article, String summaryFilePath, int length) {
		File summaryFile = new File(summaryFilePath);
		
		if (summaryFile != null) {
			try {
				FileWriter summaryWriter = new FileWriter(summaryFile);
				String summarizedText = this.summarizer.summarise(article.getDescriptionText(), length);
				String summaryTitle = String.format("%s", article.getTitle());
				String articleLinkStr = article.getGuid();
				
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
			}
		}
	}
	
	private void writeSummaries (Atom article, String[] summaryLinks) {
		this.writeSummary(article, summaryLinks[0], SHORT);
		this.writeSummary(article, summaryLinks[1], MEDIUM);
		this.writeSummary(article, summaryLinks[2], LONG);
	}//end void writeSummaries (Atom, String[])
	
	private String generateArticleFileName (String articleTitle) {
		String retval = null;
		
		retval = articleTitle.replaceAll(" ", "_");
		retval = retval.replaceAll("\\W", "");
		
		return retval;
	}//end String generateArticleFileName (String)
	
	private String[] generateSummaryFiles (Atom article) throws UnsupportedEncodingException {
		String[] summaryLinks = new String[3];
		String articleTitle = article.getTitle();
		
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
	}
	
	private void writeLink (FileWriter fw, Atom article, String[] summaryLinks) throws IOException {
		String summaryType = "Short";
		
		fw.write("\t\t\t\t<div class=\"article\">\n");
		fw.write("\t\t\t\t\t<h4 class=\"articleTitle\">\n");
		fw.write(String.format("\t\t\t\t\t\t%s\n", article.getTitle()));
		fw.write("\t\t\t\t\t</h4>\n");
		fw.write("\t\t\t\t\t<p class=\"hook\">\n");
		fw.write(String.format("\t\t\t\t\t\t%s\n", article.getTagline()));
		fw.write("\t\t\t\t\t</p>\n");
		fw.write("\t\t\t\t\t<ul class=\"summaryList\">\n");
	
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
	}//end void writeLink (FileWriter)
	
	private void writeLinks (FileWriter fw) throws IOException {
		Vector<String> categories = new Vector<String>(this.articles.keySet());
		String[] summaryLinks = null;
		
		fw.write("\t<table>\n\t\t<tbody>\n\t\t\t<tr>\n");
		
		for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
			if ((catIdx > 0) && ((catIdx % COL_NUM) == 0)) {
				fw.write("\t\t\t</tr>\n\t\t\t<tr>\n");
			}
			
			Vector<Atom> atoms = this.articles.get(categories.get(catIdx));
			fw.write("\t\t\t\t<td>\n");
			fw.write("\t\t\t\t\t<div class=\"feedTopic\">\n");
			fw.write(String.format("\t\t\t\t\t\t<h3>%s</h3>\n", categories.get(catIdx)));
			
			int artIdx = 0;
			while ((artIdx < atoms.size()) && (artIdx < 3)) {
				summaryLinks = this.generateSummaryFiles(atoms.get(artIdx));
				this.writeLink(fw, atoms.get(artIdx), summaryLinks);
				artIdx++;
			}
			
			fw.write("\t\t\t\t\t</div>\n");
			fw.write("\t\t\t\t</td>\n");
		}
		
		fw.write("\t\t\t</tr>\n\t\t</tbody>\n\t</table>\n");
	}//end void writeLinks (FileWriter)
	
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
	
	public void generateFiles () {
		if (indexFile != null) {
			try {
				FileWriter fw = new FileWriter(indexFile);
				
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
	
	public void run () {
		try {
			this.runReader();
			this.generateFiles();
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException ie) {
			System.err.println("[TerracedNews] Sleep interrupted");
		}
	}//end void run ()
	
	public TerracedNews (String rssFeedURLString, String indexFileName, String summaryFolderPath) {
		this.articles = new HashMap<String, Vector<Atom>>();
		this.rssFeedURLString = rssFeedURLString;
		this.summaryFilesFolderPath = summaryFolderPath;
		this.summarizer = new SimpleSummariser();
		
		File summaryFilesFolder = new File(this.summaryFilesFolderPath);
		
		if (summaryFilesFolder.exists() != true) {
			if (summaryFilesFolder.mkdir() != true) {
				System.err.println("[TerracedNews] Error creating summary files folder");
				System.exit(3);
			}
		}
		
		try {
			this.indexFile = new File(indexFileName);
			this.reader = new RSSReader(this.rssFeedURLString);
		} catch (RssParserException rpe) {
			System.err.println("[TerracedNews] Error initializing RSS parser");
			System.exit(2);
		}
	}//end constructor
	
	public static void main (String[] args) {
		if (args.length != 3) {
			System.err.println("Usage: terracednews [news feed URL] [index file] [summary files folder]");
			System.exit(1);
		}
		
		TerracedNews tn = new TerracedNews(args[0], args[1], args[2]);
		tn.start();
	}//end main
}//end class TerracedNews
