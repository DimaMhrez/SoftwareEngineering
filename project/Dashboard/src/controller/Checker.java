package controller;
import model.Sensor;

public class Checker {
	public static void newSensorValue(int id, int value) throws InterruptedException{
		
		Sensor s = Cache.getSensor(id);
		if(s!=null) {
			if(value>s.getTreshold())  
				//Room room = Cache.getRoomByID(s.getIdRoom());
			s.setValue(value);
			Cache.setSensor(s);
		}
	}
	
	public static void addWarningToCache(Sensor s) {
		//to do
	}
}
