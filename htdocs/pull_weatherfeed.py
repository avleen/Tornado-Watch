#!/usr/bin/python


import feedparser
import psycopg2
import re
import sys
import time

DB_CONN = None
DEBUG = False

def get_feed():
    """Fetch the RSS feed from NWS"""

    print_debug('Fetching RSS')
    if DEBUG:
        #url = 'test_rss.xml'
        url = 'http://alerts.weather.gov/cap/us.php?x=1'
    else:
        url = 'http://alerts.weather.gov/cap/us.php?x=1'
    feed = feedparser.parse(url)
    return feed


def get_tornado_warnings(feed):
    """Get a list of the current tornado warnings in effect"""

    state_list = ['AL', 'AK', 'AS', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'DC',
                  'FM', 'FL', 'GA', 'GU', 'HI', 'ID', 'IL', 'IN', 'IA', 'KS',
                  'KY', 'LA', 'ME', 'MH', 'MD', 'MA', 'MI', 'MN', 'MS', 'MO',
                  'MT', 'NE', 'NV', 'NH', 'NJ', 'NM', 'NY', 'NC', 'ND', 'MP',
                  'OH', 'OK', 'OR', 'PW', 'PA', 'PR', 'RI', 'SC', 'SD', 'TN',
                  'TX', 'UT', 'VT', 'VA', 'VI', 'WA', 'WV', 'WI', 'WY']

    print_debug('Parsing entries')
    for entry in feed.entries:
        if not entry['event']:
            continue

        if not re.search('tornado', entry['event'], re.IGNORECASE):
            print_debug('..not a tornado warning or watch')
            continue

        affected_counties = entry['areadesc'].split('; ')
        if not affected_counties:
            print_debug('Counties list empty:\n' + entry)
            debug_mail('Counties empty', entry)

        affected_state = re.search('\?x=(..)', entry['id']).group(1)
        if affected_state not in state_list:
            print_debug('State not found:\n' + entry)
            debug_mail('State not found', entry)
            continue

        starttime = time.mktime(feedparser._parse_date(entry['effective']))
        endtime = time.mktime(feedparser._parse_date(entry['expires']))
        for affected_county in affected_counties:
            yield affected_county, affected_state, starttime, endtime


def add_warning(county, state, starttime, endtime):
    """Take information about a current tornado warning and save it in the DB"""

    cur = DB_CONN.cursor()

    # Does the warning currently exist in the DB? If so, don't add again!
    sql = """SELECT starttime, endtime, county, state FROM tornado_warnings
                WHERE starttime = %s
                AND endtime = %s
                AND county = %s
                AND state = %s"""
    cur.execute(sql, (starttime, endtime, county, state))
    if cur.rowcount == 0:
        sql = """INSERT INTO tornado_warnings (starttime, endtime, county, state)
                    VALUES (%s, %s, %s, %s)"""
        print_debug('Adding warning to db: %s' % (sql %
                                                  (starttime, endtime, county, state)))
        try:
            cur.execute(sql, (starttime, endtime, county, state))
        except:
            print_debug('Tried to add tornado warning, failed: %s' % sys.exc_info()[1])
            DB_CONN.rollback()
        DB_CONN.commit()
    return


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")
    

def debug_mail(msg, entry):
    """Send an email when something goes wrong"""

    return


def print_debug(msg):
    """Print debug data in debug mode"""

    if DEBUG:
        print msg
    return


def main():
    """Main"""

    feed = get_feed()

    # Set up the global DB connection.
    # It's not the more efficient way to do it, but this whole script is
    # serialised right now so it could be worse.
    make_db_conn()

    for affected_county, affected_state, starttime, endtime in get_tornado_warnings(feed):
        add_warning(affected_county, affected_state, starttime, endtime)


if __name__ == '__main__':
    if sys.argv[1:] and sys.argv[1] == '-d':
            # First add the entry to the db, then pass it back
        DEBUG = True
    main()
