//
//  OrderedPOIPair.java
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

public class OrderedPOIPair {
	
	private int hashcode;
	private PointOfInterest p1;
	private PointOfInterest p2;
	
	public OrderedPOIPair(PointOfInterest p, PointOfInterest q){
		if( p.latitude() > q.latitude()){
			p1 = p;
			p2 = q;
		} else if(p.latitude() < q.latitude()){
			p1 = q;
			p2 = p;
		} else{
			if (p.longitude() >= q.longitude()){
				p1 = p;
				p2 = q;
			} else{
				p1 = q;
				p2 = p;
			}
		}
		hashcode = 47 * p1.longitude() + 23 * p2.longitude() + 11 * p1.latitude() + 5 * p2.latitude();
	}
	
	public int hashCode(){	return hashcode;	}
	public boolean equals(Object o){
		if (this == o)
			return true;
		if (!(o instanceof OrderedPOIPair))
			return false;
		OrderedPOIPair oopp = (OrderedPOIPair) o;
		return oopp.p1.equals(p1) && oopp.p2.equals(p2);
	}
}
