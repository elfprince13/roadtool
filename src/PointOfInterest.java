//
//  PointOfInterest.java
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

public class PointOfInterest extends GPSCoord{
	
	private String description;
	private RoadVertex closest;
	
	public PointOfInterest(int lg, int lat, String d){
		super(lg, lat);
		description = d;
	}
	public void setClosest(RoadVertex c){	closest = c;	}
	public RoadVertex getClosest(){	return closest;	}
	public String getDescription(){	return description;	}

}
