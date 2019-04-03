
import java.io.File;
import java.io.IOException;

import java.net.MalformedURLException;
import java.net.URL;//import org.apache.commons.io.FileUtils;


/**
 * Quick program to scrounge a bunch of sequentially numbered files off the
 * internet and download them.
 *
 * Just type "java [url-prefix] [starting-number]" and this'll go until
 * no more files are found (or you kill the process).
 */
public class FileSucker {

	//-----------------------
	//	constants
	//-----------------------

	private static final String USAGE =
		"USAGE:\n"
		+ "\tfilesucker <url-prefix> <start_num> <suffix>\n"
		+ "\n"
		+ "Downloads a number of sequential files from a given url.\n"
		+ "These files will be put in the current directory.\n"
		+ "All sequential files will be downloaded until an error is received\n"
		+ "(file not found) or the process is killed.\n"
		+ "\n"
		+ "<url-prefix> is the full URL of the site to suck a file from,\n"
		+ "   minus the number portion.\n"
		+ "\n"
		+ "<start_num> is the starting number.\n"
		+ "\n"
		+ "<suffix> is the file extension to use.  Do NOT include the dot!\n"
		+ "\n"
		+ "example:\n"
		+ "   filesucker http://www.farmville.com/images/weeds 3000 jpg\n"
		+ "\n"
		+ "   This will download weeds3000.jpg, weeds3001.jpg ... from farmville.com\n"
		+ "\n";

	/** an arbitrary limitation to prevent run-aways */
	private static final int MAX_FILES = 500;

	//-----------------------
	//	data
	//-----------------------

	/** the url param as supplied by user */
	private static String m_urlPrefix;

	/** the starting number as supplied by user */
	private static String m_startNum;

	/** The number to append to the base url.  This increments until no more files found. */
	private static long m_num;	// todo: this may introduce bugs that I don't intend
								// as using a long instead of a string may introduce
								// errors because of truncating preceding zeros.

	/** the extension as supplied by user */
	private static String m_extension;


	//-----------------------
	//	methods
	//-----------------------

	//	Seems that Java the args ONLY contain the params, not the program name itself!
	public static void main (String[] args) {

		if (parseParams(args) == false) {
			return;
		}

		System.out.println("m_urlPrefix = " + m_urlPrefix
						   + ", m_startNum = " + m_startNum
						   + ", m_extension = " + m_extension);

		int count;

		for (count = 0; count < MAX_FILES; count++) {
			String newUrl = getFullUrl();
			String newFilename = getFilename();

			if (downloadFile(newUrl, newFilename) == false) {
				break;
			}
			m_num++;
		}
		System.out.println("Saved " + count + " files.");

		// testing
		System.out.println("getFullUrl = " + getFullUrl() + ", filename = " + getFilename());
	}


	/**
	 * Returns TRUE iff successfully downloaded the specified file to the given filename.
	 */
	private static boolean downloadFile (String url_str, String filename) {
		return false;
	}


	/**
	 * Creates a full URL string from the current state of the member data
	 */
	private static String getFullUrl() {
		return m_urlPrefix + m_num + "." + m_extension;
	}

	/**
	 * Figures out the filename from the current state of the member data
	 */
	private static String getFilename() {

		int i = 0;
		int urlPrefixLength = m_urlPrefix.length();
		char ch = m_urlPrefix.charAt(urlPrefixLength - 1);	// get last char (position 0 in this case)

		// work backwards until a non-allowable char is found
		while ((i < m_urlPrefix.length()) && (isLegalFileChar(ch))) {
			i++;
			ch = m_urlPrefix.charAt(urlPrefixLength - (1 + i));
		}

//		System.out.println ("getFilename: i = " + i);

		String namePrefix = m_urlPrefix.substring(urlPrefixLength - i);	// todo: might be off by 1
		return namePrefix + m_num + "." + m_extension;
	}

	/**
	 * Parses the params, setting all the method data appropriately.
	 * If the params don't make sense, then the USAGE string is displayed
	 * and FALSE is returned.
	 */
	private static boolean parseParams(String[] args) {
		if (args.length != 3) {
			System.out.print(USAGE);
			return false;
		}

		m_urlPrefix = args[0];
		m_startNum = args[1];
		m_extension = args[2];

		try {
			m_num = Long.parseLong(m_startNum);
		}
		catch (NumberFormatException e) {
			System.out.println("Unable to parse '" + m_startNum + "' into a number!");
			return false;
		}

		return true;
	}

	private static boolean isLegalFileChar(char c) {
		boolean val = Character.isLetterOrDigit(c);
//		System.out.println ("isLegalFileChar ( " + c + " ) is " + val);
		return val;
	}

}