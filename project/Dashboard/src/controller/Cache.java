package controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dao.DatabaseException;
import dao.TreeQuerySet;
import model.Area;
import model.Building;
import model.City;
import model.Floor;
import model.LockedSensor;
import model.Room;
import model.Sensor;

public class Cache{
	private static City root;
	private static Map<Integer,LockedSensor> sensorMap = new HashMap<Integer,LockedSensor>();
	private static Map<Integer,Room> roomMap; 
	
	private static ReentrantReadWriteLock sensorMapLock = new ReentrantReadWriteLock();
	public static ReentrantReadWriteLock treeLock = new ReentrantReadWriteLock();
	
	
	public static void init() {
		try {
			TreeQuerySet.getTree();
			System.out.println(sensorMap.size());
			System.out.println(roomMap.size());
		} catch (DatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//-----------------------------------------------------User methods---------------------------------------------------------------------//
	
	//return the areas to the user
	public static List<Area> getAreas() {
		List<Area> list;
		treeLock.readLock().lock();
			City city= Cache.getRoot();
			Map<Integer, Area> map = city.getSubs();
			 list= new ArrayList<Area>(map.values());
			 
		treeLock.readLock().unlock();
		return list;
	}
	
	//return the buildings to the user
	public static List<Building> getBuildings(int id_area) {
		List<Building> list;
		treeLock.readLock().lock();
			City city= Cache.getRoot();
			Map<Integer,Area> areas = city.getSubs();
			Map<Integer, Building> buildings = areas.get(id_area).getSubs();
			list = new ArrayList<Building>(buildings.values());
		treeLock.readLock().unlock();
		return list;
	}
	
	//return the floors buildings to the user
	public static List<Floor> getFloors(int id_area, int id_building) {
		List<Floor> list;
		treeLock.readLock().lock();
			City city= Cache.getRoot();
			Map<Integer,Area> areas = city.getSubs();
			Map<Integer, Building> buildings = areas.get(id_area).getSubs();
			Map<Integer, Floor> floors = buildings.get(id_building).getSubs();
			list = new ArrayList<Floor>(floors.values());
		treeLock.readLock().unlock();
		return list;
		
	}
	
	//return the rooms buildings to the user
	public static List<Room> getRooms(int id_area, int id_building, int id_floor) {
		List<Room> list;
		treeLock.readLock().lock();
			City city= Cache.getRoot();
			Map<Integer,Area> areas = city.getSubs();
			Map<Integer, Building> buildings = areas.get(id_area).getSubs();
			Map<Integer, Floor> floors = buildings.get(id_building).getSubs();
			Map<Integer, Room> rooms = floors.get(id_floor).getSubs();
		    list = new ArrayList<Room>(rooms.values());
		treeLock.readLock().unlock();
		return list;
		
	}
	
	//--------------------------------------------------------------------------------------------------------//
	
	//set room map
	public static void setRoomMap(Map<Integer,Room> rmap){
		treeLock.writeLock().lock();
		roomMap=rmap;
		treeLock.writeLock().lock();
	}
	
	//set room map
	public static void setSensorMap(Map<Integer,LockedSensor> smap){
		sensorMapLock.writeLock().lock();
		sensorMap=smap;
		sensorMapLock.writeLock().lock();
	}
	
	
	//return the sensors
	public static Map<Integer,LockedSensor> getSensors(int id_area, int id_building, int id_floor, int id_room){
		treeLock.readLock().lock();
			City city= Cache.getRoot();
			Map<Integer, Area> areas = city.getSubs();
			Map<Integer, Building> buildings = areas.get(id_area).getSubs();
			Map<Integer, Floor> floors = buildings.get(id_building).getSubs();
			Map<Integer, Room> rooms = floors.get(id_floor).getSubs();
			Map<Integer, LockedSensor> sensors = rooms.get(id_room).getSubs();
		treeLock.readLock().unlock();
		return sensors;
	}
		
	public static void setRoot(City r) {
		treeLock.writeLock().lock();
		root=r;
		treeLock.writeLock().unlock();
	}
	
	public static City getRoot() {
		treeLock.readLock().lock();
		City r=root;
		treeLock.readLock().unlock();
		return r;
	}
	
/*	public static void init() {
		System.out.println("Downloading sensors");
		try {
			List<Sensor> list=new ArrayList<Sensor>();
			// 15 requests of 10000 sensors
			for(int i=0; i<15; i++) {
				list.addAll(SensorQuerySet.getSensors(i,null));
				System.out.println("Package "+(i+1)+"/15");
			}
			// insert sensors in the map
			for(Sensor curr: list) {
				insertSensor(curr);
				System.out.println(curr.getId());	
			}
		} catch (DatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	public static void setSensor(Sensor s) throws InterruptedException {
		sensorMapLock.readLock().lock();
		LockedSensor ls = sensorMap.get(s.getId());
		ls.getLock().writeLock().lock();
		ls.getSensor().setValue(s.getValue());
		ls.getLock().writeLock().unlock();
		sensorMapLock.readLock().unlock();
	}
	
	public static void insertSensor(Sensor s) throws InterruptedException {
		sensorMapLock.writeLock().lock();
		sensorMap.put(s.getId(), new LockedSensor(s));
		sensorMapLock.writeLock().unlock();

	}

	public static Sensor getSensor(int id) throws InterruptedException {
		treeLock.readLock().lock(); // inefficient but avoids deadlocks
		sensorMapLock.readLock().lock();
		Sensor s=null;
		LockedSensor ls = sensorMap.get(id);
		ls.getLock().readLock().lock();
		
		//deep copy of sensor
		s=new Sensor(ls.getSensor().getId(), ls.getSensor().getStatus(), ls.getSensor().getType(), ls.getSensor().getTreshold(), ls.getSensor().getValue(), ls.getSensor().getIdRoom(), null);
		
		ls.getLock().readLock().unlock();
		sensorMapLock.readLock().unlock();
		treeLock.readLock().unlock();
		return s;
	}

	public static LockedSensor getLockedSensor(int id) throws InterruptedException {
		sensorMapLock.readLock().lock();
		LockedSensor ls = sensorMap.get(id);
		sensorMapLock.readLock().unlock();
		return ls;
	}
	
	public static void removeSensor(int id) {
		sensorMapLock.writeLock().lock();
		sensorMap.remove(id);
		sensorMapLock.writeLock().unlock();
	}
	
	/*
	public static void removeAlert(Sensor s) {
		Room r = s.getIdRoom();
		Floor f = r.getIdFloor();
		Building b = f.getIdBuilding();
		Area area = b.getArea();
		City city = area.getArea();
	}
	
	public static void addWarning(Warning w) {
		warnings.add(w);
	}

	public static List<Warning> getWarnings(){
		return warnings;
	*/

	public static void insertRoom(Floor floor, int newRoomID,int newRoomNumber) {
		treeLock.writeLock().lock();
		floor.getSubs().putIfAbsent(newRoomID, new Room(newRoomID,newRoomNumber,floor.getId(),floor));
		roomMap.putIfAbsent(newRoomID, floor.getRooms().get(newRoomID));
		treeLock.writeLock().unlock();
	}
	public static void insertFloor(Building building, int newFloorID, int floorNumber) {
		treeLock.writeLock().lock();
		building.getSubs().putIfAbsent(newFloorID, new Floor(newFloorID,floorNumber,building.getId(),building));
		treeLock.writeLock().unlock();
	}
	public static void insertBuilding(Area area, int newBuildingID,String street,int civicNumber) {
		treeLock.writeLock().lock();
		area.getSubs().putIfAbsent(newBuildingID, new Building(newBuildingID,street,civicNumber,area.getId(),area));
		treeLock.writeLock().unlock();
	}
	public static void insertArea(int newAreaID,String name) {
		treeLock.writeLock().lock();
		root.getSubs().putIfAbsent(newAreaID, new Area(newAreaID,name,root.getId(),root));
		treeLock.writeLock().unlock();
	}
	
	public static Map<Integer, LockedSensor> getSensorsByRoom(Room room) throws InterruptedException{
		return room.getSubs(); 
	}
	
	public static Map<Integer, LockedSensor> getSensorsByRoom(int roomID) throws InterruptedException{
		treeLock.readLock().lock();
		Map<Integer, LockedSensor> res = roomMap.get(roomID).getSubs();
		treeLock.readLock().unlock();
		return res;
	}
	
	public static int sensorsNumber () {
		return sensorMap.size();
	}
	
}
