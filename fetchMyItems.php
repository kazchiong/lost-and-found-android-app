<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// connect to database
$db = new DB_CONNECT();
$db->connect();

// get user_id from GET parameter (used to fetch only this user's items)
$user_id = isset($_GET["user_id"]) ? $_GET["user_id"] : "";

// basic validation: user_id must be provided
if ($user_id == "") {
    $response["success"] = 0;
    $response["message"] = "Missing user_id";
    echo json_encode($response);
    exit;
}

// fetch items posted by this specific user
$sql = "SELECT id, user_id, type, title, description, location, date_reported, image, status
        FROM items
        WHERE user_id = '$user_id'
        ORDER BY id DESC";

$result = mysqli_query($db->myconn, $sql);

// if items exist, build JSON array
if (mysqli_num_rows($result) > 0) {

    $response["items"] = array();

    while ($row = mysqli_fetch_assoc($result)) {

        // map each database row into an item object
        $item = array();
        $item["id"] = $row["id"];
        $item["user_id"] = $row["user_id"];
        $item["type"] = $row["type"];              // lost / found
        $item["title"] = $row["title"];
        $item["description"] = $row["description"];
        $item["location"] = $row["location"];
        $item["date_reported"] = $row["date_reported"];
        $item["image"] = $row["image"];
        $item["status"] = $row["status"];          // open / claimed

        array_push($response["items"], $item);
    }

    $response["success"] = 1;

} else {
    // no items found for this user
    $response["success"] = 0;
    $response["message"] = "No items found";
}

// return JSON response back to Android
echo json_encode($response);

$db->close($db->myconn);
?>
