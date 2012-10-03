#!/usr/bin/env python2.5
#
#  MakeGraphFromDump.py
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

#this program requires PIL
#if you're a Mac user go here: http://www.p16blog.com/p16/2008/05/appengine-installing-pil-on-os-x-1053.html
#if you're a Windows user just use the official installer
#if you're a Linux user, something along the lines of sudo apt-get install python-imaging
#should do the trick, though you may have to do some Googling if its not in your repositories
#list, or you don't use a Debian-ish distro

import sys
import re
from math import log, sqrt

try:
	import Image, ImageDraw, ImageFont
	font = ImageFont.truetype("font/EBGaramond08-Regular.ttf", 16)
except:
	print("""#this program requires PIL
#if you're a Mac user go here: http://emmby.blogspot.com/2008/05/installing-python-pil-on-mac-os-x-1052.html
#you'll need MacPorts though.
#if you're a Windows user just use the official installer
#if you're a Linux user, something along the lines of sudo apt-get install python-imaging
#should do the trick, though you may have to do some Googling if its not in your repositories
#list, or you don't use a Debian-ish distro
""")
	exit(1)
	

def makepairgraph(graphpath, mode, datapairs):
	datapairs = sorted([(int(a), int(b)) for a,b in datapairs])
	makegraph(graphpath, mode, *zip(*datapairs))
	
def makegraph(graphpath, imode, xdata, ydata):
	mode = "RGB"
	if imode=="shrinkboth":
		yscale = 0.001
		ytextscale = 100
	else:
		yscale = 1
		if imode=="normal":
			ytextscale = 0.1
		else:
			ytextscale = 1
			
	if imode=="normal":
		xscale = 100
		xtextscale = 0.1
	elif imode=="shrinkx":
		xscale = 1
		xtextscale = 10
	else:
		xscale = 0.01
		xtextscale = 1000
	
		
	largest = ydata[-1]
	yaxistop = int(yscale * 200 * (largest / 20) + 200)	#hooray for integer math. pad the graph a little
	
	height = yaxistop + 80
	xaxisend = int(xscale * 5 * (xdata[-1] / 50) + 50)
	width = xaxisend + 100
	
	imgsize = (width, height)
	#print imode, imgsize
	#raw_input()
	white = (255, 255, 255)
	black = (0, 0, 0)
	graph = Image.new(mode, imgsize, white)
	draw = ImageDraw.Draw(graph)
	draw.line([(40, 10), (40,height-40)], fill=black, width=4)
	draw.line([(40,height-40), (width-10,height-40)], fill=black, width=4)

	coordlist = [((40 + xscale * (xdata[i] / 10.0)), (height - 40 - 10 * yscale * ydata[i])) for i in range(len(ydata))]
	draw.line(coordlist, fill=black, width=4)
	for i in xrange(0, yaxistop + 20, 20):
		draw.line([(30, height - 40 - i), (40, height - 40 - i)], fill=black, width=4)
		draw.text((30 - 18, height - 48 - i), str(i * ytextscale), fill=black)
		
	for i in xrange(0, xaxisend + 50, 50):
		draw.line([(40+i, height-40),(40+i,height-30)], width=4, fill=black)
		draw.text((40+i - 9, height - 31), str(i * xtextscale), fill=black)

	graph.save(graphpath)

if len(sys.argv) != 3 and len(sys.argv) != 5:
	print "Usage: MakeGraphFromDump.py dumpfile graphfile.png"
	print "Usage: MakeGraphFromDump.py statfile mediangraphfile.png maxgraphfile.png badgraphfile.png"
	exit(1)	
	
dumppath = sys.argv[1]
dumpfile = open(dumppath, 'r')
data = dumpfile.read()
dumpfile.close()

if(len(sys.argv) == 3):
	graphpath = sys.argv[2]
	
	data = data.split("#########")
	
	info = data[0]
	vertexcount = int(re.findall(r"The road network contains ([0-9]+)", info)[0])
	poicount = int(re.findall(r"The road network also contains ([0-9]+)", info)[0])
	
	
	data = [int(x.split(",\t")[1]) for x in data[2].split("\n")[2:-1]]
	data.sort()
	
	smallest = data[0]
	largest = data[-1]
	
	median = data[len(data) / 2]
	#badcount = 0
	#for nv in reversed(data):
	#	if nv > 4*median:
	#		badcount += 1
	#	else:
	#		break
	lognthworst = data[-int(1000 * log(len(data)))]
	twolognthworst = data[-int(2000 * log(len(data)))]
	sqrtnthworst = data[-int(100 * sqrt(len(data)))]
	print dumppath + "\t" + str((poicount, median)) + "\t" + str((poicount, largest)) + "\t" + str((vertexcount,	lognthworst, twolognthworst, sqrtnthworst)) + ("\t;%d %d %d" % (int(1000 * log(len(data))), int(2000 * log(len(data))), int(100 * sqrt(len(data)))))
	#makegraph(graphpath, "shrinkx", [i for i in xrange(len(data))], data)
else:
	graphpaths = sys.argv[2:]
	modes = ["normal", "normal", "shrinkboth"]
	#print graphpaths
	#data = re.findall(r"\(([0-9]+), ([0-9]+)\)", data)
	#for i in xrange(3):
	#	gp = graphpaths[i]
	#	gdata = data[i::3]
		
		#makepairgraph(gp, modes[i], gdata)