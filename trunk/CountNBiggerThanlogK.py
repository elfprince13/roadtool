#!/usr/bin/env python2.5
#
#  CountNBiggerThanlogK.py
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

import math
import re
import sys

def expectedPos(l, v):
	if v > max(l):
		return len(l)
	step = len(l) / 2
	pos = step
	while step:
		val = l[pos]
		step /= 2
		if val == v:
			break
		elif val < v:
			pos += step
		else:
			pos -= step
	return pos
	
if len(sys.argv) != 2:
	print "Usage: CountNBiggerThanlogK.py infile.txt"
	exit(1)
	
handle = open(sys.argv[1], 'r')
data = handle.read()
handle.close()

poiexp = re.compile(r"also contains ([0-9]+) points of interest")
k = int(poiexp.findall(data)[0])

dataexp = re.compile(r"[0-9]+,\t([0-9]+)")
data = dataexp.findall(data)
data = sorted([int(datum) for datum in data])
n = len(data)
B = 15 * math.log(n)
expected = expectedPos(data, B)
A = n - expectedPos(data, B)

print "%s\t%d\t%d\t%d\t%d\t%.4f" % (sys.argv[1], k, n, B, A, float(A) / math.log(n))