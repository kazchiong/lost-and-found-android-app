<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// connect to database
$db = new DB_CONNECT();
$db->connect();

// fetch all pending claims
// join with items and users so admin can see full details (item info + claimant info)
$sql = "SELECT c.id AS claim_id, c.item_id, c.user_id, c.message, c.status,
               i.title, i.type, i.location,
               u.name AS claimant_name, u.email AS claimant_email
        FROM claims c
        JOIN items i ON c.item_id = i.id
        JOIN users u ON c.user_id = u.id
        WHERE c.status = 'pending'
        ORDER BY c.id DESC";

$result = mysqli_query($db->myconn, $sql);

// if there are pending claims, build JSON array
if (mysqli_num_rows($result) > 0) {

    $response["claims"] = array();

    while ($row = mysqli_fetch_assoc($result)) {

        // map each database row into a claim object
        $claim = array();
        $claim["claim_id"] = $row["claim_id"];
        $claim["item_id"] = $row["item_id"];
        $claim["user_id"] = $row["user_id"];
        $claim["message"] = $row["message"];
        $claim["status"] = $row["status"];
        $claim["title"] = $row["title"];
        $claim["type"] = $row["type"];
        $claim["location"] = $row["location"];
        $claim["claimant_name"] = $row["claimant_name"];
        $claim["claimant_email"] = $row["claimant_email"];

        array_push($response["claims"], $claim);
    }

    $response["success"] = 1;

} else {
    // no pending claims found
    $response["success"] = 0;
    $response["message"] = "No pending claims";
}

// return JSON response back to Android
echo json_encode($response);

$db->close($db->myconn);
?>
