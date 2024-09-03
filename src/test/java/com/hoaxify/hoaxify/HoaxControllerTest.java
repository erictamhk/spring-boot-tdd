package com.hoaxify.hoaxify;

import com.hoaxify.hoaxify.error.ApiError;
import com.hoaxify.hoaxify.hoax.Hoax;
import com.hoaxify.hoaxify.hoax.HoaxRepository;
import com.hoaxify.hoaxify.hoax.HoaxService;
import com.hoaxify.hoaxify.hoax.vm.HoaxVM;
import com.hoaxify.hoaxify.user.User;
import com.hoaxify.hoaxify.user.UserRepository;
import com.hoaxify.hoaxify.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.test.context.ActiveProfiles;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HoaxControllerTest {
    public static final String API_1_0_HOAXES = "/api/1.0/hoaxes";
    @Autowired
    TestRestTemplate testRestTemplate;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Autowired
    HoaxRepository hoaxRepository;

    @Autowired
    HoaxService hoaxService;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    private void authenticate(String username) {
        testRestTemplate
                .getRestTemplate()
                .getInterceptors()
                .add(new BasicAuthenticationInterceptor(username, "P4ssword"));
    }

    @BeforeEach
    public void cleanup() {
        hoaxRepository.deleteAll();
        userRepository.deleteAll();
        testRestTemplate.getRestTemplate().getInterceptors().clear();
    }

    @AfterEach
    public void cleanupAfter() {
        hoaxRepository.deleteAll();
    }

    private <T> ResponseEntity<T> postHoax(Hoax hoax, Class<T> responseType) {
        return testRestTemplate.postForEntity(API_1_0_HOAXES, hoax, responseType);
    }

    private <T> ResponseEntity<T> getHoaxes(ParameterizedTypeReference<T> responseType) {
        return testRestTemplate.exchange(API_1_0_HOAXES, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getHoaxesOfUser(String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getOldHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getOldHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=before&page=0&size=5&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxes(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxesOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&sort=id,desc";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxesCount(long hoaxId, ParameterizedTypeReference<T> responseType) {
        String path = API_1_0_HOAXES + "/" + hoaxId + "?direction=after&count=true";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    private <T> ResponseEntity<T> getNewHoaxesCountOfUser(long hoaxId, String username, ParameterizedTypeReference<T> responseType) {
        String path = "/api/1.0/users/" + username + "/hoaxes/" + hoaxId + "?direction=after&count=true";
        return testRestTemplate.exchange(path, HttpMethod.GET, null, responseType);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveOk() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = TestUtil.createValidHoax();
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsUnauthorized_receiveUnauthorized() {
        Hoax hoax = TestUtil.createValidHoax();
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsUnauthorized_receiveApiError() {
        Hoax hoax = TestUtil.createValidHoax();
        ResponseEntity<ApiError> response = postHoax(hoax, ApiError.class);
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSaveToDatabase() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = TestUtil.createValidHoax();
        postHoax(hoax, Object.class);
        assertThat(hoaxRepository.count()).isEqualTo(1);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSaveToDatabaseWithTimestamp() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = TestUtil.createValidHoax();
        postHoax(hoax, Object.class);

        Hoax inDB = hoaxRepository.findAll().get(0);

        assertThat(inDB.getTimestamp()).isNotNull();
    }

    @Test
    public void postHoax_whenHoaxContentNullAndUserIsAuthorized_receiveBadRequest() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = new Hoax();
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postHoax_whenHoaxContentLessThen10CharactersAndUserIsAuthorized_receiveBadRequest() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = new Hoax();
        hoax.setContent("123456789");
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postHoax_whenHoaxContentIs5000CharactersAndUserIsAuthorized_receiveOk() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = new Hoax();
        String veryLongString = IntStream.rangeClosed(1, 5000).mapToObj(x -> "x").collect(Collectors.joining());
        hoax.setContent(veryLongString);
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void postHoax_whenHoaxContentMoreThan5000CharactersAndUserIsAuthorized_receiveBadRequest() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = new Hoax();
        String veryLongString = IntStream.rangeClosed(1, 5001).mapToObj(x -> "x").collect(Collectors.joining());
        hoax.setContent(veryLongString);
        ResponseEntity<Object> response = postHoax(hoax, Object.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    public void postHoax_whenHoaxContentNullAndUserIsAuthorized_receiveApiErrorWithValidationErrors() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = new Hoax();
        ResponseEntity<ApiError> response = postHoax(hoax, ApiError.class);
        Map<String, String> validationErrors = response.getBody().getValidationErrors();
        assertThat(validationErrors.get("content")).isNotNull();
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxSaveToDatabaseWithAuthenticatedUserInfo() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = TestUtil.createValidHoax();
        postHoax(hoax, Object.class);

        Hoax inDB = hoaxRepository.findAll().get(0);

        assertThat(inDB.getUser().getUsername()).isEqualTo(username);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_hoaxCanBeAccessedFromUserEntity() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = TestUtil.createValidHoax();
        postHoax(hoax, Object.class);

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        User userInDB = entityManager.find(User.class, user.getId());
        assertThat(userInDB.getHoaxes().size()).isEqualTo(1);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receiveOk() {
        ResponseEntity<Object> response = getHoaxes(new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithZeroItems() {
        ResponseEntity<TestPage<Object>> response = getHoaxes(new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithItem() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        ResponseEntity<TestPage<Object>> response = getHoaxes(new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(4);
    }

    @Test
    public void getHoaxes_whenThereAreNoHoaxes_receivePageWithHoaxVM() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        hoaxService.save(user, TestUtil.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxes(new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        HoaxVM storedHoax = response.getBody().getContent().get(0);
        assertThat(storedHoax.getUser().getUsername()).isEqualTo(username);
    }

    @Test
    public void postHoax_whenHoaxIsValidAndUserIsAuthorized_receiveHoaxVM() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        authenticate(username);
        Hoax hoax = TestUtil.createValidHoax();
        ResponseEntity<HoaxVM> response = postHoax(hoax, HoaxVM.class);
        assertThat(response.getBody().getUser().getUsername()).isEqualTo(username);
    }

    @Test
    public void getHoaxesOfUser_whenUserExists_receiveOk() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        ResponseEntity<Object> response = getHoaxesOfUser(username, new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getHoaxesOfUser_whenUserDoesNotExists_receiveNotFound() {
        ResponseEntity<Object> response = getHoaxesOfUser("unknown-user", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getHoaxesOfUser_whenUserExists_receivePageWithZeroHoaxes() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser(username, new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getHoaxesOfUser_whenUserExistsWithHoax_receivePageWithHoaxWM() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(user, TestUtil.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser(username, new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        HoaxVM storedHoax = response.getBody().getContent().get(0);
        assertThat(storedHoax.getUser().getUsername()).isEqualTo(username);
    }

    @Test
    public void getHoaxesOfUser_whenUserExistsWithMultipleHoaxes_receivePageWithMatchingHoaxesCount() {
        String username = "user1";
        User userWithFourHoaxes = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser(username, new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(4);
    }

    @Test
    public void getHoaxesOfUser_whenMultipleUserExistsWithMultipleHoaxes_receivePageWithMatchingHoaxesCount() {
        User userWithThreeHoaxes = userService.save(TestUtil.createValidUser("user1"));
        IntStream.rangeClosed(1, 3).forEach(i -> {
            hoaxService.save(userWithThreeHoaxes, TestUtil.createValidHoax());
        });
        User userWithFiveHoaxes = userService.save(TestUtil.createValidUser("user2"));
        IntStream.rangeClosed(1, 5).forEach(i -> {
            hoaxService.save(userWithFiveHoaxes, TestUtil.createValidHoax());
        });

        ResponseEntity<TestPage<HoaxVM>> response = getHoaxesOfUser(userWithFiveHoaxes.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(5);
    }

    @Test
    public void getOldHoaxes_WhenThereAreNoHoaxes_receiveOk() {
        ResponseEntity<Object> response = getOldHoaxes(5, new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOldHoaxes_WhenThereAreHoaxes_receivePageWithItemsBeforeProvidedId() {
        String username = "user1";
        User userWithFourHoaxes = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        ResponseEntity<TestPage<Object>> response = getOldHoaxes(fourth.getId(), new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getOldHoaxes_WhenThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxes(fourth.getId(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(response.getBody().getContent().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getOldHoaxesOfUser_WhenUserExistAndThereAreNoHoaxes_receiveOk() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        ResponseEntity<Object> response = getOldHoaxesOfUser(5, username, new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getOldHoaxesOfUser_WhenUserExistAndThereAreHoaxes_receivePageWithItemsBeforeProvidedId() {
        String username = "user1";
        User userWithFourHoaxes = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        ResponseEntity<TestPage<Object>> response = getOldHoaxesOfUser(fourth.getId(), username, new ParameterizedTypeReference<TestPage<Object>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(3);
    }

    @Test
    public void getOldHoaxesOfUser_WhenUserExistAndThereAreHoaxes_receivePageWithHoaxVMBeforeProvidedId() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxesOfUser(fourth.getId(), username, new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(response.getBody().getContent().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getOldHoaxesOfUser_WhenUserDoesNotExistAndThereAreNoHoaxes_receiveNotFound() {
        ResponseEntity<Object> response = getOldHoaxesOfUser(5, "unknown-user", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getOldHoaxesOfUser_WhenUserExistAndThereAreNoHoaxes_receivePageWithZeroItemsBeforeProvidedId() {
        User user1 = userService.save(TestUtil.createValidUser("user1"));
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());

        User user2 = userService.save(TestUtil.createValidUser("user2"));

        ResponseEntity<TestPage<HoaxVM>> response = getOldHoaxesOfUser(fourth.getId(), user2.getUsername(), new ParameterizedTypeReference<TestPage<HoaxVM>>() {
        });
        assertThat(response.getBody().getTotalElements()).isEqualTo(0);
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfItemsAfterProvidedId() {
        User user1 = userService.save(TestUtil.createValidUser("user1"));
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());

        ResponseEntity<List<Object>> response = getNewHoaxes(fourth.getId(), new ParameterizedTypeReference<List<Object>>() {
        });
        assertThat(response.getBody().size()).isEqualTo(1);
    }

    @Test
    public void getNewHoaxes_whenThereAreHoaxes_receiveListOfHoaxVMAfterProvidedId() {
        User user1 = userService.save(TestUtil.createValidUser("user1"));
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());

        ResponseEntity<List<HoaxVM>> response = getNewHoaxes(fourth.getId(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(response.getBody().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getNewHoaxesOfUser_WhenUserExistAndThereAreNoHoaxes_receiveOk() {
        String username = "user1";
        userService.save(TestUtil.createValidUser(username));
        ResponseEntity<Object> response = getNewHoaxesOfUser(5, username, new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void getNewHoaxesOfUser_WhenUserExistAndThereAreHoaxes_receiveListWithItemsAfterProvidedId() {
        String username = "user1";
        User userWithFourHoaxes = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        hoaxService.save(userWithFourHoaxes, TestUtil.createValidHoax());
        ResponseEntity<List<Object>> response = getNewHoaxesOfUser(fourth.getId(), username, new ParameterizedTypeReference<List<Object>>() {
        });
        assertThat(response.getBody().size()).isEqualTo(1);
    }

    @Test
    public void getNewHoaxesOfUser_WhenUserExistAndThereAreHoaxes_receivePageWithHoaxVMAfterProvidedId() {
        String username = "user1";
        User user = userService.save(TestUtil.createValidUser(username));
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user, TestUtil.createValidHoax());
        hoaxService.save(user, TestUtil.createValidHoax());
        ResponseEntity<List<HoaxVM>> response = getNewHoaxesOfUser(fourth.getId(), username, new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(response.getBody().get(0).getDate()).isGreaterThan(0);
    }

    @Test
    public void getNewHoaxesOfUser_WhenUserDoesNotExistAndThereAreNoHoaxes_receiveNotFound() {
        ResponseEntity<Object> response = getNewHoaxesOfUser(5, "unknown-user", new ParameterizedTypeReference<Object>() {
        });
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    public void getNewHoaxesOfUser_WhenUserExistAndThereAreNoHoaxes_receivePageWithZeroItemsAfterProvidedId() {
        User user1 = userService.save(TestUtil.createValidUser("user1"));
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());

        User user2 = userService.save(TestUtil.createValidUser("user2"));

        ResponseEntity<List<HoaxVM>> response = getNewHoaxesOfUser(fourth.getId(), user2.getUsername(), new ParameterizedTypeReference<List<HoaxVM>>() {
        });
        assertThat(response.getBody().size()).isEqualTo(0);
    }

    @Test
    public void getNewHoaxesCount_whenThereAreHoaxes_receiveCountAfterProvidedId() {
        User user1 = userService.save(TestUtil.createValidUser("user1"));
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());

        ResponseEntity<Map<String, Long>> response = getNewHoaxesCount(fourth.getId(), new ParameterizedTypeReference<Map<String, Long>>() {
        });
        assertThat(response.getBody().get("count")).isEqualTo(1);
    }

    @Test
    public void getNewHoaxesCountOfUser_whenThereAreHoaxes_receiveCountAfterProvidedId() {
        User user1 = userService.save(TestUtil.createValidUser("user1"));
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());
        Hoax fourth = hoaxService.save(user1, TestUtil.createValidHoax());
        hoaxService.save(user1, TestUtil.createValidHoax());

        ResponseEntity<Map<String, Long>> response = getNewHoaxesCountOfUser(fourth.getId(), user1.getUsername(), new ParameterizedTypeReference<Map<String, Long>>() {
        });
        assertThat(response.getBody().get("count")).isEqualTo(1);
    }

}
