package ru.mts.ip.workflow.engine.repository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

@Component
public class TransactionHelper {

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public <T> T inNewTransaction(Supplier<T> sup) {
		return sup.get();
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void inNewTransaction(Runnable action) {
		action.run();
	}
	
}
