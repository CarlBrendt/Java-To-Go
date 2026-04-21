package ru.mts.ip.workflow.engine.lang.plant;

import java.util.function.Consumer;
import ru.mts.ip.workflow.engine.lang.plant.WorkflowExpression.Activity;

public interface PlantInstruction {
	void visit(Consumer<PlantInstruction> consumer);
	void print(TextView style);

	Activity toActivity();
	String getId();
	
	default void setNext(PlantInstruction next) {
	}

	default void setParent(PlantInstruction parent) {
	}

	default PlantInstruction getNext() {
		return null;
	}
	
	default boolean contains(String id){
		return false;
	}

	default String getBreak(){
		return null;
	}
}