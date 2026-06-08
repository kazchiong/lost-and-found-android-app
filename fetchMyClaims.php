<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// connect to database
$db = new DB_CONNECT();
$db->connect();

// accept user_id via GET parameter from Android
// this is used to fetch only claims submitted by this user
$user_id = isset($_GET["user_id"]) ? $_GET["user_id"] : "";

// basic validation: user_id must be provided
if ($user_id == "") {
    $response["success"] = 0;
    $response["message"] = "Missing user_id";
    echo json_encode($response);
    exit;
}

// fetch all claims made by this user
// join with items table to get item details (title, type, location, item status)
$sql = "SELECT c.id AS claim_id, c.item_id, c.message, c.status,
               i.title, i.type, i.location, i.status AS item_status
        FROM claims c
        JOIN items i ON c.item_id = i.id
        WHERE c.user_id = '$user_id'
        ORDER BY c.id DESC";

$result = mysqli_query($db->myconn, $sql);

// if claims exist, build JSON array
if (mysqli_num_rows($result) > 0) {

    $response["claims"] = array();

    while ($row = mysqli_fetch_assoc($result)) {

        // map database row into claim object
        $claim = array();
        $claim["claim_id"] = $row["claim_id"];
        $claim["item_id"] = $row["item_id"];
        $claim["message"] = $row["message"];
        $claim["status"] = $row["status"];          // pending / approved / rejected

        $claim["title"] = $row["title"];
        $claim["type"] = $row["type"];
        $claim["location"] = $row["location"];
        $claim["item_status"] = $row["item_status"]; // open / claimed / etc.

        array_push($response["claims"], $claim);
    }

    $response["success"] = 1;

} else {
    // no claims found for this user
    $response["success"] = 0;
    $response["message"] = "No claims found";
}

// return JSON response to Android
echo json_encode($response);

$db->close($db->myconn);
?>
