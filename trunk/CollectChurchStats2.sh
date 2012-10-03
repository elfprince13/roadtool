#!/bin/bash
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
mode=$1
outfile=$2
shift 2

rm ${outfile}
echo "State	k POI	n	B = 15log(n)	A = count n > B	A/log(n)" >> ${outfile} 
for arg in "$@"
do
	CountNBiggerThanlogK.py data/${arg}-${mode}.txt >> ${outfile}
done

#MakeGraphFromDump.py data/churchstats.txt charts/median.png charts/max.png charts/bad.png