#!/usr/bin/python

import cgi
import json
import memcache
import psycopg2
import psycopg2.extras
import time

DB_CONN = None
TIME = time.mktime(time.gmtime())

def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


def cgi_output(msg):
    """ One place to generate CGI output"""

    print "Content-type: text/plain"
    print
    print cgi.escape(msg)


def main():
    make_db_conn()
    dict_cur = DB_CONN.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    mc = memcache.Client(['127.0.0.1:11211'], debug=0)
    
    # Try to get the data from memcache, if it fails, continue as normal.
    marker_list = mc.get("marker_list")
    if marker_list:
        cgi_output(marker_list)
        return
    marker_list = []

    all_markers_sql = """SELECT id, location, X(location) AS lng, Y(location) AS lat, priority
                            FROM user_submits
                            WHERE create_date > (%s - (60 * 60 * 24))"""
    dict_cur.execute(all_markers_sql, (TIME,))

    # For each marker submitted, do a distance check. If there is ONE other
    # marker within 10 miles, this is possibly legit and we'll display it.
    for row in dict_cur:
        check_sql = """SELECT SUM(weight)
                        FROM user_submits
                        WHERE create_date > (%s - (60 * 60 * 24))
                        AND ST_DWithin(%s, location, 32186, false)"""
        check_cur = DB_CONN.cursor()
        check_cur.execute(check_sql, (TIME, row['location']))
        check_row = check_cur.fetchone()
        if check_row[0] >= 4:
            marker_list.append({'lat': row['lat'],
                                'lng': row['lng'],
                                'priority': row['priority']})
        check_cur.close()
    # Get the user's tornado markers, so they don't think they've disappeared
    # TODO(avleen): Enable this after we enable sending the registration_id on
    # get_markers.py
    #registration_id = form.getvalue("registrationid", None)
    #if not registration_id or not device_id:
    #    registration_id = 0
    ## Sanitize data ftw!
    #registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)
    #placebo_sql = """SELECT X(location) AS lng, Y(location) AS lat, priority
    #                    FROM user_submits
    #                    WHERE create_date > (%s - (60 * 60 * 24))
    #                    AND registration_id = %s"""
    #dict_cur.execute(placebo_sql, (TIME,regisration_id))
    #if dict_cur.rowcount > 0:
    #    for row in dict_cur:
    #       marker_list.append({'lat': row['lat'],
    #                          'lng': row['lng'],
    #                          'priority': row['priority']})
    if len(marker_list) == 0:
        marker_list = [{"lat": 0, "lng": 0, "priority": 'f'}]
    jsonout = json.dumps(marker_list)
    mc.set("marker_list", jsonout, time=30)

    cgi_output(jsonout)


if __name__ == "__main__":
    main()
