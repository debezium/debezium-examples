package io.debezium.examples.hikr.rest.model;

import java.util.Date;

import io.debezium.examples.hikr.model.Person;
import io.debezium.examples.hikr.model.Trip;

public class ExternalTrip {

	private long id;
	private String name;
	private Date startDate;
	private Date endDate;
	private long price;
	private Person organizer;

	public ExternalTrip() {
	}

	public ExternalTrip(Trip trip) {
		this.id = trip.id;
		this.name = trip.tripName;
	}

	public ExternalTrip(Trip trip, boolean fullLoad) {
		this(trip);
		if ( fullLoad ) {
			this.startDate = trip.startDate;
			this.endDate = trip.endDate;
			this.price = trip.price;
			this.organizer = trip.organizer;
		}

	}

	public void populateTrip(Trip trip) {
		trip.tripName = name;
		trip.startDate = startDate;
		trip.endDate = endDate;
		trip.price = price;
		trip.organizer = organizer;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public long getPrice() {
		return price;
	}

	public void setPrice(long price) {
		this.price = price;
	}

	public Person getOrganizer() {
		return organizer;
	}

	public void setOrganizer(Person organizer) {
		this.organizer = organizer;
	}

	@Override
	public String toString() {
		return "TripDescription [id=" + id + ", name=" + name + "]";
	}

}
