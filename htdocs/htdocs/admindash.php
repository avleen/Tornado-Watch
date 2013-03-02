<?php
$conn = pg_connect("dbname=tornadowatch user=postgres host=localhost port=6432");
?>
<html>
    <head>
        <title>Admin dashboard</title>
    </head>
    <body>
        Current user count: <?php
        $sql = "SELECT COUNT(create_date) FROM user_registration";
        $result = pg_query($conn, $sql);
        $count = pg_fetch_row($result);
        echo $count[0]; ?><br>
        New users today: <?php
        $sql = "SELECT COUNT(*) FROM user_registration WHERE date_trunc('day', to_timestamp(create_date)) = TIMESTAMP 'today'";
        $result = pg_query($conn, $sql);
        $count = pg_fetch_row($result);
        echo $count[0]; ?><br>
        New users yesterday: <?php
        $sql = "SELECT COUNT(*) FROM user_registration WHERE date_trunc('day', to_timestamp(create_date)) = TIMESTAMP 'yesterday'";
        $result = pg_query($conn, $sql);
        $count = pg_fetch_row($result);
        echo $count[0]; ?><br>
        <a href="/reg-bin/make_kml.sh" target="_blank">Current users map</a><br>
        <a href="/reg-bin/make_kml_today.sh" target="_blank">New users today map</a><br>
        <a href="/reg-bin/make_kml_circle.sh" target="_blank">Current users map circles</a>
        <br>
        <?php exec("/www/silverwraith.com/canonical/tw.silverwraith.com/cgi-bin/make_registrations_graph.sh"); ?>
        <img src="/html/user_registrations_daily.png"></img><br>
        <img src="/html/user_registrations_by_hour_this_week.png"></img><br>
        <img src="/html/user_registrations_by_hour.png"></img><br>
        <img src="/html/user_registrations_hourly.png"></img><br>
    </body>
</html>
