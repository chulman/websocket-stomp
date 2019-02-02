package com.chulm.websocket.spring.server.reactive.handler;

import com.chulm.websocket.spring.server.domain.repository.MessageCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import rx.Observable;

import javax.annotation.PostConstruct;
import java.time.Duration;

@Component
@Slf4j
public class ReactiveWebSocketDefaultHandler implements WebSocketHandler {

    private Flux<Long> messageFlux;
    private UnicastProcessor<Object> messagePublisher;

    @Autowired
    private MessageCacheRepository messageCacheRepository;


    private final String id = "test";
    private final ObjectMapper mapper = new ObjectMapper();
    /**
     * Here we prepare a Flux that will emit a message every second
     */

    @PostConstruct
    private void init() {
        messageFlux = Flux.interval(Duration.ofSeconds(1));
    }


    @Bean
    public UnicastProcessor<Object> messagePublisher(){
        messagePublisher = UnicastProcessor.create();
        return messagePublisher;
    }


    /**
     * On each new client session, send the message flux to the client.
     * Spring subscribes to the flux and send every new flux event to the WebSocketSession object
     * @param session
     * @return Mono<Void>
     */
    @Override
    public Mono<Void> handle(WebSocketSession session) {

        Observable<Object> observable = messageCacheRepository.get(id);
        WebSocketMessageSubscriber subscriber =  new WebSocketMessageSubscriber(messagePublisher,messageCacheRepository);

        session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .subscribe(subscriber::onNext, subscriber::onError, subscriber::onComplete);

        observable.doOnNext(o -> {
            System.err.println("Observer:" + o);
        });

        return session.send(
                messageFlux
                        .map(l -> String.format("{ \"Message\": %s }", l)) //transform to json
                        .map(session::textMessage)); // map to Spring WebSocketMessage of type text
    }

}