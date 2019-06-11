Downloading lots of sequential files can be a pain.  One-by-one gets very tedious.  Since I couldn't find anything, I went ahead and wrote my own.

It's a java app, which means you must have java installed to run it.  So it goes.

And right now, it looks like you can only run it in the same directory as the .class files.  That's the way the ball bounces.

Here's the usage/man:

version 1.1
  USAGE:
	fsucker <url> [-m <max_count>] [-k max_skipped])

The file specified in the <url> is downloaded to the current directory.
The next file in sequential order will be downloaded as well.
This repeats until no file is found or the process is killed.

   -m   Limit the number of files to download to this many.
        Default is 1000.  Using just 1 is nice for testing.

   -k   Keep trying successive numbers this many times until quitting.
        Many sequential files may skip a file or two and then continue.
        This param tells the program to keep trying successive numbers
        for this many times before calling it quits.

examples:
   fsucker http://www.farmville.com/images/weeds3000.jpg

      This will download weeds3000.jpg, weeds3001.jpg ... from farmville.com

   fsucker http://www.myfaveimages.com/graphix/DSC-0001.jpg -m 2

      This will download DSC-0001.jpg and DSC-002.jpg only.

   fsucker http://inconsistentfiles.com/file001.png -k 10
      Let's say that file001.png exists, but there's a gap of four
      files before the sequence continues.  This will keep trying with
      file002.png, file003.png, file004.png (which all fail) and then
      get a hit with file005.png.  The program will continue along with
      sequential files until it fails 10 times in a row, which ends the run.
      
