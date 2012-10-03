//
//  RoadWorker.java
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
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import debug.Debugger;

enum WorkerMode{
	VERTEXBUILD,
	EDGEBUILD,
	POIBUILD,
	DJIKSTRA
}

public class RoadWorker implements Runnable{

	//chunk of ddata to work on
	int min;
	int max;
	
	//work mode
	WorkerMode mode;
	CountDownLatch latch;
	
	
	String[] data;
	RoadVertex[] vertices;
	RoadEdge[] edges;
	PointOfInterest[] pointsOfInterest;
	//int[] catCounts;
	AtomicIntegerArray catCounts;
	RoadGraph graph;
	Mode mMode;
	String poiDesc1;
	String poiDesc2;
	int ignoredPoi;
	ConcurrentHashMap<OrderedPOIPair, Float> lookup;
	public RoadWorker(int mn, int mx, CountDownLatch cdl, WorkerMode m){
		min = mn;
		max = mx;
		latch = cdl;
		mode = m;
	}
	
	public void setupPParser(PointOfInterest[] pois, RoadVertex[] v){
		pointsOfInterest = pois;
		vertices = v;
		ignoredPoi = 0;
	}
	
	public void setupVParser(String[] d, RoadVertex[] v){
		data = d;
		vertices = v;
	}
	
	public void setupEParser(String[] d, RoadVertex[] v, RoadEdge[] e, AtomicIntegerArray c){
		data = d;
		vertices = v;
		edges = e;
		catCounts = c;
	}
	
	public void setupDjikstra(ConcurrentHashMap<OrderedPOIPair, Float> l, RoadGraph g, Mode m, String pd){
		graph = g;
		vertices = g.getVertices();
		mMode = m;
		poiDesc1 = pd;
		poiDesc2 = null;
		lookup = l;
	}
	
	public void setupDjikstra(ConcurrentHashMap<OrderedPOIPair, Float> l, RoadGraph g, Mode m, String pd1, String pd2){
		graph = g;
		vertices = g.getVertices();
		mMode = m;
		poiDesc1 = pd1;
		poiDesc2 = pd2;
		lookup = l;
	}
	
	public void edgeLoop(){
		//edges = new RoadEdge[(max - min) / 2];
		for(int i=min; i < max; i+=2){
			String[] ids = data[i].trim().split(" ");
			String[] weight = data[i+1].trim().split(" ");
			//int ei = (i - min) / 2;
			int ei		= (i - 1- vertices.length) / 2;
			int id1		= Integer.parseInt(ids[0]);
			int id2		= Integer.parseInt(ids[1]);
			float time		= Float.parseFloat(weight[0]);
			float meters	= Float.parseFloat(weight[1]);
			byte  category	= (byte)((Byte.parseByte(weight[2]) / 10) - 1);	//this nonsense converts the read in type to the A1, A2, etc constants
																			//defined by RoadEdge. be aware that it discards the road details for just the class
			
			catCounts.incrementAndGet(category);
			edges[ei] = new RoadEdge(id1, id2, time, meters, category);
			vertices[id1].addEdge(edges[ei]);
			vertices[id2].addEdge(edges[ei]);
		}
	}
	
	public void vertexLoop(){
		//vertices = new RoadVertex[max - min];
		for(int i = min; i< max; i++){
			String[] coords = data[i].trim().split(" ");
			int id			= Integer.parseInt(coords[0]);
			int longitude	= Integer.parseInt(coords[1]);
			int latitude	= Integer.parseInt(coords[2]);
			RoadVertex rv = new RoadVertex(longitude, latitude);
			//vertices[1 + id - min] = rv;
			vertices[id] = rv;
		}
	}
	
	public void djikstraLoop(){
		
		for(int i = min; i < max; i++){
			RoadVertex v = vertices[i];
			//countNearestPOI(RoadVertex s, RoadGraph g, Mode m, String poiDescription){
			//countDynamicNearestPOI(ConcurrentHashMap<OrderedPOIPair, Float> lookup, RoadVertex s, RoadGraph g, Mode m, String poiDescription)
			int m, n;
			if (poiDesc2 == null){
				m = (RoadTool.staticCircles) ?  RoadDjikstra.countNearestPOI(v, graph, mMode, poiDesc1) : RoadDjikstra.countDynamicNearestPOI(lookup, v, graph, mMode, poiDesc1);
				n = -1;
			} else{
				if(RoadTool.staticCircles){
					Debugger.printError("This test only runs in dynamic mode");	
					System.exit(1);
				}
				int[] ans = RoadDjikstra.countDynamicNearestTwoColorPOI(lookup, v, graph, mMode, poiDesc1, poiDesc2);
				m = ans[0];
				n = ans[1];
			}
			StringBuilder sb = new StringBuilder(16);
			sb.append(i);
			sb.append(",\t");
			sb.append(m);
			if(n > -1){
				sb.append(",\t");
				sb.append(n);
			}
			Debugger.printInfo(sb.toString());
		}
	}
	
	public void poiLoop(){
		//Debugger.printInfo(min + " " + max);
		PointOfInterest poi;
		PointOfInterest oops;
		for(int i = min; i < max; i++){
			//Debugger.printInfo("" + i);
			poi = pointsOfInterest[i];
			RoadVertex closest = poi.findClosest(vertices);
			StringBuilder sb = new StringBuilder(41);
			sb.append("POI: ");
			sb.append(poi.toStringBuffer());
			sb.append("\nClosest Vertex: ");
			sb.append(closest.toStringBuffer());
			Debugger.printChatter(sb.toString());
			poi.setClosest(closest);
			oops = closest.addPointOfInterest(pointsOfInterest[i]);
			if(oops != null){
				oops.setClosest(null);
				ignoredPoi++;
				sb = new StringBuilder(100);
				sb.append("A POI of type ");
				sb.append(poi.getDescription());
				sb.append(" is already associated with the RoadVertex at ");
				sb.append(closest.toStringBuffer());
				sb.append("\nOnly one will be counted. This is most likely due to multiple buildings at the same address");
				Debugger.printError(sb.toString());	
			}
		}	
	}
	
	public void run(){
		try{
			if(latch != null)	latch.await();
	
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
		switch (mode) {
			case VERTEXBUILD:
				vertexLoop();
				break;
			case EDGEBUILD:
				edgeLoop();
				break;
			case POIBUILD:
				poiLoop();
				break;
			case DJIKSTRA:
				djikstraLoop();
				break;
			default:
				break;
		}
	}
	
	public static int calcMin(int step, int i){	return step * i;	}
	public static int calcMin(int offset, int step, int i){	return offset + calcMin(step, i);	}
	public static int calcMax(int step, int i, int max){	return (step * (i + 1) > max) ? max : step * (i + 1);	}
	public static int calcMax(int offset, int step, int i, int max){	return offset + calcMax(step, i, max);	}
	public static int chunkedTaskStepSize(int items, int jobs){
		int leftovers = (items % jobs);
		return (items - leftovers + ((leftovers >= 0) ? jobs : 0)) / jobs;
	}
	
	
}
