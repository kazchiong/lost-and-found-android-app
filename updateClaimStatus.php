<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// read JSON body sent from Android (Volley POST)
$json = file_get_contents("php://input");
$data = json_decode($json, true);

// basic validation: ensure Android sent the required fields
if (!isset($data["claim_id"], $data["status"], $data["admin_id"])) {
    $response["success"] = 0;
    $response["message"] = "Missing fields";
    echo json_encode($response);
    exit;
}

// extract values from request
$claim_id = $data["claim_id"];
$status = $data["status"]; // approved or rejected
$admin_id = $data["admin_id"];

// extra validation: only allow these 2 statuses (prevents random values)
if ($status !== "approved" && $status !== "rejected") {
    $response["success"] = 0;
    $response["message"] = "Invalid status";
    echo json_encode($response);
    exit;
}

$db = new DB_CONNECT();
$db->connect();

// update claim status + store which admin approved/rejected it
$sql = "UPDATE claims SET status='$status', admin_id='$admin_id' WHERE id='$claim_id'";
$result = mysqli_query($db->myconn, $sql);

// if update fails, stop here and return error to Android
if (!$result) {
    $response["success"] = 0;
    $response["message"] = "DB error updating claim";
    echo json_encode($response);
    $db->close($db->myconn);
    exit;
}

// if approved, do extra backend logic so database stays consistent
// 1) mark item as claimed
// 2) reject all other pending claims for the same item
if ($status === "approved") {

    // find the item_id linked to this claim
    $q = mysqli_query($db->myconn, "SELECT item_id FROM claims WHERE id='$claim_id' LIMIT 1");

    if ($q && mysqli_num_rows($q) == 1) {
        $row = mysqli_fetch_assoc($q);
        $item_id = $row["item_id"];

        // once approved, item is no longer available, so mark it claimed
        mysqli_query($db->myconn, "UPDATE items SET status='claimed' WHERE id='$item_id'");

        // reject other pending claims for the same item
        // (so only ONE person can successfully claim it)
        mysqli_query($db->myconn,
            "UPDATE claims 
             SET status='rejected' 
             WHERE item_id='$item_id' 
             AND status='pending' 
             AND id != '$claim_id'"
        );
    }
}

// final response back to Android
$response["success"] = 1;
$response["message"] = "Claim updated";
echo json_encode($response);

$db->close($db->myconn);
?>
