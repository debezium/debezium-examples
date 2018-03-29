package io.debezium.examples.hikr.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import io.debezium.examples.hikr.model.Person;
import io.debezium.examples.hikr.model.Trip;
import io.debezium.examples.hikr.repository.TripRepository;
import io.debezium.examples.hikr.rest.model.ExternalTrip;
import io.debezium.examples.hikr.rest.model.ExternalTripWithHikes;

@Path("/trips")
@Stateless
public class TripResource {

	@Inject
	private TripRepository tripRepository;

	public TripResource() {
	}

	@GET
	@Path("/")
	@Produces("application/json")
	public List<ExternalTrip> getAllTrips() {
		List<Trip> trips = tripRepository.getAllTrips();
		List<ExternalTrip> externalTrip = new ArrayList<>( trips.size() );

		for ( Trip trip : trips ) {
			externalTrip.add( new ExternalTrip( trip, true ) );
		}

		return externalTrip;
	}

	@GET
	@Path("/{id}")
	@Produces("application/json")
	public ExternalTripWithHikes getTripById(@PathParam("id") long tripId) {
		// full load
		return new ExternalTripWithHikes( tripRepository.getTripById( tripId ) );
	}

	@POST
	@Path("/")
	@Consumes("application/json")
	@Produces("application/json")
	public long createTrip(ExternalTrip externalTrip) {
		Trip trip = new Trip();
		externalTrip.populateTrip( trip );
		trip = tripRepository.createTrip( trip );
		return trip.id;
	}
}
