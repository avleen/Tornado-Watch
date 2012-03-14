#!/usr/bin/python

import cgi
import psycopg2
import re

DB_CONN = None

def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


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

    # lng and lat in postgis are decimal, not microdegrees
    lng = float(lng) / 1000000
    lat = float(lat) / 1000000

    make_db_conn()
    cur = DB_CONN.cursor()
    sql = """UPDATE user_registration
                SET location = makepoint(%s, %s)
                WHERE registration_id = %s"""
    cur.execute(sql, (lng, lat, registration_id))
    DB_CONN.commit()

    print "Content-type: text/plain"
    print
    print cgi.escape("Location updated")


if __name__ == "__main__":
    main()
