#!/usr/bin/python


import feedparser
import psycopg2
import re
import sys
import time

sys.path.insert(0, '/www/silverwraith.com/canonical/tw.silverwraith.com')
import pidlock

DB_CONN = None
DEBUG = False
STAT_FILE = "/tmp/pull_weatherfeed.tmp"

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

    # Trim down the list of alerts to actual tornado warnings
    tornado_alerts = [ x for x in feed.entries if
                      re.search('tornado', x['cap_event'], re.IGNORECASE) and
                      x['cap_status'] == 'Actual' ]
    for entry in tornado_alerts:
        affected_counties = entry['cap_areadesc'].split('; ')
        if not affected_counties:
            print_debug('Counties list empty:\n' + entry)
            debug_mail('Counties empty', entry)

        affected_state = re.search('\?x=(..)', entry['id']).group(1)
        if affected_state not in state_list:
            print_debug('State not found:\n' + entry)
            debug_mail('State not found', entry)
            continue
        if entry.cap_event == 'Tornado Watch':
            alert_type='watch'
        elif entry.cap_event == 'Tornado Warning':
            alert_type='warning'
        else:
            alert_type='Unknown'

        starttime = time.mktime(feedparser._parse_date(entry['cap_effective'])) - time.timezone
        endtime = time.mktime(feedparser._parse_date(entry['cap_expires'])) - time.timezone
        for affected_county in affected_counties:
            yield affected_county, affected_state, starttime, endtime, alert_type


def add_warning(county, state, starttime, endtime, alert_type):
    """Take information about a current tornado warning and save it in the DB"""

    cur = DB_CONN.cursor()

    # Does the warning currently exist in the DB? If so, don't add again!
    # We actually need to check if the current check is there, see if it needs
    # to be upgraded or downgraded.
    sql = """SELECT id, starttime, endtime, county, state, alert_type FROM tornado_warnings
                WHERE starttime = %s
                AND endtime = %s
                AND county = %s
                AND state = %s"""
    cur.execute(sql, (starttime, endtime, county, state))
    if cur.rowcount > 0:
        check_row = cur.fetchone()
        if (check_row[5] == 'watch' and alert_type == 'warning') or \
                (check_row[5] == 'warning' and alert_type == 'watch'):
                    print_debug('Deleting old tornado warning, the status has changed')
                    del_sql = """DELETE FROM tornado_warnings
                                    WHERE id = %s"""
                    cur.execute(del_sql, (check_row[0],))
        else:
            print_debug('No change needed to NWS tornado alert')
            return
    sql = """INSERT INTO tornado_warnings (starttime, endtime, county, state, alert_type)
                VALUES (%s, %s, %s, %s, %s)"""
    print_debug('Adding warning to db: %s' % (sql %
                                              (starttime, endtime, county, state, alert_type)))
    try:
        cur.execute(sql, (starttime, endtime, county, state, alert_type))
    except:
        print_debug('Tried to add tornado warning, failed: %s' % sys.exc_info()[1])
    return


def get_true_county_name(affected_county, affected_state):
    """Find the full county name from the DB"""

    cur = DB_CONN.cursor()
    sql = """SELECT DISTINCT county
                FROM counties
                WHERE county ~* %s
                AND state = %s
                LIMIT 1"""
    cur.execute(sql, (affected_county, affected_state))
    if cur.rowcount == 0:
        return '%s County' % affected_county
    else:
        row = cur.fetchone()
        return '%s' % row[0]


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")
    DB_CONN.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)
    

def debug_mail(msg, entry):
    """Send an email when something goes wrong"""

    return


def print_debug(msg):
    """Print debug data in debug mode"""

    if DEBUG:
        print "%s %s" % (time.strftime("%Y%m%d-%H%M%S", time.gmtime()), msg)
    return


def main():
    """Main"""

    feed = get_feed()

    # Set up the global DB connection.
    # It's not the more efficient way to do it, but this whole script is
    # serialised right now so it could be worse.
    make_db_conn()

    for affected_county, affected_state, starttime, endtime, alert_type in get_tornado_warnings(feed):
        # A "county" name here is missing its suffix. The suffix can be
        # "County", or "Parish" or a few other things. We need to first find out
        # what it is, and then save that. Doing so lets us do an exact join
        # between two tables instead of having to do a slow, expensive regex
        # match.
        true_county_name = get_true_county_name(affected_county, affected_state)
        add_warning(true_county_name, affected_state, starttime, endtime, alert_type)


if __name__ == '__main__':
    if sys.argv[1:] and sys.argv[1] == '-d':
            # First add the entry to the db, then pass it back
        DEBUG = True
    pidlocking = pidlock.Pidlock('pull_weatherfeed')
    pidlocking.start()
    main()
    pidlocking.stop()
    open(STAT_FILE, 'w')
