<?php
require_once __DIR__ . '/db_connect.php';

$response = array();

// Receive JSON from Android
$json = file_get_contents("php://input");
$data = json_decode($json, true);

if (!isset($data["name"], $data["email"], $data["password"])) {
    $response["success"] = 0;
    $response["message"] = "Missing required fields";
    echo json_encode($response);
    exit;
}

$name = $data["name"];
$email = $data["email"];
$role = "user";


// only allow official school email domain!!!!!!!!!!
$email = trim(strtolower($data["email"]));

// Staff: firstname_lastname@tp.edu.sg
$staffPattern = '/^[a-z]+_[a-z]+@tp\.edu\.sg$/i';

// Student: 7 digits + 1 letter @student.tp.edu.sg
// e.g. XX12345A@student.tp.edu.sg
$studentPattern = '/^\d{7}[a-z]@student\.tp\.edu\.sg$/i';

if (!preg_match($staffPattern, $email) && !preg_match($studentPattern, $email)) {
    $response["success"] = 0;
    $response["message"] = "Use a valid TP email"; // firstname_lastname@tp.edu.sg or matric@student.tp.edu.sg
    echo json_encode($response);
    exit;
}


$rawPassword = $data["password"];
// Password rules: >=8 chars, at least 1 uppercase, 1 lowercase, 1 number
if (
    strlen($rawPassword) < 8 ||
    !preg_match('/[A-Z]/', $rawPassword) ||
    !preg_match('/[a-z]/', $rawPassword) ||
    !preg_match('/[0-9]/', $rawPassword)
) {
    $response["success"] = 0;
    $response["message"] = "Password must be at least 8 characters and include uppercase, lowercase, and a number";
    echo json_encode($response);
    exit;
}

// Hash only after it passes validation
$password = password_hash($rawPassword, PASSWORD_DEFAULT);


$db = new DB_CONNECT();
$db->connect();


// Check if email exists (secure)
$stmt = $db->myconn->prepare("SELECT id FROM users WHERE email = ?");
$stmt->bind_param("s", $email);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows > 0) {
    $response["success"] = 0;
    $response["message"] = "Email already registered";
    echo json_encode($response);
    $stmt->close();
    $db->close($db->myconn);
    exit;
}
$stmt->close();

// Insert user (secure)
$stmt = $db->myconn->prepare(
    "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)"
);
$stmt->bind_param("ssss", $name, $email, $password, $role);
$result = $stmt->execute();
$stmt->close();


if ($result) {
    $response["success"] = 1;
    $response["message"] = "User registered successfully";
} else {
    $response["success"] = 0;
    $response["message"] = "Database error";
}

echo json_encode($response);
$db->close($db->myconn);
?>
