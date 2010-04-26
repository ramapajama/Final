all: jar

jar: classes
	jar -cvmf manifest.mf Final.jar control rss_processing

classes: clean
	javac -classpath .:./rssutils.jar:./Classifier4J-0.6.jar */*.java

clean:
	rm -rf control/*.class rss_processing/*.class *~ Final.jar
