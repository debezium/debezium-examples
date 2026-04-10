package io.debezium.demos.auditing.vegetables.transactioncontext;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.microprofile.jwt.JsonWebToken;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;

@Interceptor
@Priority(value = Interceptor.Priority.APPLICATION + 100)
@Audited(useCase = "")
public class TransactionInterceptor {

    @Inject
    JsonWebToken jwt;

    @Inject
    EntityManager entityManager;

    @Inject
    ContainerRequestContext requestContext;

    @AroundInvoke
    public Object manageTransaction(InvocationContext ctx) throws Exception {
        Long txtId = (Long) entityManager.createNativeQuery("SELECT txid_current()").getSingleResult();
        String useCase = ctx.getMethod().getAnnotation(Audited.class).useCase();

        TransactionContextData context = new TransactionContextData();

        context.transactionId = txtId;
        context.userName = jwt.<String>claim("sub").orElse("anonymous");
        context.clientDate = getRequestDate();
        context.useCase = useCase;

        entityManager.persist(context);

        return ctx.proceed();
    }

    private ZonedDateTime getRequestDate() {
        String requestDate = requestContext.getHeaderString(HttpHeaders.DATE);
        return requestDate != null ? ZonedDateTime.parse(requestDate, DateTimeFormatter.RFC_1123_DATE_TIME) : null;
    }
}
