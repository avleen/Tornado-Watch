#!/bin/bash

BASE_DIR="/www/silverwraith.com/canonical/tw.silverwraith.com"

nohup ${BASE_DIR}/pull_weatherfeed.py -d >> ${BASE_DIR}/logs/pull_weatherfeed.log &
nohup ${BASE_DIR}/add_alerts.py -d >> ${BASE_DIR}/logs/add_alerts.log &
nohup ${BASE_DIR}/send_alerts.py -d >> ${BASE_DIR}/logs/send_alerts.log &
