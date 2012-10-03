//
//  RoadEdge.java
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
import java.io.Serializable;

enum Mode implements Serializable{	DISTANCE, TIME	}
public class RoadEdge implements Serializable, Comparable<RoadEdge>{

	public static final Mode DISTANCE = Mode.DISTANCE;
	public static final Mode TIME = Mode.TIME;
	public static final int A1 = 0;
	public static final int A2 = 1;
	public static final int A3 = 2;
	public static final int A4 = 3;	
	public static final float[] AVERAGESPEED = {1.0f, 0.8f, 0.6f, 0.4f}; 
	
	private int id1;
	private int id2;
	private float meters;
	private byte category;
	private float travelTime;
	
	public RoadEdge(int i1, int i2, float t, float m, byte c){
		id1 = i1;
		id2 = i2;
		travelTime = t;
		meters = m;
		
		//if(!(c >= 0 && c <= 4)) throw new IllegalArgumentException("category must be in the range [0,3]");
		category = c;
	}
	
	//compare travel times
	public int compareTo(RoadEdge e){
		return (int)Math.signum(this.travelTime - e.travelTime);
	}
	
	//test if they represent the same road
	public boolean equals(Object o){
		//assumes that if(o.id1 == id1 && o.id2 == id2) then o.meters == meters
		//but just in case we have, for example, a town road and a highway with the same endpoints
		//we compare the category too. since travelTime is (meters / AVERAGESPEED[category])
		//we effectively check that all 5 attributes are the same, with only three comparisons
		return ((RoadEdge)o).id1 == id1 && ((RoadEdge)o).id2 == id2 && ((RoadEdge)o).category == category;
	}
	
	public int startPoint(){	return id1;	}
	public int endPoint(){	return id2;	}
	public float weight(Mode mode){	return (mode == DISTANCE) ? meters : travelTime;	}
	
	public float meters(){
		return meters;
	}
	
	public byte category(){
		return category;
	}
	
	public float travelTime(){
		return travelTime;
	}
	
}
