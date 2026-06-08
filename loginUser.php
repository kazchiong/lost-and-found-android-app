<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// read JSON body sent from Android (Volley POST)
$json = file_get_contents("php://input");
$data = json_decode($json, true);

// basic validation: ensure login fields exist
if (!isset($data["email"], $data["password"])) {
    $response["success"] = 0;
    $response["message"] = "Missing login data";
    echo json_encode($response);
    exit;
}

// make email consistent (same style as register)
// trim spaces and lowercase so login is not case-sensitive
$email = trim(strtolower($data["email"]));
$rawPassword = $data["password"];

$db = new DB_CONNECT();
$db->connect();

// get user record by email using prepared statement (safer than string query)
$stmt = $db->myconn->prepare(
    "SELECT id, name, email, password, role FROM users WHERE email = ? LIMIT 1"
);
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if ($result && $result->num_rows === 1) {

    // fetch user row from database
    $user = $result->fetch_assoc();

    // verify hashed password from database using password_verify
    // (so we never store or compare raw password directly)
    if (password_verify($rawPassword, $user["password"])) {

        // login success: return user details so Android can store session (user_id/name/role)
        $response["success"] = 1;
        $response["user"] = array(
            "id" => $user["id"],
            "name" => $user["name"],
            "email" => $user["email"],
            "role" => $user["role"]
        );

    } else {
        // wrong password (use same message so people can't guess which part is wrong)
        $response["success"] = 0;
        $response["message"] = "Invalid email or password";
    }

} else {
    // no matching user found
    $response["success"] = 0;
    $response["message"] = "Invalid email or password";
}

// return JSON response back to Android
echo json_encode($response);

$stmt->close();
$db->close($db->myconn);
?>
