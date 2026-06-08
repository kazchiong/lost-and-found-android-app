package mdad.networkdata.lostfoundapp;

public class ApiConfig {
    public static final String BASE_URL = "http://10.0.2.2/lostfound_api/";

    public static final String REGISTER = BASE_URL + "registerUser.php";
    public static final String LOGIN = BASE_URL + "loginUser.php";
    public static final String FETCH_ITEMS = BASE_URL + "fetchItems.php";
    public static final String ADD_ITEM = BASE_URL + "addItem.php";
    public static final String SUBMIT_CLAIM = BASE_URL + "submitClaim.php";
    public static final String FETCH_PENDING_CLAIMS = BASE_URL + "fetchPendingClaims.php";
    public static final String UPDATE_CLAIM_STATUS = BASE_URL + "updateClaimStatus.php";
    public static final String FETCH_MY_CLAIMS = BASE_URL + "fetchMyClaims.php";
    public static final String CHECK_CLAIM_ALLOWED = BASE_URL + "checkClaimAllowed.php";
    public static final String FETCH_MY_ITEMS = BASE_URL + "fetchMyItems.php";

}
