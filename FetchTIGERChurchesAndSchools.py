#!/usr/bin/env python2.5
#
#  FetchTIGERChurchesAndSchools.py
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


import urllib
import re
import zipfile
from StringIO import StringIO
import sys
import os.path

args = sys.argv
if len(args) < 5:
	print "Usage: FetchTIGERChurchesAndSchools.py zipdir churchpath schoolpath [-L] state1 [state2 [state3 [..]]]"
	exit(1)

schooldata = "Educational Institution\n"
churchdata = "Religious Institution\n"
whitespace = re.compile(r"\s+")
latlong = re.compile(r"([+-][0-9]{0,3}[0-9]{6})")
schoolorchurch = re.compile(r"(D4[34])")
zipdir = args[1]
churchpath = args[2]
schoolpath = args[3]
if args[4] == "-L":
	local = True
	statelist = args[5:]
else:
	local = False
	statelist = args[4:]

for state in statelist:
	state = state.lower()
	print "Fetching county listing for %s" % state.upper()
	stateurl = "http://www2.census.gov/geo/tiger/tiger2k/%s/" % state
	
	#req = urllib2.Request(stateurl, headers = {"User-Agent" : "Mozilla/5.0 (X11; U; Linux i686) Gecko/20071127 Firefox/2.0.0.11"}
	#conn = urllib.urlopen(req)
	statezipdir = "%s/%s" % (zipdir, state)
	if(not os.path.isdir(statezipdir)):
		os.mkdir(statezipdir)
	if(not local):
		conn = urllib.urlopen(stateurl)
		data = conn.read()
		conn.close()
		data = data[data.find("<pre>")+len("<pre>")	:	data.find("</pre>")]
		countyfiles = re.findall(r'"tgr([0-9]{5})\.zip"', data)
	else:
		data = '"' + ('"\n"').join(os.listdir(statezipdir)) + '"'
	countyfiles = re.findall(r'"tgr([0-9]{5})\.zip"', data)
	print countyfiles
	for county in countyfiles:
		zipname = "%s/tgr%s.zip" % (statezipdir, county)
		if(not local):
			countyurl = "%stgr%s.zip" % (stateurl, county)
			conn = urllib.urlopen(countyurl)
			print "Fetching %s" % countyurl
			outfile = open(zipname, 'w')
			data = conn.read()
			size = len(data)
			outfile.write(data)
			outfile.close()
			print "\t%dKB fetched" % (size / 1024)
			conn.close()
		print "Extracting information from %s" % zipname
		zipped = zipfile.ZipFile(zipname, 'r')
		contents = StringIO(zipped.read("TGR%s.RT7" % county))
		zipped.close()
		for line in contents.readlines():
			line = whitespace.split(line)
			if schoolorchurch.search(line[1]):
				for ll in line[2:]:
					ll = latlong.findall(ll)
					if len(ll) == 2:
						longitude, latitude = ll
						nline = "%d %d\n" % (int(longitude), int(latitude))
						if "D43" in line[1]:	schooldata += nline
						else:	churchdata += nline
						break
				
		print ""
		contents.close()
print "Writing %s" % churchpath
churches = open(churchpath, 'w')
churches.write(churchdata)
churches.close()
print "Writing %s" % schoolpath
schools = open(schoolpath, 'w')
schools.write(schooldata)
schools.close()
print "Done"