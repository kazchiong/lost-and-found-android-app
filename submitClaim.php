<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// read JSON body sent from Android (Volley POST)
$json = file_get_contents("php://input");
$data = json_decode($json, true);

// basic validation: make sure required fields exist
if (!isset($data["item_id"], $data["user_id"], $data["message"])) {
    $response["success"] = 0;
    $response["message"] = "Missing fields";
    echo json_encode($response);
    exit;
}

// extract values from request
$item_id = $data["item_id"];
$user_id = $data["user_id"];
$message = $data["message"];

$db = new DB_CONNECT();
$db->connect();

// block admin from making claims (admin should review, not claim)
$roleQ = mysqli_query($db->myconn, "SELECT role FROM users WHERE id='$user_id' LIMIT 1");
if ($roleQ && mysqli_num_rows($roleQ) == 1) {
    $r = mysqli_fetch_assoc($roleQ);
    if ($r["role"] == "admin") {
        $response["success"] = 0;
        $response["message"] = "Admin cannot submit claims";
        echo json_encode($response);
        $db->close($db->myconn);
        exit;
    }
}

// prevent duplicate pending claim for the same item by same user
// (so user can't spam claim button and create multiple records)
$check = mysqli_query($db->myconn,
    "SELECT id FROM claims WHERE item_id='$item_id' AND user_id='$user_id' AND status='pending' LIMIT 1"
);

if (mysqli_num_rows($check) > 0) {
    $response["success"] = 0;
    $response["message"] = "You already submitted a claim for this item";
    echo json_encode($response);
    $db->close($db->myconn);
    exit;
}

// prevent user from claiming their own item
// (claim feature is meant for other people, not the poster themselves)
$qOwner = mysqli_query($db->myconn, "SELECT user_id FROM items WHERE id='$item_id' LIMIT 1");
if ($qOwner && mysqli_num_rows($qOwner) == 1) {
    $row = mysqli_fetch_assoc($qOwner);
    $owner_id = $row["user_id"];

    if ($owner_id == $user_id) {
        $response["success"] = 0;
        $response["message"] = "You cannot claim your own item";
        echo json_encode($response);
        $db->close($db->myconn);
        exit;
    }
}

// if all checks pass, insert claim as "pending"
$sql = "INSERT INTO claims (item_id, user_id, message, status) VALUES ('$item_id', '$user_id', '$message', 'pending')";
$result = mysqli_query($db->myconn, $sql);

// build JSON response back to Android
if ($result) {
    $response["success"] = 1;
    $response["message"] = "Claim submitted";
} else {
    $response["success"] = 0;
    $response["message"] = "DB error";
}

echo json_encode($response);
$db->close($db->myconn);
?>
