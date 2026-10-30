package ma.adria.accountservice.web.graphql;

import graphql.GraphQLError;
import ma.adria.accountservice.service.AccountNotFoundException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class GraphQlErrorHandler {

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(AccountNotFoundException e) {
        return GraphQLError.newError().errorType(ErrorType.NOT_FOUND).message(e.getMessage()).build();
    }

    @GraphQlExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public GraphQLError handleBadRequest(RuntimeException e) {
        return GraphQLError.newError().errorType(ErrorType.BAD_REQUEST).message(e.getMessage()).build();
    }
}