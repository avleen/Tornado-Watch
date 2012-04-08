#!/bin/sh

mydir=$( dirname $0 )
kmlfile=${mydir}/../htdocs/auto_rep_circle.kml
kmlfilez=${mydir}/../htdocs/auto_rep_circle.kmz

echo '<?xml version="1.0" encoding="UTF-8"?>' > ${kmlfile}
echo '<kml xmlns="http://www.opengis.net/kml/2.2">' >> ${kmlfile}
echo '<Folder>' >> ${kmlfile}
echo "DELETE FROM tmploc" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
echo "INSERT INTO tmploc SELECT location FROM user_registration order by create_date" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
#echo "SELECT '<Placemark>' || ST_AsKML(location) || '</Placemark>' FROM tmploc" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
echo "SELECT '<Placemark>' || ST_AsKML(ST_Buffer(ST_GeographyFromText(location), 32185)) || '<name>' || c.county || ', ' || c.state || '</name><description>' || c.county || ', ' || c.state || '</description></Placemark>' FROM user_registration u, counties c where ST_Intersects(u.location, c.the_geom)" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
echo '</Folder>' >> ${kmlfile}
echo '</kml>' >> ${kmlfile}

zip -9 ${kmlfilez} ${kmlfile}

echo 'Content-type: text/html'
echo 'Location: http://maps.google.com/maps?q=http:%2F%2Ftw.silverwraith.com%2Fauto_rep_circle.kmz'
echo
echo
