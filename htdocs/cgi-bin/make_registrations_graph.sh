#!/bin/bash

SQL="select count(*), date_trunc('day', to_timestamp(create_date)) as create_dated from user_registration group by create_dated order by create_dated;"
result=$( echo ${SQL} | psql -qtU postgres -d tornadowatch | awk '{ print $1, $3 }' | grep 2012 )
start=$( echo "${result}" | head -n1 | awk '{print $2}' )
end=$( echo "${result}" | tail -n1 | awk '{print $2}' )
echo "${result}" > /tmp/reg.dat
cat > /tmp/reg.plt<<EOF
set terminal png size 600,400
set xdata time
set timefmt "%Y-%m-%d"
set format x "%m/%d"
set output "/www/silverwraith.com/canonical/tw.silverwraith.com/htdocs/user_registrations_daily.png"
# time range must be in same format as data file
set xrange ["${start}":"${end}"]
#set yrange [0:50]
set grid
set xlabel "Date\\nTime"
set ylabel "Users"
set title "User registrations by day"
set key left box
plot "/tmp/reg.dat" using 2:1 index 0 title "User registrations" with lines
EOF
cat /tmp/reg.plt | gnuplot

SQL="select count(*), date_trunc('hour', to_timestamp(create_date)) as create_dated from user_registration where create_date > (date_part('epoch', now()) - (60*60*24*7)) group by create_dated order by create_dated;"
result=$( echo ${SQL} | psql -qtU postgres -d tornadowatch | awk '{ print $1, $3 "_" $4 }' | grep 2012 )
start=$( echo "${result}" | head -n1 | awk '{print $2}' )
end=$( echo "${result}" | tail -n1 | awk '{print $2}' )
echo "${result}" > /tmp/reg2.dat
cat > /tmp/reg.plt<<EOF
set terminal png size 600,400
set xdata time
set timefmt "%Y-%m-%d_%H:00:00-07"
set format x "%m/%d"
set output "/www/silverwraith.com/canonical/tw.silverwraith.com/htdocs/user_registrations_by_hour_this_week.png"
# time range must be in same format as data file
set xrange ["${start}":"${end}"]
#set yrange [0:50]
set grid
set xlabel "Date\\nTime"
set ylabel "Users"
set title "User registrations by day this week"
set key left box
plot "/tmp/reg2.dat" using 2:1 index 0 title "User registrations" with lines
EOF
cat /tmp/reg.plt | gnuplot

SQL="select count(*), date_trunc('hour', to_timestamp(create_date)) as create_dated from user_registration group by create_dated order by create_dated;"
result=$( echo ${SQL} | psql -qtU postgres -d tornadowatch | awk '{ print $1, $3 "_" $4 }' | grep 2012 )
start=$( echo "${result}" | head -n1 | awk '{print $2}' )
end=$( echo "${result}" | tail -n1 | awk '{print $2}' )
echo "${result}" > /tmp/reg2.dat
cat > /tmp/reg.plt<<EOF
set terminal png size 600,400
set xdata time
set timefmt "%Y-%m-%d_%H:00:00-07"
set format x "%m/%d"
set output "/www/silverwraith.com/canonical/tw.silverwraith.com/htdocs/user_registrations_by_hour.png"
# time range must be in same format as data file
set xrange ["${start}":"${end}"]
#set yrange [0:50]
set grid
set xlabel "Date\\nTime"
set ylabel "Users"
set title "User registrations by day"
set key left box
plot "/tmp/reg2.dat" using 2:1 index 0 title "User registrations" with lines
EOF
cat /tmp/reg.plt | gnuplot

SQL="select count(*), date_part('hour', to_timestamp(create_date)) as create_dated from user_registration group by create_dated order by create_dated;"
result=$( echo ${SQL} | psql -qtU postgres -d tornadowatch | awk '{ print $1, $3 }' | grep "[0-9]" )
echo "${result}" > /tmp/reg.dat
cat > /tmp/reg.plt<<EOF
set terminal png size 600,400
set xdata time
set timefmt "%H"
set format x "%H"
set output "/www/silverwraith.com/canonical/tw.silverwraith.com/htdocs/user_registrations_hourly.png"
# time range must be in same format as data file
set xrange ["0":"23"]
#set yrange [0:50]
set grid
set xlabel "Hour of day"
set ylabel "Users"
set title "User registrations by hour of day"
set key left box
plot "/tmp/reg.dat" using 2:1 index 0 title "User registrations" with lines
EOF
cat /tmp/reg.plt | gnuplot
