//
//  RoadGraph.java
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
import java.nio.FloatBuffer;
import com.sun.opengl.util.BufferUtil;
import java.io.Serializable;
import debug.Debugger;

public class RoadGraph implements Serializable{
	
	private RoadEdge[] edges;
	private RoadVertex[] vertices;
	private PointOfInterest[] pointsOfInterest;
	private int[] catCounts;
	private int ignoredPoiCount;
	
	private static final int DEFAULTCAPACITY = 16;		//16 and 0.75 are the system defaults.
	private static final float DEFAULTLOADFACTOR = 0.75f;	//16 is *really* low for pointsOfInterest
														//I may set this higher later
	
	public RoadGraph(RoadEdge[] e, RoadVertex[] v, int[] counts){
		edges = e;
		vertices = v;
		catCounts = counts;
		pointsOfInterest = null;
		ignoredPoiCount = 0;
	}
	
	public PointOfInterest[] getPointsOfInterest(){	return pointsOfInterest;	}
	public void assignPointsOfInterest(PointOfInterest[] pois){
		int oldLen, newLen;
		if(pointsOfInterest == null){
			pointsOfInterest = pois;
			oldLen = 0;
			newLen = pois.length;
		} else{
			PointOfInterest[] tmp = pointsOfInterest;
			oldLen = tmp.length;
			newLen = pois.length;
			pointsOfInterest = new PointOfInterest[oldLen + newLen];
			System.arraycopy(tmp, 0, pointsOfInterest, 0, oldLen);
			System.arraycopy(pois, 0, pointsOfInterest, oldLen, newLen);
		}
		ignoredPoiCount = 0;
		int threadCount = RoadTool.countAppropriateThreads();
		//threadCount = 0;
		if(threadCount == 0){
			RoadWorker job = new RoadWorker(oldLen, oldLen+newLen, null, WorkerMode.POIBUILD);
			job.setupPParser(pointsOfInterest, vertices);
			job.poiLoop();
		} else{
			int jobCount = threadCount;
			int step = RoadWorker.chunkedTaskStepSize(newLen, jobCount);
			RoadWorker[] jobs = new RoadWorker[jobCount];
			CountDownLatch latch = new CountDownLatch(1);
			for(int i = 0; i < jobCount; i++){
				int min = RoadWorker.calcMin(oldLen, step, i);
				int max = RoadWorker.calcMax(oldLen, step, i, newLen);
				jobs[i] = new RoadWorker(min, max, latch, WorkerMode.POIBUILD);
				jobs[i].setupPParser(pointsOfInterest, vertices);
			}
			RoadTool.doWork(jobs, latch);
			for(int i = 0; i < jobCount; i++) ignoredPoiCount += jobs[i].ignoredPoi;
		}
	}
	
	public RenderInfo getRenderInfo(){
		return new RenderInfo(this);
	}
	
	public int countEdges(){
		return edges.length;
	}
	
	public RoadEdge getEdge(int i){
		return edges[i];
	}
	
	public RoadEdge[] getEdges(){
		return edges;
	}
	
	public int countVertices(){
		return vertices.length;
	}
	
	public RoadVertex getVertex(int i){
		return vertices[i];
	}
	
	public RoadVertex[] getVertices(){
		return vertices;
	}
	
	public int[] countCategories(){
		return catCounts;
	}
	
	public int countPointsOfInterest(){
		if(pointsOfInterest == null || pointsOfInterest.length > 0 && pointsOfInterest[pointsOfInterest.length - 1] == null) //hasn't been fully initialized
			return 0;
		else
			return pointsOfInterest.length - ignoredPoiCount;
	}
	
	class RenderInfo {
		FloatBuffer xyzA1;
		FloatBuffer xyzA2;
		FloatBuffer xyzA3;
		FloatBuffer xyzA4;
		FloatBuffer xyzPOI;
		int poiCount;
		int catCounts[];
		float minx, maxx;
		float miny, maxy;
		float minz,maxz;
		
		public RenderInfo(RoadGraph g){
			RoadEdge[] edges = g.getEdges();
			RoadVertex[] vertices = g.getVertices();
			PointOfInterest[] pointsOfInterest = g.getPointsOfInterest();
			catCounts = g.countCategories();
			xyzA1 = BufferUtil.newFloatBuffer(6 * catCounts[RoadEdge.A1]);
			xyzA2 = BufferUtil.newFloatBuffer(6 * catCounts[RoadEdge.A2]);
			xyzA3 = BufferUtil.newFloatBuffer(6 * catCounts[RoadEdge.A3]);
			xyzA4 = BufferUtil.newFloatBuffer(6 * catCounts[RoadEdge.A4]);
			float[] initvertex = vertices[0].latlongToRect();
			minx = maxx = initvertex[0];	//ugly but compact
			miny = maxy = initvertex[1];
			minz = maxz = initvertex[2];
			for(int i = 0; i < edges.length; i++){
				RoadEdge edge = edges[i];
				float[] vertex = vertices[edge.startPoint()].latlongToRect();
				if (vertex[0] > maxx) maxx = vertex[0];
				else if (vertex[0] < minx) minx = vertex[0];
				if (vertex[1] > maxy) maxy = vertex[1];
				else if (vertex[2] < miny) miny = vertex[1];
				if (vertex[2] > maxz) maxz = vertex[2];
				else if (vertex[2] < minz) minz = vertex[2];
				float[] vertex2 = vertices[edge.endPoint()].latlongToRect();
				if (vertex2[0] > maxx) maxx = vertex2[0];
				else if (vertex2[0] < minx) minx = vertex2[0];
				if (vertex2[1] > maxy) maxy = vertex2[1];
				else if (vertex2[2] < miny) miny = vertex2[1];
				if (vertex2[2] > maxz) maxz = vertex2[2];
				else if (vertex2[2] < minz) minz = vertex2[2];
				switch(edge.category()){
					case RoadEdge.A1:
						xyzA1.put(vertex);
						xyzA1.put(vertex2);
						break;
					case RoadEdge.A2:
						xyzA2.put(vertex);
						xyzA2.put(vertex2);
						break;
					case RoadEdge.A3:
						xyzA3.put(vertex);
						xyzA3.put(vertex2);
						break;
					case RoadEdge.A4:
					default:
						xyzA4.put(vertex);
						xyzA4.put(vertex2);
				}
			}
			xyzA1.rewind();
			xyzA2.rewind();
			xyzA3.rewind();
			xyzA4.rewind();
			if(pointsOfInterest != null){
				poiCount = pointsOfInterest.length;
				xyzPOI = BufferUtil.newFloatBuffer(3 * pointsOfInterest.length);
				for(int i = 0; i < poiCount; i++) xyzPOI.put(pointsOfInterest[i].latlongToRect());
				xyzPOI.rewind();
			} else{
				xyzPOI = null;
			}
			StringBuilder sb = new StringBuilder(120);
			sb.append("minx: ");
			sb.append(minx);
			sb.append(" maxx: ");
			sb.append(maxx);
			sb.append(" miny: ");
			sb.append(miny);
			sb.append(" maxy: ");
			sb.append(maxy);
			sb.append(" minz: ");
			sb.append(minz);
			sb.append(" maxz: ");
			sb.append(maxz);
			Debugger.printChatter(sb.toString()); 
		}
	}
}
