#!/bin/bash

BASE_DIR="/www/silverwraith.com/canonical/tw.silverwraith.com"

nohup ${BASE_DIR}/add_alerts.py -d >> ${BASE_DIR}/logs/add_alerts.log 2>&1 &
nohup ${BASE_DIR}/send_alerts.py -d >> ${BASE_DIR}/logs/send_alerts.log 2>&1 &
