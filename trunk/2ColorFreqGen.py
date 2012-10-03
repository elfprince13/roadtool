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
	
def weave(l1, l2):
	woven = []
	for j in range(max(len(l1), len(l2))):
		woven.append(l1[j] if j < len(l1) else 0)
		woven.append(l2[j] if j < len(l2) else 0)
	return woven

def repeatn(delim, format, count):
	return delim.join([format] * count)
	
delimiter = ","
infiles = sys.argv[1:-1]
fcount = len(infiles)
alldata = []	
dataexp = re.compile(r"[0-9]+,\t([0-9]+),\t([0-9]+)")
vexp = re.compile(r"contains ([0-9]+) vertices and ([0-9]+) edges")
siteexp = re.compile(r"also contains ([0-9]+) points")
prepexp = re.compile(r" ([0-9]+\.[0-9]+) each\.\s+.+ ([0-9]+\.[0-9]+) each\.\s+POI lookup contains ([0-9]+) pairings")

vertexcount = []
sitecount = []
sitepairs = []
avgply1 = []
avgply2 = []
edgecount = []
avgpairspercircle = []

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
		ap1, ap2, sp = apsp[0]
		avgply1.append(float(ap1))
		avgply2.append(float(ap2))
		sitepairs.append(int(sp))
	
maxfound = 0
allfreqs1 = []
allfreqs2 = []

for i in range(len(alldata)):
	data = alldata[i]
	allfreqs1.append({})
	allfreqs2.append({})
	avgpairspercircle.append(0)
	for datum in data:
		datum1 = int(datum[0])
		datum2 = int(datum[1])
		avgpairspercircle[i] += datum1*datum2
		if max(datum1, datum2) > maxfound:
			maxfound = max(datum1, datum2)
		if datum1 in allfreqs1[i]:
			allfreqs1[i][datum1] += 1
		else:
			allfreqs1[i][datum1] = 1
			
		if datum2 in allfreqs2[i]:
			allfreqs2[i][datum2] += 1
		else:
			allfreqs2[i][datum2] = 1
	avgpairspercircle[i] /= vertexcount[i]		
			
handle = open(sys.argv[-1], 'w')
nintline = repeatn(delimiter+delimiter, "%d", fcount) + "\n"
tnintline = repeatn(delimiter, "%d", fcount * 2) + "\n"
handle.write("Value" + delimiter + (delimiter+delimiter).join(["\"%s\"" % filename for filename in infiles]) + "\n")
for i in range(maxfound+1):
	nums1 = [(fdict[i] if i in fdict else 0) for fdict in allfreqs1]
	nums2 = [(fdict[i] if i in fdict else 0) for fdict in allfreqs2]
	woven = weave(nums1, nums2)
	woven.insert(0, i)
	outstr = "%d" + delimiter + tnintline 
	handle.write(outstr % tuple(woven))

handle.write(("\"Vertices\"" + delimiter + nintline) % tuple(vertexcount))
handle.write(("\"Sites\"" + delimiter + nintline) % tuple(sitecount))
if len(apsp):
	handle.write(("\"Site Pairs\"" + delimiter + nintline) % tuple(sitepairs))
	handle.write(("\"Avg Ply\"" + delimiter + repeatn(delimiter, "%0.2f", fcount*2) + "\n") % tuple(weave(avgply1, avgply2)))
#handle.write(("\"Max Ply\"" + delimiter + nintline) % tuple([max(fdict.keys()) for fdict in allfreqs]))
handle.write(("\"Edge Count\"" + delimiter + nintline) % tuple(edgecount))
handle.write("\n")
handle.write("State" + delimiter + "Vertices" + delimiter + "Sites" + delimiter + "Site Pairs" + delimiter + "Avg Ply1" + delimiter + "Avg Ply2" + delimiter + "Avg Pairs/Circle\n")
fmtstr = ("%s" + delimiter + "%d" + delimiter + "%d" + delimiter + "%d" + delimiter + "%0.2f" + delimiter + "%0.2f" + delimiter + "%0.2f\n")
for i in range(fcount):
	argtup = (infiles[i], vertexcount[i], sitecount[i], sitepairs[i], avgply1[i], avgply2[i], avgpairspercircle[i])
	handle.write(fmtstr % argtup)

handle.close()

exit(0)
