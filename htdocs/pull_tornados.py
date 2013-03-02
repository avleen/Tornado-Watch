#!/usr/bin/python

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
        logfile = '/usr/local/www/silverwraith.com/canonical/tw.silverwraith.com/logs/pull_tornados.log'
        open(logfile, 'a').write("%s %s\n" % (time.strftime("%Y%m%d-%H%M%S", time.gmtime()), msg))
    return


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")
    DB_CONN.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)


def main():
    """Pull the CSV data for tornado reports, and insert them into the database."""

    csv_url = 'http://www.spc.noaa.gov/climo/reports/today_raw_torn.csv'
    make_db_conn()
    cur = DB_CONN.cursor()

    try:
        url_fh = urllib.urlopen(csv_url)
    except:
        print 'Unable to open URL: %s' % sys.exc_info[0]
        sys.exit(1)

    # csv.reader iterates over url_fh.
    # The first two rows of the CSV and headers:
    # ['Raw Tornado LSR for 130225 12Z to 11:59Z the next day'],
    # ['Time', 'EF_Scale', 'Location', 'County', 'State', 'LAT', 'LON', 'Remarks']
    labels = ['Time', 'EF_Scale', 'Location', 'County', 'State', 'LAT', 'LON', 'Remarks']
    csv_data = [x for x in csv.DictReader(url_fh, labels)][2:]

    # Quit if csv_data is empty
    if len(csv_data) == 0:
        print "No tornado reports"
        sys.exit(0)

    for row in csv_data:
        # Before we can insert the time, we have to convert it to UTC seconds
        # since epoch. The only thing we get is the hours and minutes. This
        # presents a problem: Normally, we could just check what the day is
        # right now in UTC. This only doesn't work if an alert is timed 23:59,
        # and we then check it after midnight. So we first check if '%H%M' in
        # the CSV is larger than '%H%M' right now. If it is, we're looking at
        # yesterday's alert. What a pain.
        csv_hourmin = int(row['Time'].replace(':', ''))
        now_hourmin = int('%s%s' % (time.gmtime()[3], time.gmtime()[4]))
        if csv_hourmin > now_hourmin:
            day_delta = -1
        else:
            day_delta = 0
        temp_datetime = datetime.datetime.utcnow() + datetime.timedelta(days=day_delta)
        # Now construct a new datetime with the data from report_datetime, with
        # the correct hour and minute, then convert that to UTC time.
        report_datetime = datetime.datetime(temp_datetime.year,
                temp_datetime.month, temp_datetime.day, int(row['Time'][0:2]),
                int(row['Time'][2:4]))
        report_datetime = report_datetime.strftime('%a %b %d %H:%M:%S %Y')
        # Convert to seconds
        report_datetime_utc = time.mktime(time.strptime(report_datetime))

        # Loop over the rows of csv data. Look for the time and location in the
        # database. If they exist, skip it. If they don't, insert it.
        sql = """SELECT * FROM nws_submits
                    WHERE time = %s
                    AND address = %s LIMIT 1"""
        cur.execute(sql, (report_datetime_utc, row['Location']))
        if cur.rowcount == 1:
            print_debug('%s @ %s found in DB, skipping' % (row['Time'], row['Location']))
            continue
        print_debug('%s @ %s not found in DB, inserting' % (row['Time'], row['Location']))

        insert_sql = """INSERT INTO nws_submits
                            (time, address, county, state, location, remarks)
                            VALUES (%s, %s, %s, %s, makepoint(%s, %s), %s)"""
        print_debug(cur.mogrify(insert_sql, (report_datetime_utc, row['Location'],
            row['County'], row['State'], row['LON'], row['LAT'], row['Remarks'])))
        cur.execute(insert_sql, (report_datetime_utc, row['Location'],
            row['County'], row['State'], row['LON'], row['LAT'], row['Remarks']))


if __name__ == '__main__':
    if sys.argv[1:] and sys.argv[1] == '-d':
        DEBUG = True
    main()
