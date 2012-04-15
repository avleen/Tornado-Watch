#!/usr/bin/python

import copy
import csv
import datetime
import psycopg2
import sys
import time
import urllib

DB_CONN = None
DEBUG = False


def print_debug(msg):
    """Print debug data in debug mode"""

    if DEBUG:
        logfile = '/usr/local/www/silverwraith.com/canonical/tw.silverwraith.com/logs/update_karma.log'
        open(logfile, 'a').write("%s %s\n" % (time.strftime("%Y%m%d-%H%M%S", time.gmtime()), msg))
    return


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")
    DB_CONN.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)


def main():
    make_db_conn()
    cur = DB_CONN.cursor()
    sighting_distance = 1609 * 5
    today = datetime.datetime.today()

    print_debug("Fetching CSV data from NOAA")
    url = 'http://www.spc.noaa.gov/climo/reports/today_torn.csv'
    csv_data = csv.reader(urllib.urlopen(url))
    # Skip the first line, it's a header
    csv_data.next()
    print_debug("Iterating over tornado sightings")
    for line in csv_data:
        lat = line[5]
        lng = line[6]
        hour = int(line[0][0:2])
        minute = int(line[0][2:])
        print_debug("Tornado seen at: %s, %s @ %s:%s" % (lng, lat, hour, minute))
        # The NWS reports are motherfuckers. The start a noon UTC. What? I don't
        # know why. So we make an adjustment. If the hours is <= 11, we've
        # passed midnight UTC into the next day, so add a day.
        _today = copy.deepcopy(today)
        if hour <= 11:
            _today = _today + datetime.timedelta(1)
        # The csv has the time as UTC. We need to convert it to epoch localtime,
        # as we store everything in the DB as epoch localtime.
        tornado_local_time = float(_today.replace(hour=hour, minute=minute).strftime('%s')) - time.timezone
        # We pull every 5 minutes. Ignore any reports more than 5 minutes old.
        if tornado_local_time < (time.time() - 600):
            continue
        print_debug('Found a new tornado report')
        # Find all tornados reported within an hour, either side, of the NOAA
        # tornado report time
        ids_sql = """SELECT registration_id FROM user_submits
                        WHERE ST_DWithin(location, ST_MakePoint(%s, %s), %s, false)
                        AND create_date BETWEEN (%s - 1440) AND (%s + 1440)"""
        print_debug("Looking for users who reported this tornado with:")
        print_debug(cur.mogrify(ids_sql, (lng, lat, sighting_distance, tornado_local_time, tornado_local_time)))
        cur.execute(ids_sql, (lng, lat, sighting_distance, tornado_local_time, tornado_local_time))
        if cur.rowcount == 0:
            continue
        else:
            print_debug("Users found!")
        update_cur = DB_CONN.cursor()
        for row in cur.fetchall():
            update_sql = """UPDATE user_registration SET karma = karma + 1
                                WHERE registration_id = %s"""
            update_cur.execute(update_sql, (row[0],))


if __name__ == '__main__':
    if sys.argv[1:] and sys.argv[1] == '-d':
        DEBUG = True
    main()
