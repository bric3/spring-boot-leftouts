package com.github.bric3.spring;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.boot.autoconfigure.condition.ConditionMessage.forCondition;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.match;
import static org.springframework.boot.autoconfigure.condition.ConditionOutcome.noMatch;


@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class OnPropertiesCollectionCondition extends SpringBootCondition {
    private static final Pattern INDEX_PATTERN = Pattern.compile("\\[(\\w+)\\]");

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String name = (String) metadata.getAnnotationAttributes(ConditionalOnPropertiesCollection.class.getName())
                                       .get("name");
        String[] wantedSubProperties = (String[]) metadata.getAnnotationAttributes(ConditionalOnPropertiesCollection.class.getName())
                                                          .get("subProperties");

        RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(context.getEnvironment());
        Map<String, Object> actualSubProperties = resolver.getSubProperties(name);

        if (actualSubProperties.isEmpty()) {
            return noMatch(forCondition(ConditionalOnPropertiesCollection.class)
                               .didNotFind("property", "properties")
                               .items(Stream.of(wantedSubProperties)
                                            .map(p -> format("%s[].%s", name, p))
                                            .collect(toList())));
        }

        return actualSubProperties.keySet()
                                  .stream()
                                  .map(INDEX_PATTERN::matcher)
                                  .flatMap(matcher -> {
                                      if (!matcher.find()) {
                                          return Stream.of(wantedSubProperties)
                                                       .map(p -> format("%s[].%s", name, p));
                                      }
                                      String index = matcher.group(1);
                                      return Stream.of(wantedSubProperties)
                                                   .map(subProperty -> format("%s[%s].%s",
                                                                              name,
                                                                              index,
                                                                              subProperty))
                                                   .filter(fullProperty -> !resolver.containsProperty(fullProperty))
                                          ;
                                  })
                                  .collect(collectingAndThen(toSet(), missing ->
                                      missing.isEmpty() ?
                                      match(forCondition(ConditionalOnPropertiesCollection.class)
                                                .foundExactly(name)) :
                                      noMatch(forCondition(ConditionalOnPropertiesCollection.class)
                                                  .didNotFind("property", "properties")
                                                  .items(missing))));
    }
}
