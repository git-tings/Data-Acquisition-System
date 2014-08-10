
public class FlightDataPacket {
	
	private double roll, pitch, alt, speed;
	private String status;
	
	public FlightDataPacket(double _roll, double _pitch, double _alt, double _speed, String _status) {
		roll = _roll;
		pitch = _pitch;
		alt = _alt;
		speed = _speed;
		status = _status;
	}
	
	public double getRoll() { return roll; }
	
	public double getPitch() { return pitch; }
	
	public double getAlt() { return alt; }
	
	public double getSpeed() { return speed; }
	
	public String getStatus() { return status; }
	
}
