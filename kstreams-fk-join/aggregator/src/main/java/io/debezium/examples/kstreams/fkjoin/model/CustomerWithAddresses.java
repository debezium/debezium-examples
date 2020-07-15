package io.debezium.examples.kstreams.fkjoin.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerWithAddresses {

	private static final Logger LOGGER = LoggerFactory.getLogger(CustomerWithAddresses.class);
	
	public Customer customer;
	public List<Address> addresses = new ArrayList<>();
	
	public void addAddress(AddressAndCustomer addressAndCustomer) {
		LOGGER.info("Adding: " + addressAndCustomer);

		customer = addressAndCustomer.customer;
		addresses.add(addressAndCustomer.address);
	}
	
	public void removeAddress(AddressAndCustomer addressAndCustomer) {
		LOGGER.info("Removing: " + addressAndCustomer);

		Iterator<Address> it = addresses.iterator();
		while (it.hasNext()) {
			Address a = it.next();
			if (a.id == addressAndCustomer.address.id) {
				it.remove();
				return;
			}
		}
	}

	@Override
	public String toString() {
		return "CustomerWithAddresses [customer=" + customer + ", addresses=" + addresses + "]";
	}
}
