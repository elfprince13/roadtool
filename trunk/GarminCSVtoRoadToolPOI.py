#!/usr/bin/env python2.5
#
#  GarminCSVtoRoadToolPOI.py
#
#  Copyright Thomas Dickerson, 2012.
#  This file is part of RoadTool.
#
#  RoadTool is free software: you can redistribute it and/or modify it
#  under the terms of the GNU Lesser General Public License as published
#  by the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  RoadTool is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU Lesser General Public License for more details.
#
#  You should have received a copy of the GNU Lesser General Public License
#  long with RoadTool.  If not, see <http://www.gnu.org/licenses/>.
#

import sys, os

if len(sys.argv) != 4:
	print "Usage: GarminCSVtoRoadToolPOI.py [POI type] [infile.csv] [outfile.poi]"
	exit(1)

typeStr = sys.argv[1]
ifp = sys.argv[2]
ofp = sys.argv[3]
print "Converting Garmn CSV file to RoadTool POI file"
print "input: " + ifp
print "output: " + ofp
print "This file contains '" + typeStr + "' POIs"


infile = open(ifp, 'r')
input = infile.readlines()
output = typeStr
for line in input:
	line = line.rstrip()
	if line == "" or line[0] == ";":
		pass
	else:
		line = line.split(", ")
		long = float(line[0])
		lat = float(line[1])
		output += "\n%d %d" % (int(long * 10e5), int(lat * 10e5))
output += "\n"
outfile = open(ofp, 'w')
outfile.write(output)
outfile.flush()
outfile.close()
		