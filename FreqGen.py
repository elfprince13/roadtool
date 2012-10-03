#!/usr/bin/env python2.5
#
#  FreqGen.py
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

if len(sys.argv) < 3:
	print "Usage: FreqGen.py infile.txt [infile2.txt [..]] outfile.csv"
	exit(1)
	
def repeatn(delim, format, count):
	return delim.join([format] * count)
	
delimiter = ","
infiles = sys.argv[1:-1]
fcount = len(infiles)
alldata = []	
dataexp = re.compile(r"[0-9]+,\t([0-9]+)")
vexp = re.compile(r"contains ([0-9]+) vertices and ([0-9]+) edges")
siteexp = re.compile(r"also contains ([0-9]+) points")
prepexp = re.compile(r"([0-9]+\.[0-9]+) each\.\s+POI lookup contains ([0-9]+) pairings")

vertexcount = []
sitecount = []
sitepairs = []
avgply = []
edgecount = []

for	fname in infiles:
	handle = open(fname, 'r')
	data = handle.read()
	handle.close()
	alldata.append(dataexp.findall(data))
	vc, ec = vexp.findall(data)[0]
	vertexcount.append(int(vc))
	edgecount.append(int(ec))
	sc = siteexp.findall(data)[0]
	sitecount.append(int(sc))
	apsp = prepexp.findall(data)
	if len(apsp):
		ap, sp = apsp[0]
		avgply.append(float(ap))
		sitepairs.append(int(sp))
	
maxfound = 0
allfreqs = []

for i in range(len(alldata)):
	data = alldata[i]
	allfreqs.append({})
	for datum in data:
		datum = int(datum)
		if datum > maxfound:
			maxfound = datum
		if datum in allfreqs[i]:
			allfreqs[i][datum] += 1
		else:
			allfreqs[i][datum] = 1
			
handle = open(sys.argv[-1], 'w')
nintline = repeatn(delimiter, "%d", fcount) + "\n"
handle.write("Value" + delimiter + delimiter.join(["\"%s\"" % filename for filename in infiles]) + "\n")
for i in range(maxfound+1):
	outstr = ("%d"+ delimiter + nintline)
	nums = [(fdict[i] if i in fdict else 0) for fdict in allfreqs]
	nums.insert(0, i)
	#print outstr
	#print nums
	handle.write(outstr % tuple(nums))

handle.write(("\"Vertices\"" + delimiter + nintline) % tuple(vertexcount))
handle.write(("\"Sites\"" + delimiter + nintline) % tuple(sitecount))
if len(apsp):
	handle.write(("\"Site Pairs\"" + delimiter + nintline) % tuple(sitepairs))
	handle.write(("\"Avg Ply\"" + delimiter + repeatn(delimiter, "%0.2f", fcount) + "\n") % tuple(avgply))
handle.write(("\"Max Ply\"" + delimiter + nintline) % tuple([max(fdict.keys()) for fdict in allfreqs]))
handle.write(("\"Edge Count\"" + delimiter + nintline) % tuple(edgecount))
handle.close()

exit(0)