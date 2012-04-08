<?php
$conn = pg_connect("dbname=tornadowatch user=postgres");
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
        echo $count[0]; ?>
        <br>
        <a href="/cgi-bin/make_kml.sh" target="_blank">Current users map</a><br>
        <a href="/cgi-bin/make_kml_circle.sh" target="_blank">Current users map circles</a>
        <br>
        <?php exec("/www/silverwraith.com/canonical/tw.silverwraith.com/cgi-bin/make_registrations_graph.sh"); ?>
        <img src="/user_registrations_daily.png"></img><br>
        <img src="/user_registrations_hourly.png"></img><br>
    </body>
</html>
