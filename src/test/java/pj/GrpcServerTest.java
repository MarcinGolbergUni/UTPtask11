package pj;

import grpc.circles.CircleDTO;
import grpc.circles.CircleResponse;
import grpc.circles.CircleServiceGrpc;
import grpc.circles.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GrpcServerTest {

    private static Server server;
    private static ManagedChannel channel;
    private static CircleServiceGrpc.CircleServiceBlockingStub blockingStub;
    private static CircleServiceGrpc.CircleServiceStub asyncStub;

    private static final String SERVER_NAME = "test-server";

    @BeforeAll
    static void startServer() throws IOException {
        // Serwer in-process (nie wymaga portu sieciowego)
        server = InProcessServerBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .addService(new GrpcServer.CircleServiceImpl())
                .build()
                .start();

        // Kanał do serwera
        channel = InProcessChannelBuilder
                .forName(SERVER_NAME)
                .directExecutor()
                .build();

        blockingStub = CircleServiceGrpc.newBlockingStub(channel);
        asyncStub = CircleServiceGrpc.newStub(channel);
    }

    @AfterAll
    static void stopServer() {
        if (channel != null) {
            channel.shutdownNow();
        }
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    @DisplayName("GetCircles - zwraca odpowiedź (nie null)")
    void getCircles_returnsResponse() {
        // given
        Empty request = Empty.newBuilder().build();

        // when
        CircleResponse response = blockingStub.getCircles(request);

        // then
        assertNotNull(response);
        assertNotNull(response.getCirclesList());
    }

    @Test
    @DisplayName("GetCircles - zwraca listę kółek z bazy")
    void getCircles_returnsCirclesFromDatabase() {
        // given
        Empty request = Empty.newBuilder().build();

        // when
        CircleResponse response = blockingStub.getCircles(request);

        // then
        // Jeśli baza ma dane, lista nie będzie pusta
        // Jeśli baza jest pusta, test też przejdzie (sprawdzamy tylko strukturę)
        System.out.println("Liczba kółek z bazy: " + response.getCirclesCount());

        for (CircleDTO circle : response.getCirclesList()) {
            // Sprawdzamy czy wartości są w sensownym zakresie
            assertTrue(circle.getX() >= 0, "X powinno być >= 0");
            assertTrue(circle.getY() >= 0, "Y powinno być >= 0");
            assertTrue(circle.getR() >= 0 && circle.getR() <= 255, "R powinno być 0-255");
            assertTrue(circle.getG() >= 0 && circle.getG() <= 255, "G powinno być 0-255");
            assertTrue(circle.getB() >= 0 && circle.getB() <= 255, "B powinno być 0-255");
        }
    }

    @Test
    @DisplayName("GetCircles - asynchroniczne wywołanie")
    void getCircles_asyncCall() throws InterruptedException {
        // given
        Empty request = Empty.newBuilder().build();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<CircleResponse> responseRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // when
        asyncStub.getCircles(request, new StreamObserver<CircleResponse>() {
            @Override
            public void onNext(CircleResponse response) {
                responseRef.set(response);
            }

            @Override
            public void onError(Throwable t) {
                errorRef.set(t);
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        // then
        boolean completed = latch.await(5, TimeUnit.SECONDS);

        assertTrue(completed, "Wywołanie powinno zakończyć się w 5 sekund");
        assertNull(errorRef.get(), "Nie powinno być błędu");
        assertNotNull(responseRef.get(), "Odpowiedź nie powinna być null");
    }

    @Test
    @DisplayName("GetCircles - wielokrotne wywołania")
    void getCircles_multipleCallsReturnConsistentResults() {
        // given
        Empty request = Empty.newBuilder().build();

        // when
        CircleResponse response1 = blockingStub.getCircles(request);
        CircleResponse response2 = blockingStub.getCircles(request);

        // then
        assertEquals(
            response1.getCirclesCount(),
            response2.getCirclesCount(),
            "Wielokrotne wywołania powinny zwracać tę samą liczbę kółek"
        );
    }

    @Test
    @DisplayName("CircleDTO - poprawna struktura danych")
    void circleDTO_hasCorrectStructure() {
        // given
        CircleDTO circle = CircleDTO.newBuilder()
                .setX(100)
                .setY(200)
                .setR(255)
                .setG(128)
                .setB(0)
                .build();

        // then
        assertEquals(100, circle.getX());
        assertEquals(200, circle.getY());
        assertEquals(255, circle.getR());
        assertEquals(128, circle.getG());
        assertEquals(0, circle.getB());
    }
}