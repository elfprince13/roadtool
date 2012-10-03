//
//  Debugger.java
//
//  Copyright Thomas Dickerson, 2012.
//  This file is part of RoadTool.
//
//  RoadTool is free software: you can redistribute it and/or modify it
//  under the terms of the GNU Lesser General Public License as published
//  by the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  RoadTool is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public License
//  long with RoadTool.  If not, see <http://www.gnu.org/licenses/>.
//

package debug;

import java.io.*;

public class Debugger {
	
	public static final int SILENT = 0;
	public static final int ERR = 1;
	public static final int NORMAL = 2;
	public static final int ALL = 3;
	static int VERBOSITY = SILENT;
	static PrintStream errStream = System.err;
	static PrintStream outStream = System.out;
	
	public static void printInfo(String str){	if(VERBOSITY >= NORMAL) outStream.println(str);	} //If I get less lazy make a way to log to file too
	public static void printChatter(String str){	if(VERBOSITY == ALL) outStream.println(str);	}
	public static void printTiming(long start, long end){	if(VERBOSITY != SILENT) outStream.println("-task completed in " + (end - start) + "ms\n");		}
	public static void printError(String str){	if(VERBOSITY != SILENT) errStream.println(str);	} //Or they could just redirect stdout/stdin
	public static void redirectDebugOut(PrintStream out){	outStream = out;	}
	public static void redirectDebugErr(PrintStream err){	errStream = err;	}
	public static void setVerbosity(int i){	VERBOSITY = i % 4;	}
	public static int getVerbosity(){	return VERBOSITY;	}
}
