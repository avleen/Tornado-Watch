#!/bin/sh

mydir=$( dirname $0 )
kmlfile=${mydir}/../htdocs/auto_rep.kml
kmlfilez=${mydir}/../htdocs/auto_rep.kmz


echo '<?xml version="1.0" encoding="UTF-8"?>' > ${kmlfile}
echo '<kml xmlns="http://www.opengis.net/kml/2.2">' >> ${kmlfile}
echo '<Document>' >> ${kmlfile}
echo '    <Style id="yellowLineGreenPoly">' >> ${kmlfile}
echo '      <LineStyle>' >> ${kmlfile}
echo '        <color>7f00ffff</color>' >> ${kmlfile}
echo '        <width>4</width>' >> ${kmlfile}
echo '      </LineStyle>' >> ${kmlfile}
echo '      <PolyStyle>' >> ${kmlfile}
echo '        <color>7f00ff00</color>' >> ${kmlfile}
echo '      </PolyStyle>' >> ${kmlfile}
echo '    </Style>' >> ${kmlfile}
echo '<Folder>' >> ${kmlfile}
echo '<styleUrl>#yellowLineGreenPoly</styleUrl>' >> ${kmlfile}
#echo "DELETE FROM tmploc" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
#echo "INSERT INTO tmploc SELECT location FROM user_registration order by create_date desc limit 500" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
#echo "SELECT '<Placemark>' || ST_AsKML(location) || '</Placemark>' FROM tmploc" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
echo "SELECT '<Placemark><Point><coordinates>' || X(location) || ',' || Y(location) || '</coordinates></Point><name>' || c.county || ', ' || c.state || '</name><description>' || date_trunc('day', to_timestamp(u.create_date))::date  || '</description></Placemark>' FROM user_registration u, counties c where ST_Intersects(u.location, c.the_geom) ORDER BY u.create_date desc" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
echo "SELECT '<Placemark>' || ST_AsKML(ST_GeographyFromText(the_geom)) || '<name>' || c.county || ', ' || c.state || '</name><description>' || c.county || ', ' || c.state || '</description></Placemark>' FROM tornado_warnings t, counties c where t.endtime > date_part('epoch', now()) and c.county ~* t.county and c.state = t.state" | psql -qtU postgres -d tornadowatch >> ${kmlfile}
echo '</Folder>' >> ${kmlfile}
echo '</Document>' >> ${kmlfile}
echo '</kml>' >> ${kmlfile}

#gzip -v9c ${kmlfile} > ${kmlfilez}
zip -9 ${kmlfilez} ${kmlfile}
echo 'Content-type: text/html'
echo 'Location: http://maps.google.com/maps?q=http:%2F%2Ftw.silverwraith.com%2Fhtml%2Fauto_rep.kmz'
echo
echo
