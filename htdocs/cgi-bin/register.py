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
    device_id = form.getvalue("deviceid", None)
    registration_id = form.getvalue("registrationid", None)
    if not registration_id or not device_id:
        return

    # Sanitize data ftw!
    device_id = re.sub('[^A-Za-z0-9_\-]+', '', device_id)
    registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)

    make_db_conn()
    cur = DB_CONN.cursor()
    check_sql = """SELECT * FROM user_registration
                    WHERE device_id = %s
                    AND registration_id = %s"""
    cur.execute(check_sql, (device_id, registration_id))
    if cur.rowcount == 0:
        add_sql = """INSERT INTO user_registration (device_id, registration_id)
                        VALUES (%s, %s)"""
        cur.execute(add_sql, (device_id, registration_id))
        DB_CONN.commit()

    print "Content-type: text/plain"
    print
    print cgi.escape("Registration successful")


if __name__ == "__main__":
    main()
