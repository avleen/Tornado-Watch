#!/usr/bin/python

import cgi
import json
import psycopg2
import psycopg2.extras
import time

DB_CONN = None
TIME = time.mktime(time.gmtime())

def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


def main():
    make_db_conn()
    dict_cur = DB_CONN.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
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
    if len(marker_list) == 0:
        marker_list = [{"lat": 0, "lng": 90, "priority": 'f'}]
    jsonout = json.dumps(marker_list)
    print "markers: %d" % len(marker_list)

    print "Content-type: text/plain"
    print
    print cgi.escape(jsonout)


if __name__ == "__main__":
    main()
