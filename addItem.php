<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// read JSON body from Android (Volley POST)
$json = file_get_contents("php://input");
$data = json_decode($json, true);

// basic validation: ensure required item fields exist
if (!isset($data["user_id"], $data["type"], $data["title"], $data["description"], $data["location"], $data["date_reported"])) {
    $response["success"] = 0;
    $response["message"] = "Missing fields";
    echo json_encode($response);
    exit;
}

// extract values from request
$user_id = $data["user_id"];
$type = $data["type"]; // lost or found
$title = $data["title"];
$description = $data["description"];
$location = $data["location"];
$date_reported = $data["date_reported"];

// image is optional in this app (can be NULL / empty)
$image = isset($data["image"]) ? $data["image"] : NULL;

// DEBUG: check whether image was received
// $response["debug_has_image_key"] = isset($data["image"]) ? 1 : 0;
// $response["debug_image_len"] = ($image === NULL) ? -1 : strlen($image);


// new items start as open until someone successfully claims it
$status = "open";

$db = new DB_CONNECT();
$db->connect();

// if no image provided, insert without image column
// if image exists, insert it together
if ($image === NULL || $image === "") {
    $sql = "INSERT INTO items (user_id, type, title, description, location, date_reported, status)
            VALUES ('$user_id','$type','$title','$description','$location','$date_reported','$status')";
} else {
    $sql = "INSERT INTO items (user_id, type, title, description, location, date_reported, image, status)
            VALUES ('$user_id','$type','$title','$description','$location','$date_reported','$image','$status')";
}

$result = mysqli_query($db->myconn, $sql);

// return JSON response back to Android
if ($result) {
    $response["success"] = 1;
    $response["message"] = "Item added";
} else {
    $response["success"] = 0;
    $response["message"] = "DB error" . mysqli_error($db->myconn);
}

echo json_encode($response);

$db->close($db->myconn);
?>
