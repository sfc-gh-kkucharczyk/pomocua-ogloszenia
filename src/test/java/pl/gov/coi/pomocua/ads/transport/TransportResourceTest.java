package pl.gov.coi.pomocua.ads.transport;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import pl.gov.coi.pomocua.ads.BaseResourceTest;
import pl.gov.coi.pomocua.ads.Location;
import pl.gov.coi.pomocua.ads.Offers;
import pl.gov.coi.pomocua.ads.UserId;
import pl.gov.coi.pomocua.ads.accomodations.AccommodationsTestDataGenerator;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.gov.coi.pomocua.ads.transport.TransportTestDataGenerator.aTransportOffer;

class TransportResourceTest extends BaseResourceTest<TransportOffer> {

    @Autowired
    private TransportOfferRepository repository;

    @Override
    protected Class<TransportOffer> getClazz() {
        return TransportOffer.class;
    }

    @Override
    protected String getOfferSuffix() {
        return "transport";
    }

    @Override
    protected TransportOffer sampleOfferRequest() {
        return aTransportOffer().build();
    }

    @Override
    protected CrudRepository<TransportOffer, Long> getRepository() {
        return repository;
    }

    @Nested
    class Searching {
        @Test
        void shouldFindByOrigin() {
            TransportOffer transportOffer1 = postOffer(aTransportOffer()
                    .origin(new Location("mazowieckie", "warszawa"))
                    .build());
            postOffer(aTransportOffer()
                    .origin(new Location("Pomorskie", "Wejherowo"))
                    .build());
            postOffer(aTransportOffer()
                    .origin(new Location("Wielkopolskie", "Warszawa"))
                    .build());

            TransportOfferSearchCriteria searchCriteria = new TransportOfferSearchCriteria();
            searchCriteria.setOrigin(new Location("Mazowieckie", "Warszawa"));
            var results = searchOffers(searchCriteria);

            assertThat(results).hasSize(1);
            assertThat(results).first().isEqualTo(transportOffer1);
        }

        @Test
        void shouldFindByDestination() {
            TransportOffer transportOffer1 = postOffer(aTransportOffer()
                    .destination(new Location("pomorskie", "GdyniA"))
                    .build());
            postOffer(aTransportOffer()
                    .destination(new Location("Pomorskie", "Wejherowo"))
                    .build());

            TransportOfferSearchCriteria searchCriteria = new TransportOfferSearchCriteria();
            searchCriteria.setDestination(new Location("Pomorskie", "Gdynia"));
            var results = searchOffers(searchCriteria);

            assertThat(results).hasSize(1);
            assertThat(results).first().isEqualTo(transportOffer1);
        }

        @Test
        void shouldFindByCapacity() {
            TransportOffer transportOffer1 = postOffer(aTransportOffer()
                    .capacity(10)
                    .build());
            TransportOffer transportOffer2 = postOffer(aTransportOffer()
                    .capacity(11)
                    .build());
            postOffer(aTransportOffer()
                    .capacity(1)
                    .build());

            TransportOfferSearchCriteria searchCriteria = new TransportOfferSearchCriteria();
            searchCriteria.setCapacity(10);
            var results = searchOffers(searchCriteria);
            assertThat(results).containsExactly(transportOffer1,transportOffer2);
        }

        @Test
        void shouldFindByTransportDate() {
            TransportOffer transportOffer1 = postOffer(aTransportOffer()
                    .transportDate(LocalDate.of(2022, 3, 21))
                    .build());
            postOffer(aTransportOffer()
                    .transportDate(LocalDate.of(2022, 3, 22))
                    .build());

            TransportOfferSearchCriteria searchCriteria = new TransportOfferSearchCriteria();
            searchCriteria.setTransportDate(LocalDate.of(2022, 3, 21));
            var results = searchOffers(searchCriteria);
            assertThat(results).containsExactly(transportOffer1);
        }

        @Test
        void shouldReturnPageOfData() {
            postOffer(aTransportOffer().title("a").build());
            postOffer(aTransportOffer().title("b").build());
            postOffer(aTransportOffer().title("c").build());
            postOffer(aTransportOffer().title("d").build());
            postOffer(aTransportOffer().title("e").build());
            postOffer(aTransportOffer().title("f").build());

            PageRequest page = PageRequest.of(1, 2);
            var results = searchOffers(page);

            assertThat(results.totalElements).isEqualTo(6);
            assertThat(results.content)
                    .hasSize(2)
                    .extracting(r -> r.title)
                    .containsExactly("c", "d");
        }

        @Test
        void shouldSortResults() {
            postOffer(aTransportOffer().title("a").build());
            postOffer(aTransportOffer().title("bb").build());
            postOffer(aTransportOffer().title("bą").build());
            postOffer(aTransportOffer().title("c").build());
            postOffer(aTransportOffer().title("ć").build());
            postOffer(aTransportOffer().title("d").build());

            PageRequest page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "title"));
            var results = searchOffers(page);

            assertThat(results.content)
                    .extracting(r -> r.title)
                    .containsExactly("d", "ć", "c", "bb", "bą", "a");
        }
    }

    @Nested
    class ValidationTest {

        @Test
        void shouldRejectNullCapacity() {
            TransportOffer offer = sampleOfferRequest();
            offer.capacity = null;
            assertPostResponseStatus(offer, HttpStatus.BAD_REQUEST);
        }

        @ParameterizedTest
        @ValueSource(ints = {-10, -1, 0, 100, 101, 1000})
        void shouldRejectIncorrectCapacity(int capacity) {
            TransportOffer offer = sampleOfferRequest();
            offer.capacity = capacity;
            assertPostResponseStatus(offer, HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldRejectNullOrigin() {
            TransportOffer offer = sampleOfferRequest();
            offer.origin = null;
            assertPostResponseStatus(offer, HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldRejectNullDestination() {
            TransportOffer offer = sampleOfferRequest();
            offer.destination = null;
            assertPostResponseStatus(offer, HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class Updating {
        @Test
        void shouldUpdateOffer() {
            TransportOffer offer = postSampleOffer();
            var updateJson = TransportTestDataGenerator.sampleUpdateJson();

            ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            TransportOffer updatedOffer = getOfferFromRepository(offer.id);
            assertThat(updatedOffer.title).isEqualTo("new title");
            assertThat(updatedOffer.description).isEqualTo("new description");
            assertThat(updatedOffer.origin.region).isEqualTo("dolnośląskie");
            assertThat(updatedOffer.origin.city).isEqualTo("Wrocław");
            assertThat(updatedOffer.destination.region).isEqualTo("podlaskie");
            assertThat(updatedOffer.destination.city).isEqualTo("Białystok");
            assertThat(updatedOffer.capacity).isEqualTo(35);
            assertThat(updatedOffer.transportDate).isEqualTo(LocalDate.of(2022, 3, 8));
        }

        @Test
        public void shouldUpdateModifiedDate() {
            testTimeProvider.setCurrentTime(Instant.parse("2022-03-07T15:23:22Z"));
            TransportOffer offer = postSampleOffer();
            var updateJson = TransportTestDataGenerator.sampleUpdateJson();

            testTimeProvider.setCurrentTime(Instant.parse("2022-04-01T12:00:00Z"));
            ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            TransportOffer updatedOffer = getOfferFromRepository(offer.id);
            assertThat(updatedOffer.modifiedDate).isEqualTo(Instant.parse("2022-04-01T12:00:00Z"));
        }

        @Test
        void shouldReturn404WhenOfferDoesNotExist() {
            var updateJson = TransportTestDataGenerator.sampleUpdateJson();

            ResponseEntity<Void> response = updateOffer(123L, updateJson);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldReturn404WhenOfferDoesNotBelongToCurrentUser() {
            testUser.setCurrentUserWithId(new UserId("other-user-2"));
            TransportOffer offer = postSampleOffer();
            var updateJson = TransportTestDataGenerator.sampleUpdateJson();

            testUser.setCurrentUserWithId(new UserId("current-user-1"));
            ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Nested
        class Validation {
            @ParameterizedTest
            @NullAndEmptySource
            @ValueSource(strings = {"<", ">", "(", ")", "%", "#", "@", "\"", "'"})
            void shouldRejectMissingOrInvalidTitle(String title) {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.title = title;

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.title).isEqualTo(offer.title);
            }

            @Test
            void shouldRejectTooLongTitle() {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.title = "a".repeat(81);

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.title).isEqualTo(offer.title);
            }

            @ParameterizedTest
            @NullAndEmptySource
            @ValueSource(strings = {"<", ">", "(", ")", "%", "#", "@", "\"", "'"})
            void shouldRejectMissingOrInvalidDescription(String description) {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.description = description;

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.description).isEqualTo(offer.description);
            }

            @Test
            void shouldRejectTooLongDescription() {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.description = "a".repeat(81);

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.description).isEqualTo(offer.description);
            }

            @ParameterizedTest
            @NullSource
            @MethodSource("pl.gov.coi.pomocua.ads.transport.TransportResourceTest#invalidLocations")
            void shouldRejectMissingOrInvalidOrigin(Location location) {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.origin = location;

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.origin).isEqualTo(offer.origin);
            }

            @ParameterizedTest
            @NullSource
            @MethodSource("pl.gov.coi.pomocua.ads.transport.TransportResourceTest#invalidLocations")
            void shouldRejectMissingOrInvalidDestination(Location location) {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.destination = location;

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.destination).isEqualTo(offer.destination);
            }

            @ParameterizedTest
            @NullSource
            @ValueSource(ints = {-1, 0, 100, 101})
            void shouldRejectMissingOrInvalidCapacity(Integer capacity) {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.capacity = capacity;

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.capacity).isEqualTo(offer.capacity);
            }

            @Test
            void shouldRejectMissingTransportDate() {
                TransportOffer offer = postSampleOffer();
                var updateJson = TransportTestDataGenerator.sampleUpdateJson();
                updateJson.transportDate = null;

                ResponseEntity<Void> response = updateOffer(offer.id, updateJson);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                TransportOffer notUpdatedOffer = getOfferFromRepository(offer.id);
                assertThat(notUpdatedOffer.transportDate).isEqualTo(offer.transportDate);
            }
        }
    }

    static Stream<Location> invalidLocations() {
        return Stream.of(
                new Location(null, "city"),
                new Location("   ", "city"),
                new Location("region", null),
                new Location("region", "   ")
        );
    }

    private List<TransportOffer> searchOffers(TransportOfferSearchCriteria searchCriteria) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/" + getOfferSuffix());
        if (searchCriteria.getOrigin() != null) {
            builder
                    .queryParam("origin.city", searchCriteria.getOrigin().getCity())
                    .queryParam("origin.region", searchCriteria.getOrigin().getRegion());
        }
        if (searchCriteria.getDestination() != null) {
            builder
                    .queryParam("destination.city", searchCriteria.getDestination().getCity())
                    .queryParam("destination.region", searchCriteria.getDestination().getRegion());
        }
        if (searchCriteria.getCapacity() != null) {
            builder.queryParam("capacity", searchCriteria.getCapacity());
        }
        if (searchCriteria.getTransportDate() != null) {
            builder.queryParam("transportDate", searchCriteria.getTransportDate());
        }
        String url = builder.encode().toUriString();

        return listOffers(URI.create(url)).content;
    }

    private Offers<TransportOffer> searchOffers(PageRequest pageRequest) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/api/" + getOfferSuffix());
        builder.queryParam("page", pageRequest.getPageNumber());
        builder.queryParam("size", pageRequest.getPageSize());
        pageRequest.getSort().forEach(sort -> {
            builder.queryParam("sort", "%s,%s".formatted(sort.getProperty(), sort.getDirection()));
        });
        String url = builder.encode().toUriString();

        return listOffers(URI.create(url));
    }
}
