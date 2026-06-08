<?php
// this class handles database connection for all API files
// other PHP files will create a DB_CONNECT object to connect/close DB

class DB_CONNECT {

    var $myconn; // store active MySQL connection

    // connect to MySQL database using project credentials
    function connect() {

        define('DB_USER', "root");
        define('DB_PASSWORD', "");
        define('DB_DATABASE', "mdad_test");  // database used for this project
        define('DB_SERVER', "localhost");

        // establish connection
        $con = mysqli_connect(DB_SERVER, DB_USER, DB_PASSWORD, DB_DATABASE)
            or die(mysqli_error($con));

        $this->myconn = $con;
        return $this->myconn;
    }

    // close database connection after query is done
    function close($myconn) {
        mysqli_close($myconn);
    }
}
?>
