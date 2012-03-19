#!/bin/bash

kill $( cat /tmp/add_alerts.pid )
kill $( cat /tmp/send_alerts.pid )
