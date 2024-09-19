package com.bookit.step_definitions;


import com.bookit.pages.SelfPage;
import com.bookit.utilities.BookitUtils;
import com.bookit.utilities.ConfigurationReader;
import com.bookit.utilities.DB_Util;
import com.bookit.utilities.Environment;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apiguardian.api.API;
import org.junit.Assert;

import java.util.Map;

public class ApiStepDefs {
    String token;
    Response response;
    String emailGlobal;
    String expectedTeamId;

    @Given("I logged Bookit api as a {string}")
    public void i_logged_bookit_api_as_a(String role) {
        token = BookitUtils.generateTokenByRole(role);
        System.out.println("token = " + token);

        Map<String, String> credentialMap = BookitUtils.returnCredentials(role);
        emailGlobal = credentialMap.get("email");

    }

    @When("I sent get request to {string} endpoint")
    public void i_sent_get_request_to_endpoint(String endpoint) {
        response = RestAssured.given()
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .when().get(Environment.BASE_URL + endpoint);


    }

    @Then("status code should be {int}")
    public void status_code_should_be(int expectedStatusCode) {


        System.out.println("response.statusCode() = " + response.statusCode());
        // verify status code
        Assert.assertEquals(expectedStatusCode, response.statusCode());

    }

    @Then("content type is {string}")
    public void content_type_is(String expectedContentType) {

        System.out.println("response.contentType() = " + response.contentType());
        Assert.assertEquals(expectedContentType, response.contentType());
    }

    @Then("role is {string}")
    public void role_is(String expectedRole) {
        //response.prettyPeek();
        String actualRole = response.path("role");
        Assert.assertEquals(expectedRole, actualRole);

    }

    @Then("the information about current user from api and database should match")
    public void the_information_about_current_user_from_api_and_database_should_match() {
        //Get data from api
        JsonPath jsonPath = response.jsonPath();
        String actualFirstName = jsonPath.getString("firstName");
        String actualLastName = jsonPath.getString("lastName");
        String actualRole = jsonPath.getString("role");


        //  get data from database
        // first we need to create connection which will hande by custom hooks
        String query = "select firstname,lastname,role\n" +
                "from users\n" +
                "where email = '" + emailGlobal + "'";
        // run the query to get result
        DB_Util.runQuery(query);

        //get the result to Map
        Map<String, String> dbMap = DB_Util.getRowMap(1);
        System.out.println("bdMap = " + dbMap);
        String expectedFistName = dbMap.get("firstname");
        String expectedLastName = dbMap.get("lastname");
        String expectedRole = dbMap.get("role");

        // compare API VS DB

        Assert.assertEquals(expectedFistName, actualFirstName);
        Assert.assertEquals(expectedLastName, actualLastName);
        Assert.assertEquals(expectedRole, actualRole);


    }

    @Then("UI,API and Database user information must be match")
    public void ui_api_and_database_user_information_must_be_match() {

        //Get data from api
        JsonPath jsonPath = response.jsonPath();
        String actualFirstName = jsonPath.getString("firstName");
        String actualLastName = jsonPath.getString("lastName");
        String actualRole = jsonPath.getString("role");


        //  get data from database
        // first we need to create connection which will hande by custom hooks
        String query = "select firstname,lastname,role\n" +
                "from users\n" +
                "where email = '" + emailGlobal + "'";
        // run the query to get result
        DB_Util.runQuery(query);

        //get the result to Map
        Map<String, String> dbMap = DB_Util.getRowMap(1);
        System.out.println("bdMap = " + dbMap);
        String expectedFistName = dbMap.get("firstname");
        String expectedLastName = dbMap.get("lastname");
        String expectedRole = dbMap.get("role");

        // compare API VS DB

        Assert.assertEquals(expectedFistName, actualFirstName);
        Assert.assertEquals(expectedLastName, actualLastName);
        Assert.assertEquals(expectedRole, actualRole);

        //Get Data from UI
        SelfPage selfPage = new SelfPage();

        String actualFullNameUI = selfPage.name.getText();
        String actualRoleUI = selfPage.role.getText();

        // UI vs DB
        String expectedFullName = expectedFistName + " " + expectedLastName;
        Assert.assertEquals(expectedFullName, actualFullNameUI);
        Assert.assertEquals(expectedRole, actualRoleUI);

        // UI vs API
        String expectedNameFromAPI = actualFirstName + " " + actualLastName;
        Assert.assertEquals(expectedNameFromAPI, actualFullNameUI);
        Assert.assertEquals(expectedRole, actualRoleUI);

    }
// ADDING A NEW STUDENT AND DELETING IT

    @When("I send POST request {string} endpoint with following information")
    public void i_send_post_request_endpoint_with_following_information(String endpoint, Map<String,String> studentInformation) {
        response = RestAssured.given()
                .accept(ContentType.JSON)
                .queryParams(studentInformation)
                .header("Authorization", token)

                .when().post(Environment.BASE_URL + endpoint).prettyPeek();
      //  .then().extract().response();




    }
    @Then("I delete previously added student")
    public void i_delete_previously_added_student() {
// we need to get the entryId from the post request and send delete request to it.

int idToDelete = response.path("entryId");
        System.out.println("idToDelete = " + idToDelete);
//Send DELETE request to idToDelete path parameters
        RestAssured.given()
                .headers("Authorization",token)
                .pathParam("id",idToDelete)
                .when().delete(Environment.BASE_URL +"/api/students/{id}").then().statusCode(204);

    }


// POST NEW TEAM

    Map<String,String> expectedMap;
    @When("Users sends POST request to {string} with following info:")
    public void users_sends_post_request_to_with_following_info(String endpoint, Map<String,String> dataMap) {


        response=RestAssured.given().log().all()
                .queryParams(dataMap)
                .header("Authorization",token)
                .when().post(Environment.BASE_URL+endpoint);

        System.out.println(Environment.BASE_URL+endpoint);

        expectedMap=dataMap;


    }
    int newTeamID;
    @Then("Database should persist same team info")
    public void database_should_persist_same_team_info() {

        newTeamID = response.path("entryiId");
        System.out.println("------> NEW TEAM is CREATED "+newTeamID);

        String query="select location,batch_number,name " +
                "from team t inner join campus c on t.campus_id=c.id " +
                "where t.id="+newTeamID;

        DB_Util.runQuery(query);

        Map<String, String> actualMap = DB_Util.getRowMap(1);
        System.out.println("actualMap = " + actualMap);


        // Assertions
        Assert.assertEquals(expectedMap.get("campus-location"),actualMap.get("location"));
        Assert.assertEquals(expectedMap.get("batch-number"),actualMap.get("batch_number"));
        Assert.assertEquals(expectedMap.get("team-name"),actualMap.get("name"));




    }
    //Deleting the created team
    @Then("User deletes previously created team")
    public void user_deletes_previously_created_team() {

        RestAssured.given().accept(ContentType.JSON)
                .header("Authorization",token)
                .pathParam("id",newTeamID)
                .when().delete(Environment.BASE_URL+"/api/teams/{id}")
                .then().statusCode(200);

        System.out.println("------> NEW TEAM is DELETED "+newTeamID);


//   # POST Team
//    # Store teamID info
//    # Get same tem info from database with using ID from API response
//       # To get related information you need use JOINS
//    # Delete team that we generate
//
//
//    # Which one is EXPECTED ?  Since we add data from API, DB needs to show this info as we provide
//    # EXPECTED --> API
//    # ACTUAL   --> DB
//
    }

}
