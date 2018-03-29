package io.debezium.examples.hikr.rest.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.debezium.examples.hikr.model.Hike;
import io.debezium.examples.hikr.model.Person;
import io.debezium.examples.hikr.model.Trip;

public class ExternalTripWithHikes {

	private long id;
	private String name;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date startDate;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date endDate;
	private double price;
	private Person organizer;
	private Set<ExternalHikeSummary> availableHikes;

	public ExternalTripWithHikes() {
	}

	public ExternalTripWithHikes(Trip trip) {
		this.id = trip.id;
		this.name = trip.tripName;
		this.startDate = trip.startDate;
		this.endDate = trip.endDate;
		this.price = trip.price;
		this.organizer = trip.organizer;
		this.availableHikes = new HashSet<>( trip.availableHikes.size() );
		for ( Hike hike : trip.availableHikes ) {
			this.availableHikes.add( new ExternalHikeSummary( hike ) );
		}
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

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public Person getOrganizer() {
		return organizer;
	}

	public void setOrganizer(Person organizer) {
		this.organizer = organizer;
	}

	public Set<ExternalHikeSummary> getAvailableHikes() {
		return availableHikes;
	}

	public void setAvailableHikes(Set<ExternalHikeSummary> availableHikes) {
		this.availableHikes = availableHikes;
	}

	@Override
	public String toString() {
		return "TripDescription [id=" + id + ", name=" + name + "]";
	}

}
