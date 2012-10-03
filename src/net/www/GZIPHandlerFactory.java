//
//  GZIPHandlerFactory.java
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
package net.www;

import debug.Debugger;
import java.net.*;

public class GZIPHandlerFactory implements ContentHandlerFactory { 
	
	public ContentHandler createContentHandler(String mimetype) { 
		if ( mimetype.equalsIgnoreCase( "application/x-gzip" ) || mimetype.equalsIgnoreCase("application/octet-stream") )	//this is bad form and limits our options!
			return new net.www.content.application.x_gzip();																//lots of things are octet streams
		else{
			Debugger.printInfo("THATS NOT GZIPPED! " + mimetype);
			return null; 
		}  
	} 
} 