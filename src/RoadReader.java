//
//  RoadReader.java
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

import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.ByteBuffer;
import debug.Debugger;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * RoadParser contains utility functions for downloading a road network and
 * turning it into a useable RoadMap data structure.
 * @author Thomas Dickerson
 */
public class RoadReader {

		
	public static byte[] fetchDataFile(String protocol, String host, int port, String path, int bufsize) throws IllegalArgumentException, IOException{
		try{
			URL remoteFile = new URL(protocol, host, port, path);
			return fetchDataFile(remoteFile, bufsize);

		} catch(MalformedURLException e){
			throw new IllegalArgumentException("The arguments must form a valid URL (protocol://host:port/path)", e);
		}
	}
	
	public static byte[] fetchDataFile(URL remoteFile, int bufsize) throws IllegalArgumentException, IOException{
		if(remoteFile == null) throw new IllegalArgumentException("argument remoteFile must not be null");
		if(bufsize < 16){
			Debugger.printError("bufsize ( " + bufsize + " ) too small! setting to 4096");
			bufsize = 4096;
		}
		
		URLConnection remoteConnection = remoteFile.openConnection();
		InputStream roadDataStream = (InputStream)(remoteConnection.getContent());
		byte[] buf = new byte[bufsize]; //a good buffer size? at some point this should become tweakable based on network speed
		ByteArrayOutputStream result = new ByteArrayOutputStream(); //a reasonably sized .gz, though much smaller than any of the road networks
		int readlen = -1;
		int readcount = 0;
		do {
			readlen = roadDataStream.read(buf, 0, bufsize);
			if(readlen > 0){
				result.write(buf, 0, readlen);
				readcount += readlen;
				StringBuilder sb = new StringBuilder(30);
				sb.append("Unpacked ");
				sb.append(readcount / 1024);
				sb.append("KiB ( ");
				sb.append(readcount / (1024 * 1024));
				sb.append("MiB )");
				Debugger.printChatter(sb.toString());
			} 			
		} while( readlen >= 0 );
		result.flush();
		byte[] retval = result.toByteArray();
		roadDataStream.close();
		result.close();
		return retval;
	}
	
	public static RoadGraph deserializeRoadNetwork(byte[] data){
		try{
			ByteArrayInputStream bais = new ByteArrayInputStream(data);
			ObjectInputStream in = new ObjectInputStream(bais);
			return (RoadGraph)(in.readObject());	
		} catch(IOException ioe){
			ioe.printStackTrace();
			return null;
		} catch (ClassNotFoundException cnfe) {
			throw new IllegalArgumentException("The serialized file must be a valid RoadGraph object",cnfe);
		}
	}
	
	public static void serializeRoadNetwork(OutputStream out, RoadGraph graph){
		try{
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(graph);
			oos.close();	
		} catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	public static PointOfInterest[] parsePOIList(String poiData){
		String[] poiTextArray = poiData.split("\n");
		String description = poiTextArray[0].trim();
		int poiCount = poiTextArray.length - 1;
		PointOfInterest[] pointsOfInterest = new PointOfInterest[poiCount];
		int npCount = 0;
		for(int i = 1; i <= poiCount; i++){
			String[] coords	= poiTextArray[i].trim().split(" ");
			if(coords.length == 2){
				int longitude	= Integer.parseInt(coords[0]);
				int latitude	= Integer.parseInt(coords[1]);
				pointsOfInterest[npCount] = new PointOfInterest(longitude, latitude, description);
				npCount++;
			}
			
		}
		return pointsOfInterest;
	}
	
	public static RoadGraph parseRoadNetwork(String mapData){
		String[] mapArray = mapData.split("\n");
		int vertexCount = Integer.parseInt(mapArray[0].trim());
	//	int[] catCounts = {0, 0, 0, 0};
	
		int[] catCounts = {0, 0, 0, 0};
		RoadVertex[] vertices = new RoadVertex[vertexCount];
		int edgeCount = Integer.parseInt(mapArray[vertexCount + 1].trim());
		RoadEdge[] edges = new RoadEdge[edgeCount];
		
		int threadCount = RoadTool.countAppropriateThreads();
		//threadCount = 0;
		AtomicIntegerArray cc = new AtomicIntegerArray(catCounts);
		if(threadCount == 0){
			Debugger.printError("starting vertices");
			RoadWorker job = new RoadWorker(1, vertexCount+1, null, WorkerMode.VERTEXBUILD);
			job.setupVParser(mapArray, vertices);
			job.vertexLoop();
			//vertices = job.vertices;
			
			Debugger.printError("starting edges");
			job = new RoadWorker(vertexCount+2, 2*edgeCount+vertexCount+2, null, WorkerMode.EDGEBUILD);
			job.setupEParser(mapArray, vertices, edges, cc);
			job.edgeLoop();
			edges = job.edges;
		} else{
			int jobCount = threadCount;
			
			Debugger.printError("starting vertices");
			int step = RoadWorker.chunkedTaskStepSize(vertexCount, jobCount);
			RoadWorker[] jobs = new RoadWorker[jobCount];
			CountDownLatch latch = new CountDownLatch(1);
			for(int i = 0; i < jobCount; i++){
				int min = RoadWorker.calcMin(1, step, i);
				int max = RoadWorker.calcMax(1, step, i, vertexCount);
				jobs[i] = new RoadWorker(min, max, latch, WorkerMode.VERTEXBUILD);
				jobs[i].setupVParser(mapArray, vertices);
			}
			RoadTool.doWork(jobs, latch);
			//vertices = new RoadVertex[vertexCount];
			//for(int i = 0; i < jobCount; i++)	System.arraycopy(jobs[i].vertices, 0, vertices, jobs[i].min - 1, jobs[i].vertices.length);
			
			Debugger.printError("starting edges");
			latch = new CountDownLatch(1);
			step = 2 * RoadWorker.chunkedTaskStepSize(edgeCount, jobCount);
			for(int i = 0; i < jobCount; i++){
				int min = RoadWorker.calcMin(2 + vertexCount, step, i);
				int max = RoadWorker.calcMax(2 + vertexCount, step, i, 2 * edgeCount);
				jobs[i] = new RoadWorker(min, max, latch, WorkerMode.EDGEBUILD);
				jobs[i].setupEParser(mapArray, vertices, edges, cc);
			}
			RoadTool.doWork(jobs, latch);
			catCounts[0] = cc.get(0);
			catCounts[1] = cc.get(1);
			catCounts[2] = cc.get(2);
			catCounts[3] = cc.get(3);
			//edges = new RoadEdge[edgeCount];
			//THIS CAN BE MADE FASTER + LESS MEMORY INTENSIVE
			//for(int i = 0; i < jobCount; i++)	System.arraycopy(jobs[i].edges, 0, edges, (jobs[i].min - 2 - vertexCount) / 2, jobs[i].edges.length);
		}

		
		
		
	/*	RoadVertex[] vertices = new RoadVertex[vertexCount];
		int i;
		for(i = 1; i<= vertexCount; i++){
			String[] coords = mapArray[i].trim().split(" ");
			int id			= Integer.parseInt(coords[0]);
			int longitude	= Integer.parseInt(coords[1]);
			int latitude	= Integer.parseInt(coords[2]);
			vertices[id] = new RoadVertex(longitude, latitude);
		}
		
		int edgeCount = Integer.parseInt(mapArray[i].trim());
		RoadEdge[] edges = new RoadEdge[edgeCount];
		for(i++; i <= (1 + vertexCount + (2 * edgeCount)); i+=2){
			String[] ids = mapArray[i].trim().split(" ");
			String[] weight = mapArray[i+1].trim().split(" ");
			int ei		= (i - 1- vertexCount) / 2;
			int id1		= Integer.parseInt(ids[0]);
			int id2		= Integer.parseInt(ids[1]);
			float time		= Float.parseFloat(weight[0]);
			float meters	= Float.parseFloat(weight[1]);
			byte  category	= (byte)((Byte.parseByte(weight[2]) / 10) - 1);	//this nonsense converts the read in type to the A1, A2, etc constants
																			//defined by RoadEdge. be aware that it discards the road details for just the class
			
			catCounts[category]++;
			edges[ei] = new RoadEdge(id1, id2, time, meters, category);
			vertices[id1].addEdge(edges[ei]);
			vertices[id2].addEdge(edges[ei]);
			
		}
		*/
		return new RoadGraph(edges, vertices, catCounts);
	}

}
