package io.lettuce.core.dynamic;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.core.dynamic.output.CodecAwareOutputFactoryResolver;
import io.lettuce.core.dynamic.output.OutputRegistry;
import io.lettuce.core.dynamic.output.OutputRegistryCommandOutputFactoryResolver;
import io.lettuce.core.dynamic.segment.AnnotationCommandSegmentFactory;
import io.lettuce.core.dynamic.segment.CommandSegments;
import io.lettuce.core.dynamic.support.ReflectionUtils;
import io.lettuce.core.output.StreamingOutput;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class ReactiveCommandSegmentCommandFactoryUnitTests {

    private CodecAwareOutputFactoryResolver outputFactoryResolver = new CodecAwareOutputFactoryResolver(
            new OutputRegistryCommandOutputFactoryResolver(new OutputRegistry()), StringCodec.UTF8);

    @Test
    void commandCreationWithTimeoutShouldFail() {

        try {
            createCommand("get", ReactiveWithTimeout.class, String.class, Timeout.class);
            fail("Missing CommandCreationException");
        } catch (CommandCreationException e) {
            assertThat(e).hasMessageContaining("Reactive command methods do not support Timeout parameters");
        }
    }

    @Test
    void shouldResolveNonStreamingOutput() {

        RedisCommand<?, ?, ?> command = createCommand("getOne", ReactiveWithTimeout.class, String.class);

        assertThat(command.getOutput()).isNotInstanceOf(StreamingOutput.class);
    }

    @Test
    void shouldResolveStreamingOutput() {

        RedisCommand<?, ?, ?> command = createCommand("getMany", ReactiveWithTimeout.class, String.class);

        assertThat(command.getOutput()).isInstanceOf(StreamingOutput.class);
    }

    RedisCommand<?, ?, ?> createCommand(String methodName, Class<?> interfaceClass, Class<?>... parameterTypes) {

        Method method = ReflectionUtils.findMethod(interfaceClass, methodName, parameterTypes);

        CommandMethod commandMethod = DeclaredCommandMethod.create(method);

        AnnotationCommandSegmentFactory segmentFactory = new AnnotationCommandSegmentFactory();
        CommandSegments commandSegments = segmentFactory.createCommandSegments(commandMethod);

        ReactiveCommandSegmentCommandFactory factory = new ReactiveCommandSegmentCommandFactory(commandSegments, commandMethod,
                new StringCodec(), outputFactoryResolver);

        return factory.createCommand(new Object[] { "foo" });
    }

    private static interface ReactiveWithTimeout {

        Publisher<String> get(String key, Timeout timeout);

        Mono<String> getOne(String key);

        Flux<String> getMany(String key);

    }

}
