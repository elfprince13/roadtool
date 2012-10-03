//
//  RoadTool.java
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



import java.util.*;
import java.net.*;
import java.io.*;
import debug.Debugger;
import java.util.concurrent.*;

public class RoadTool {
	
	static int verbosity;
	
	static LinkedList<String> poiPaths;
	static boolean remotePath;
	static String inPath;
	static String outPath;
	static boolean serializedInput;
	static boolean display;
	static boolean serialize;
	static boolean doDjikstra;
	static boolean allNodes;
	static boolean dumpTree;
	static boolean staticCircles;
	static int startNode;
	static Mode weightMode;
	static RenderMode renderMode;
	static String poiDesc1;
	static String poiDesc2;
	
	static ExecutorService exec = null;
	static ExecutorCompletionService<Boolean> ecs = null;
	
	private static enum Argument{
		VERBOSITY,
		INPATH,
		OUTPATH,
		REMOTEPATH,
		SERIALIN,
		DISPLAYOFF,
		ADDPOILIST,
		RUNDJIKSTRA,
		RUNALLDJIKSTRA,
		STATICCIRCLES,
		TWOCOLOR,
		DUMPDJIKSTRATREE,
		WEIGHTMODE,
		RENDERMODE,
		UNKNOWN;
		
		static Argument fromString(String arg){
			if(arg.equals("-v"))	return VERBOSITY;
			else if(arg.equals("-i")) return INPATH;
			else if(arg.equals("-o")) return OUTPATH;
			else if(arg.equals("-r")) return REMOTEPATH;
			else if(arg.equals("-s")) return SERIALIN;
			else if(arg.equals("-t")) return STATICCIRCLES;
			else if(arg.equals("-n")) return DISPLAYOFF;
			else if(arg.equals("-p")) return ADDPOILIST;
			else if(arg.equals("-d")) return RUNDJIKSTRA;
			else if(arg.equals("-2")) return TWOCOLOR;
			else if(arg.equals("-dA")) return RUNALLDJIKSTRA;
			else if(arg.equals("-u")) return DUMPDJIKSTRATREE;
			else if(arg.equals("-w")) return WEIGHTMODE;
			else if(arg.equals("-m")) return RENDERMODE;
			else return UNKNOWN;
		}
	}
	
	public static void usage(String msg){	System.err.println(msg + "\n");	usage(1);	}
	public static void usage(){	usage(0);	}
	public static void usage(int status){
		System.out.println("java -jar -Xmx[memory allocation] RoadTool.jar [options] -i inputfile\n"+
						   "-v [0-3]\t\tSet Verbosity. Defaults to 2\n"+
						   "\t\t\t0 is silent\n"+
						   "\t\t\t1 is errors only\n"+
						   "\t\t\t2 is normal\n"+
						   "\t\t\t3 is chatter\n"+
						   "-o filename\t\tWrite serialized RoadGraph to filename\n"+
						   "-r\t\t\tinputfile is an Internet URL computer\n"+
						   "-s\t\t\tinputfile is a serialized RoadGraph\n"+
						   "-n\t\t\tdon't attempt to display inputfile\n"+
						   "-p filename\t\tLoad POIs from filename\n"+
						   "-dA poiDescription\truns Djikstra and dumps information\n"+
						   "\t\t\tabout POI distribution\n"+
						   "-2 poiDescription\truns Djikstra with a second set\n"+
						   "\t\t\tof POIs (2 color). Requires -dA\n"+
						   "-d nodenumber\t\truns Djikstra with nodenumber as root\n"+
						   "-u\t\t\tdump the Djikstra tree to the console\n"+
						   "\t\t\timplies -d, by default node is set to 0\n"+
						   "\t\t\tunless specified otherwise by -d\n"+
						   "\t\t\tand ignored by -dA\n"+
						   "-t\t\t\tUse a static radius when\n"+
						   "\t\t\tanalyzing POI distribution\n"+
						   "\t\t\tignored if not used with -dA\n"+
						   "-w DISTANCE/TIME\tchooses between weighting the graph by DISTANCE\n"+
						   "\t\t\tand TIME. Defaults to TIME\n"+
						   "-m RECT/FLATPOL\t\tchooses between interpreting coordinates\n"+
						   "\t\t\tas a grid (FLATPOL) or converting them\n"+
						   "\t\t\tto rectangular coordinates (RECT)");
		System.exit(status);
	}
	
	private static int intArg(Stack<String> args, String msg){	return Integer.parseInt(nextArg(args, msg));	}
	private static String stringArg(Stack<String> args, String msg){	return nextArg(args, msg);	}
	private static String nextArg(Stack<String> args, String msg){
		if(args.isEmpty())	usage(msg);	//this will exit.	
		return args.pop();
		
	}
		
	private static int intArg(Stack<String> args, String msg, String arg, int min, int max){
		int val = intArg(args, msg);
		if(val < min || val > max) usage("Argument to " + arg + " must be in the range [" + min + "-" + max + "]");
		return val;
	}
	
	
	public static void handleArgs(String[] argv){
		verbosity = Debugger.NORMAL;	//-v [0-3]
		inPath = "";					//-i filename
		outPath = "";					//-o filename
		remotePath = false;				//-r
		serialize = false;				//set by -o
		serializedInput = false;		//set by -s
		display = true;					//turned off by -n
		poiPaths = new LinkedList<String>();	//added by -p filename
		doDjikstra = false;						//turned on by -d
		allNodes = false;						//-dA
		startNode = 0;							//-d nodenumber
		dumpTree = false;						//-u IMPLIES -d, by default node is set to 0
		staticCircles = false;
		weightMode = Mode.TIME;					//-w DISTANCE/TIME
		renderMode = RenderMode.RECT;			//-m RECT/FLATPOL
		poiDesc1 = "";					//set by -dA
		poiDesc2 = "";					//set by -2, but ignored withou -dA
		
		
		Stack<String> args = new Stack<String>();
		for(int i=(argv.length - 1); i >= 0; i--){
			args.push(argv[i]);
		}
		
		if(args.empty()) usage();
		while(!args.empty()){
			String arg = args.pop();
			switch(Argument.fromString(arg)){
				case VERBOSITY:
					verbosity = intArg(args, "-v option must be followed by a numerical argument", arg, 0, 3);
					break;
				case INPATH:
					inPath = stringArg(args, "-i option must be followed by a filename");
					break;
				case OUTPATH:
					serialize = true;
					outPath = stringArg(args, "-o option musg be followed by a filename");
					break;
				case REMOTEPATH:
					remotePath = true;
					break;
				case SERIALIN:
					serializedInput = true;
					break;
				case DISPLAYOFF:
					display = false;
					break;
				case ADDPOILIST:
					poiPaths.add(stringArg(args, "-p option must be followed by a filename"));
					break;
				case RUNDJIKSTRA:
					doDjikstra = true;
					String node = stringArg(args, "-d option must be followed by a root node or A to specify all nodes");
					startNode = Integer.parseInt(node);
					break;
				case RUNALLDJIKSTRA:
					doDjikstra = true;
					allNodes = true;
					poiDesc1 = stringArg(args, "-dA should be followed by a poiDescription to match");
					break;
				case TWOCOLOR:
					poiDesc2 = stringArg(args, "-2 should be followed by a poiDescription to match");
					break;
				case DUMPDJIKSTRATREE:
					dumpTree = true;
					doDjikstra = true;
					break;
				case STATICCIRCLES:
					staticCircles = true;
					break;
				case WEIGHTMODE:
					String modeText = stringArg(args, "-w must be followed by either DISTANCE or TIME");
					if (modeText.toLowerCase().equals("distance"))	weightMode = Mode.DISTANCE;
					else if(modeText.toLowerCase().equals("time"))	weightMode = Mode.TIME;
					else	usage("Unrecognized argument '" + modeText + "' for -w");
					break;
				case RENDERMODE:
					modeText = stringArg(args, "-m must be followed by either RECT or FLATPOL");
					if (modeText.toLowerCase().equals("rect"))	renderMode = RenderMode.RECT;
					else if(modeText.toLowerCase().equals("flatpol"))	renderMode = RenderMode.FLATPOL;
					else	usage("Unrecognized argument '" + modeText + "' for -m");
					break;
				default:
					usage("Unrecognized option: " + arg);
			}
		}
	}

	public static void main (String argv[]) {
		//will likely need to be run with something like the following settings:
		//java -Xms32m -Xmx512m -jar RoadTool.jar
		//or higher...this works at least for all of New England
		//took about 45 seconds to display, as opposed to the 6 seconds or so for just VT
		//window scaling isn't smooth, but is at least without too much lag.
		URLConnection.setContentHandlerFactory(new net.www.GZIPHandlerFactory() );
		
		handleArgs(argv);
		Debugger.setVerbosity(verbosity);
		RoadDisplay.setRenderMode(renderMode);
				
		int numThreads = countAppropriateThreads();
		if(numThreads > 0){
			exec = Executors.newFixedThreadPool(numThreads);
			ecs = new ExecutorCompletionService<Boolean>(exec);
		}
		
		try{
			//ignore the first run, if you're timing for performance
			//its bogus data because the next runs through have already seen the data at least once...
			//which means no disk access for them
			
			//Usage 1 (from interwebs)
			//http://www.dis.uniroma1.it/~challenge9/data/tiger/
			//String roadData = RoadReader.fetchRoadNetwork("http", "www.dis.uniroma1.it", 80, "/~challenge9/data/tiger/VT.tmp.gz");
			
			//Usage 2 (from local computer)
			
			RoadGraph graph;
			long start;
			//inPath = "/Users/thomas/Downloads/DIMACS/CTRI.tmp.gz";
			if(!inPath.equals("")){
				Debugger.printInfo("Opening road network file");
				start = System.currentTimeMillis();
				URL input = remotePath ? (new URL(inPath)) : (new File(inPath)).toURL();
				
				byte[] roadData = RoadReader.fetchDataFile(input, 4096);
				Debugger.printInfo("Unpacked road network contains " + roadData.length	+ " bytes");
				Debugger.printTiming(start, System.currentTimeMillis());
				if(!serializedInput){
					Debugger.printInfo("Building graph datastructure");
					start = System.currentTimeMillis();
					graph = RoadReader.parseRoadNetwork(new String(roadData));
					Debugger.printTiming(start, System.currentTimeMillis());

				} else{
					//handle serialized input
					Debugger.printInfo("Deserializing graph datastructure");
					start = System.currentTimeMillis();
					graph = RoadReader.deserializeRoadNetwork(roadData);
					Debugger.printTiming(start, System.currentTimeMillis());
				}
				//make sure you clear this up too, to free memory if you do more runs.
				roadData = null;
				System.runFinalization();
				System.gc();
				
				while(poiPaths.size() > 0){
					String poiPath = poiPaths.remove();
					Debugger.printInfo("Opening POI list " + poiPath);
					start = System.currentTimeMillis();
					String poiData = new String(RoadReader.fetchDataFile((new File(poiPath)).toURL(), 4096));
					Debugger.printInfo("Unpacked POI list contains " + poiData.length()	+ " characters");
					Debugger.printTiming(start, System.currentTimeMillis());
					
					
					Debugger.printInfo("Parsing POI list:");
					start = System.currentTimeMillis();
					PointOfInterest[] pointsOfInterest = RoadReader.parsePOIList(poiData);
					Debugger.printInfo("\t" + pointsOfInterest.length + " points of interest");
					Debugger.printTiming(start, System.currentTimeMillis());
					
					
					Debugger.printInfo("Assigning POIs to Vertices");
					start = System.currentTimeMillis();
					graph.assignPointsOfInterest(pointsOfInterest);
					Debugger.printTiming(start, System.currentTimeMillis());			
					
				}
				
				Debugger.printInfo("Other statistics follow:");
				Debugger.printInfo("The road network contains " + graph.countVertices() + " vertices and " + graph.countEdges() + " edges.");
				int[] counts = graph.countCategories();
				Debugger.printInfo("Of these, " + counts[RoadEdge.A1] + " are Interstate Highways, " +
								   counts[RoadEdge.A2] + " are US Highways, " +
								   counts[RoadEdge.A3] + " are state highways, and " +
								   counts[RoadEdge.A4] + " are local roads.\n");
				Debugger.printInfo("The road network also contains " + graph.countPointsOfInterest() + " points of interest");
				
				//RoadDjikstra.preprocessPOIs(graph, weightMode, poiDescription);
				//System.exit(0);
				
				if(serialize){
					Debugger.printChatter("SERIALIZE!!");
					if(!outPath.equals("")){
						Debugger.printInfo("Serializing graph datastructure to file " + outPath);
						start = System.currentTimeMillis();
						try{
							File outFile = new File(outPath);
							FileOutputStream fos = new FileOutputStream(outFile);
							RoadReader.serializeRoadNetwork(fos, graph);
						} catch (IOException ioe) {
							Debugger.printError("Serialization of graph failed");
							ioe.printStackTrace();
						}	
						Debugger.printTiming(start, System.currentTimeMillis());
					} else{	Debugger.printError("No output file specified!");	System.exit(1);	}
				} else{
					Debugger.printChatter("DON'T SERIALIZE!!!");
				}
				
				if(doDjikstra){
					if(!allNodes){
						Debugger.printInfo("Running Djikstra SSSPP algorithm");
						start = System.currentTimeMillis();
						RoadDjikstra djikstra = RoadDjikstra.initializeSSSPP(graph.getVertex(startNode), graph, weightMode);
						Debugger.printTiming(start, System.currentTimeMillis());
						if(dumpTree) djikstra.dumpTree();
					} else{
						//run on all nodes and dump the results
						Debugger.printInfo("Running POI bounded Djikstra SSSPP");
						start = System.currentTimeMillis();
						if(poiDesc2.equals("")) RoadDjikstra.dumpAllNearestPoi(graph, weightMode, poiDesc1);
						else RoadDjikstra.dumpAllNearestTwoColorPoi(graph, weightMode, poiDesc1, poiDesc2);
						Debugger.printTiming(start, System.currentTimeMillis());
					}
				}
				
				if(display){
					Debugger.printInfo("Generating Rendering Info");
					start = System.currentTimeMillis();
					RoadGraph.RenderInfo data = graph.getRenderInfo();
					Debugger.printTiming(start, System.currentTimeMillis());
					
					Thread displayT = new Thread(new RoadDisplay(data));
					RoadDisplay.displayT = displayT;
					displayT.start();
				}
				
			} else{	Debugger.printError("Input file required!");	System.exit(1);	}
			
			
		} catch (IOException e){
			e.printStackTrace();
		}
		if(exec != null && !exec.isShutdown())	exec.shutdown();
	}
	
	public static int countAppropriateThreads(){
		int procs = Runtime.getRuntime().availableProcessors();
		//if this is a single core machine, don't start a thread for, just do the processing in main
		return (procs == 1) ? 0 : procs;
	}
	
	public static <T extends Runnable> T[] doWork(T[] jobs, CountDownLatch cdl/*, String msg*/){
		int numThreads = countAppropriateThreads();
		Debugger.printError("Available Procs: " + numThreads);
		//Debugger.printInfo(msg);
		//Debugger.printInfo("Running " + jobs.length + " tasks in " + numThreads + " threads");
		if(exec == null || exec.isShutdown()){
			exec = Executors.newFixedThreadPool(numThreads);
			ecs = new ExecutorCompletionService<Boolean>(exec);
		}
		//synchronized (jobs) {
		for(int i = 0; i < jobs.length; i++){
			ecs.submit(jobs[i], new Boolean(true));
		}
		cdl.countDown();
		//}
		for(int i = 0; i < jobs.length; i++){
			try{
				Debugger.printError("Job " + i + " of " + jobs.length + " complete");
				ecs.take();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		return jobs;
	}
}
