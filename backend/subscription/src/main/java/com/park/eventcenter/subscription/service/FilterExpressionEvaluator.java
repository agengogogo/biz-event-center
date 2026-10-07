package com.park.eventcenter.subscription.service;

import com.park.eventcenter.common.error.BadRequestException;
import com.park.eventcenter.model.CanonicalEvent;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.Expression;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Evaluates subscription filter expressions with a conservative guard: only a restricted character
 * set and a curated set of built-in functions are allowed, reflection/IO type references are
 * rejected, and evaluation is bounded by a timeout.
 */
@Component
public class FilterExpressionEvaluator {

  private static final Duration TIMEOUT = Duration.ofSeconds(1);
  private static final Pattern ALLOWED =
      Pattern.compile("^[A-Za-z0-9_\\s.(),'\"\\[\\]{}<>=!&|+\\-*/%]+$");
  private static final List<String> FORBIDDEN =
      List.of(
          "t(",
          "new ",
          "getclass",
          ".class",
          "@",
          "#",
          "system",
          "runtime",
          "processbuilder",
          "java.");

  private final SpelExpressionParser parser = new SpelExpressionParser();
  private final ExecutorService executor =
      Executors.newCachedThreadPool(
          runnable -> {
            Thread thread = new Thread(runnable, "spel-evaluator");
            thread.setDaemon(true);
            return thread;
          });

  /** Validates that an expression is safe and syntactically correct. */
  public void validate(String expression) {
    if (expression == null || expression.isBlank()) {
      return;
    }
    if (!ALLOWED.matcher(expression).matches()) {
      throw new BadRequestException("Filter expression contains unsupported characters");
    }
    String lower = expression.toLowerCase(Locale.ROOT);
    for (String token : FORBIDDEN) {
      if (lower.contains(token)) {
        throw new BadRequestException("Filter expression uses a forbidden construct: " + token);
      }
    }
    try {
      parser.parseExpression(expression);
    } catch (ParseException ex) {
      throw new BadRequestException("Invalid filter expression: " + ex.getMessage());
    }
  }

  /**
   * Returns true when the event satisfies the expression; a blank expression matches everything.
   */
  public boolean matches(String expression, CanonicalEvent event) {
    if (expression == null || expression.isBlank()) {
      return true;
    }
    validate(expression);
    Expression parsed = parser.parseExpression(expression);
    StandardEvaluationContext context =
        new StandardEvaluationContext(new EventExpressionRoot(event));
    context.addPropertyAccessor(new MapAccessor());
    Future<Boolean> future = executor.submit(() -> parsed.getValue(context, Boolean.class));
    try {
      return Boolean.TRUE.equals(future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
    } catch (TimeoutException ex) {
      future.cancel(true);
      throw new BadRequestException("Filter expression timed out");
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new BadRequestException("Filter evaluation interrupted");
    } catch (ExecutionException ex) {
      Throwable cause = ex.getCause() == null ? ex : ex.getCause();
      throw new BadRequestException("Filter evaluation failed: " + cause.getMessage());
    }
  }
}
