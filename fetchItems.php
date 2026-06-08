<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// connect to database
$db = new DB_CONNECT();
$db->connect();

// fetch all items (latest first)
$sql = "SELECT * FROM items ORDER BY id DESC";
$result = mysqli_query($db->myconn, $sql);

// if items exist, build JSON array
if (mysqli_num_rows($result) > 0) {

    $response["items"] = array();

    while ($row = mysqli_fetch_assoc($result)) {

        // map each database row into an item object
        $item = array();
        $item["id"] = $row["id"];
        $item["title"] = $row["title"];
        $item["description"] = $row["description"];
        $item["location"] = $row["location"];
        $item["type"] = $row["type"];          // lost / found
        $item["status"] = $row["status"];      // open / claimed
        $item["date"] = $row["date_reported"];
        $item["image"] = ($row["image"] == NULL) ? "" : $row["image"];
        $item["user_id"] = $row["user_id"];

        array_push($response["items"], $item);
    }

    $response["success"] = 1;

} else {
    // no items found in database
    $response["success"] = 0;
    $response["message"] = "No items found";
}

// return JSON response to Android
echo json_encode($response);

$db->close($db->myconn);
?>
