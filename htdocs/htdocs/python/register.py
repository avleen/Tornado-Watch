#!/usr/bin/python

from mod_python import apache, util
import psycopg2
import re

DB_CONN = None

def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


def cgi_output(req, msg):
    """ One place to generate CGI output"""

    req.content_type = "text/plain"
    req.send_http_header()
    req.write(msg)


def index(req):
    form = util.FieldStorage(req)
    device_id = form.getvalue("deviceid", None)
    registration_id = form.getvalue("registrationid", None)
    if not registration_id or not device_id:
        cgi_output(req, "No registration ID given")
        return apache.OK

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

    cgi_output(req, "Registration successful")
    return apache.OK
