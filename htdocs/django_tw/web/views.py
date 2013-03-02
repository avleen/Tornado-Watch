from django.http import HttpResponse
from django.shortcuts import render_to_response
from django.views.decorators.csrf import csrf_exempt


import json
import memcache
import psycopg2
import psycopg2.extras
import re
import time

STAT_FILE = "/tmp/user_submits.tmp"
DB_CONN = psycopg2.connect("dbname=tornadowatch user=postgres host=localhost port=6432")
DB_CONN.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)
mc = memcache.Client(['127.0.0.1:11211'], debug=0)

@csrf_exempt
def get_markers(request):
    TIME = time.mktime(time.gmtime())
    dict_cur = DB_CONN.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

    # Try to get the data from memcache, if it fails, continue as normal.
    marker_list = mc.get("marker_list")
    if marker_list:
        return HttpResponse(marker_list, mimetype="text/plain")
    marker_list = []

    all_markers_sql = """SELECT s.id, s.location, X(s.location) AS lng, Y(s.location) AS lat, s.priority
                            FROM user_submits s, user_registration r
                            WHERE s.create_date > (%s - (60 * 60 * 24))
                            AND s.registration_id = r.registration_id
                            AND r.karma > 0"""
    dict_cur.execute(all_markers_sql, (TIME,))

    # For each marker submitted, do a distance check. If there is ONE other
    # marker within 10 miles, this is possibly legit and we'll display it.
    for row in dict_cur:
        check_sql = """SELECT SUM(weight)
                        FROM user_submits
                        WHERE create_date > (%s - (60 * 60 * 24))
                        AND ST_DWithin(%s, location, 32186, false)"""
        check_cur = DB_CONN.cursor()
        check_cur.execute(check_sql, (TIME, row['s.location']))
        check_row = check_cur.fetchone()
        if check_row[0] >= 4:
            marker_list.append({'lat': row['lat'],
                                'lng': row['lng'],
                                'priority': row['s.priority']})
        check_cur.close()

    # Get all NWS markers for the last 12 hours.

    nws_markers_sql = """SELECT s.id, s.location, X(s.location) AS lng, Y(s.location) AS lat
                            FROM nws_submits s
                            WHERE create_date > (%s - (60 * 60 * 12))"""
    nws_cur = DB_CONN.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
    nws_cur.execute(nws_markers_sql, (TIME,))
    for row in nws_cur:
        marker_list.append({'lat': row['lat'],
                            'lng': row['lng'],
                            'priority': 'high'})
    # Get the user's tornado markers, so they don't think they've disappeared
    # TODO(avleen): Enable this after we enable sending the registration_id on
    # get_markers.py
    #registration_id = form.getvalue("registrationid", None)
    #if not registration_id or not device_id:
    #    registration_id = 0
    ## Sanitize data ftw!
    #registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)
    #placebo_sql = """SELECT X(location) AS lng, Y(location) AS lat, priority
    #                    FROM user_submits
    #                    WHERE create_date > (%s - (60 * 60 * 24))
    #                    AND registration_id = %s"""
    #dict_cur.execute(placebo_sql, (TIME,regisration_id))
    #if dict_cur.rowcount > 0:
    #    for row in dict_cur:
    #       marker_list.append({'lat': row['lat'],
    #                          'lng': row['lng'],
    #                          'priority': row['priority']})
    if len(marker_list) == 0:
        marker_list = [{"lat": 0, "lng": 0, "priority": 'f'}]
    jsonout = json.dumps(marker_list)
    mc.set("marker_list", jsonout, time=30)
    return HttpResponse(jsonout, mimetype="text/plain")


@csrf_exempt
def register(request):
    if 'deviceid' in request.POST:
        device_id = request.POST['deviceid']
    else:
        device_id = None
    if 'registrationid' in request.POST:
        registration_id = request.POST['registrationid']
    else:
        registration_id = None
    if not registration_id or not device_id:
        return HttpResponse("No registration ID given", mimetype="text/plain")

    # Sanitize data ftw!
    device_id = re.sub('[^A-Za-z0-9_\-]+', '', device_id)
    registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)

    cur = DB_CONN.cursor()
    check_sql = """SELECT * FROM user_registration
                    WHERE device_id = %s
                    AND registration_id = %s"""
    cur.execute(check_sql, (device_id, registration_id))
    if cur.rowcount == 0:
        add_sql = """INSERT INTO user_registration (device_id, registration_id)
                        VALUES (%s, %s)"""
        cur.execute(add_sql, (device_id, registration_id))

    return HttpResponse("Registration successful", mimetype="text/plain")


@csrf_exempt
def updatelocation(request):
    lng = request.POST['lng']
    lat = request.POST['lat']
    registration_id = request.POST['registrationId']

    # Sanitize data ftw!
    lng = re.sub('[^0-9\-]+', '', lng)
    lat = re.sub('[^0-9\-]+', '', lat)
    registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)
    if not lng or not lat:
        return HttpResponse("", mimetype="text/plain")

    # lng and lat in postgis are decimal, not microdegrees
    lng = float(lng) / 1000000
    lat = float(lat) / 1000000

    sql = """UPDATE user_registration
                SET location = makepoint(%s, %s)
                WHERE registration_id = %s"""
    cur = DB_CONN.cursor()
    cur.execute(sql, (lng, lat, registration_id))

    return HttpResponse("Location updated", mimetype="text/plain")


def check_submit_in_zone(lng, lat):
    """Check that the submittion is currently in a tornado warning or watch
    zone"""

    cur = DB_CONN.cursor()
    sql = """SELECT c.state, c.county FROM counties c, tornado_warnings t
                WHERE ST_Intersects(makepoint(%s, %s), c.the_geom)
                AND c.county ~* t.county
                AND t.endtime > date_part('epoch', now())
                AND t.starttime < date_part('epoch', now())"""
    #open('/tmp/av', 'w').write(cur.mogrify(sql, (lng, lat)))
    cur.execute(sql, (lng, lat))
    if cur.rowcount == 0:
        return False
    else:
        return True


def check_recent_user(registration_id):
    """Returns True if this is a recently registered user"""

    cur = DB_CONN.cursor()

    # First get the device_id, if it registered in the last 30 minutes.
    sql = """SELECT device_id FROM user_registration
                WHERE registration_id = %s
                AND create_date < (date_part('epoch', now()) - (60 * 30))"""
    cur.execute(sql, (registration_id,))

    # If it didn't register in the last 30 minutes, we can return False
    if cur.rowcount == 0:
        return False

    # If we get here, it DID register in the last 30 minutes. But this might
    # have been a re-registration, so check if the device_id has been seen
    # before but more than 30 minutes ago. If so, it's old and return False,
    # otherwise return True.
    row = cur.fetchone()
    sql = """SELECT registration_id FROM user_registration
                WHERE device_id = %s
                AND create_date > (date_part('epoch', now()) - (60 * 30))"""
    cur.execute(sql, (row[0],))
    if cur.rowcount > 0:
        return False
    else:
        return True


@csrf_exempt
def user_submit(request):
    if 'lng' in request.GET:
        lng = request.GET['lng']
    else:
        return HttpResponse("Please upgrade Tornado Alert!", mimetype="text/plain")

    if 'lat' in request.GET:
        lat = request.GET['lat']
    else:
        return HttpResponse("Please upgrade Tornado Alert!", mimetype="text/plain")

    registration_id = request.GET['registrationId']

    # Sanitize data ftw!
    lng = re.sub('[^0-9\-]+', '', lng)
    lat = re.sub('[^0-9\-]+', '', lat)
    registration_id = re.sub('[^A-Za-z0-9_\-]+', '', registration_id)
    if lng < 10000 or lat < 10000:
        return HttpResponse("Please upgrade Tornado Alert!", mimetype="text/plain")

    # lng and lat in postgis are decimal, not microdegrees
    lng = float(lng) / 1000000
    lat = float(lat) / 1000000

    # Make sure this is a user who didn't register in the last 30 minutes. If
    # they did, ignore the submittion - high probability it was just a test.
    recent_user = check_recent_user(registration_id)
    if recent_user:
        return HttpResponse("Marker submitted, awaiting confirmation.", mimetype="text/plain")

    # Make sure we're in a tornado alert zone
    in_zone = check_submit_in_zone(lng, lat)
    if in_zone:
        priority = 't'
        weight = 2
        msg = 'Marker submitted, awaiting confirmation.'
    else:
        priority = 'f'
        weight = 1
        msg = 'Marker submitted, awaiting confirmation.'

    cur = DB_CONN.cursor()
    # First make sure the user hasn't submitted an alert already in the last 30
    # mins.
    check_sql = """SELECT id FROM user_submits
                    WHERE registration_id = %s
                    AND create_date > (date_part('epoch', now()) - 1800)"""
    cur.execute(check_sql, (registration_id,))
    if cur.rowcount > 0:
         return HttpResponse("Another marker recently submitted", mimetype="text/plain")

    sql = """INSERT INTO user_submits (registration_id, location, priority, weight)
                VALUES (%s, makepoint(%s, %s), %s, %s)"""
    cur.execute(sql, (registration_id, lng, lat, priority, weight))

    open(STAT_FILE, 'w')
    return HttpResponse(msg, mimetype="text/plain")
