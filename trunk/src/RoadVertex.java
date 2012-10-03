//
//  RoadVertex.java
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
import debug.Debugger;

public class RoadVertex extends GPSCoord{
	
	//skip the id, RoadVertices are stored in an array
	//afaik they don't need to be self aware, so this will
	//save us a couple MB on large datasets.
	private HashSet<RoadEdge> edges;
	private ConcurrentHashMap<String, PointOfInterest> pointsOfInterest;
	
	public RoadVertex(int lg, int lat){
		super(lg, lat);
		edges = new HashSet<RoadEdge>(8, 0.75f); //if anyone has ever seen more than a 6-way intersection, slap me and change this
		pointsOfInterest = new ConcurrentHashMap<String, PointOfInterest>(4, 0.75f, 1 + RoadTool.countAppropriateThreads());
	}
	
	public RoadVertex(int lg, int lat, RoadEdge[] es){
		this(lg, lat);
		for(int i=0; i<es.length; i++){
			edges.add(es[i]);
		}
	}
	
	public synchronized boolean addEdge(RoadEdge e){	return edges.add(e);	}
	public synchronized boolean removeEdge(RoadEdge e){	return edges.remove(e);	}
	public PointOfInterest addPointOfInterest(PointOfInterest p){	return pointsOfInterest.put(p.getDescription(), p);	}
	public PointOfInterest removePointOfInterest(PointOfInterest p){	return pointsOfInterest.remove(p.getDescription());	}
	public boolean hasPointOfInterest(String description){	return pointsOfInterest.containsKey(description);	}
	public PointOfInterest getPointOfInterest(String description){	return pointsOfInterest.get(description);	}
	
	public Set<RoadEdge> getEdges(){	return edges;	}
	public ConcurrentHashMap<String, PointOfInterest> getPointsOfInterest(){	return pointsOfInterest;	}

}
