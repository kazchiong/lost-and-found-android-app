<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// connect to database
$db = new DB_CONNECT();
$db->connect();

// accept item_id and user_id via GET
// Android uses this to check if claim button should be allowed
$item_id = isset($_GET["item_id"]) ? $_GET["item_id"] : "";
$user_id = isset($_GET["user_id"]) ? $_GET["user_id"] : "";

// basic validation: both values are needed to do checking
if ($item_id == "" || $user_id == "") {
    $response["success"] = 0;
    $response["message"] = "Missing fields";
    echo json_encode($response);
    exit;
}

// check item status first
// if item is not open, user shouldn't be able to claim anymore
$q1 = mysqli_query($db->myconn, "SELECT status FROM items WHERE id='$item_id' LIMIT 1");
if ($q1 && mysqli_num_rows($q1) == 1) {
    $row = mysqli_fetch_assoc($q1);
    if ($row["status"] != "open") {
        $response["success"] = 1;
        $response["allowed"] = 0;
        $response["reason"] = "Item is already claimed";
        echo json_encode($response);
        $db->close($db->myconn);
        exit;
    }
}

// check if the same user already has a pending claim for the same item
// this prevents duplicates / spam claims
$q2 = mysqli_query($db->myconn,
    "SELECT id FROM claims WHERE item_id='$item_id' AND user_id='$user_id' AND status='pending' LIMIT 1"
);

if ($q2 && mysqli_num_rows($q2) > 0) {
    // not allowed because user already submitted pending claim
    $response["success"] = 1;
    $response["allowed"] = 0;
    $response["reason"] = "You already submitted a pending claim";
} else {
    // allowed if item is open and no pending claim exists
    $response["success"] = 1;
    $response["allowed"] = 1;
    $response["reason"] = "";
}

// return JSON result so Android can decide to enable/disable claim button
echo json_encode($response);

$db->close($db->myconn);
?>
