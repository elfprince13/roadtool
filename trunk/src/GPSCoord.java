//
//  GPSCoord.java
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
import debug.Debugger;

abstract class GPSCoord implements Serializable{
	public static final RenderMode RECT = RenderMode.RECT;
	public static final RenderMode FLATPOL = RenderMode.FLATPOL;
	
	private int longitude;
	private int latitude;
	public GPSCoord(int lg, int lat){
		longitude = lg;
		latitude = lat;
	}
	
	public int longitude(){ return longitude;	}
	public int latitude(){	return latitude;	}
	
	//latlongToRect algorithm
	public float[] latlongToRect(){
		
		//radius of small circle at lat degrees north/south is cos(lat)
		float rLong = (longitude / 1e6f) * (float)Math.PI / 180.0f;
		float rLat = (latitude / 1e6f) * (float)Math.PI / 180.0f;
		float rad = (float)Math.cos(rLat);
		//y coord is sin(lat)
		//z coord is radius * sin(long)
		//x coord is radius * cos(long)
		float[] xyz;
		if(RoadDisplay.getRenderMode() == RECT){
			float[] tmp = {rad * (float)Math.cos(rLong), (float)Math.sin(rLat), rad * (float)Math.sin(rLong)};
			xyz = tmp;
		} else{
			float[] tmp = {rLong, rLat, 0.0f};
			xyz = tmp;
		}
		return xyz;
	}
	
	public double distanceTo(GPSCoord gc){	return distanceBetween(this, gc);	}
	public static double distanceBetween(GPSCoord gc1, GPSCoord gc2){	
		float[] xyz1 = gc1.latlongToRect();
		float[] xyz2 = gc2.latlongToRect();
		return Math.sqrt(Math.pow(xyz1[0] - xyz2[0], 2) + Math.pow(xyz1[1] - xyz2[1], 2) + Math.pow(xyz1[2] - xyz2[2], 2));
	}
	
	public <T extends GPSCoord> T findClosest(T[] points){	return findClosestTo(this, points);	}
	public static <S extends GPSCoord, T extends GPSCoord> T findClosestTo(S pt, T[] points){
		T closest = null;
		for(int i=0; i<points.length; i++){
			if(closest == null || distanceBetween(pt, closest) > distanceBetween(pt, points[i])){
				closest = points[i];
			}
		}
		return closest;
	}
	
	public String toString(){	return toStringBuffer().toString();	}
	public StringBuffer toStringBuffer(){
		StringBuffer sb = new StringBuffer(21);
		sb.append(longitude);
		sb.append(" ");
		sb.append(latitude);
		return sb;
	}
	
	
}
