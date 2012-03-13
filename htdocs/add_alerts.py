#!/usr/bin/python

import psycopg2
import time

DB_CONN = None
DEBUG = False
TIME = time.mktime(time.gmtime())

def add_alert(registration_id, reference_id, alert_type):
    """Add an alert to the alert_queue to warn a user"""

    cur = DB_CONN.cursor()
    # First check if an alert is pending
    print_debug('Checking if an alert is pending for %s' % registration_id)
    sql = """SELECT id FROM alert_queue
                WHERE registration_id = %s
                AND reference_id = %s
                AND alert_type = %s
                AND alerted = 'f'"""
    cur.execute(sql, (registration_id, reference_id, alert_type))
    if cur.rowcount == 0:
        print_debug('No pending alert found! Adding one.')
        sql = """INSERT INTO alert_queue
                    (registration_id, reference_id, alert_type)
                    VALUES (%s, %s, %s)"""
        print_debug('Adding alert to db %s' %
                    (sql % (registration_id, reference_id, alert_type)))
        cur.execute(sql, (registration_id, reference_id, alert_type))
        DB_CONN.commit()


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


def print_debug(msg):
    """Print debug data in debug mode"""

    if DEBUG:
        print msg
    return


def main():
    """Main"""

    # Some initial setup
    make_db_conn()
    main_cur = DB_CONN.cursor()

    # Get a list of users in current tornado alert zones
    sql = """SELECT r.registration_id, t.id
                FROM user_registration r, counties c, tornado_warnings t
                WHERE ST_Intersects(r.location, c.the_geom)
                AND c.county ~* t.county
                AND t.starttime < %s
                AND t.endtime > %s"""
    print_debug('Getting the list of users in tornado zones')
    main_cur.execute(sql, (TIME, TIME))
    print_debug('%s users found in tornado zones' % main_cur.rowcount)

    # See if there is an alert on the queue for each user in the zone
    # After that, see if they're within 10mi of a user submitted alert
    print_debug('Iterating through found users')
    for record in main_cur:
        registration_id = record[0]
        reference_id = record[1]
        check_cur = DB_CONN.cursor()

        print_debug('Checking if user %s has pending zone alerts' % registration_id)
        check_sql = """SELECT id FROM alert_queue
                        WHERE registration_id = %s
                        AND reference_id = %s
                        AND alert_type = 'zone'"""
        check_cur.execute(check_sql, (registration_id, reference_id))
        if check_cur.rowcount == 0:
            print_debug('They dont! Adding an alert. Hope they get it soon.')
            add_alert(registration_id, reference_id, 'zone')

        # Check if a user's last known location (r.location) is within 10mi of a
        # reported tornado (s.location) which was reported in the last 30
        # minutes.
        print_debug('Checking if user %s is near a user submittion' % registration_id)
        check_sql = """SELECT r.registration_id, s.priority, s.id
                        FROM user_registration r, user_submits s
                        WHERE distance(r.location, s.location) < 16093
                        AND s.create_date < %s
                        AND r.registration_id = %s"""
        check_cur.execute(check_sql, (TIME - 1800, registration_id))
        check_row = check_cur.fetchone()
        user_submit_id = check_row[2]
        if check_row[1] == 't':
            distance_type = 'distance-high'
        else:
            distance_type = 'distance-low'

        # If we got a result back, see if there has been an alert in the last 30
        # minutes. If so, don't alert again. Otherwise, warn them!
        if check_cur.rowcount > 0:
            print_debug('They are! Lets see if we need to alert them.')

            check_sql = """SELECT id FROM alert_queue
                            WHERE alert_type = 'distance'
                            AND create_date < %s
                            AND registration_id = %s"""
            check_cur.execute(check_sql, (TIME - 1800, registration_id))
            if check_cur.rowcount == 0:
                print_debug('We do - they havent had an alert in 30 minutes.')
                add_alert(registration_id, user_submit_id, distance_type)

    return


if __name__ == '__main__':
    main()