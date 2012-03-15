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
    sql = """SELECT X(location) AS lng, Y(location) AS lat, priority
                FROM user_submits
                WHERE create_date > (%s - (60 * 60 * 24))"""
    dict_cur.execute(sql, (TIME,))
    jsonout = json.dumps(dict_cur.fetchall())

    print "Content-type: text/plain"
    print
    print cgi.escape(jsonout)


if __name__ == "__main__":
    main()
