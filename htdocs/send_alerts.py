#!/usr/bin/python

from email.mime.text import MIMEText
from threading import Thread
from Queue import Queue
import psycopg2
import smtplib
import sys
import time
import urllib
import urllib2

sys.path.insert(0, '/www/silverwraith.com/canonical/tw.silverwraith.com')
import pidlock

num_threads=20
queue = Queue()
DB_CONN = None
AUTH_KEY = None
BACKOFF = 0
DEBUG = False

def alert_runner(i, q):
    """Get people to alert, and alert them"""
    global BACKOFF

    while True:
        if BACKOFF > 0:
            print_debug("BACKOFF IN EFFECT: %s seconds" % BACKOFF)
            time.sleep(BACKOFF)
        serial_id, registration_id, alert_type = queue.get()
        if alert_type.startswith("distance"):
            payload = "Tornado spotted in your area"
        elif alert_type == ("zone-watch"):
            payload = "Your county is under a TORNADO WATCH - BE PREPARED"
        elif alert_type == ("zone-warning"):
            payload = "Your county is under a tornado warning"
        #else:
        #    print "Unknown alert_type."
        #    return
        post_dict = {
            "data.payload": payload,
            "collapse_key": "0",
            "registration_id": registration_id
        }
        headers = {"Authorization": "GoogleLogin auth=" + AUTH_KEY}
        request = urllib2.Request("https://android.apis.google.com/c2dm/send",
                                  urllib.urlencode(post_dict),
                                  headers)
        try:
            print_debug("Sending notification for %s" % alert_type)
            response = urllib2.urlopen(request)
            response_as_str = response.read()
            print_debug("Response: %s" % response_as_str)
            if not response_as_str.startswith("Error"):
                if BACKOFF > 0:
                    BACKOFF = 0
                if response.headers.has_key('Update-Client-Auth'):
                    update_auth_key(response.headers.getheader('Update-Client-Auth'))
                sql = """UPDATE alert_queue
                            SET alerted = 't',
                            alert_date = date_part('epoch', now())
                            WHERE id = %s"""
                cursor = DB_CONN.cursor()
                cursor.execute(sql, (serial_id,))
                cursor.close()
                q.task_done()
            elif response_as_str == "Error=NotRegistered":
                # The user is not registered any more! Delete them from the DB!
                cursor = DB_CONN.cursor()
                regid_sql = """SELECT registration_id FROM alert_queue
                                WHERE id = %s"""
                cursor.execute(regid_sql, (serial_id,))
                registration_id = cursor.fetchone()[0]
                cursor.close()

                print_debug("%s is no longer registered" % registration_id)
                delete_cursor = DB_CONN.cursor()
                delete_sql = """DELETE FROM alert_queue
                                    WHERE registration_id = %s"""
                delete_cursor.execute(delete_sql, (registration_id,))
                delete_sql = """DELETE FROM user_registration
                                    WHERE registration_id = %s"""
                delete_cursor.execute(delete_sql, (registration_id,))
                delete_cursor.close()
                q.task_done()
            else:
                if BACKOFF == 0:
                    BACKOFF = 0.1
                else:
                    BACKOFF = BACKOFF * 1.5
                print_debug("Backoff increased. Error: " + response_as_str)
                msg = MIMEText("Uh oh!")
                msg['Subject'] = 'Failed to alert user: ' + response_as_str
                msg['From'] = 'postmaster@silverwraith.com'
                msg['To'] = 'postmaster@silverwraith.com'
                s = smtplib.SMTP('localhost')
                s.sendmail('postmaster@silverwraith.com',
                           ['postmaster@silverwraith.com'],
                           msg.as_string())
                s.quit()
                q.task_done()
        except urllib2.HTTPError, e:
            print_debug('HTTPError ' + str(e))


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")
    DB_CONN.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)


def update_auth_key(submitted_key=None):
    global AUTH_KEY

    if submitted_key:
        print_debug("Updated auth key received in headers")
        AUTH_KEY=submitted_key
        return

    if not AUTH_KEY:
        print_debug("Requesting new auth key")
        auth_pass = open('AUTH_PASS').read()
        auth_post_dict = {'Email': 'avleen@gmail.com',
                'Passwd': auth_pass,
                'accountType': 'GOOGLE',
                'source': 'com.silverwraith.tornadowatch',
                'service': 'ac2dm'}
        auth_request = urllib2.Request("https://www.google.com/accounts/ClientLogin",
                                       urllib.urlencode(auth_post_dict))
        auth_response = urllib2.urlopen(auth_request)
        for line in auth_response.read().split('\n'):
            if line.startswith('Auth='):
                AUTH_KEY = line.split('=')[1]
        if not AUTH_KEY:
            print_debug("Cannot get AUTH KEY! Quitting!")
            sys.exit(1)


def print_debug(msg):
    """Print debug data in debug mode"""

    if DEBUG:
        logfile = '/usr/local/www/silverwraith.com/canonical/tw.silverwraith.com/logs/send_alerts.log'
        open(logfile, 'a').write("%s %s\n" % (time.strftime("%Y%m%d-%H%M%S", time.gmtime()), msg))
    return


def main():
    """Main"""

    update_auth_key()

    ### Start the threads
    for i in range(num_threads):
        worker = Thread(target=alert_runner, args=(i, queue))
        worker.setDaemon(True)
        worker.start()

    ### Loop over the queue to find alerts
    make_db_conn()
    pending_alerts_sql = """SELECT id, registration_id, alert_type
                               FROM alert_queue
                               WHERE alerted = 'f'"""
    while True:
        main_cur = DB_CONN.cursor()
        print_debug("Selecting and sending alerts... ")
        main_cur.execute(pending_alerts_sql)
        print_debug(str(main_cur.rowcount) + " rows... ")
        for row in main_cur:
            queue.put(row)
        queue.join()
        print_debug("done.")
        main_cur.close()
        time.sleep(5)



if __name__ == "__main__":
    pidlocking = pidlock.Pidlock('send_alerts')
    pidlocking.start()
    if sys.argv[1:] and sys.argv[1] == '-d':
        DEBUG = True
    main()
    pidlocking.stop()
