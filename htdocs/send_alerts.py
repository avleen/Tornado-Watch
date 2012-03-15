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

num_threads=20
queue = Queue()
DB_CONN = None
AUTH_KEY = None
BACKOFF = 0

def alert_runner(i, q):
    """Get people to alert, and alert them"""
    global BACKOFF

    while True:
        if BACKOFF > 0:
            print "BACKOFF IN EFFECT: %s seconds" % BACKOFF
            time.sleep(BACKOFF)
        serial_id, registration_id, alert_type = queue.get()
        if alert_type == "distance-low" or alert_type == "distance-high":
            payload = "Tornado spotted in your area"
        elif alert_type == "zone":
            payload = "Your county is under a tornado watch"
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
            print "Sending notification for %s" % alert_type
            response = urllib2.urlopen(request)
            response_as_str = response.read()
            print "Response: %s" % response_as_str
            if not response_as_str.startswith("Error"):
                if BACKOFF > 0:
                    BACKOFF = 0
                if response.headers.has_key('Update-Client-Auth'):
                    update_auth_key(response.headers.getheader('Update-Client-Auth'))
                sql = """UPDATE alert_queue SET alerted = 't'
                            WHERE id = %s"""
                cursor = DB_CONN.cursor()
                cursor.execute(sql, (serial_id,))
                DB_CONN.commit()
                q.task_done()
            else:
                if BACKOFF == 0:
                    BACKOFF = 0.1
                else:
                    BACKOFF = BACKOFF * 1.5
                print "Backoff increased. Error: " + response_as_str
                msg = MIMEText("Uh oh!")
                msg['Subject'] = 'Failed to alert user: ' + response_as_str
                msg['From'] = 'postmaster@silverwraith.com'
                msg['To'] = 'postmaster@silverwraith.com'
                s = smtplib.SMTP('localhost')
                s.sendmail('postmaster@silverwraith.com',
                           ['postmaster@silverwraith.com'],
                           msg.as_string())
                s.quit()
        except urllib2.HTTPError, e:
            print 'HTTPError ' + str(e)


def make_db_conn():
    """Establish a database connection"""

    global DB_CONN
    #print_debug('Setting up DB connection')
    DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres")


def update_auth_key(submitted_key=None):
    global AUTH_KEY

    if submitted_key:
        print "Updated auth key received in headers"
        AUTH_KEY=submitted_key
        return

    if not AUTH_KEY:
        print "Requesting new auth key"
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
            print "Cannot get AUTH KEY! Quitting!"
            sys.exit(1)


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
    main_cur = DB_CONN.cursor()
    pending_alerts_sql = """SELECT id, registration_id, alert_type
                               FROM alert_queue
                               WHERE alerted = 'f'"""
    while True:
        sys.stderr.write("Selecting and sending alerts... ")
        main_cur.execute(pending_alerts_sql)
        sys.stderr.write(str(main_cur.rowcount) + " rows... ")
        for row in main_cur:
            print row
            queue.put(row)
        queue.join()
        sys.stderr.write("done.\n")
        time.sleep(5)



if __name__ == "__main__":
    main()
