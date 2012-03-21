#!/usr/bin/python

import sys
sys.path.insert(0, '/www/silverwraith.com/canonical/tw.silverwraith.com')

import psycopg2
import time
import pidlock

DB_CONN = None
DEBUG = False
LOW_WEIGHT = 1
HIGH_WEIGHT = 2
REQUIRED_WEIGHT = 4

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
    cur.close()


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")
    DB_CONN.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)


def print_debug(msg):
    """Print debug data in debug mode"""

    if DEBUG:
        logfile = '/usr/local/www/silverwraith.com/canonical/tw.silverwraith.com/logs/add_alerts.log'
        open(logfile, 'a').write("%s %s\n" % (time.strftime("%Y%m%d-%H%M%S", time.gmtime()), msg))
    return


def main():
    """Main"""

    # Some initial setup
    make_db_conn()

    # The rest of this is basically a big loop with a sleep
    while True:
        time_now = time.mktime(time.localtime())
        time.sleep(5)
        main_cur = DB_CONN.cursor()
        # Get a list of users in current tornado alert zones
        sql = """SELECT r.registration_id, t.id, t.alert_type
                    FROM user_registration r, counties c, tornado_warnings t
                    WHERE ST_Intersects(r.location, c.the_geom)
                    AND c.county ~* t.county
                    AND t.starttime < %s
                    AND t.endtime > %s"""
        print_debug('Getting the list of users in tornado zones')
        main_cur.execute(sql, (time_now, time_now))
        print_debug('%s users found in tornado zones' % main_cur.rowcount)

        # See if there is an alert on the queue for each user in the zone
        # After that, see if they're within 10mi of a user submitted alert
        print_debug('Iterating through found users')
        for record in main_cur:
            registration_id = record[0]
            reference_id = record[1]
            nws_alert_type = record[2]
            check_cur = DB_CONN.cursor()

            print_debug('Checking if user %s has pending zone alerts' % registration_id)
            check_sql = """SELECT id FROM alert_queue
                            WHERE registration_id = %s
                            AND reference_id = %s
                            AND alert_type = %s"""
            check_cur.execute(check_sql, (registration_id, reference_id, nws_alert_type))
            if check_cur.rowcount == 0:
                print_debug('They dont! Adding an alert. Hope they get it soon.')
                add_alert(registration_id, reference_id, '%s' % nws_alert_type)

            # Check if a user's last known location (r.location) is within 20mi of a
            # reported tornado (s.location) which was reported in the last 60
            # minutes.
            print_debug('Checking if user %s is near TWO user submission' % registration_id)
            check_sql = """SELECT s.registration_id AS s_id, s.priority, s.id, s.weight
                            FROM user_submits s
                            INNER JOIN user_registration r ON ST_DWithin(r.location, s.location, 32186, false)
                            WHERE s.create_date > %s
                            AND r.registration_id = %s
                            AND s.registration_id != %s"""
            #print_debug(check_sql % (time_now - 3600, registration_id, registration_id))
            check_cur.execute(check_sql, (time_now - 3600, registration_id, registration_id))
            # If we didn't find a match against user_submit, continue
            if not check_cur.rowcount > 1:
                print_debug('They are not, carry on.')
                check_cur.close()
                continue

            # If we got a result back, see if there has been an alert in the last 30
            # minutes. If so, don't alert again. Otherwise, warn them!
            weight_count = 0
            for check_row in check_cur:
                print_debug('They are near: %s, %s, %s, %s' % (check_row[0], check_row[1], check_row[2], check_row[3]))
                weight_count = weight_count + check_row[3]
            if weight_count < 4:
                print_debug('Weight too low to alert: %s' % weight_count)
                continue
            else:
                print_debug('High weight found: %s. Preparing to alert.' % weight_count)

            # Grab the most recent user_submit_id and distance_type to record in
            # the alert_queue table
            user_submit_id = check_row[2]
            distance_type = 'distance-high' if check_row[1] == 't' else 'distance-low'

            print_debug('Lets see if we need to alert them.')
            check_sql = """SELECT id FROM alert_queue
                            WHERE alert_type LIKE 'distance%%'
                            AND create_date > %s
                            AND registration_id = %s"""
            check_cur.execute(check_sql, (time_now - 1800, registration_id))
            if check_cur.rowcount == 0:
                print_debug('We do - they havent had an alert in 30 minutes.')
                add_alert(registration_id, user_submit_id, distance_type)
            else:
                print_debug('We dont!')
            check_cur.close()
        main_cur.close()
    return


if __name__ == '__main__':
    pidlocking = pidlock.Pidlock('add_alerts')
    pidlocking.start()
    if sys.argv[1:] and sys.argv[1] == '-d':
        DEBUG = True
    main()
    pidlocking.stop()
