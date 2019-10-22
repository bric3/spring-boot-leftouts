package com.github.bric3.spring.condition;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.boot.autoconfigure.condition.ConditionMessage.forCondition;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.match;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.noMatch;

@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class OnPropertiesCollectionCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String name = (String) metadata.getAnnotationAttributes(ConditionalOnPropertiesCollection.class.getName())
                                       .get("name");
        String[] wantedSubProperties = (String[]) metadata.getAnnotationAttributes(ConditionalOnPropertiesCollection.class.getName())
                                                          .get("subProperties");

        @SuppressWarnings("unchecked")
        final Map<String, Map<String, String>> subProperties = (Map<String, Map<String, String>>)
                Binder.get(context.getEnvironment())
                      .bind(ConfigurationPropertyName.of(name),
                            Bindable.of(ResolvableType.forClassWithGenerics(
                                    Map.class,
                                    ResolvableType.forClass(String.class),
                                    ResolvableType.forClassWithGenerics(
                                            Map.class,
                                            String.class,
                                            String.class))),
                            new IgnoreErrorsBindHandler())
                      .orElse(Collections.emptyMap());

        if (subProperties.isEmpty()) {
            return noMatch(forCondition(ConditionalOnPropertiesCollection.class)
                                   .didNotFind("property", "properties")
                                   .items(Stream.of(wantedSubProperties)
                                                .map(p -> format("%s[].%s", name, p))
                                                .collect(toList())));
        }

        return subProperties.entrySet()
                            .stream()
                            .flatMap(subMap -> {
                                return Stream.of(wantedSubProperties)
                                             .filter(wantedSubProperty -> !subMap.getValue().containsKey(wantedSubProperty));
                            })
                            .collect(collectingAndThen(toSet(),
                                                       missing ->
                                                               missing.isEmpty() ?
                                                               match(forCondition(ConditionalOnPropertiesCollection.class)
                                                                             .foundExactly(name)) :
                                                               noMatch(forCondition(ConditionalOnPropertiesCollection.class)
                                                                               .didNotFind("property", "properties")
                                                                               .items(missing))));
    }
}
