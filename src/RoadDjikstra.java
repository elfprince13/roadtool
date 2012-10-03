//
//  RoadDjikstra.java
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import debug.Debugger;

public class RoadDjikstra {
	
	private Mode mode;
	private RoadVertex source;
	private int sourceId;
	private RoadGraph graph;
	private RDIndexData[] index;
	private RDIndexData[] heap;
	private int heapCount;
	private int vCount;
	
	private RoadDjikstra(RoadVertex s, RoadGraph g, Mode m){
		heapCount = 0;
		source = s;
		graph = g;
		mode = m;
		vCount = g.getVertices().length;
		index = new RDIndexData[vCount];
		heap = new RDIndexData[vCount];
	}
	
	public static void dumpAllNearestPoi(RoadGraph g, Mode m, String poiDescription){
		
		int threadCount = RoadTool.countAppropriateThreads();
		//threadCount = 0;
		int vertexCount = g.countVertices();
		
		ConcurrentHashMap<OrderedPOIPair, Float> lookup;
		if(!RoadTool.staticCircles) lookup = preprocessPOIs(g, m, poiDescription);
		else lookup = null;
		
		Debugger.printInfo("#########\tBEGIN POI DISTRIBTION DUMP\t#########");
		Debugger.printInfo("Vertex ID (P),\tPOI within Dp");
		//threadCount = 0;
		if(threadCount == 0){
			RoadWorker job = new RoadWorker(0, vertexCount, null, WorkerMode.DJIKSTRA);
			job.setupDjikstra(lookup, g, m, poiDescription);
			job.djikstraLoop();
		} else{
			int jobCount = threadCount * 4;
			int step = RoadWorker.chunkedTaskStepSize(vertexCount, jobCount);
			RoadWorker[] jobs = new RoadWorker[jobCount];
			CountDownLatch latch = new CountDownLatch(1);
			for(int i = 0; i < jobCount; i++){
				int min = RoadWorker.calcMin(step, i);
				int max = RoadWorker.calcMax(step, i, vertexCount);
				jobs[i] = new RoadWorker(min, max, latch, WorkerMode.DJIKSTRA);
				jobs[i].setupDjikstra(lookup, g, m, poiDescription);
			}
			RoadTool.doWork(jobs, latch);
		}
		Debugger.printInfo("#########\tEND POI DISTRIBTION DUMP\t#########");
		
	}
	
	public static void dumpAllNearestTwoColorPoi(RoadGraph g, Mode m, String pDesc1, String pDesc2){
		
		int threadCount = RoadTool.countAppropriateThreads();
		//threadCount = 0;
		int vertexCount = g.countVertices();
		
		ConcurrentHashMap<OrderedPOIPair, Float> lookup;
		if(!RoadTool.staticCircles) lookup = preprocessTwoColorPOIs(g, m, pDesc1, pDesc2);
		else{
			lookup = null;
			Debugger.printError("This test only runs in dynamic mode");	
			System.exit(1);
		} 
		
		Debugger.printInfo("Vertex ID (P),\tPOI(1) within Dp\tPOI(2) within Dp");
		//threadCount = 0;
		if(threadCount == 0){
			RoadWorker job = new RoadWorker(0, vertexCount, null, WorkerMode.DJIKSTRA);
			job.setupDjikstra(lookup, g, m, pDesc1, pDesc2);
			job.djikstraLoop();
		} else{
			int jobCount = threadCount * 4;
			int step = RoadWorker.chunkedTaskStepSize(vertexCount, jobCount);
			RoadWorker[] jobs = new RoadWorker[jobCount];
			CountDownLatch latch = new CountDownLatch(1);
			for(int i = 0; i < jobCount; i++){
				int min = RoadWorker.calcMin(step, i);
				int max = RoadWorker.calcMax(step, i, vertexCount);
				jobs[i] = new RoadWorker(min, max, latch, WorkerMode.DJIKSTRA);
				jobs[i].setupDjikstra(lookup, g, m, pDesc1, pDesc2);
			}
			RoadTool.doWork(jobs, latch);
		}
		Debugger.printInfo("#########\tEND POI DISTRIBTION DUMP\t#########");
		
	}
	
	public static ConcurrentHashMap<OrderedPOIPair, Float> preprocessTwoColorPOIs(RoadGraph g, Mode m, String pDesc1, String pDesc2){
		//step 1
		long start = System.currentTimeMillis();
		PointOfInterest[] pointsOfInterest = g.getPointsOfInterest();
		RoadVertex[] vertices;
		PointOfInterest poi;
		String curType;
		RoadVertex v;
		int appropriateThreads = 1+ RoadTool.countAppropriateThreads();
		int poiCount = pointsOfInterest.length;
		int rVisited = 0;
		int bVisited = 0;
		ConcurrentHashMap<OrderedPOIPair, Float> lookup = new ConcurrentHashMap<OrderedPOIPair, Float>(10 * poiCount, 0.8f, appropriateThreads);
		ConcurrentHashMap<PointOfInterest, AtomicInteger> counts = new ConcurrentHashMap<PointOfInterest, AtomicInteger>((5 * poiCount) / 4, 0.8f, appropriateThreads);
		
		for(int i = 0; i < pointsOfInterest.length; i++){
			poi = pointsOfInterest[i];
			curType = poi.getDescription();
			v = poi.getClosest();
			if(v != null && (curType.equals(pDesc1) || curType.equals(pDesc2))){
				RoadDjikstra djikstra = new RoadDjikstra(v, g, m);
				vertices = djikstra.getVertices();
				djikstra.buildIndexedHeap(v, vertices);
				if(curType.equals(pDesc1)){
				   rVisited += djikstra.lookupTwoColorPOIDistances(vertices, pDesc1, pDesc2, lookup, counts);
				} else{
					bVisited += djikstra.lookupTwoColorPOIDistances(vertices, pDesc2, pDesc1, lookup, counts);
				}
			}
		}
		
		long end = System.currentTimeMillis();
		Debugger.printInfo("Preprocessing:");
		Debugger.printInfo("\t" + g.countEdges() + " edges were visited ( " + pDesc1 + " -> " + pDesc2 + " ), on average, " + (rVisited / ((float)g.countEdges())) + " each.");
		Debugger.printInfo("\t" + g.countEdges() + " edges were visited ( " + pDesc2 + " -> " + pDesc1 + " ), on average, " + (bVisited / ((float)g.countEdges())) + " each.");
		Debugger.printInfo("\t" + "POI lookup contains " + lookup.keySet().size() + " pairings.");
		Debugger.printTiming(start, end);
		return lookup;
	}
	
	public static ConcurrentHashMap<OrderedPOIPair, Float> preprocessPOIs(RoadGraph g, Mode m, String poiDescription){
		//step 1
		long start = System.currentTimeMillis();
		PointOfInterest[] pointsOfInterest = g.getPointsOfInterest();
		RoadVertex[] vertices;
		PointOfInterest poi;
		RoadVertex v;
		int appropriateThreads = 1 + RoadTool.countAppropriateThreads();
		int poiCount = pointsOfInterest.length;
		int visited = 0;
		ConcurrentHashMap<OrderedPOIPair, Float> lookup = new ConcurrentHashMap<OrderedPOIPair, Float>(10 * poiCount, 0.8f, appropriateThreads);
		ConcurrentHashMap<PointOfInterest, AtomicInteger> counts = new ConcurrentHashMap<PointOfInterest, AtomicInteger>((5 * poiCount) / 4, 0.8f, appropriateThreads);
		
		for(int i = 0; i < pointsOfInterest.length; i++){
			poi = pointsOfInterest[i];
			v = poi.getClosest();
			if(v != null){
				RoadDjikstra djikstra = new RoadDjikstra(v, g, m);
				vertices = djikstra.getVertices();
				djikstra.buildIndexedHeap(v, vertices);
				visited += djikstra.lookupPOIDistances(vertices, poiDescription, lookup, counts);
			}
		}
		long end = System.currentTimeMillis();
		Debugger.printInfo("Preprocessing:");
		Debugger.printInfo("\t" + g.countEdges() + " edges were visited, on average, " + (visited / ((float)g.countEdges())) + " each.");
		Debugger.printInfo("\t" + "POI lookup contains " + lookup.keySet().size() + " pairings.");
		Debugger.printTiming(start, end);
		return lookup;
	}
	
	public static int[] countDynamicNearestTwoColorPOI(ConcurrentHashMap<OrderedPOIPair, Float> lookup, RoadVertex s, RoadGraph g, Mode m, String poiDesc1, String poiDesc2){
		RoadDjikstra djikstra = new RoadDjikstra(s, g, m);
		RoadVertex[] vertices = djikstra.getVertices();
		djikstra.buildIndexedHeap(s, vertices);
		return djikstra.buildDynamicallyTwoColorPOIBoundedSSSPPTree(lookup, vertices, poiDesc1, poiDesc2);
	}
	
	public static int countDynamicNearestPOI(ConcurrentHashMap<OrderedPOIPair, Float> lookup, RoadVertex s, RoadGraph g, Mode m, String poiDescription){
		RoadDjikstra djikstra = new RoadDjikstra(s, g, m);
		RoadVertex[] vertices = djikstra.getVertices();
		djikstra.buildIndexedHeap(s, vertices);
		return djikstra.buildDynamicallyPOIBoundedSSSPPTree(lookup, vertices, poiDescription);
	}
										 
	public static int countNearestPOI(RoadVertex s, RoadGraph g, Mode m, String poiDescription){
		RoadDjikstra djikstra = new RoadDjikstra(s, g, m);
		RoadVertex[] vertices = djikstra.getVertices();
		djikstra.buildIndexedHeap(s, vertices);
		return djikstra.buildPOIBoundedSSSPPTree(vertices, poiDescription);
	}
	
	public static RoadDjikstra initializeSSSPP(RoadVertex s, RoadGraph g, Mode m){
		RoadDjikstra djikstra = new RoadDjikstra(s, g, m);
		RoadVertex[] vertices = djikstra.getVertices();
		djikstra.buildIndexedHeap(s, vertices);
		djikstra.buildSSSPPTree(vertices);
		return djikstra;
	}
	
	private void buildIndexedHeap(RoadVertex source, RoadVertex[] vertices){
		for(int i = 0; i < vCount; i++){
			if(vertices[i] == source) addSource(vertices[i], i);
			else addToHeap(vertices[i], i);
		}
	}
	
	private int lookupTwoColorPOIDistances(RoadVertex[] vertices, String pDescSrc, String pDescDst, ConcurrentHashMap<OrderedPOIPair, Float> lookup, ConcurrentHashMap<PointOfInterest, AtomicInteger> counts){
		processNode(vertices, sourceId);
		float searchDistance = -1;
		int visited = 0;
		PointOfInterest center = source.getPointOfInterest(pDescSrc);
		counts.put(center, new AtomicInteger(1));
		while(heapCount > 0){
			RDIndexData node = deleteMin();
			if(node.getWeight() == Float.POSITIVE_INFINITY) break; //We're on an island. screw the rest of the graph. 
			visited += processNode(vertices, node.getId());
			RoadVertex v = node.getData();
			if(v.hasPointOfInterest(pDescDst)){
				if(searchDistance < 0) searchDistance = 2 * node.getWeight();
				lookup.put(new OrderedPOIPair(center, v.getPointOfInterest(pDescDst)), new Float(node.getWeight()));
				counts.get(center).incrementAndGet();
				
			}
			if(node.getWeight() >= searchDistance && searchDistance >= 0) break;
		}
		return visited;
	}
	
	private int lookupPOIDistances(RoadVertex[] vertices, String poiDescription, ConcurrentHashMap<OrderedPOIPair, Float> lookup, ConcurrentHashMap<PointOfInterest, AtomicInteger> counts){
		processNode(vertices, sourceId);
		float searchDistance = -1;
		int visited = 0;
		PointOfInterest center = source.getPointOfInterest(poiDescription);
		counts.put(center, new AtomicInteger(1));
		while(heapCount > 0){
			RDIndexData node = deleteMin();
			if(node.getWeight() == Float.POSITIVE_INFINITY) break; //We're on an island. screw the rest of the graph. 
			visited += processNode(vertices, node.getId());
			RoadVertex v = node.getData();
			if(v.hasPointOfInterest(poiDescription)){
				if(searchDistance < 0) searchDistance = 2 * node.getWeight();
				lookup.put(new OrderedPOIPair(center, v.getPointOfInterest(poiDescription)), new Float(node.getWeight()));
				counts.get(center).incrementAndGet();
				
			}
			if(node.getWeight() >= searchDistance && searchDistance >= 0) break;
		}
		return visited;
	}
	
	private float minWeightInSet(HashSet<RDIndexData> found){
		Iterator<RDIndexData> i = found.iterator();
		float min = i.next().getWeight();
		float tmp;
		while (i.hasNext()) {
			tmp = i.next().getWeight();
			if (tmp < min){
				min = tmp;
			}
		}
		return min;
	}
	
	private int[] buildDynamicallyTwoColorPOIBoundedSSSPPTree(ConcurrentHashMap<OrderedPOIPair, Float> lookup, RoadVertex[] vertices, String poiDesc1, String poiDesc2){
		processNode(vertices, sourceId);
		HashSet[] found = {new HashSet<RDIndexData>(20, 0.75f), new HashSet<RDIndexData>(20, 0.75f)};
		float maxSearchDistance = -1;
		int curType, otherType;
		boolean has1, has2;
		while(heapCount > 0){
			RDIndexData node = deleteMin();
			if(node.getWeight() == Float.POSITIVE_INFINITY) break; //We're on an island. screw the rest of the graph. 
			processNode(vertices, node.getId());
			RoadVertex v = node.getData();
			curType = otherType = 0;
			has1 = v.hasPointOfInterest(poiDesc1);
			has2 = v.hasPointOfInterest(poiDesc2);
			
			if (has1 && has2){
				// SPECIAL CASE - TWO Sites at the same vertex.
				found[0].add(v.getPointOfInterest(poiDesc1));
				found[1].add(v.getPointOfInterest(poiDesc2));
				maxSearchDistance = node.getWeight();
			} else if(has1 ^ has2){
				PointOfInterest poi;
				String otherDescription;
				if(has1){
					curType = 0;
					otherType = 1;
					poi = v.getPointOfInterest(poiDesc1);
					otherDescription = poiDesc2;
				} else{
					curType = 1;
					otherType = 0;
					poi = v.getPointOfInterest(poiDesc2);
					otherDescription = poiDesc1;
				}
				
				if(found[otherType].size() > 0 && found[curType].size() == 0){
					maxSearchDistance = node.getWeight() + minWeightInSet(found[otherType]);
					//Debugger.printInfo("Radius set at: " + maxSearchDistance);
				} else if (found[curType].size() > 0 && found[otherType].size() > 0){
					//Iterator<RDIndexData> i = found.iterator();
					for(RDIndexData inode : (HashSet<RDIndexData>)found[otherType]){ //wtfsyntax. For those who don't know, its Java's version of a for-each loop.
						float dsp2 = inode.getWeight();
						float dsp1 = node.getWeight();
						PointOfInterest poi2 = inode.getData().getPointOfInterest(otherDescription);
						Float dp1p2 = lookup.get(new OrderedPOIPair(poi, poi2));
						if(dp1p2 != null){
							float perimeter = (dsp2 + dsp1 + dp1p2.floatValue()) / 2.0f;
							if(maxSearchDistance > perimeter){
								//Debugger.printInfo("Radius shrunk to: " + perimeter);
								maxSearchDistance = perimeter;	
							}
						}
					}
				}
				found[curType].add(node);
			}
			if(node.getWeight() >= maxSearchDistance && maxSearchDistance >= 0) break;
		}
		int[] ans = {found[0].size(), found[1].size()};
		return ans;
	}
	
	private int buildDynamicallyPOIBoundedSSSPPTree(ConcurrentHashMap<OrderedPOIPair, Float> lookup, RoadVertex[] vertices, String poiDescription){
		processNode(vertices, sourceId);
		HashSet<RDIndexData> found = new HashSet<RDIndexData>(20, 0.75f);
		float maxSearchDistance = -1;
		while(heapCount > 0){
			RDIndexData node = deleteMin();
			if(node.getWeight() == Float.POSITIVE_INFINITY) break; //We're on an island. screw the rest of the graph. 
			processNode(vertices, node.getId());
			RoadVertex v = node.getData();
			if(v.hasPointOfInterest(poiDescription)){
				PointOfInterest poi = v.getPointOfInterest(poiDescription);
				if(found.size() == 1){
					maxSearchDistance = node.getWeight() + found.iterator().next().getWeight();
					//Debugger.printInfo("Radius set at: " + maxSearchDistance);
				}else if (found.size() > 1){
					//Iterator<RDIndexData> i = found.iterator();
					for(RDIndexData inode : found){ //wtfsyntax. For those who don't know, its Java's version of a for-each loop.
						float dsp2 = inode.getWeight();
						float dsp1 = node.getWeight();
						PointOfInterest poi2 = inode.getData().getPointOfInterest(poiDescription);
						Float dp1p2 = lookup.get(new OrderedPOIPair(poi, poi2));
						if(dp1p2 != null){
							float perimeter = (dsp2 + dsp1 + dp1p2.floatValue()) / 2.0f;
							if(maxSearchDistance > perimeter){
								//Debugger.printInfo("Radius shrunk to: " + perimeter);
								maxSearchDistance = perimeter;	
							}
						}
					}
				}
				found.add(node);
			}
			if(node.getWeight() >= maxSearchDistance && maxSearchDistance >= 0) break;
		}
		return found.size();
	}
	
	private int buildPOIBoundedSSSPPTree(RoadVertex[] vertices, String poiDescription){
		processNode(vertices, sourceId);
		int found = 0;
		float searchDistance1 = -1;
		float searchDistance2 = -1;
		while(heapCount > 0){
			RDIndexData node = deleteMin();
			if(node.getWeight() == Float.POSITIVE_INFINITY) break; //We're on an island. screw the rest of the graph. 
			processNode(vertices, node.getId());
			RoadVertex v = node.getData();
			if(v.hasPointOfInterest(poiDescription)){
				if(found == 0)		searchDistance1 = node.getWeight();
				else if(found == 1)	searchDistance2 = node.getWeight();
				found++;
			}
			if(node.getWeight() >= (searchDistance1 + searchDistance2) && searchDistance1 >= 0 && searchDistance2 >= 0) break;
		}
		return found;
	}
	
	private void buildSSSPPTree(RoadVertex[] vertices){
		processNode(vertices, sourceId);
		while(heapCount > 0){
			RDIndexData node = deleteMin();
			if(node.getWeight() == Float.POSITIVE_INFINITY){
				Debugger.printInfo("This graph contains islands of disconnected vertices");
				break;
			}
			processNode(vertices, node.getId());
		}
	}
	
	public RoadVertex[] getVertices(){	return graph.getVertices();	}
	
	private int processNode(RoadVertex[] vertices, int id){
		Set<RoadEdge> edges = vertices[id].getEdges();
		Iterator<RoadEdge> i = edges.iterator();
		int visited = 0;
		while(i.hasNext()){
			RoadEdge e = i.next();
			int curNode = (e.startPoint() == id) ? e.endPoint() : e.startPoint();
			if(!index[curNode].isVisited()){
				float newWeight =  e.weight(mode) + index[id].weight;
				visited++;
				try{
					index[curNode].decreaseWeight(index, heap, newWeight, id);	
				} catch (IllegalArgumentException iae){	/*Debugger.printError(newWeight + " is not less than " + index[curNode].weight);*/	}
			}
		}
		return visited;
	}
	
	private void addSource(RoadVertex v, int id){
		Debugger.printChatter("Source Node has ID: " + id);
		RDIndexData node = new RDIndexData(id, v, -1, 0, -1);
		node.visit();
		sourceId = id;
		index[id] = node;
	}
	
	private void addToHeap(RoadVertex v, int id){	addToHeap(v, id, Float.POSITIVE_INFINITY, -1);	}
	private void addToHeap(RoadVertex v, int id, float weight, int parent){
		RDIndexData node = new RDIndexData(id, v, heapCount, weight, parent);
		index[id] = node;
		heap[heapCount] = node;
		node.bubbleUp(index, heap, heapCount);
		heapCount++;
	}
	
	
	private RDIndexData deleteMin(){
		RDIndexData min = heap[0];
		heapCount--;
		if(heapCount != 0){
			heap[0] = heap[heapCount];
			heap[0].hPtr = 0;
			heap[heapCount] = null;
			min.forceDown(index, heap, 0);
		}
		min.visit();
		return min;
	}
	
	public void dumpTree(){
		System.out.println("[ ");
		for(int i = 0; i < index.length; i++) System.out.println(index[i] + ", ");
		System.out.println("]");
	}
	
	private class RDIndexData{
		int id;
		RoadVertex data;
		
		int hPtr;
		boolean visited;
		float weight;
		int parent;
		
		public RDIndexData(int i, RoadVertex d, int h, float w, int p){
			id = i;
			data = d;
			hPtr = h;
			visited = false;
			weight = w;
			parent = p;
		}
		
		public RoadVertex getData(){	return data;	}
		public float getWeight(){	return weight;	}
		public int getId(){	return id;	}
		public void visit(){	visited = true; hPtr = -1;	}
		public boolean isVisited(){	return visited;	}
		public void decreaseWeight(RDIndexData[] index, RDIndexData[] heap, float newWeight, int newParent) throws IllegalArgumentException{
			if(newWeight >= weight || newWeight < 0) throw new IllegalArgumentException("New weight must be less than the old weight. Must not be negative");
			else if(newWeight == Float.POSITIVE_INFINITY){
				Debugger.printError("dude, can't decrease to infinity");
				System.exit(1);
			}
			weight = newWeight;
			parent = newParent;
			hPtr = heap[hPtr].bubbleUp(index, heap, hPtr);
		}
		public void setParent(int p){	parent = p;	}
		public int getParent(){	return parent;	}
		public void forceDown(RDIndexData[] index, RDIndexData[] heap, int curPos){
			RDIndexData newMin = heap[curPos];
			while (newMin.hLeft() < heapCount) {
				RDIndexData tmp = heap[(newMin.hRight() >= heapCount) || 
									   (heap[newMin.hLeft()].getWeight() <= heap[newMin.hRight()].getWeight())
									   ? newMin.hLeft() : newMin.hRight()];
				int tmpPtr = tmp.hPtr;
				if(tmp.getWeight() < newMin.getWeight()){
					newMin.hPtr = tmpPtr;
					heap[tmpPtr] = newMin;
					tmp.hPtr = curPos;
					heap[curPos] = tmp;
					curPos = tmpPtr;
				} else{
					break;
				}
			}
		}
		
		public int bubbleUp(RDIndexData[] index, RDIndexData[] heap, int curPos){
			while (curPos != 0 && heap[hParent(curPos)].getWeight() > getWeight()) {
				RDIndexData tmp = heap[hParent(curPos)];
				this.hPtr = hParent(curPos);
				heap[hParent(curPos)] = this;
				tmp.hPtr = curPos;
				heap[curPos] = tmp;
				curPos = hParent(curPos);
			}
			return curPos;
		}
		
		public int hParent(int k) { return (k - 1) / 2;  }
		public int hLeft(int k)   { return 2 * k + 1; }
		public int hRight(int k)  { return 2 * (k + 1); }
		public int hParent(){ return (hPtr - 1) / 2;}
		public int hLeft(){ return 2 * hPtr + 1;}
		public int hRight(){ return 2 * (hPtr + 1);}
		
		public String toString(){	return toStringBuffer().toString();	}
		public StringBuffer toStringBuffer(){
			StringBuffer sb = new StringBuffer(40);
			sb.append("id: ");
			sb.append(id);
			sb.append(" distance to source: ");
			sb.append(weight);
			sb.append(" parent id: ");
			sb.append(parent);
			return sb;
		}
	}
}
