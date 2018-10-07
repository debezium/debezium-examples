package io.debezium.examples.graphql.wsclient;

public class Main {

	private static final String DEFAULT_URI = "ws://localhost:8079/graphql";
	private static final String DEFAULT_QUERY = "subscription { order: onNewOrder { id productId } }";

	public static void main(String[] args) {
		try {
			String serverUri = args.length > 0 ? args[0] : DEFAULT_URI;
			String subscriptionQuery = args.length > 1 ? args[1] : DEFAULT_QUERY;

			System.out.printf("Using GraphQL Endpoint at '%s'%n", serverUri);
			System.out.printf("Running GraphQL Query     '%s'%n", subscriptionQuery);

			GraphQLSubscriptionClient.runSubscription(serverUri, subscriptionQuery, msg -> {
				if (msg.hasErrors()) {
					System.out.printf("RECEIVED ERROR RESPONSE: %n%s%n", msg.getErrorsAsFormattedJSON());
				} else {
					System.out.printf("RECEIVED DATA: %n%s%n", msg.getDataAsFormattedJSON());
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
