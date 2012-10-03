#!/usr/bin/env python2.5
#
#  FilterSetToSizeAndSort.py
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

import sys, re

if len(sys.argv) != 4:
	print "Usage: FilterSetToSizeAndSort.py infile.txt outfile.csv outsize"
	exit(1)
	
handle = open(sys.argv[1], 'r')
data = handle.read()
handle.close()

dataexp = re.compile(r"[0-9]+,\t([0-9]+)")
data = dataexp.findall(data)

cursize = len(data)
outsize = int(sys.argv[3])

everynth = cursize / outsize
if(everynth == 0):
	print "outsize must be less than current size %d" % cursize
	exit(1)
	
data = sorted([int(data[i]) for i in range(0, outsize * everynth, everynth)])
handle = open(sys.argv[2], 'w')
for i in range(outsize):
	handle.write("%d\t%d\n" % (i * everynth, data[i]))
handle.close()

exit(0)