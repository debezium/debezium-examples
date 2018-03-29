package io.debezium.examples.hikr.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class Section {

    @Column(name="start")
	public String from;

    @Column(name="destination")
	public String to;

	Section() {
	}

	public Section(String from, String to) {
		this.from = from;
		this.to = to;
	}

}
