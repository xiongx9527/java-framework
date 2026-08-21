//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.reactor;

import dev.langchain4j.service.TokenStream;
import dev.langchain4j.spi.services.TokenStreamAdapter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class TokenStreamToFluxAdapter implements TokenStreamAdapter {
    public boolean canAdaptTokenStreamTo(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType() == Flux.class) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                return typeArguments.length == 1 && typeArguments[0] == String.class;
            }
        }
        return false;
    }

    public Object adapt(TokenStream tokenStream) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        Objects.requireNonNull(sink);
        TokenStream var10000 = tokenStream.onPartialResponse(sink::tryEmitNext).onCompleteResponse((aiMessageResponse) -> sink.tryEmitComplete());
        Objects.requireNonNull(sink);
        var10000.onError(sink::tryEmitError).start();
        return sink.asFlux();
    }
}
