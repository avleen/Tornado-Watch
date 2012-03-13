#!/usr/bin/python

import cgi
import psycopg2
import re

DB_CONN = None

def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


def check_submit_in_zone(lng, lat):
    """Check that the submittion is currently in a tornado warning or watch
    zone"""

    cur = DB_CONN.cursor()
    sql = """SELECT c.state, c.county FROM counties c, tornado_warnings t
                WHERE ST_Intersects(makepoint(%s, %s), c.the_geom)
                AND c.county ~* t.county
                AND t.endtime > date_part('epoch', now())
                AND t.starttime < date_part('epoch', now())"""
    open('/tmp/av', 'w').write(cur.mogrify(sql, (lng, lat)))
    if cur.rowcount == 0:
        return False
    else:
        return True


def main():
    form = cgi.FieldStorage()
    lng = form.getvalue("lng", None)
    lat = form.getvalue("lat", None)
    registration_id = form.getvalue("registrationId", None)

    # Sanitize data ftw!
    lng = re.sub('[^0-9\-]+', '', lng)
    lat = re.sub('[^0-9\-]+', '', lat)
    registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)
    if not lng or not lat:
        return

    make_db_conn()

    # lng and lat in postgis are decimal, not microdegrees
    lng = float(lng) / 1000000
    lat = float(lat) / 1000000

    # Make sure we're in a tornado alert zone
    in_zone = check_submit_in_zone(lng, lat)
    if in_zone:
        priority = 't'
    else:
        priority = 'f'

    cur = DB_CONN.cursor()
    sql = """INSERT INTO user_submits (registration_id, location, priority)
                VALUES (%s, makepoint(%s, %s), %s)"""
    cur.execute(sql, (registration_id, lng, lat, priority))
    DB_CONN.commit()

    print "Content-type: text/html"
    print
    print cgi.escape(registration_id)


if __name__ == "__main__":
    main()
