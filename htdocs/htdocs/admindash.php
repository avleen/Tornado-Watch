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
        <a href="/cgi-bin/make_kml.sh" target="_blank">Current users map</a>
        <a href="/cgi-bin/make_kml_circle.sh" target="_blank">Current users map circles</a>
        <br>
    </body>
</html>
