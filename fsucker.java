import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.MalformedURLException;
import java.net.URL;

import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Quick program to scrounge a bunch of sequentially numbered files off the
 * internet and download them.
 *
 * Just type "java [url-prefix] [starting-number]" and this'll go until
 * no more files are found (or you kill the process).
 */
public class fsucker {

	//-----------------------
	//	constants
	//-----------------------

	private static final String USAGE =
		"version 1.1\n"
		+ "  USAGE:\n"
		+ "\t" + ConsoleColors.BOLD + "fsucker" + ConsoleColors.RESET + " <url> [-m <max_count>] [-k max_skipped])\n"
		+ "\n"
		+ "The file specified in the <url> is downloaded to the current directory.\n"
		+ "The next file in sequential order will be downloaded as well.\n"
		+ "This repeats until no file is found or the process is killed.\n"
		+ "\n"
		+ "   " + ConsoleColors.BOLD + "-s" + ConsoleColors.RESET + "   Define a suffix.  This is a set of characters\n"
		+ "        that appear AFTER the group of changing numbers (but does not include\n"
		+ "        the extension or the dot).\n"
		+ "\n"
		+ "   " + ConsoleColors.BOLD + "-m" + ConsoleColors.RESET + "   Limit the number of files to download to this many.\n"
		+ "        Default is 1000.  Using just 1 is nice for testing.\n"
		+ "\n"
		+ "   " + ConsoleColors.BOLD + "-k" + ConsoleColors.RESET + "   Keep trying successive numbers this many times until quitting.\n"
		+ "        Many sequential files may skip a file or two and then continue.\n"
		+ "        This param tells the program to keep trying successive numbers\n"
		+ "        for this many times before calling it quits.\n"
		+ "\n"
		+ "examples:\n"
		+ "   fsucker http://www.farmville.com/images/weeds3000.jpg\n"
		+ "\n"
		+ "      This will download weeds3000.jpg, weeds3001.jpg ... from farmville.com\n"
		+ "\n"
		+ "   fsucker http://www.myfaveimages.com/graphix/DSC-0001.jpg -m 2\n"
		+ "\n"
		+ "      This will download DSC-0001.jpg and DSC-002.jpg only.\n"
		+ "\n"
		+ "   fsucker http://annoyingname.net/p1001_t.jpg -s _t\n"
		+ "      Will download the p1001_t.jpg, p1002_t.jpg, p1003_t.jpg...\n"
		+ "      until no more files can be found.\n"
		+ "\n"
		+ "   fsucker http://inconsistentfiles.com/file001.png -k 10\n"
		+ "      Let's say that file001.png exists, but there's a gap of four\n"
		+ "      files before the sequence continues.  This will keep trying with\n"
		+ "      file002.png, file003.png, file004.png (which all fail) and then\n"
		+ "      get a hit with file005.png.  The program will continue along with\n"
		+ "      sequential files until it fails 10 times in a row, which ends the run.\n";

	/** an arbitrary limitation to prevent run-aways */
	private static final int MAX_FILES = 1000;

	/** Skip this many files before calling it quits */
	private static final int DEFAULT_SKIP_COUNT = 0;

	/** Indicates a command line switch */
	private static final char COMMAND_LINE_SWITCH_INDICATOR = '-';

	/** Command line switch letter indicating that the next param will be suffix string */
	private static final char SUFFIX_SWITCH_LETTER = 's';

	/** Command line switch that indicates the next param will be the max count */
	private static final char MAX_COUNT_SWITCH_LETTER = 'm';

	/** Command line switch that indicates the next param will be the skip count */
	private static final char SKIP_SWITCH_LETTER = 'k';

	//-----------------------
	//	data
	//-----------------------

	/** The maximum number of files to get. Defaults go MAX_FILES. */
	private static int m_maxCount;

	/** The number of consecutive file numbers to try and fail before giving up. */
	private static int m_skipCount = DEFAULT_SKIP_COUNT;

	/** Original URL supplied by the user. */
	private static String m_origUrl;

	/** The url minus the filename (should end with a slash) */
	private static String m_urlPrefix;

	/**
	 * The filename prefix (no suffix, dot, or extension).
	 * This is the item that gets incremented.
	 */
	private static String m_filenamePrefix;

	/**
	 * If there is a suffix (characters that appear AFTER the incrementing
	 * number), this is it.  Defaults to the empty String so that rebuilding
	 * works correctly.
	 */
	private static String m_suffix = "";

	/*
	 * The separator between the filename and the extension.
	 * If there is no extension, then this will be changed to
	 * the empty string.
	 */
	private static String m_dot = ".";

	/** The file extension for the files we're sucking */
	private static String m_ext;


	//-----------------------
	//	methods
	//-----------------------

	//	Seems that Java the args ONLY contain the params, not the program name itself!
	public static void main (String[] args) {

		// Examine the input params and set the globals accordingly
		if (parseParams(args) == false) {
			System.out.println(USAGE);
			return;
		}

		// Pretend to be a browser (many servers will only send data to browsers)
		System.setProperty("http.agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36");

		if (initData() == false) {
			System.out.println("Error during intialization.");
			return;
		}

		// pre-set a few variables before the loop
		int count = 0;
		int skipped = 0;

		// Now start sucking up files
		while (count < m_maxCount) {
			String filename = m_filenamePrefix + m_suffix + m_dot + m_ext;
			String url = m_urlPrefix + filename;
			if (downloadFile (url, filename)
						  == false) {
				if (skipped == m_skipCount) {
					// Stop trying
					break;
				}

				System.out.println("Unable to download " + filename + ", skipping...");
				skipped++;
				m_filenamePrefix = nextNumStr(m_filenamePrefix);
				if (m_filenamePrefix == null) {
					break;	// no longer able to increment
				}
				continue;	// try again
			}

			skipped = 0;	// reset skipped
			count++;
			m_filenamePrefix = nextNumStr(m_filenamePrefix);
			if (m_filenamePrefix == null) {
				break;	// no longer able to increment
			}
		}

		if (count == MAX_FILES) {
			System.out.println("WARNING: max files reached (" + MAX_FILES + ")!");
		}
		System.out.println("Saved " + count + " files.");

	} // main

	/**
	 * Examines m_origUrl and sets some working data accordingly.
	 *
	 * preconditions
	 *		m_origUrl	Should be set.
	 *
	 * side effects
	 *		m_urlPrefix		Set to the appropriate substring of m_origUrl
	 *
	 *		m_filenamePrefix	Ibid
	 *
	 *		m_dot			May be changed to empty string
	 *
	 *		m_ext			Ibid
	 *
	 *	@return		True if all went well.
	 *				False if there was an error that can't be recovered.
	 */
	private static boolean initData() {
		// Figure out the extension, the filename prefix,
		// and the url prefix
		int dotPos = m_origUrl.lastIndexOf('.');
		int slashPos = m_origUrl.lastIndexOf('/');

		if (slashPos == -1) {
			System.out.println("Error: Illegal url supplied (no filename).");
			return false;
		}

		m_urlPrefix = m_origUrl.substring(0, slashPos + 1);	// include the slash

		if (dotPos == -1) {
			// no dot found, so no extension used at all.
			// Change the extension and dot to empty strings so the
			// string assemblies will still work properly.
			m_ext = "";
			m_dot = "";
			m_filenamePrefix = m_origUrl.substring(slashPos + 1, m_origUrl.length());
		}
		else {
			m_ext = m_origUrl.substring(dotPos + 1, m_origUrl.length());
			m_filenamePrefix = m_origUrl.substring(slashPos + 1, dotPos);
		}

		// If there's a suffix, handle that.
		int len = m_suffix.length();
		if (len > 0) {
			// first, check to make sure that the last bit of m_filenamePrefix
			// actually IS m_suffix!
			int filenameLen = m_filenamePrefix.length();
			String sub = m_filenamePrefix.substring(filenameLen - len, filenameLen);
			if (m_suffix.equals(sub) == false) {
				System.out.println("Suffix (" + m_suffix + ") did not match the end of " + m_origUrl + ". Aborting.");
				System.out.println("len = " + len + ", sub = " + sub + ", m_suffix = " + m_suffix);
				return false;
			}

			m_filenamePrefix = m_filenamePrefix.substring(0, filenameLen - len);
		}

		// System.out.println("m_urlPrefix = " + m_urlPrefix +
		// 				   ", m_filenamePrefix = " + m_filenamePrefix +
		// 				   ", m_suffix = " + m_suffix +
		// 				   ", m_dot = " + m_dot +
		// 				   ", m_ext = " + m_ext);
		return true;
	}

	/**
	 * Returns TRUE iff successfully downloaded the specified file to the given filename.
	 *
	 *	@param	url_str		The complete url to access.
	 *
	 *	@param	filename	The complete filename to save this as.
	 *
	 *	@return	False if error occurs (prob file-not-found).
	 */
	private static boolean downloadFile (String url_str, String filename) {

		System.out.println("downloadFile ( " + url_str + ", " + filename + " )");

		ReadableByteChannel rbc = null;
		FileOutputStream fos = null;

		try {
			URL website = new URL(url_str);
			rbc = Channels.newChannel(website.openStream());

			fos = new FileOutputStream(filename);
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

			fos.close();
			rbc.close();
		}
		catch (IOException e) {
			if (e instanceof FileNotFoundException) {
				System.out.println("Cannot find file!");
			}
			else {
				e.printStackTrace();
			}
			return false;
		}

		return true;
	} // downloadFile


	/**
	 * Parses the params, setting all the method data appropriately.
	 * If the params don't make sense, then FALSE is returned.
	 *
	 * side effects
	 *		m_origUrl	Will be set to the url passed in by the user.
	 *
	 *		m_maxCount	Will be set to either MAX_COUNT or a number passed
	 *					by the user.
	 */
	private static boolean parseParams(String[] args) {

		m_maxCount = MAX_FILES;

		switch (args.length) {
			case 7:
				if (parseOption (args[5], args[6]) == false) {
					return false;
				}
				// fall through...

			case 5:
				if (parseOption (args[3], args[4]) == false) {
					return false;
				}
				// fall through...

			case 3:
				if (parseOption(args[1], args[2]) == false) {
					return false;
				}
				// fall through...

			case 1:
				m_origUrl = args[0];
				break;

			default:
				return false;
		}

		return true;
	}

	/**
	 * Given a swith and its parameter, figure out which switch it is
	 * and set any global variables to reflect this option.
	 *
	 *	@param	switch		The switch string.  Should start with a dash.
	 *
	 *	@param	param		The data associated with this switch.
	 *
	 *	@return		True if the switch and param are correctly interpreted.
	 *				False if something wasn't right.
	 *
	 *	side effects
	 *		m_suffix	May be changed to reflect command params.
	 *		m_maxCount	"										"
	 *		m_skipCount	"										"
	 */
	private static boolean parseOption(String switchStr, String paramStr) {

		// check for appropriate switch
		if (switchStr.charAt(0) != COMMAND_LINE_SWITCH_INDICATOR) {
			System.out.println("Unknown command line switch indicator.");
			return false;
		}

		char switchLetter = switchStr.charAt(1);
		switch (switchLetter) {
			case SUFFIX_SWITCH_LETTER:
				m_suffix = paramStr;
				break;

			case MAX_COUNT_SWITCH_LETTER:
				m_maxCount = Integer.parseInt(paramStr);
				break;

			case SKIP_SWITCH_LETTER:
				m_skipCount = Integer.parseInt(paramStr);
				break;

			default:
			System.out.println("Could not recognize command switch letter.");
				return false;
		}

		return true;
	}

	/**
	 * Given a string ending with a number, this returns the
	 * same string with the number incremented by 1.
	 *
	 * No digits are added. This means that some numbers cannot
	 * be incremented.  If numStr cannot be incremented,
	 * null is returned.
	 *
	 * Null is also returned if the last digit is not a number.
	 */
	private static String nextNumStr(String numStr) {

		// This works recursively, working from the last digit
		// towards the front until no more digits are found.

		// base cases
		if ((numStr == null) || (numStr.length() == 0)) {
			return null;
		}

		// Some useful things to pre-calculate
		int len = numStr.length();
		String prefix = numStr.substring(0, len - 1);	// prefix = all but the last letter
		char lastDigit = numStr.charAt(len - 1);

		if (lastDigit == '9') {
			// This is the most complicated case, but with recursion
			// it's not so bad.
			// Since we're on 9, we need to increment the prefix string.
			// Then append 0 to the result.  For example, 2039 would
			// yield 204 (incremented) with a 0 appended --> 2040.
			String prefixIncremented = nextNumStr(prefix);
			if (prefixIncremented == null) {
				// Couldn't increment the prefix!  That means we can't
				// properly increment numStr either.
				return null;
			}

			// Prefix was properly incremented.  Append '0' and we're done.
			return prefixIncremented + "0";
		}

		// Now the most expected case: simply incrementing our lastDigit
		if ((lastDigit >= '0') && (lastDigit < '9')) {
			lastDigit++;
			return prefix + lastDigit;	// works because the + operator implies StringBuilder
		}

		// Anything that reaches this far doesn't have a number at the
		// end and is discarded.
		return null;
	}


}
